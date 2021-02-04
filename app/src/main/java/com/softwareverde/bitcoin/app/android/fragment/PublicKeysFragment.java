package com.softwareverde.bitcoin.app.android.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.softwareverde.android.util.FragmentUtil;
import com.softwareverde.bitcoin.address.Address;
import com.softwareverde.bitcoin.address.AddressInflater;
import com.softwareverde.bitcoin.app.R;
import com.softwareverde.bitcoin.app.android.adapter.PublicKeyAdapter;
import com.softwareverde.bitcoin.app.android.dialog.ButtonDialog;
import com.softwareverde.bitcoin.app.lib.BitcoinVerde;
import com.softwareverde.bitcoin.server.database.DatabaseConnection;
import com.softwareverde.bitcoin.slp.SlpTokenId;
import com.softwareverde.bitcoin.wallet.Wallet;
import com.softwareverde.constable.list.mutable.MutableList;
import com.softwareverde.cryptography.secp256k1.key.PrivateKey;
import com.softwareverde.cryptography.secp256k1.key.PublicKey;
import com.softwareverde.database.query.Query;
import com.softwareverde.database.row.Row;
import com.softwareverde.util.ReflectionUtil;

import java.util.List;

public class PublicKeysFragment extends VerdeFragment {
    protected static final String TAG = PublicKeysFragment.class.getSimpleName();

    protected View _view;
    protected BitcoinVerde _bitcoinVerde;

    protected PublicKeyAdapter _publicKeyAdapter;

