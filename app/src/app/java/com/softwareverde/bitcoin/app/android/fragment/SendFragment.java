package com.softwareverde.bitcoin.app.android.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.softwareverde.bitcoin.address.Address;
import com.softwareverde.bitcoin.address.AddressInflater;
import com.softwareverde.bitcoin.app.R;
import com.softwareverde.bitcoin.app.android.BitcoinUtil;
import com.softwareverde.bitcoin.app.android.adapter.SortAlgorithms;
import com.softwareverde.bitcoin.app.android.adapter.TransactionOutputsAdapter;
import com.softwareverde.bitcoin.app.android.dialog.EditValueDialog;
import com.softwareverde.bitcoin.android.lib.BitcoinVerde;
import com.softwareverde.bitcoin.slp.SlpTokenId;
import com.softwareverde.bitcoin.transaction.Transaction;
import com.softwareverde.bitcoin.transaction.output.TransactionOutput;
import com.softwareverde.bitcoin.transaction.output.identifier.TransactionOutputIdentifier;
import com.softwareverde.bitcoin.wallet.PaymentAmount;
import com.softwareverde.bitcoin.wallet.Wallet;
import com.softwareverde.bitcoin.wallet.slp.SlpPaymentAmount;
import com.softwareverde.bitcoin.wallet.slp.SlpToken;
import com.softwareverde.bitcoin.wallet.utxo.SpendableTransactionOutput;
import com.softwareverde.constable.list.List;
import com.softwareverde.constable.list.mutable.MutableList;
import com.softwareverde.logging.Logger;
import com.softwareverde.qrcodescanner.ScanQrCodeActivity;
import com.softwareverde.util.StringUtil;
import com.softwareverde.util.Util;
import com.softwareverde.util.type.time.SystemTime;

import java.util.HashMap;
import java.util.HashSet;

