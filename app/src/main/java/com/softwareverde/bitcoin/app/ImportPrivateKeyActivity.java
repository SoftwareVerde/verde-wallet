package com.softwareverde.bitcoin.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.widget.FrameLayout;

import com.softwareverde.android.util.FragmentUtil;
import com.softwareverde.bitcoin.app.android.activity.VerdeActivity;
import com.softwareverde.bitcoin.app.android.fragment.ImportPrivateKeyFragment;
import com.softwareverde.bitcoin.app.lib.BitcoinVerde;
import com.softwareverde.cryptography.secp256k1.key.PrivateKey;

public class ImportPrivateKeyActivity extends VerdeActivity {
    protected static final String TAG = ImportPrivateKeyActivity.class.getSimpleName();

    protected BitcoinVerde _bitcoinVerde;

    protected FrameLayout _frameLayout;
    protected FragmentManager _getFragmentManager() {
        return this.getSupportFragmentManager();
    }

    public ImportPrivateKeyActivity() { }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.import_private_key_activity_layout);
        _frameLayout = this.findViewById(R.id.frame_layout);

        final ImportPrivateKeyFragment fragment = new ImportPrivateKeyFragment();
        fragment.setImportPrivateKeyCallback(new ImportPrivateKeyFragment.ImportPrivateKeyCallback() {
            @Override
            public void onPrivateKeyImported(final PrivateKey privateKey) {
                if (_bitcoinVerde == null) { return; }

                _bitcoinVerde.addPrivateKey(privateKey);
                ImportPrivateKeyActivity.this.finish();
            }
        });
        FragmentUtil.pushFragment(_getFragmentManager(), fragment, R.id.frame_layout, null);
    }

    @Override
    public void onServiceConnected() {
        _bitcoinVerde = _bitcoinVerdeService.getBitcoinVerdeInstance();
    }

    @Override
    public void onServiceDisconnected() {
        _bitcoinVerde = null;
    }

    @Override
    public void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        for (final Fragment fragment : this.getSupportFragmentManager().getFragments()) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
