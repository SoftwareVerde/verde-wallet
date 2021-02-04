package com.softwareverde.bitcoin.app.android.adapter;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.softwareverde.android.util.AndroidUtil;
import com.softwareverde.bitcoin.address.Address;
import com.softwareverde.bitcoin.app.R;
import com.softwareverde.bitcoin.app.android.BitcoinUtil;
import com.softwareverde.bitcoin.transaction.output.TransactionOutput;
import com.softwareverde.bitcoin.transaction.output.identifier.TransactionOutputIdentifier;
import com.softwareverde.bitcoin.wallet.utxo.SpendableTransactionOutput;
import com.softwareverde.constable.list.List;
import com.softwareverde.constable.list.mutable.MutableList;
import com.softwareverde.cryptography.hash.sha256.Sha256Hash;
import com.softwareverde.util.StringUtil;
import com.softwareverde.util.Util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class TransactionOutputsAdapter extends RecyclerView.Adapter<TransactionOutputsAdapter.ViewHolder> {
    protected static int[] CONFIRMATION_COUNT_COLORS = new int[] {
        Color.parseColor("#CCCCCC"),
        Color.parseColor("#FF0000"),
        Color.parseColor("#FFAA00"),
        Color.parseColor("#EEEE00"),
        Color.parseColor("#AAFF00"),
        Color.parseColor("#00FF00")
    };

    public interface OnClickListener {
        void onClick(TransactionOutputIdentifier transactionOutputIdentifier);
    }

    protected interface _OnClickListener {
        void onClick(int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final View view;
        public final TextView addressView;
        public final View leftPaneView;
        public final TextView amountView;
        public final TextView tokenAmountView;
        public final TextView transactionHashView;
        public final TextView transactionIndexView;
        public final View[] confirmationViews;

        public ViewHolder(final View view) {
            super(view);

            this.view = view;
            this.addressView = view.findViewById(R.id.txo_address);
            this.leftPaneView = view.findViewById(R.id.txo_left_pane);
            this.amountView = view.findViewById(R.id.txo_amount);
            this.tokenAmountView = view.findViewById(R.id.txo_token_amount);
            this.transactionHashView = view.findViewById(R.id.txo_transaction_hash);
            this.transactionIndexView = view.findViewById(R.id.txo_transaction_index);

            this.confirmationViews = new View[] {
                view.findViewById(R.id.txo_confirmation_0),
                view.findViewById(R.id.txo_confirmation_1),
                view.findViewById(R.id.txo_confirmation_2),
                view.findViewById(R.id.txo_confirmation_3),
                view.findViewById(R.id.txo_confirmation_4),
                view.findViewById(R.id.txo_confirmation_5)
            };
        }
    }

    protected Activity _activity;
    protected LayoutInflater _inflater;
    protected MutableList<SpendableTransactionOutput> _dataSet = new MutableList<SpendableTransactionOutput>();
    protected HashSet<TransactionOutputIdentifier> _selectedTransactionOutputs = new HashSet<TransactionOutputIdentifier>();
    protected HashSet<TransactionOutputIdentifier> _highlightedTransactionOutputs = new HashSet<TransactionOutputIdentifier>();
    protected HashMap<TransactionOutputIdentifier, Long> _confirmationCounts = new HashMap<TransactionOutputIdentifier, Long>();
    protected HashMap<TransactionOutputIdentifier, Long> _tokenAmounts = new HashMap<TransactionOutputIdentifier, Long>();
    protected HashSet<TransactionOutputIdentifier> _tokenBatonHolders = new HashSet<TransactionOutputIdentifier>();
    protected OnClickListener _onClickListener;
    protected Comparator<SpendableTransactionOutput> _sortAlgorithm = SortAlgorithms.AMOUNT_ASCENDING_UNSPENT_BEFORE_SPENT;

    protected final _OnClickListener __onClickListener = new _OnClickListener() {
        @Override
        public void onClick(final int position) {
            final OnClickListener onClickListener = _onClickListener;
            if (onClickListener == null) { return; }

            final SpendableTransactionOutput spendableTransactionOutput = _dataSet.get(position);
            final TransactionOutputIdentifier transactionOutputIdentifier = spendableTransactionOutput.getIdentifier();
            onClickListener.onClick(transactionOutputIdentifier);
        }
    };

    protected void _sortDataSet() {
        final Comparator<SpendableTransactionOutput> sortAlgorithm = _sortAlgorithm;
        if (sortAlgorithm != null) {
            _dataSet.sort(sortAlgorithm);
        }
    }

    public TransactionOutputsAdapter(final Activity activity) {
        _activity = activity;
        _inflater = LayoutInflater.from(_activity.getApplicationContext());
    }

    @Override
    public TransactionOutputsAdapter.ViewHolder onCreateViewHolder(final ViewGroup viewGroup, final int position) {
        final View view = _inflater.inflate(R.layout.transaction_output_item, viewGroup, false);
        final ViewHolder viewHolder = new TransactionOutputsAdapter.ViewHolder(view);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                final int position = viewHolder.getAdapterPosition();
                if (position < 0) { return; }

                __onClickListener.onClick(position);
            }
        });

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final TransactionOutputsAdapter.ViewHolder viewHolder, final int position) {
        final SpendableTransactionOutput spendableTransactionOutput = _dataSet.get(position);

        final TransactionOutput transactionOutput = spendableTransactionOutput.getTransactionOutput();
        final TransactionOutputIdentifier transactionOutputIdentifier = spendableTransactionOutput.getIdentifier();

        final Resources resources = _activity.getResources();
        final int primaryColor = resources.getColor(R.color.colorPrimary, null);

        { // Set the textViews content...
            final Address address = spendableTransactionOutput.getAddress();
            viewHolder.addressView.setText((address != null) ? address.toBase58CheckEncoded() : "");
            viewHolder.amountView.setText(StringUtil.formatNumberString(transactionOutput.getAmount()));

            final Sha256Hash transactionHash = transactionOutputIdentifier.getTransactionHash();
            final String transactionHashString = BitcoinUtil.spaceHex(transactionHash.toString(), 8, 2);
            viewHolder.transactionHashView.setText(transactionHashString);

            viewHolder.transactionIndexView.setText(StringUtil.formatNumberString(transactionOutputIdentifier.getOutputIndex()));

            final Long tokenAmount = _tokenAmounts.get(transactionOutputIdentifier);
            if (Util.coalesce(tokenAmount) > 0) {
                viewHolder.tokenAmountView.setVisibility(View.VISIBLE);
                final String tokenAmountString = StringUtil.formatNumberString(tokenAmount);
                viewHolder.tokenAmountView.setText(tokenAmountString);
                if (Util.coalesce(tokenAmountString).length() > 10) {
                    viewHolder.tokenAmountView.setTextSize(8F);
                }
                else {
                    viewHolder.tokenAmountView.setTextSize(14F);
                }
            }
            else if (_tokenBatonHolders.contains(transactionOutputIdentifier)) {
                viewHolder.tokenAmountView.setVisibility(View.VISIBLE);
                viewHolder.tokenAmountView.setTextSize(14F);
                viewHolder.tokenAmountView.setText("BATON");
            }
            else {
                viewHolder.tokenAmountView.setVisibility(View.GONE);
            }
        }

        final boolean isSpent = (spendableTransactionOutput.isSpent());
        { // Render the item-spent effects...
            if (isSpent) {
                final int spentTextColor = Color.parseColor("#A0A0A0");
                viewHolder.addressView.setTextColor(spentTextColor);
                viewHolder.amountView.setTextColor(spentTextColor);
                viewHolder.transactionHashView.setTextColor(spentTextColor);
                viewHolder.transactionIndexView.setTextColor(spentTextColor);

                final int spentBackgroundColor = Color.parseColor("#505050");
                viewHolder.view.setBackgroundColor(spentBackgroundColor);

                final int unspentAmountBackgroundColor = Color.parseColor("#303530");
                final int spentAmountBackgroundColor = AndroidUtil.setAlpha(unspentAmountBackgroundColor, 0.25F);
                viewHolder.amountView.setBackgroundColor(spentAmountBackgroundColor);

                final int unspentTokenAmountBackgroundColor = primaryColor;
                final int spentTokenAmountBackgroundColor = AndroidUtil.setAlpha(unspentTokenAmountBackgroundColor, 0.25F);
                viewHolder.tokenAmountView.setBackgroundColor(spentTokenAmountBackgroundColor);
            }
            else {
                final int unspentTextColor = Color.parseColor("#FFFFFF");
                viewHolder.addressView.setTextColor(unspentTextColor);
                viewHolder.amountView.setTextColor(unspentTextColor);
                viewHolder.transactionHashView.setTextColor(unspentTextColor);
                viewHolder.transactionIndexView.setTextColor(unspentTextColor);

                final int unspentBackgroundColor = Color.parseColor("#505050");
                viewHolder.view.setBackgroundColor(unspentBackgroundColor);

                final int unspentAmountBackgroundColor = Color.parseColor("#303530");
                viewHolder.amountView.setBackgroundColor(unspentAmountBackgroundColor);

                final int unspentTokenAmountBackgroundColor = primaryColor;
                viewHolder.tokenAmountView.setBackgroundColor(unspentTokenAmountBackgroundColor);
            }
        }

        final boolean isHighlighted = (_highlightedTransactionOutputs.contains(transactionOutputIdentifier));
        final boolean isSelected = (_selectedTransactionOutputs.contains(transactionOutputIdentifier));
        { // Render the item-selected/highlighted effects...
            if (isSelected) {
                viewHolder.view.setBackgroundResource(R.drawable.selected_transaction_output_background);
            }
            else if (isHighlighted) {
                viewHolder.view.setBackgroundResource(R.drawable.highlighted_transaction_output_border);
            }
            else {
                viewHolder.view.setBackgroundResource(0);
                viewHolder.view.setBackgroundColor(Color.parseColor("#202020"));;
            }
        }

        { // Render the confirmation count bars...
            final Long confirmationCount = Util.coalesce(_confirmationCounts.get(transactionOutputIdentifier));
            final int colorIndex = (confirmationCount >= CONFIRMATION_COUNT_COLORS.length ? (CONFIRMATION_COUNT_COLORS.length - 1) : confirmationCount.intValue());
            final int color = (spendableTransactionOutput.isSpent() ? CONFIRMATION_COUNT_COLORS[0] : CONFIRMATION_COUNT_COLORS[colorIndex]);
            final int disabledColor = CONFIRMATION_COUNT_COLORS[0];

            for (int i = 0; i < viewHolder.confirmationViews.length; ++i) {
                final View confirmationView = viewHolder.confirmationViews[i];
                confirmationView.setBackgroundColor((i < confirmationCount) ? color : disabledColor);
            }
        }
    }

    @Override
    public int getItemCount() {
        return _dataSet.getSize();
    }

    public boolean isEmpty() {
        return (_dataSet.getSize() == 0);
    }

    public void setSortAlgorithm(final Comparator<SpendableTransactionOutput> sortAlgorithm) {
        _sortAlgorithm = sortAlgorithm;
    }

    public SpendableTransactionOutput getItem(final int position) {
        if ( (position < 0) || (position >= _dataSet.getSize()) ) { return null; }

        return _dataSet.get(position);
    }

    public List<SpendableTransactionOutput> getItems() {
        return _dataSet;
    }

    public void selectItem(final TransactionOutputIdentifier transactionOutputIdentifier) {
        _selectedTransactionOutputs.add(transactionOutputIdentifier);
    }

    public void deselectItem(final TransactionOutputIdentifier transactionOutputIdentifier) {
        _selectedTransactionOutputs.remove(transactionOutputIdentifier);
    }

    public void toggleItem(final TransactionOutputIdentifier transactionOutputIdentifier) {
        if (_selectedTransactionOutputs.contains(transactionOutputIdentifier)) {
            _selectedTransactionOutputs.remove(transactionOutputIdentifier);
        }
        else {
            _selectedTransactionOutputs.add(transactionOutputIdentifier);
        }
    }

    public void clearSelectedItems() {
        _selectedTransactionOutputs.clear();
    }

    public Boolean isItemSelected(final TransactionOutputIdentifier transactionOutputIdentifier) {
        return _selectedTransactionOutputs.contains(transactionOutputIdentifier);
    }

    public List<TransactionOutputIdentifier> getSelectedItems() {
        final MutableList<TransactionOutputIdentifier> selectedTransactionOutputIdentifiers = new MutableList<TransactionOutputIdentifier>(_selectedTransactionOutputs.size());
        for (final TransactionOutputIdentifier transactionOutputIdentifier : _selectedTransactionOutputs) {
            selectedTransactionOutputIdentifiers.add(transactionOutputIdentifier);
        }
        return selectedTransactionOutputIdentifiers;
    }

    public void setOnClickListener(final OnClickListener onClickListener) {
        _onClickListener = onClickListener;
    }

    public void add(final SpendableTransactionOutput spendableTransactionOutput) {
        _dataSet.add(spendableTransactionOutput);

        _sortDataSet();
    }

    public void addAll(final List<SpendableTransactionOutput> spendableTransactionOutputs) {
        _dataSet.addAll(spendableTransactionOutputs);

        _sortDataSet();
    }

    public void removeItem(final TransactionOutputIdentifier transactionOutputIdentifier) {
        final Iterator<SpendableTransactionOutput> dataSetIterator = _dataSet.mutableIterator();
        while (dataSetIterator.hasNext()) {
            final SpendableTransactionOutput spendableTransactionOutput = dataSetIterator.next();
            if (Util.areEqual(spendableTransactionOutput.getIdentifier(), transactionOutputIdentifier)) {
                dataSetIterator.remove();
                break;
            }
        }

        _selectedTransactionOutputs.remove(transactionOutputIdentifier);
        _highlightedTransactionOutputs.remove(transactionOutputIdentifier);
        _confirmationCounts.remove(transactionOutputIdentifier);
        _tokenAmounts.remove(transactionOutputIdentifier);
    }

    public void setConfirmationCount(final TransactionOutputIdentifier transactionOutputIdentifier, final Long confirmationCount) {
        _confirmationCounts.put(transactionOutputIdentifier, confirmationCount);
    }

    public void setTokenAmount(final TransactionOutputIdentifier transactionOutputIdentifier, final Long confirmationCount) {
        _tokenAmounts.put(transactionOutputIdentifier, confirmationCount);
    }

    public void setIsTokenBatonHolder(final TransactionOutputIdentifier transactionOutputIdentifier) {
        _tokenBatonHolders.add(transactionOutputIdentifier);
    }

    public List<TransactionOutputIdentifier> getHighlightedItems() {
        final MutableList<TransactionOutputIdentifier> highlightedTransactionOutputIdentifiers = new MutableList<TransactionOutputIdentifier>(_highlightedTransactionOutputs.size());
        for (final TransactionOutputIdentifier transactionOutputIdentifier : _highlightedTransactionOutputs) {
            highlightedTransactionOutputIdentifiers.add(transactionOutputIdentifier);
        }
        return highlightedTransactionOutputIdentifiers;
    }

    public void highlightItem(final TransactionOutputIdentifier transactionOutputIdentifier) {
        _highlightedTransactionOutputs.add(transactionOutputIdentifier);
    }

    public void clearHighlightedItems() {
        _highlightedTransactionOutputs.clear();
    }

    public void clear() {
        _dataSet.clear();
        _selectedTransactionOutputs.clear();
        _highlightedTransactionOutputs.clear();
        _confirmationCounts.clear();
        _tokenAmounts.clear();
    }
}