public class SendFragment extends VerdeFragment {
    protected static final String TAG = SendFragment.class.getSimpleName();
    protected static final int SCAN_QR_CODE_REQUEST_CODE = 1;

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
            _updateView();
        }
    };

    protected Boolean _displayAsLegacyAddress = true;
    protected Address _sendToAddress = null;

    protected Long _selectedTokenBalance = 0L;
    protected Long _desiredTokenSendBalance = 0L;
    protected Long _selectedBalance = 0L;
    protected Long _desiredSendBalance = _getMinSendBalance();
    protected Long _calculatedFees = 0L;

    protected void _updateTransactionOutputs() {
        if (_bitcoinVerde == null) { return; }

        final Wallet wallet = _bitcoinVerde.getWallet();

        final Activity activity = SendFragment.this.getActivity();
        if (activity == null) { return; }

        final MutableList<SpendableTransactionOutput> transactionOutputs = new MutableList<SpendableTransactionOutput>();
        final HashMap<TransactionOutputIdentifier, Long> confirmationCounts = new HashMap<TransactionOutputIdentifier, Long>();
        final HashMap<TransactionOutputIdentifier, Long> tokenAmounts = new HashMap<TransactionOutputIdentifier, Long>();

        final SlpTokenId slpTokenId = _bitcoinVerde.getSlpTokenId();

        transactionOutputs.addAll(wallet.getNonSlpTokenTransactionOutputs());
        for (final SpendableTransactionOutput spendableTransactionOutput : transactionOutputs) {
            final TransactionOutputIdentifier transactionOutputIdentifier = spendableTransactionOutput.getIdentifier();
            final Long confirmationCount = _bitcoinVerde.getConfirmationCount(transactionOutputIdentifier);
            confirmationCounts.put(transactionOutputIdentifier, confirmationCount);
        }

        if (slpTokenId != null) {
            final List<SlpToken> slpTokens = wallet.getSlpTokens(slpTokenId);
            for (final SlpToken slpToken : slpTokens) {
                final TransactionOutputIdentifier transactionOutputIdentifier = slpToken.getIdentifier();

                final Long confirmationCount = _bitcoinVerde.getConfirmationCount(transactionOutputIdentifier);
                confirmationCounts.put(transactionOutputIdentifier, confirmationCount);

                if (slpToken.isBatonHolder()) { continue; }

                final Long tokenAmount = slpToken.getTokenAmount();
                tokenAmounts.put(transactionOutputIdentifier, tokenAmount);
                transactionOutputs.add(slpToken);
            }
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // NOTE: TransactionOutputsAdapter::setTokenAmount does not update the UI, so this process is invoked before TransactionOutputsAdapter::add...
                for (final TransactionOutputIdentifier transactionOutputIdentifier : tokenAmounts.keySet()) {
                    final Long tokenAmount = tokenAmounts.get(transactionOutputIdentifier);
                    _transactionOutputsAdapter.setTokenAmount(transactionOutputIdentifier, tokenAmount);
                }

                final HashSet<TransactionOutputIdentifier> itemsToRemove;
                {
                    final List<SpendableTransactionOutput> existingItems = _transactionOutputsAdapter.getItems();
                    final HashSet<TransactionOutputIdentifier> existingIdentifiers = new HashSet<TransactionOutputIdentifier>(existingItems.getSize());
                    for (final SpendableTransactionOutput spendableTransactionOutput : existingItems) {
                        existingIdentifiers.add(spendableTransactionOutput.getIdentifier());
                    }
                    itemsToRemove = existingIdentifiers;
                }

                for (final SpendableTransactionOutput transactionOutput : transactionOutputs) {
                    final TransactionOutputIdentifier transactionOutputIdentifier = transactionOutput.getIdentifier();

                    if (transactionOutput.isSpent()) {
                        itemsToRemove.add(transactionOutputIdentifier);
                        continue;
                    }

                    final Long confirmationCount = confirmationCounts.get(transactionOutputIdentifier);

                    final boolean itemExisted = itemsToRemove.remove(transactionOutputIdentifier);
                    if (! itemExisted) {
                        _transactionOutputsAdapter.add(transactionOutput);
                    }
                    _transactionOutputsAdapter.setConfirmationCount(transactionOutputIdentifier, confirmationCount);
                }

                for (final TransactionOutputIdentifier itemToRemove : itemsToRemove) {
                    _transactionOutputsAdapter.removeItem(itemToRemove);
                }

                _transactionOutputsAdapter.notifyDataSetChanged();
            }
        });
    }

    protected void _recalculateSelectedBalance() {
        if (_bitcoinVerde == null) { return; }

        final SlpTokenId slpTokenId = _bitcoinVerde.getSlpTokenId();
        final Wallet wallet = _bitcoinVerde.getWallet();
        _selectedBalance = 0L;
        _selectedTokenBalance = 0L;
        for (final TransactionOutputIdentifier transactionOutputIdentifier : _transactionOutputsAdapter.getSelectedItems()) {
            final SpendableTransactionOutput spendableTransactionOutput = wallet.getTransactionOutput(transactionOutputIdentifier);
            if (spendableTransactionOutput.isSpent()) { continue; }

            final TransactionOutput transactionOutput = spendableTransactionOutput.getTransactionOutput();
            _selectedBalance += transactionOutput.getAmount();

            if (slpTokenId != null) {
                _selectedTokenBalance += wallet.getSlpTokenAmount(slpTokenId, transactionOutputIdentifier);
            }
        }
    }

    protected Long _getMinSendBalance() {
        if (_bitcoinVerde == null) {
            return Wallet.getDefaultDustThreshold();
        }

        final Wallet wallet = _bitcoinVerde.getWallet();
        return wallet.getDustThreshold(true);
    }

    protected void _recalculateFees() {
        if (_bitcoinVerde == null) { return; }

        final Wallet wallet = _bitcoinVerde.getWallet();
        final List<TransactionOutputIdentifier> selectedOutputsToSpend = _transactionOutputsAdapter.getSelectedItems();

        final int newOutputCount = 2;

        if (! selectedOutputsToSpend.isEmpty()) {
            _calculatedFees = wallet.calculateFees(newOutputCount, selectedOutputsToSpend.getSize());
        }
        else {
            final List<TransactionOutputIdentifier> suggestedOutputsToSpend = wallet.getOutputsToSpend(newOutputCount, _desiredSendBalance);
            final int outputsBeingSpentCount = ( (suggestedOutputsToSpend != null) ? suggestedOutputsToSpend.getSize() : 1);
            _calculatedFees = wallet.calculateFees(newOutputCount, outputsBeingSpentCount);
        }
    }

    protected void _updateView() {
        final BitcoinVerde bitcoinVerde = _bitcoinVerde;
        final SlpTokenId slpTokenId = ((bitcoinVerde != null) ? bitcoinVerde.getSlpTokenId() : null);

        final TextView sendToAddress = _view.findViewById(R.id.send_to_address);
        final TextView selectedBalanceView = _view.findViewById(R.id.selected_amount_total);
        final View desiredSendBalanceContainer = _view.findViewById(R.id.desired_send_amount_container);
        final TextView desiredSendBalanceView = _view.findViewById(R.id.desired_send_amount);
        final TextView totalFeesView = _view.findViewById(R.id.calculated_fee_amount);

        final TextView desiredTokenSendBalanceView = _view.findViewById(R.id.desired_send_token_amount);
        desiredTokenSendBalanceView.setVisibility(slpTokenId != null ? View.VISIBLE : View.GONE);
        desiredTokenSendBalanceView.setText(StringUtil.formatNumberString(_desiredTokenSendBalance));

        final TextView desiredUsdSendBalanceView = _view.findViewById(R.id.desired_send_usd_amount);
        desiredUsdSendBalanceView.setVisibility(slpTokenId != null ? View.GONE : View.VISIBLE);

        final double dollarsPerBitcoin = (bitcoinVerde != null ? bitcoinVerde.getDollarsPerBitcoin() : 0);
        desiredUsdSendBalanceView.setText(BitcoinUtil.formatDollars((_desiredSendBalance / Transaction.SATOSHIS_PER_BITCOIN.doubleValue()) * dollarsPerBitcoin));

        final TextView selectedTokenAmountTextView = _view.findViewById(R.id.selected_token_amount_total);
        selectedTokenAmountTextView.setVisibility(slpTokenId != null ? View.VISIBLE : View.GONE);
        selectedTokenAmountTextView.setText(StringUtil.formatNumberString(_selectedTokenBalance));

        selectedBalanceView.setText(StringUtil.formatNumberString(_selectedBalance));
        desiredSendBalanceView.setText(StringUtil.formatNumberString(_desiredSendBalance));
        totalFeesView.setText("+" + StringUtil.formatNumberString(_calculatedFees) + " Fee");

        if (_selectedBalance >= _desiredSendBalance) {
            selectedBalanceView.setTextColor(Color.parseColor("#FFFFFF"));
        }
        else {
            selectedBalanceView.setTextColor(Color.parseColor("#FF5555"));
        }

        final Address address = _sendToAddress;
        if (address != null) {
            sendToAddress.setText((_displayAsLegacyAddress ? address.toBase58CheckEncoded() : address.toBase32CheckEncoded(false)));
        }
        else {
            sendToAddress.setText("");
        }

        final Long minSendBalance = _getMinSendBalance();

        final View scanButton = _view.findViewById(R.id.scan_button);
        final TextView seekBarStatusText = _view.findViewById(R.id.send_confirm_status);
        final SeekBar seekBar = _view.findViewById(R.id.send_confirm_bar);
        if (_sendToAddress == null) {
            seekBarStatusText.setText("Scan an address to send coins to.");
            seekBarStatusText.setTextColor(Color.parseColor("#202020"));
            seekBar.setBackgroundColor(Color.parseColor("#E0E0E0"));
            seekBar.setProgress(0, false);

            scanButton.setBackgroundResource(R.drawable.red_border);
            desiredSendBalanceContainer.setBackground(null);
        }
        else if (_desiredSendBalance < minSendBalance) {
            if (_desiredSendBalance >= 0) {
                seekBarStatusText.setText("Transaction amount must be at least " + minSendBalance + " satoshis.");
            }
            else {
                seekBarStatusText.setText("Set the amount to send.");
            }

            seekBarStatusText.setTextColor(Color.parseColor("#202020"));
            seekBar.setBackgroundColor(Color.parseColor("#E0E0E0"));
            seekBar.setProgress(0, false);

            scanButton.setBackground(null);
            desiredSendBalanceContainer.setBackgroundResource(R.drawable.red_border);
            _transactionOutputsAdapter.clearHighlightedItems();
        }
        else if ( (_selectedBalance < (_desiredSendBalance + _calculatedFees)) || (_selectedTokenBalance < _desiredTokenSendBalance) ) {
            if ((_selectedTokenBalance < _desiredTokenSendBalance)) {
                seekBarStatusText.setText("Select tokens to spend.");
            }
            else {
                seekBarStatusText.setText("Select coins to spend.");
            }

            seekBarStatusText.setTextColor(Color.parseColor("#202020"));
            seekBar.setBackgroundColor(Color.parseColor("#E0E0E0"));
            seekBar.setProgress(0, false);

            scanButton.setBackground(null);
            desiredSendBalanceContainer.setBackground(null);

            _transactionOutputsAdapter.clearHighlightedItems();

            if (_bitcoinVerde != null) {
                final Wallet wallet = _bitcoinVerde.getWallet();
                final List<TransactionOutputIdentifier> transactionOutputs = ((slpTokenId == null) ? wallet.getOutputsToSpend(2, _desiredSendBalance, _transactionOutputsAdapter.getSelectedItems()) : wallet.getOutputsToSpend(3, _desiredSendBalance, slpTokenId, _desiredTokenSendBalance, _transactionOutputsAdapter.getSelectedItems()));
                if (transactionOutputs != null) {
                    for (final TransactionOutputIdentifier transactionOutputIdentifier : transactionOutputs) {
                        _transactionOutputsAdapter.highlightItem(transactionOutputIdentifier);
                    }
                }
            }
        }
        else {
            final Resources resources = this.getResources();
            seekBarStatusText.setText("Swipe right to send.");
            seekBarStatusText.setTextColor(Color.parseColor("#B6FFB6"));
            seekBar.setBackgroundColor(resources.getColor(R.color.colorPrimary, null));
            seekBar.setProgress(0, false);

            scanButton.setBackground(null);
            desiredSendBalanceContainer.setBackground(null);
            _transactionOutputsAdapter.clearHighlightedItems();
        }

        _transactionOutputsAdapter.notifyDataSetChanged();
    }

    protected void _broadcastTransaction() {
        if (_bitcoinVerde == null) { return; }

        final SlpTokenId slpTokenId = _bitcoinVerde.getSlpTokenId();
        final Wallet wallet = _bitcoinVerde.getWallet();

        final List<TransactionOutputIdentifier> transactionOutputsToSpend = _transactionOutputsAdapter.getSelectedItems();

        final Address changeAddress = _bitcoinVerde.getChangeAddress();

        final Transaction transaction;
        if (slpTokenId == null) {
            final MutableList<PaymentAmount> paymentAmounts = new MutableList<PaymentAmount>(1);
            paymentAmounts.add(new PaymentAmount(_sendToAddress, _desiredSendBalance));
            transaction = wallet.createTransaction(paymentAmounts, changeAddress, transactionOutputsToSpend);
        }
        else {
            final MutableList<SlpPaymentAmount> paymentAmounts = new MutableList<SlpPaymentAmount>(1);
            paymentAmounts.add(new SlpPaymentAmount(_sendToAddress, _desiredSendBalance, _desiredTokenSendBalance));
            transaction = wallet.createSlpTokenTransaction(slpTokenId, paymentAmounts, changeAddress, transactionOutputsToSpend);
        }

        if (transaction != null) {
            Logger.info("Broadcasting Tx: " + transaction.getHash());
            _bitcoinVerde.broadcastTransaction(transaction);
        }
        else {
            Logger.warn("Error creating Tx.");
        }

        _sendToAddress = null;
        _calculatedFees = 0L;
        _desiredSendBalance = _getMinSendBalance();
        _transactionOutputsAdapter.clearSelectedItems();
        _transactionOutputsAdapter.clearHighlightedItems();
        _updateView();
    }

    public SendFragment() { }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.send_fragment_layout, container, false);

        final RecyclerView outputsListView = view.findViewById(R.id.send_transaction_outputs_list_view);
        outputsListView.setLayoutManager(new GridLayoutManager(this.getContext(), 2));
        outputsListView.setAdapter(_transactionOutputsAdapter);

        _transactionOutputsAdapter.setOnClickListener(new TransactionOutputsAdapter.OnClickListener() {
            @Override
            public void onClick(final TransactionOutputIdentifier clickedTransactionOutputIdentifier) {
                _transactionOutputsAdapter.toggleItem(clickedTransactionOutputIdentifier);

                _recalculateSelectedBalance();
                _recalculateFees();

                _updateView();
            }
        });

        view.findViewById(R.id.desired_send_amount_container).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                final EditValueDialog editValueDialog = new EditValueDialog();
                editValueDialog.setActivity(SendFragment.this.getActivity());
                editValueDialog.setTitle("Set Amount");
                editValueDialog.setType(EditValueDialog.Type.NUMERIC);
                editValueDialog.setValue(_desiredSendBalance.toString());
                editValueDialog.setContent("Enter the amount of BCH to send.");
                editValueDialog.setCallback(new EditValueDialog.Callback() {
                    @Override
                    public void run(final String originalValueString, final String newValueString) {
                        final Long newValue = Util.parseLong(newValueString);

                        _desiredSendBalance = Math.max(newValue, _getMinSendBalance());
                        _recalculateFees();
                        _updateView();
                    }
                });
                editValueDialog.show(SendFragment.this.getFragmentManager(), TAG);
            }
        });

        view.findViewById(R.id.desired_send_token_amount).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                final EditValueDialog editValueDialog = new EditValueDialog();
                editValueDialog.setActivity(SendFragment.this.getActivity());
                editValueDialog.setTitle("Set Token Amount");
                editValueDialog.setType(EditValueDialog.Type.NUMERIC);
                editValueDialog.setValue(_desiredTokenSendBalance.toString());
                editValueDialog.setContent("Enter the amount of tokens to send.");
                editValueDialog.setCallback(new EditValueDialog.Callback() {
                    @Override
                    public void run(final String originalValueString, final String newValueString) {
                        final Long newValue = Util.parseLong(newValueString);
                        if (newValue < 0L) { return; }

                        _desiredTokenSendBalance = newValue;
                        _recalculateFees();
                        _updateView();
                    }
                });
                editValueDialog.show(SendFragment.this.getFragmentManager(), TAG);
            }
        });

        view.findViewById(R.id.send_to_address).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                final EditValueDialog editValueDialog = new EditValueDialog();
                editValueDialog.setActivity(SendFragment.this.getActivity());
                editValueDialog.setTitle("Change Address");
                editValueDialog.setType(EditValueDialog.Type.STRING);
                editValueDialog.setValue(_sendToAddress != null ? _sendToAddress.toBase58CheckEncoded() : "");
                editValueDialog.setContent("Change the send-to address.");
                editValueDialog.setCallback(new EditValueDialog.Callback() {
                    @Override
                    public void run(final String originalValueString, final String newValueString) {
                        final AddressInflater addressInflater = new AddressInflater();
                        final Address newAddress = addressInflater.fromBase58Check(newValueString, false);
                        if (newAddress == null) { return; }

                        _sendToAddress = newAddress;
                        _updateView();
                    }
                });
                editValueDialog.show(SendFragment.this.getFragmentManager(), TAG);
            }
        });

        view.findViewById(R.id.scan_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                final Activity activity = SendFragment.this.getActivity();
                activity.startActivityForResult(new Intent(activity, ScanQrCodeActivity.class), SCAN_QR_CODE_REQUEST_CODE);
            }
        });

        final SeekBar seekBar = view.findViewById(R.id.send_confirm_bar);
        final int minProgress = 5;
        seekBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent event) {
                if (_sendToAddress == null) { return true; }
                if (_selectedBalance < _desiredSendBalance) { return true; }
                if (_desiredSendBalance < 1L) { return true; }

                final Drawable thumb = seekBar.getThumb();
                final Rect thumbBounds = thumb.getBounds();

                final int buffer = (int) (thumbBounds.width() * 0.10F);
                final boolean touchIsOnThumb = (
                    event.getX() >= (thumbBounds.left - buffer)
                    && event.getX() <= (thumbBounds.right + buffer)
                    && event.getY() <= (thumbBounds.bottom + buffer)
                    && event.getY() >= (thumbBounds.top - buffer)
                );

                if (! touchIsOnThumb) {
                    seekBar.setProgress(minProgress, true);
                }

                return (! touchIsOnThumb);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) { }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
                final int progress = seekBar.getProgress();
                final boolean isSwipeComplete = (progress > 90);
                final int finalProgress = (isSwipeComplete ? 100 : minProgress);

                seekBar.setProgress(finalProgress, true);

                if (isSwipeComplete) {
                    seekBar.setProgress(minProgress, true);
                    _broadcastTransaction();
                }
            }
        });

        _view = view;

        _updateView();

        return view;
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);

        final Activity activity = this.getActivity();
        _transactionOutputsAdapter = new TransactionOutputsAdapter(activity);
        _transactionOutputsAdapter.setSortAlgorithm(SortAlgorithms.TOKEN_FIRST_AMOUNT_ASCENDING_UNSPENT_BEFORE_SPENT);
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
        _updateView();
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

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == SCAN_QR_CODE_REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK) { return; }

            final String addressString = data.getStringExtra(ScanQrCodeActivity.BARCODE_DATA);
            final String addressStringWithoutProtocol = addressString.replaceFirst("^[ \\t\\n\\r]*([a-zA-Z_\\-]+:)*", "");

            final AddressInflater addressInflater = new AddressInflater();

            final Boolean displayAsLegacyAddress;
            final Address address;
            {
                final Address legacyAddress = addressInflater.fromBase58Check(addressStringWithoutProtocol, false);
                if (legacyAddress != null) {
                    address = legacyAddress;
                    displayAsLegacyAddress = true;
                }
                else {
                    Logger.info("Scanned address: " + addressString);
                    final Address base32Address = addressInflater.fromBase32Check(addressString, true);
                    if (base32Address == null) { return; }
                    address = base32Address;
                    displayAsLegacyAddress = false;
                }
            }

            _sendToAddress = address;
            _displayAsLegacyAddress = displayAsLegacyAddress;
            _updateView();
        }
    }
}
