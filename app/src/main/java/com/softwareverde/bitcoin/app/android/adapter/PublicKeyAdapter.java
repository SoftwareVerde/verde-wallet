package com.softwareverde.bitcoin.app.android.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.softwareverde.bitcoin.address.Address;
import com.softwareverde.bitcoin.address.AddressInflater;
import com.softwareverde.bitcoin.app.R;
import com.softwareverde.bitcoin.app.StringUtil;
import com.softwareverde.constable.list.List;
import com.softwareverde.constable.list.mutable.MutableList;
import com.softwareverde.cryptography.secp256k1.key.PublicKey;
import com.softwareverde.util.Util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

public class PublicKeyAdapter extends RecyclerView.Adapter<PublicKeyAdapter.ViewHolder> {

    /**
     * The provided publicKey is always the decompressed public key of the provided key.
     */
    public interface OnClickListener {
        void onClick(PublicKey publicKey);
    }

    protected interface _OnClickListener {
        void onClick(int position);
    }

    protected Activity _activity;
    protected LayoutInflater _inflater;

    protected HashSet<PublicKey> _publicKeySet = new HashSet<PublicKey>();
    protected MutableList<PublicKey> _dataSet = new MutableList<PublicKey>();
    protected HashMap<PublicKey, Long> _balances = new HashMap<PublicKey, Long>();

    protected Address _changeAddress = null;
    protected OnClickListener _onClickListener;

    protected final _OnClickListener __onClickListener = new _OnClickListener() {
        @Override
        public void onClick(final int position) {
            final OnClickListener onClickListener = _onClickListener;
            if (onClickListener == null) { return; }

            final PublicKey publicKey = _dataSet.get(position);
            onClickListener.onClick(publicKey);
        }
    };

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public final View view;
        public final TextView compressedAddressTextView;
        public final TextView decompressedAddressTextView;
        public final TextView balanceTextView;
        public final View changeAddressIcon;

        public ViewHolder(final View view) {
            super(view);

            this.view = view;
            this.compressedAddressTextView = view.findViewById(R.id.public_key_compressed_address);
            this.decompressedAddressTextView = view.findViewById(R.id.public_key_decompressed_address);
            this.balanceTextView = view.findViewById(R.id.public_key_balance);
            this.changeAddressIcon = view.findViewById(R.id.public_key_change_address_icon);
        }
    }

    protected void _sortDataSet() {
        _dataSet.sort(new Comparator<PublicKey>() {
            @Override
            public int compare(final PublicKey publicKey0, final PublicKey publicKey1) {
                final Long publicKeyBalance0 = Util.coalesce(_balances.get(publicKey0));
                final Long publicKeyBalance1 = Util.coalesce(_balances.get(publicKey1));

                if (! Util.areEqual(publicKeyBalance0, publicKeyBalance1)) {
                    return (publicKeyBalance1.compareTo(publicKeyBalance0));
                }

                return (publicKey0.toString().compareTo(publicKey1.toString()));
            }
        });
    }

    public PublicKeyAdapter(final Activity activity) {
        _activity = activity;
        _inflater = LayoutInflater.from(_activity.getApplicationContext());
    }

    @Override
    public synchronized int getItemCount() {
        return _dataSet.getSize();
    }

    @Override
    public synchronized void onBindViewHolder(final ViewHolder viewHolder, final int position) {
        if (position >= _dataSet.getSize()) { return; }

        final PublicKey publicKey = _dataSet.get(position);
        final Long balance = _balances.get(publicKey);

        final AddressInflater addressInflater = new AddressInflater();
        final Address address = addressInflater.uncompressedFromPublicKey(publicKey);
        final Address compressedAddress = addressInflater.compressedFromPublicKey(publicKey);

        viewHolder.decompressedAddressTextView.setText(address.toBase58CheckEncoded());
        viewHolder.compressedAddressTextView.setText(compressedAddress.toBase58CheckEncoded());
        viewHolder.balanceTextView.setText(StringUtil.formatNumberString(Util.coalesce(balance)));

        final boolean isChangeAddress = (Util.areEqual(_changeAddress, address) || Util.areEqual(_changeAddress, compressedAddress));
        viewHolder.changeAddressIcon.setVisibility(isChangeAddress ? View.VISIBLE : View.GONE);
    }

    @Override
    public PublicKeyAdapter.ViewHolder onCreateViewHolder(final ViewGroup viewGroup, final int position) {
        final View view = _inflater.inflate(R.layout.public_key_item, viewGroup, false);
        final ViewHolder viewHolder = new ViewHolder(view);

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


    public synchronized PublicKey getItem(final int position) {
        if ( (position < 0) || (position >= _dataSet.getSize()) ) { return null; }

        return _dataSet.get(position);
    }

    public synchronized boolean isEmpty() {
        return (_dataSet.getSize() == 0);
    }

    public void setOnClickListener(final OnClickListener onClickListener) {
        _onClickListener = onClickListener;
    }

    public synchronized void add(final PublicKey publicKey) {
        final PublicKey decompressedPublicKey = publicKey.decompress();

        final boolean isNewPublicKey = _publicKeySet.add(decompressedPublicKey);
        if (! isNewPublicKey) { return; }

        _dataSet.add(decompressedPublicKey);

        _sortDataSet();

        this.notifyDataSetChanged();
    }

    public synchronized void addAll(final List<PublicKey> publicKeys) {
        for (final PublicKey publicKey : publicKeys) {
            final PublicKey decompressedPublicKey = publicKey.decompress();

            final boolean isNewPublicKey = _publicKeySet.add(decompressedPublicKey);
            if (! isNewPublicKey) { continue; }

            _dataSet.add(decompressedPublicKey);
        }

        _sortDataSet();

        this.notifyDataSetChanged();
    }

    public synchronized void setBalance(final PublicKey publicKey, final Long balance) {
        final PublicKey decompressedPublicKey = publicKey.decompress();
        final PublicKey compressedPublicKey = publicKey.compress();

        _balances.put(decompressedPublicKey, balance);
        _balances.put(compressedPublicKey, balance);
    }

    public void setChangeAddress(final Address changeAddress) {
        _changeAddress = changeAddress;
        this.notifyDataSetChanged();
    }

    public synchronized void clear() {
        _publicKeySet.clear();
        _dataSet.clear();
        _balances.clear();

        this.notifyDataSetChanged();
    }
}
