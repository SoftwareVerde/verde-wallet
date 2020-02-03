package com.softwareverde.bitcoin.app.android.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;

import com.softwareverde.bitcoin.app.VerdeWalletComponent;
import com.softwareverde.bitcoin.app.android.service.BitcoinVerdeService;

public abstract class VerdeActivity extends AppCompatActivity implements VerdeWalletComponent {
    private Context _applicationContext = null;
    private ServiceConnection _serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName className, final IBinder service) {
            _bitcoinVerdeService = (BitcoinVerdeService.Binder) service;
            VerdeActivity.this.onServiceConnected();
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            VerdeActivity.this.onServiceDisconnected();
            _bitcoinVerdeService = null;
        }
    };

    protected BitcoinVerdeService.Binder _bitcoinVerdeService;

    @Override
    protected void onResume() {
        _applicationContext = this.getApplicationContext();
        _applicationContext.bindService(
            new Intent(_applicationContext, BitcoinVerdeService.class),
            _serviceConnection,
            Context.BIND_AUTO_CREATE
        );

        super.onResume();
    }

    @Override
    protected void onPause() {
        _applicationContext.unbindService(_serviceConnection);
        _applicationContext = null;

        super.onPause();
    }
}

