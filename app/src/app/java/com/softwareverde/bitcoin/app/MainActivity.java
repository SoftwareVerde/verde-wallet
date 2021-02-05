package com.softwareverde.bitcoin.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.softwareverde.android.util.AndroidUtil;
import com.softwareverde.android.util.FragmentUtil;
import com.softwareverde.bitcoin.app.android.activity.VerdeActivity;
import com.softwareverde.bitcoin.app.android.dialog.SelectValueDialog;
import com.softwareverde.bitcoin.app.android.fragment.DashboardFragment;
import com.softwareverde.bitcoin.app.android.fragment.PublicKeysFragment;
import com.softwareverde.bitcoin.app.android.fragment.SendFragment;
import com.softwareverde.bitcoin.app.android.fragment.SynchronizationFragment;
import com.softwareverde.bitcoin.app.android.service.BitcoinVerdeService;
import com.softwareverde.bitcoin.android.lib.BitcoinVerde;
import com.softwareverde.bitcoin.server.module.spv.SpvModule;
import com.softwareverde.bitcoin.slp.SlpTokenId;
import com.softwareverde.bitcoin.wallet.Wallet;
import com.softwareverde.concurrent.service.SleepyService;
import com.softwareverde.constable.list.mutable.MutableList;
import com.softwareverde.util.Util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends VerdeActivity {
    protected static final String TAG = MainActivity.class.getSimpleName();

    public static final String EXIT_ACTIVITY_ACTION = "EXIT_ACTIVITY";

    protected static AtomicInteger INSTANCE_COUNT = new AtomicInteger(0);
    public static void closeApplication(final Context context) {
        if (INSTANCE_COUNT.get() == 0) { return; }

        final Intent stopMainActivityIntent = new Intent(context, MainActivity.class);
        stopMainActivityIntent.setAction(MainActivity.EXIT_ACTIVITY_ACTION);
        stopMainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(stopMainActivityIntent);
    }

    protected final AtomicBoolean _activityIsActive = new AtomicBoolean(false);

    protected final SleepyService _connectivityService;

    protected BitcoinVerde _bitcoinVerde;
    protected final AtomicBoolean _isShowingLoadingScreen = new AtomicBoolean(true);

    protected FrameLayout _frameLayout;
    protected FragmentManager _getFragmentManager() {
        return this.getSupportFragmentManager();
    }

    protected Long _walletBalance = 0L;
    protected final Runnable _onWalletUpdatedRunnable = new Runnable() {
        @Override
        public void run() {
            final BitcoinVerde bitcoinVerde = _bitcoinVerdeService.getBitcoinVerdeInstance();
            if (bitcoinVerde == null) { return; }
            if (! bitcoinVerde.isInit()) { return; }

            final Wallet wallet = bitcoinVerde.getWallet();
            final Long newWalletBalance = wallet.getBalance();

            final long difference = (newWalletBalance - _walletBalance);
            _walletBalance = newWalletBalance;

            if (difference != 0L) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        _renderFloatingText(difference);
                    }
                });
            }
        }
    };

    protected TextView _loadingStatusTextView = null;

    protected void _updateSyncStatus() {
        if (_bitcoinVerde == null) { return; }

        final BitcoinVerde.Status status = _bitcoinVerde.getStatus();
        final SpvModule.Status spvStatus = _bitcoinVerde.getSpvStatus();

        final Activity mainActivity = this;
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final RelativeLayout container = mainActivity.findViewById(R.id.container);

                final LayoutInflater layoutInflater = mainActivity.getLayoutInflater();
                final TextView textView = (TextView) layoutInflater.inflate(R.layout.loading_status_template, container, false);

                textView.setText("Wallet: " + status.getValue() + "\n" + "Network: " + spvStatus.getValue());
                container.addView(textView);

                if (_loadingStatusTextView == null) {
                    _loadingStatusTextView = mainActivity.findViewById(R.id.loading_status_text_view);
                }

                _loadingStatusTextView.post(new Runnable() {
                    @Override
                    public void run() {
                        _animateView(container, _loadingStatusTextView, (int) _loadingStatusTextView.getX(), (int) _loadingStatusTextView.getY(), -(_loadingStatusTextView.getY()));
                        _loadingStatusTextView = textView;
                    }
                });
            }
        });
    }

    protected void _animateView(final RelativeLayout container, final View textView, final int itemX, final int itemY, final float deltaY) {
        final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.leftMargin = itemX;
        layoutParams.topMargin = itemY;
        textView.setLayoutParams(layoutParams);

        final AnimationSet animationSet = new AnimationSet(true);

        final Animation translateAnimation = new TranslateAnimation(0, 0, 0, deltaY);
        translateAnimation.setDuration(1500);
        animationSet.addAnimation(translateAnimation);

        final Animation fadeAnimation = new AlphaAnimation(1.0F, 0.0F);
        fadeAnimation.setDuration(1500);
        animationSet.addAnimation(fadeAnimation);

        animationSet.setAnimationListener(new TranslateAnimation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }

            @Override
            public void onAnimationRepeat(Animation animation) { }

            @Override
            public void onAnimationEnd(Animation animation) {
                container.post(new Runnable() {
                    @Override
                    public void run() {
                        container.removeView(textView);
                    }
                });
            }
        });

        textView.startAnimation(animationSet);
    }

    protected void _renderFloatingText(final Long amount) {
        final BottomNavigationView bottomNavigationView = this.findViewById(R.id.navigation);
        if (bottomNavigationView == null) { return; } // Still loading...

        final View menuItemView = this.findViewById((amount >= 0) ? R.id.navigation_receive : R.id.navigation_send);

        final String displayText = ( (amount > 0 ? "+" : "") + StringUtil.formatNumberString(amount) );

        final TextView textView = new TextView(this);
        textView.setText(displayText);
        textView.setTextSize(14F);
        textView.setTextColor((amount >= 0) ? Color.parseColor("#00AA00") : Color.parseColor("#AA0000"));

        final int textViewWidth;
        final int textViewHeight;
        {
            final Point deviceSize = AndroidUtil.getDeviceSize(this);
            final int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(deviceSize.x, View.MeasureSpec.AT_MOST);
            final int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            textView.measure(widthMeasureSpec, heightMeasureSpec);
            textViewHeight = textView.getMeasuredHeight();
            textViewWidth = textView.getMeasuredWidth();
        }

        final int itemX = (int) ((bottomNavigationView.getX() + menuItemView.getX()) + (menuItemView.getWidth() / 2) - (textViewWidth / 2));
        final int itemY = (int) (bottomNavigationView.getY() - textViewHeight);

        final RelativeLayout container = this.findViewById(R.id.container);
        container.addView(textView);
        _animateView(container, textView, itemX, itemY, (itemY / -10F));
    }

    protected final BottomNavigationView.OnNavigationItemSelectedListener _onNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull final MenuItem item) {
            final Fragment fragment;

            switch (item.getItemId()) {
                case R.id.navigation_dashboard: {
                    fragment = new DashboardFragment();
                } break;

                case R.id.navigation_receive: {
                    fragment = new PublicKeysFragment();
                } break;

                case R.id.navigation_sync_status: {
                    fragment = new SynchronizationFragment();
                } break;

                case R.id.navigation_send: {
                    fragment = new SendFragment();
                } break;

                default: { return false; }
            }

            FragmentUtil.replaceCurrentFragment(_getFragmentManager(), fragment, R.id.frame_layout, null);

            return true;
        }
    };

    protected void _displayTokenSelector() {
        final SelectValueDialog<String> selectValueDialog = new SelectValueDialog<String>();
        selectValueDialog.setActivity(this);
        selectValueDialog.setTitle("Select Token");

        final BitcoinVerde bitcoinVerde = BitcoinVerde.getInstance();
        if (bitcoinVerde == null) {
            Log.w(TAG, "BitcoinVerde object is null");
            return;
        }
        final Wallet wallet = bitcoinVerde.getWallet();

        final String bitcoinCash = "Bitcoin (BCH)";
        final MutableList<String> tokens = new MutableList<String>();
        tokens.add(bitcoinCash);
        for (final SlpTokenId slpTokenId : wallet.getSlpTokenIds()) {
            tokens.add(slpTokenId.toString());
        }

        final SlpTokenId slpTokenId = bitcoinVerde.getSlpTokenId();
        selectValueDialog.setValue((slpTokenId != null ? slpTokenId.toString() : bitcoinCash));
        selectValueDialog.setValues(tokens);

        selectValueDialog.setCallback(new SelectValueDialog.Callback<String>() {
            @Override
            public void run(final String originalValue, final String newValue) {
                if (Util.areEqual(bitcoinCash, newValue)) {
                    bitcoinVerde.setSlpTokenId(null);
                }
                else {
                    bitcoinVerde.setSlpTokenId(SlpTokenId.fromHexString(newValue));
                }
            }
        });

        selectValueDialog.show(_getFragmentManager(), TAG);
    }

    public MainActivity() {
        _connectivityService = new SleepyService() {
            protected Boolean _wasConnected = false;

            @Override
            protected void _onStart() { }

            @Override
            protected Boolean _run() {
                final Context context = MainActivity.this.getApplicationContext();
                final ConnectivityManager connectivityManager = (ConnectivityManager) (context.getSystemService(Context.CONNECTIVITY_SERVICE));
                if (connectivityManager == null) { return false; }

                final NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                boolean isConnected = ( (activeNetwork != null) && (activeNetwork.isConnectedOrConnecting()) );

                if (_wasConnected != isConnected) {
                    _wasConnected = isConnected;

                    final BitcoinVerde bitcoinVerde = _bitcoinVerdeService.getBitcoinVerdeInstance();
                    bitcoinVerde.setIsConnected(isConnected);
                }

                if (isConnected) { return false; }

                try { Thread.sleep(15000L); }
                catch (final InterruptedException exception) { return false; }

                return true;
            }

            @Override
            protected void _onSleep() { }
        };
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        INSTANCE_COUNT.incrementAndGet();

        this.setContentView(R.layout.loading_layout);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        _activityIsActive.set(true);
        super.onResume();
    }

    @Override
    protected void onPause() {
        final BitcoinVerdeService.Binder bitcoinVerdeService = _bitcoinVerdeService;
        if (bitcoinVerdeService != null) {
            // Start the BitcoinVerde Service to facilitate completing initialize in the background.
            //  Once BitcoinVerde::initialize is completed, if there are no clients bound, it will stop itself.
            final BitcoinVerde bitcoinVerde = bitcoinVerdeService.getBitcoinVerdeInstance();
            if ( (bitcoinVerde == null) || (! bitcoinVerde.isSynchronizationComplete()) ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    this.startForegroundService(new Intent(this, BitcoinVerdeService.class));
                }
                else {
                    this.startService(new Intent(this, BitcoinVerdeService.class));
                }
            }
        }

        _activityIsActive.set(false);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        INSTANCE_COUNT.decrementAndGet();
        super.onDestroy();
    }

    @Override
    public void onServiceConnected() {
        _bitcoinVerde = _bitcoinVerdeService.getBitcoinVerdeInstance();
        _bitcoinVerde.addWalletUpdatedCallback(_onWalletUpdatedRunnable);

        _bitcoinVerde.setOnStatusUpdatedCallback(new Runnable() {
            @Override
            public void run() {
                _updateSyncStatus();
            }
        });

        final Runnable showMainLayout = new Runnable() {
            @Override
            public void run() {
                final Activity mainActivity = MainActivity.this;
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (! _isShowingLoadingScreen.compareAndSet(true, false)) { return; } // Don't fire multiple times...

                        mainActivity.setContentView(R.layout.main_activity_layout);

                        _frameLayout = mainActivity.findViewById(R.id.frame_layout);

                        final BottomNavigationView navigation = mainActivity.findViewById(R.id.navigation);
                        navigation.setSelectedItemId(R.id.navigation_sync_status);
                        navigation.setOnNavigationItemSelectedListener(_onNavigationItemSelectedListener);

                        { // Set the synchronization fragment on start-up...
                            final SynchronizationFragment synchronizationFragment = new SynchronizationFragment();
                            FragmentUtil.replaceCurrentFragment(_getFragmentManager(), synchronizationFragment, R.id.frame_layout, null);
                        }
                    }
                });
            }
        };

        _bitcoinVerde.setOnSynchronizationComplete(showMainLayout);

        _connectivityService.wakeUp();

        if (true) {
            showMainLayout.run();
        }
    }

    @Override
    public void onServiceDisconnected() {
        final BitcoinVerde bitcoinVerde = _bitcoinVerdeService.getBitcoinVerdeInstance();
        bitcoinVerde.removeWalletUpdatedCallback(_onWalletUpdatedRunnable);

        _connectivityService.wakeUp();
    }

    @Override
    public void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);

        if (Util.areEqual(EXIT_ACTIVITY_ACTION, intent.getAction())) {
            this.finish();
        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        for (final Fragment fragment : this.getSupportFragmentManager().getFragments()) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        this.getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();

        switch (id) {
            case R.id.action_repair_merkle_blocks: {
                if (_bitcoinVerde == null) { return false; }

                _bitcoinVerde.repairMerkleBlocks();
                _bitcoinVerde.repairSlpValidity();
                return true;
            }

            case R.id.action_filter_token: {
                _displayTokenSelector();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public void requestNoNavigationBar() {
        final View navigationView = this.findViewById(R.id.navigation);
        if (navigationView == null) { return; }

        navigationView.setVisibility(View.GONE);
    }

    public void requestNavigationBar() {
        final View navigationView = this.findViewById(R.id.navigation);
        if (navigationView == null) { return; }

        navigationView.setVisibility(View.VISIBLE);
    }

    public void requestNoTitleBar() {
        final ActionBar actionBar = this.getSupportActionBar();
        if (actionBar == null) { return; }

        actionBar.hide();
    }

    public void requestTitleBar() {
        final ActionBar actionBar = this.getSupportActionBar();
        if (actionBar == null) { return; }

        actionBar.show();
    }

    public void requestFullLayout() {
        final View navigationView = this.findViewById(R.id.navigation);
        if (navigationView != null) {
            navigationView.setVisibility(View.GONE);
        }

        final ActionBar actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }

    public void requestNormalLayout() {
        final View navigationView = this.findViewById(R.id.navigation);
        if (navigationView != null) {
            navigationView.setVisibility(View.VISIBLE);
        }

        final ActionBar actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            actionBar.show();
        }
    }
}
