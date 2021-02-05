package com.softwareverde.bitcoin.app.android.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.softwareverde.android.util.AndroidUtil;
import com.softwareverde.android.util.QrUtil;
import com.softwareverde.bitcoin.address.Address;
import com.softwareverde.bitcoin.app.R;
import com.softwareverde.bitcoin.android.lib.BitcoinVerde;

public class DisplayReceiveAddressFragment extends VerdeFragment {
    protected static final String TAG = DisplayReceiveAddressFragment.class.getSimpleName();

    protected View _view;
    protected BitcoinVerde _bitcoinVerde;
    protected Boolean _displayLegacyAddress = false;
    protected Address _receivingAddress;

    protected void _updateView() {
        if (_receivingAddress == null) { return; }

        final View progressSpinner = _view.findViewById(R.id.display_qr_code_progress_spinner);
        final ImageView imageView = _view.findViewById(R.id.qr_code_image_view);
        final TextView receiveAddressTextView = _view.findViewById(R.id.receive_address_text_view);
        final View swapButton = _view.findViewById(R.id.receive_address_swap_format_button);

        progressSpinner.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.GONE);
        receiveAddressTextView.setVisibility(View.GONE);
        swapButton.setVisibility(View.GONE);

        if (_bitcoinVerde == null) { return; }

        final Context context = this.getContext();
        if (context == null) { return; }

        final Point deviceSize = AndroidUtil.getDeviceSize(this.getContext());
        if (deviceSize == null) { return; }

        final String qrCodeAddressString = (_displayLegacyAddress ? _receivingAddress.toBase58CheckEncoded() : _receivingAddress.toBase32CheckEncoded(true));
        final String addressString = (_displayLegacyAddress ? _receivingAddress.toBase58CheckEncoded() : _receivingAddress.toBase32CheckEncoded(false));

        final Bitmap bitmap = QrUtil.createQrCodeBitmap(qrCodeAddressString, (int) (deviceSize.x * 0.85F));
        if (bitmap == null) { return; }

        imageView.setImageBitmap(bitmap);

        receiveAddressTextView.setText(addressString);

        progressSpinner.setVisibility(View.GONE);
        imageView.setVisibility(View.VISIBLE);
        receiveAddressTextView.setVisibility(View.VISIBLE);
        swapButton.setVisibility(View.VISIBLE);
    }

    public DisplayReceiveAddressFragment() { }

    public void setReceivingAddress(final Address address) {
        _receivingAddress = address;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        _view = inflater.inflate(R.layout.display_receive_address_layout, container, false);

        final View.OnClickListener copyOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (_receivingAddress == null) { return; }

                final Context context = DisplayReceiveAddressFragment.this.getContext();
                if (context == null) { return; }

                final String addressString = (_displayLegacyAddress ? _receivingAddress.toBase58CheckEncoded() : _receivingAddress.toBase32CheckEncoded(true));
                AndroidUtil.copyToClipboard("Address", addressString, context);

                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show();
            }
        };

        final View addressTextView = _view.findViewById(R.id.receive_address_text_view);
        addressTextView.setOnClickListener(copyOnClickListener);

        final View qrCodeView = _view.findViewById(R.id.qr_code_display);
        qrCodeView.setOnClickListener(copyOnClickListener);

        final View swapAddressFormatButton = _view.findViewById(R.id.receive_address_swap_format_button);
        swapAddressFormatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                _displayLegacyAddress = (! _displayLegacyAddress);
                _updateView();
            }
        });

        _updateView();
        return _view;
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onServiceConnected() {
        _bitcoinVerde = _bitcoinVerdeService.getBitcoinVerdeInstance();

        _updateView();
    }

    @Override
    public void onServiceDisconnected() {
        _bitcoinVerde = null;
    }
}
