package com.softwareverde.bitcoin.app.android.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.softwareverde.bitcoin.app.R;
import com.softwareverde.bitcoin.app.android.BitcoinUtil;
import com.softwareverde.bitcoin.app.android.adapter.TransactionOutputsAdapter;
import com.softwareverde.bitcoin.android.lib.BitcoinVerde;
import com.softwareverde.bitcoin.slp.SlpTokenId;
import com.softwareverde.bitcoin.transaction.Transaction;
import com.softwareverde.bitcoin.transaction.output.identifier.TransactionOutputIdentifier;
import com.softwareverde.bitcoin.wallet.Wallet;
import com.softwareverde.bitcoin.wallet.slp.SlpToken;
import com.softwareverde.bitcoin.wallet.utxo.SpendableTransactionOutput;
import com.softwareverde.constable.list.List;
import com.softwareverde.constable.list.immutable.ImmutableListBuilder;
import com.softwareverde.constable.list.mutable.MutableList;
import com.softwareverde.util.StringUtil;
import com.softwareverde.util.type.time.SystemTime;

import java.util.HashMap;

public class DashboardFragment extends VerdeFragment {
    protected static final String TAG = DashboardFragment.class.getSimpleName();

    protected final SystemTime _systemTime = new SystemTime();

    protected View _view;
    protected BitcoinVerde _bitcoinVerde;
    protected TransactionOutputsAdapter _transactionOutputsAdapter;

    protected final Runnable _onWalletUpdated = new Runnable() {
        @Override
        public void run() {
            _updateTransactionOutputs();
        }
    };

    protected final Runnable _onSlpTokenChanged = new Runnable() {
        @Override
        public void run() {
            _updateTransactionOutputs();
        }
    };

