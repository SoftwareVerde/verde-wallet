package com.softwareverde.bitcoin.app.android.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import androidx.fragment.app.Fragment;

import com.softwareverde.bitcoin.app.VerdeWalletComponent;
import com.softwareverde.bitcoin.app.android.service.BitcoinVerdeService;

public abstract class VerdeFragment extends Fragment implements VerdeWalletComponent {
    private Context _applicationContext = null;
    private ServiceConnection _serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName className, final IBinder service) {
            _bitcoinVerdeService = (BitcoinVerdeService.Binder) service;
            VerdeFragment.this.onServiceConnected();
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            VerdeFragment.this.onServiceDisconnected();
            _bitcoinVerdeService = null;
        }
    };

    protected BitcoinVerdeService.Binder _bitcoinVerdeService;

    @Override
    public void onAttach(final Context context) {

        _applicationContext = context.getApplicationContext();
        _applicationContext.bindService(
            new Intent(context, BitcoinVerdeService.class),
            _serviceConnection,
            Context.BIND_AUTO_CREATE
        );

        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        _applicationContext.unbindService(_serviceConnection);
        _applicationContext = null;

        super.onDetach();
    }
}
