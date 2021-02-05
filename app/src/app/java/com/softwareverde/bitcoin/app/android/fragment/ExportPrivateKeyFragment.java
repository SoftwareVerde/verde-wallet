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
import com.softwareverde.bitcoin.app.R;
import com.softwareverde.bitcoin.android.lib.BitcoinVerde;
import com.softwareverde.cryptography.secp256k1.key.PrivateKey;
import com.softwareverde.cryptography.secp256k1.key.PublicKey;

public class ExportPrivateKeyFragment extends VerdeFragment {
    protected static final String TAG = DisplayReceiveAddressFragment.class.getSimpleName();

    protected View _view;
    protected BitcoinVerde _bitcoinVerde;
    protected PublicKey _publicKey;

    protected void _updateView() {
        if (_publicKey == null) { return; }

        final View progressSpinner = _view.findViewById(R.id.display_qr_code_progress_spinner);
        final ImageView imageView = _view.findViewById(R.id.qr_code_image_view);
        final TextView exportSeedPhraseTextView = _view.findViewById(R.id.export_seed_phrase_text_view);

        progressSpinner.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.GONE);
        exportSeedPhraseTextView.setVisibility(View.GONE);

        if (_bitcoinVerde == null) { return; }

        final Context context = this.getContext();
        if (context == null) { return; }

        final Point deviceSize = AndroidUtil.getDeviceSize(this.getContext());
        if (deviceSize == null) { return; }

        final PrivateKey privateKey = _bitcoinVerde.getPrivateKey(_publicKey);
        final String seedPhrase = _bitcoinVerde.getSeedPhrase(privateKey);

        final Bitmap bitmap = QrUtil.createQrCodeBitmap(privateKey.toString(), (int) (deviceSize.y * 0.50F));
        if (bitmap == null) { return; }

        imageView.setImageBitmap(bitmap);

        exportSeedPhraseTextView.setText(seedPhrase);

        progressSpinner.setVisibility(View.GONE);
        imageView.setVisibility(View.VISIBLE);
        exportSeedPhraseTextView.setVisibility(View.VISIBLE);
    }

    public ExportPrivateKeyFragment() { }

    public void setPublicKey(final PublicKey publicKey) {
        _publicKey = publicKey;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        _view = inflater.inflate(R.layout.export_private_key_layout, container, false);
        final TextView exportSeedPhraseTextView = _view.findViewById(R.id.export_seed_phrase_text_view);

        final View.OnClickListener copyOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                final Context context = ExportPrivateKeyFragment.this.getContext();
                if (context == null) { return; }

                final String seedPhrase = exportSeedPhraseTextView.getText().toString();
                AndroidUtil.copyToClipboard("Seed Phrase", seedPhrase, context);

                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show();
            }
        };


        exportSeedPhraseTextView.setOnClickListener(copyOnClickListener);

        final View qrCodeView = _view.findViewById(R.id.qr_code_display);
        qrCodeView.setOnClickListener(copyOnClickListener);

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