    protected final Runnable _slpTokenChangedCallback = new Runnable() {
        @Override
        public void run() {
            _runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    _publicKeyAdapter.clear();
                    _updateView();
                }
            });
        }
    };

    protected final Runnable _walletUpdatedCallback = new Runnable() {
        @Override
        public void run() {
            _runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    _publicKeyAdapter.clear();
                    _updateView();
                }
            });
        }
    };

    protected void _runOnUiThread(final Runnable runnable) {
        final Activity activity = this.getActivity();
        if (activity == null) { return; }

        activity.runOnUiThread(runnable);
    }

    protected void _updateView() {
        if (_view == null) { return; }
        if (_bitcoinVerde == null) { return; }

        final Wallet wallet = _bitcoinVerde.getWallet();
        final SlpTokenId slpTokenId = _bitcoinVerde.getSlpTokenId();

        for (final PublicKey publicKey : wallet.getPublicKeys()) {
            _publicKeyAdapter.add(publicKey);

            final Long balance = wallet.getBalance(publicKey, slpTokenId);
            _publicKeyAdapter.setBalance(publicKey, balance);
        }
    }

    protected void _showPublicKeyInput() {
        final FragmentActivity activity = PublicKeysFragment.this.getActivity();
        if (activity == null) { return; }

        final ImportPrivateKeyFragment fragment = new ImportPrivateKeyFragment();
        fragment.setImportPrivateKeyCallback(new ImportPrivateKeyFragment.ImportPrivateKeyCallback() {
            @Override
            public void onPrivateKeyImported(final PrivateKey privateKey) {
                if (_bitcoinVerde == null) { return; }
                if (privateKey == null) { return; }

                _bitcoinVerde.addPrivateKey(privateKey);

                _updateView();
            }
        });

        final FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentUtil.pushFragment(fragmentManager, fragment, R.id.frame_layout, null);

        // activity.startActivity(new Intent(activity, ImportPrivateKeyActivity.class));
    }

    protected void _showAddress(final Address address) {
        final FragmentActivity activity = PublicKeysFragment.this.getActivity();
        if (activity == null) { return; }

        final DisplayReceiveAddressFragment fragment = new DisplayReceiveAddressFragment();
        fragment.setReceivingAddress(address);

        final FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentUtil.pushFragment(fragmentManager, fragment, R.id.frame_layout, null);
    }

    protected void _showKeyExport(final PublicKey publicKey) {
        final FragmentActivity activity = PublicKeysFragment.this.getActivity();
        if (activity == null) { return; }

        final ExportPrivateKeyFragment fragment = new ExportPrivateKeyFragment();
        fragment.setPublicKey(publicKey);

        final FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentUtil.pushFragment(fragmentManager, fragment, R.id.frame_layout, null);
    }

    protected void _expandMenu() {
        final View actionButton = _view.findViewById(R.id.public_keys_action_button);
        final View actionCancelButton = _view.findViewById(R.id.public_keys_action_cancel_button);
        final View createKeyButton = _view.findViewById(R.id.public_keys_create_key_button);
        final View importKeyButton = _view.findViewById(R.id.public_keys_import_key_button);

        final int mainButtonHeight = actionButton.getHeight();

        actionButton.setVisibility(View.GONE);
        actionCancelButton.setVisibility(View.VISIBLE);
        createKeyButton.setVisibility(View.VISIBLE);
        importKeyButton.setVisibility(View.VISIBLE);

        importKeyButton.animate().translationY(mainButtonHeight * -1.1F);
        createKeyButton.animate().translationY(mainButtonHeight * -2.1F);
    }

    protected void _closeMenu() {
        final View actionButton = _view.findViewById(R.id.public_keys_action_button);
        final View actionCancelButton = _view.findViewById(R.id.public_keys_action_cancel_button);
        final View createKeyButton = _view.findViewById(R.id.public_keys_create_key_button);
        final View importKeyButton = _view.findViewById(R.id.public_keys_import_key_button);

        actionButton.setVisibility(View.VISIBLE);
        actionCancelButton.setVisibility(View.GONE);
        createKeyButton.setVisibility(View.GONE);
        importKeyButton.setVisibility(View.GONE);

        importKeyButton.setTranslationY(0F);
        createKeyButton.setTranslationY(0F);
    }

    public PublicKeysFragment() { }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        _view = inflater.inflate(R.layout.show_public_keys_layout, container, false);

        final RecyclerView recyclerView = _view.findViewById(R.id.public_keys_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        recyclerView.setAdapter(_publicKeyAdapter);

        _publicKeyAdapter.setOnClickListener(new PublicKeyAdapter.OnClickListener() {
            @Override
            public void onClick(final PublicKey publicKey) {
                final AddressInflater addressInflater = new AddressInflater();
                final Address address = addressInflater.fromPublicKey(publicKey, true);

                final String SET_CHANGE_ADDRESS = "Set Change Address";
                final String EXPORT_ACTION = "Export";
                final String VIEW_ACTION = "View";

                final ButtonDialog<String> buttonDialog = new ButtonDialog<String>();
                buttonDialog.setActivity(PublicKeysFragment.this.getActivity());
                buttonDialog.setCallback(new ButtonDialog.Callback<String>() {
                    @Override
                    public void run(final String selectedItem) {
                        switch (selectedItem) {
                            case SET_CHANGE_ADDRESS: {
                                _bitcoinVerde.setChangeAddress(address);
                                _publicKeyAdapter.setChangeAddress(address);
                            } break;

                            case VIEW_ACTION: {
                                _showAddress(address);
                            } break;

                            case EXPORT_ACTION: {
                                _showKeyExport(publicKey);
                            } break;

                            default: {
                                System.out.println(selectedItem);
                            }
                        }
                    }
                });
                buttonDialog.setTitle("Public Key");
                buttonDialog.setContent(address.toBase58CheckEncoded());

                final MutableList<String> buttons = new MutableList<String>();
                buttons.add(SET_CHANGE_ADDRESS);
                buttons.add(EXPORT_ACTION);
                buttons.add(VIEW_ACTION);

                buttonDialog.setValues(buttons);

                buttonDialog.show(PublicKeysFragment.this.getFragmentManager(), TAG);
            }
        });

        _view.findViewById(R.id.public_keys_action_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                _expandMenu();
            }
        });

        _view.findViewById(R.id.public_keys_action_cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                _closeMenu();
            }
        });

        _view.findViewById(R.id.public_keys_create_key_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (_bitcoinVerde == null) { return; }

                final Address address = _bitcoinVerde.createReceivingAddress();
                _showAddress(address);
            }
        });

        _view.findViewById(R.id.public_keys_import_key_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (_bitcoinVerde == null) { return; }

                _showPublicKeyInput();
            }
        });

        _updateView();
        return _view;
    }

    @Override
    public void onAttach(final Context context) {
        _publicKeyAdapter = new PublicKeyAdapter(this.getActivity());

        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onServiceConnected() {
        _bitcoinVerde = _bitcoinVerdeService.getBitcoinVerdeInstance();
        _bitcoinVerde.addWalletUpdatedCallback(_walletUpdatedCallback);
        _bitcoinVerde.addSlpTokenChangedCallback(_slpTokenChangedCallback);

        final Address changeAddress = _bitcoinVerde.getChangeAddress();
        _publicKeyAdapter.setChangeAddress(changeAddress);

        _runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _updateView();
            }
        });

        final BitcoinVerde.InitData initData = ReflectionUtil.getValue(_bitcoinVerde, "_initData");
        try (final DatabaseConnection databaseConnection = initData.database.newConnection()) {
            final List<Row> rows = databaseConnection.query(
//                new Query("SELECT blocks.id, blocks.hash, block_merkle_trees.merkle_tree_data FROM blocks INNER JOIN block_merkle_trees ON blocks.id = block_merkle_trees.block_id WHERE blocks.hash = ?")
//                    .setParameter("000000000000000001A448CEB8E53E425EAA7EED291E22C1FE12F581B8C39286")
                new Query("SELECT id, hash FROM transactions WHERE hash = ?")
                    .setParameter("89DF9B15D2370998A660F501597489772B61FF935D5ADA1769511238019B8278")
            );
            // Log.d(PublicKeysFragment.TAG, MutableByteArray.wrap(rows.get(0).getBytes("merkle_tree_data")).toString());
            Log.d(PublicKeysFragment.TAG, "" + rows.size());
        }
        catch (final Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void onServiceDisconnected() {
        final BitcoinVerde bitcoinVerde = _bitcoinVerde;
        if (bitcoinVerde != null) {
            bitcoinVerde.removeWalletUpdatedCallback(_walletUpdatedCallback);
            bitcoinVerde.removeSlpTokenChangedCallback(_slpTokenChangedCallback);
        }

        _bitcoinVerde = null;
    }

    @Override
    public void onDetach() {
        final BitcoinVerde bitcoinVerde = _bitcoinVerde;
        if (bitcoinVerde != null) {
            bitcoinVerde.removeWalletUpdatedCallback(_walletUpdatedCallback);
            bitcoinVerde.removeSlpTokenChangedCallback(_slpTokenChangedCallback);
        }

        super.onDetach();
    }
}
