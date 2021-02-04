package com.softwareverde.bitcoin.app.android.fragment;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.softwareverde.android.swipe.SwipeController;
import com.softwareverde.bitcoin.app.R;
import com.softwareverde.bitcoin.app.android.adapter.NodeListAdapter;
import com.softwareverde.bitcoin.app.lib.BitcoinVerde;
import com.softwareverde.bitcoin.app.lib.MerkleBlockSyncUpdateCallback;
import com.softwareverde.bitcoin.app.lib.NewBlockHeaderCallback;
import com.softwareverde.bitcoin.app.lib.Node;
import com.softwareverde.bitcoin.block.header.BlockHeader;
import com.softwareverde.bitcoin.chain.time.MedianBlockTime;
import com.softwareverde.bitcoin.server.module.spv.SpvModule;
import com.softwareverde.network.ip.Ip;
import com.softwareverde.util.StringUtil;
import com.softwareverde.util.type.time.SystemTime;

public class SynchronizationFragment extends VerdeFragment {
    protected static final String TAG = SynchronizationFragment.class.getSimpleName();

    protected final SystemTime _systemTime = new SystemTime();
    protected final NewBlockHeaderCallback _newBlockHeaderCallback;
    protected final MerkleBlockSyncUpdateCallback _newMerkleBlockSyncUpdateCallback;

    protected BitcoinVerde _bitcoinVerde;
    protected NodeListAdapter _nodeListAdapter;