    protected void _updateTransactionOutputs() {
        if (_bitcoinVerde == null) { return; }

        final Wallet wallet = _bitcoinVerde.getWallet();

        final Activity activity = DashboardFragment.this.getActivity();
        if (activity == null) { return; }

        final List<SpendableTransactionOutput> transactionOutputs;
        final HashMap<TransactionOutputIdentifier, Long> confirmationCounts = new HashMap<TransactionOutputIdentifier, Long>();
        final HashMap<TransactionOutputIdentifier, Long> tokenAmounts = new HashMap<TransactionOutputIdentifier, Long>();
        final MutableList<TransactionOutputIdentifier> slpTokenBatonOutputs = new MutableList<TransactionOutputIdentifier>();

        final SlpTokenId slpTokenId = _bitcoinVerde.getSlpTokenId();

        if (slpTokenId == null) {
            transactionOutputs = wallet.getNonSlpTokenTransactionOutputs();
            for (final SpendableTransactionOutput spendableTransactionOutput : transactionOutputs) {
                final TransactionOutputIdentifier transactionOutputIdentifier = spendableTransactionOutput.getIdentifier();
                final Long confirmationCount = _bitcoinVerde.getConfirmationCount(transactionOutputIdentifier);
                confirmationCounts.put(transactionOutputIdentifier, confirmationCount);
            }
        }
        else {
            final ImmutableListBuilder<SpendableTransactionOutput> transactionOutputsBuilder = new ImmutableListBuilder<SpendableTransactionOutput>();
            final List<SlpToken> slpTokens = wallet.getSlpTokens(slpTokenId);
            for (final SlpToken slpToken : slpTokens) {
                final TransactionOutputIdentifier transactionOutputIdentifier = slpToken.getIdentifier();

                final Long confirmationCount = _bitcoinVerde.getConfirmationCount(transactionOutputIdentifier);
                confirmationCounts.put(transactionOutputIdentifier, confirmationCount);

                if (slpToken.isBatonHolder()) {
                    slpTokenBatonOutputs.add(transactionOutputIdentifier);
                }
                else {
                    final Long tokenAmount = slpToken.getTokenAmount();
                    tokenAmounts.put(transactionOutputIdentifier, tokenAmount);
                }

                transactionOutputsBuilder.add(slpToken);
            }
            transactionOutputs = transactionOutputsBuilder.build();
        }

        final Long walletBalance = (slpTokenId == null ? wallet.getBalance() : wallet.getSlpTokenBalance(slpTokenId));

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _transactionOutputsAdapter.clear();

                // NOTE: TransactionOutputsAdapter::setTokenAmount does not update the UI, so this process is invoked before TransactionOutputsAdapter::add...
                for (final TransactionOutputIdentifier transactionOutputIdentifier : tokenAmounts.keySet()) {
                    final Long tokenAmount = tokenAmounts.get(transactionOutputIdentifier);
                    _transactionOutputsAdapter.setTokenAmount(transactionOutputIdentifier, tokenAmount);
                }

                for (final TransactionOutputIdentifier transactionOutputIdentifier : slpTokenBatonOutputs) {
                    _transactionOutputsAdapter.setIsTokenBatonHolder(transactionOutputIdentifier);
                }

                for (final SpendableTransactionOutput transactionOutput : transactionOutputs) {
                    final TransactionOutputIdentifier transactionOutputIdentifier = transactionOutput.getIdentifier();
                    final Long confirmationCount = confirmationCounts.get(transactionOutputIdentifier);

                    _transactionOutputsAdapter.add(transactionOutput);
                    _transactionOutputsAdapter.setConfirmationCount(transactionOutputIdentifier, confirmationCount);
                }

                if (_view != null) {
                    final TextView balanceTextView = _view.findViewById(R.id.dashboard_balance);
                    final String walletBalanceString = StringUtil.formatNumberString(walletBalance);

                    balanceTextView.setText(walletBalanceString);
                    balanceTextView.setTextSize((walletBalanceString.length() > 10) ? 20 : 24);

                    final TextView usdBalanceTextView = _view.findViewById(R.id.dashboard_balance_usd);
                    if (slpTokenId == null) {
                        final Double dollarAmount = ((walletBalance / Transaction.SATOSHIS_PER_BITCOIN.doubleValue()) * _bitcoinVerde.getDollarsPerBitcoin());
                        final String usdWalletBalanceString = BitcoinUtil.formatDollars(dollarAmount);

                        usdBalanceTextView.setText(usdWalletBalanceString);
                        usdBalanceTextView.setVisibility(View.VISIBLE);
                    }
                    else {
                        usdBalanceTextView.setText(slpTokenId.toString());
                        usdBalanceTextView.setVisibility(View.VISIBLE);
                    }
                }

                _transactionOutputsAdapter.notifyDataSetChanged();
            }
        });
    }

    public DashboardFragment() { }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.dashboard_fragment_layout, container, false);

        final RecyclerView outputsListView = view.findViewById(R.id.dashboard_transaction_outputs_list_view);
        outputsListView.setLayoutManager(new GridLayoutManager(this.getContext(), 2));
        outputsListView.setAdapter(_transactionOutputsAdapter);

        _transactionOutputsAdapter.setOnClickListener(new TransactionOutputsAdapter.OnClickListener() {
            @Override
            public void onClick(final TransactionOutputIdentifier transactionOutputIdentifier) {
                System.out.println("Clicked: " + transactionOutputIdentifier);
            }
        });

        _view = view;

        return view;
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);

        final Activity activity = this.getActivity();
        _transactionOutputsAdapter = new TransactionOutputsAdapter(activity);
    }

    @Override
    public void onResume() {
        super.onResume();
        _updateTransactionOutputs();
    }

    @Override
    public void onServiceConnected() {
        _bitcoinVerde = _bitcoinVerdeService.getBitcoinVerdeInstance();
        _bitcoinVerde.addWalletUpdatedCallback(_onWalletUpdated);
        _bitcoinVerde.addSlpTokenChangedCallback(_onSlpTokenChanged);

        _updateTransactionOutputs();
    }

    @Override
    public void onServiceDisconnected() {
        final BitcoinVerde bitcoinVerde = _bitcoinVerde;
        if (bitcoinVerde != null) {
            bitcoinVerde.removeWalletUpdatedCallback(_onWalletUpdated);
            bitcoinVerde.removeSlpTokenChangedCallback(_onSlpTokenChanged);
        }

        _bitcoinVerde = null;
    }

    @Override
    public void onDetach() {
        final BitcoinVerde bitcoinVerde = _bitcoinVerde;
        if (bitcoinVerde != null) {
            bitcoinVerde.removeWalletUpdatedCallback(_onWalletUpdated);
            bitcoinVerde.removeSlpTokenChangedCallback(_onSlpTokenChanged);
        }

        super.onDetach();
    }
}