    public SynchronizationFragment() {
        _newBlockHeaderCallback = new NewBlockHeaderCallback() {
            @Override
            public void newBlockHeight(final Long blockHeight) {
                final Float percentComplete;

                if (! _bitcoinVerde.isInit()) {
                    // Display the bootstrapping progress of blocks...
                    percentComplete = (blockHeight.floatValue() / _bitcoinVerde.getBootstrapHeight());
                }
                else {
                    final BlockHeader blockHeader = _bitcoinVerde.getBlockHeader(blockHeight);
                    if (blockHeader == null) { return; }

                    final Long blockTimestamp = blockHeader.getTimestamp();
                    final Long currentTimestamp = _systemTime.getCurrentTimeInSeconds();

                    percentComplete = (blockTimestamp - MedianBlockTime.GENESIS_BLOCK_TIMESTAMP) / ((float) (currentTimestamp - MedianBlockTime.GENESIS_BLOCK_TIMESTAMP));
                }

                final Activity context = SynchronizationFragment.this.getActivity();
                if (context != null) {
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            _setProgressPercent(percentComplete);
                        }
                    });
                }
            }
        };

        _newMerkleBlockSyncUpdateCallback = new MerkleBlockSyncUpdateCallback() {
            @Override
            public void onMerkleBlockHeightUpdated(final Long currentBlockHeight, final Boolean isSynchronizing) {
                final Activity context = SynchronizationFragment.this.getActivity();
                if (context != null) {
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            _setMerkleProgress(currentBlockHeight, _bitcoinVerde.getBlockHeight(), isSynchronizing);
                        }
                    });
                }
            }
        };
    }

    protected void _setViewLayoutWeight(final Float weight, final View view) {
        final LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) view.getLayoutParams();
        layoutParams.weight = weight;
        view.setLayoutParams(layoutParams);
    }

    protected void _setProgressPercent(final Float percent) {
        final View view = this.getView();
        if (view == null) { return; }

        final TextView progressPercentView = view.findViewById(R.id.sync_progress_label_percent);
        progressPercentView.setText("(" + StringUtil.formatPercent((percent * 100F), true) + ")");

        final View progressView = view.findViewById(R.id.sync_progress_bar);
        final View progressVoidView = view.findViewById(R.id.sync_progress_bar_void);
        if ((progressView == null) || (progressVoidView == null)) { return; }

        _setViewLayoutWeight(percent, progressView);
        _setViewLayoutWeight((1.0F - percent), progressVoidView);
    }

    protected void _setMerkleProgress(final Long currentHeight, final Long maxHeight, final Boolean isSyncing) {
        final View view = this.getView();
        if (view == null) { return; }

        final TextView merkleProgressTextView = view.findViewById(R.id.sync_merkle_block_height);
        merkleProgressTextView.setText(currentHeight + " / " + maxHeight);
        merkleProgressTextView.setTextColor(Color.parseColor(isSyncing ? "#20AA20" : "#202020"));
        merkleProgressTextView.setVisibility(View.VISIBLE);
    }

    protected void _updateSyncStatus() {
        if (_bitcoinVerde == null) { return; }

        final BitcoinVerde.Status status = _bitcoinVerde.getStatus();
        final SpvModule.Status spvStatus = _bitcoinVerde.getSpvStatus();
        final Activity activity = this.getActivity();
        if (activity == null) { return; }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final View view = SynchronizationFragment.this.getView();
                final TextView statusTextView = view.findViewById(R.id.sync_status_text);
                statusTextView.setText("Wallet: " + status.getValue() + " - Network: " + spvStatus.getValue());
            }
        });

        if (status == BitcoinVerde.Status.ONLINE) {
            final Long blockHeight = _bitcoinVerde.getBlockHeight();
            _newBlockHeaderCallback.newBlockHeight(blockHeight);
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(final Context context) {
        final Activity activity = this.getActivity();
        _nodeListAdapter = new NodeListAdapter(activity);

        super.onAttach(context);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.synchronization_fragment_layout, container, false);

        final RecyclerView nodeListView = view.findViewById(R.id.sync_node_list);
        nodeListView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        nodeListView.setAdapter(_nodeListAdapter);

        final SwipeController swipeController = new SwipeController();
        swipeController.setBackgroundColor(Color.parseColor("#404040"));

        { // Right Button (Disconnect)
            final SwipeController.Button rightButton = new SwipeController.Button();
            rightButton.text = "BAN";
            rightButton.textColor = Color.parseColor("#F01010");
            rightButton.textSize = 45F;
            rightButton.backgroundColor = Color.parseColor("#FEFEFE");
            rightButton.onButtonClickedCallback = new SwipeController.OnButtonClickedCallback() {
                @Override
                public void onClick(final int itemPosition) {
                    final Node node = _nodeListAdapter.getItem(itemPosition);
                    if (node == null) { return; }

                    // NOTE: Banning the node must happen on a non-UiThread, since getting the nodeIp may result in a hostname lookup of the socket.
                    //  The hostname lookup may happen if the node is in the process of disconnecting...
                    (new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final BitcoinVerde bitcoinVerde = _bitcoinVerde;
                            if (bitcoinVerde != null) {
                                final String nodeIpString = node.getIp();
                                if (nodeIpString == null) { return; }

                                final Ip nodeIp = Ip.fromString(nodeIpString);
                                if (nodeIp == null) { return; }

                                bitcoinVerde.banNode(nodeIp);
                            }
                        }
                    })).start();
                }
            };
            swipeController.setRightButton(rightButton);
        }

        { // Left Button (Disconnect)
            final SwipeController.Button button = new SwipeController.Button();
            button.text = "DISCONNECT";
            button.textColor = Color.parseColor("#202020");
            button.textSize = 30F;
            button.backgroundColor = Color.parseColor("#FEFEFE");
            button.onButtonClickedCallback = new SwipeController.OnButtonClickedCallback() {
                @Override
                public void onClick(final int itemPosition) {
                    final Node node = _nodeListAdapter.getItem(itemPosition);
                    if (node == null) { return; }

                    _bitcoinVerde.disconnectNode(node.getId());
                }
            };
            swipeController.setLeftButton(button);
        }

        final ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeController);
        itemTouchhelper.attachToRecyclerView(nodeListView);

        nodeListView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void onDraw(final Canvas canvas, final RecyclerView recyclerView, final RecyclerView.State state) {
                swipeController.onDraw(canvas, recyclerView, state);
            }
        });

        final TextView merkleProgressTextView = view.findViewById(R.id.sync_merkle_block_height);
        merkleProgressTextView.setVisibility(View.INVISIBLE);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        _updateSyncStatus();
    }

    @Override
    public void onServiceConnected() {
        _bitcoinVerde = _bitcoinVerdeService.getBitcoinVerdeInstance();
        _bitcoinVerde.setOnStatusUpdatedCallback(new Runnable() {
            @Override
            public void run() {
                _updateSyncStatus();
            }
        });

        _bitcoinVerde.addNewBlockHeaderCallback(_newBlockHeaderCallback);
        _bitcoinVerde.addNewMerkleBlockSyncUpdateCallback(_newMerkleBlockSyncUpdateCallback);

        if (_bitcoinVerde.isInit()) {
            _nodeListAdapter.addAll(_bitcoinVerde.getConnectedNodes());
        }

        _bitcoinVerde.setOnConnectedNodesChanged(new Runnable() {
            @Override
            public void run() {
                final Activity activity = SynchronizationFragment.this.getActivity();
                if (activity == null) { return; }

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        _nodeListAdapter.clear();
                        _nodeListAdapter.addAll(_bitcoinVerde.getConnectedNodes());
                    }
                });
            }
        });

        _updateSyncStatus();
    }

    @Override
    public void onServiceDisconnected() {
        final BitcoinVerde bitcoinVerde = _bitcoinVerde;
        if (bitcoinVerde != null) {
            bitcoinVerde.removeNewBlockHeaderCallback(_newBlockHeaderCallback);
            bitcoinVerde.removeMerkleBlockSyncUpdateCallback(_newMerkleBlockSyncUpdateCallback);
            bitcoinVerde.setOnConnectedNodesChanged(null);
        }

        _bitcoinVerde = null;
    }

    @Override
    public void onDetach() {
        final BitcoinVerde bitcoinVerde = _bitcoinVerde;
        if (bitcoinVerde != null) {
            bitcoinVerde.removeNewBlockHeaderCallback(_newBlockHeaderCallback);
            bitcoinVerde.removeMerkleBlockSyncUpdateCallback(_newMerkleBlockSyncUpdateCallback);
            bitcoinVerde.setOnConnectedNodesChanged(null);
        }

        super.onDetach();
    }
}
