package com.softwareverde.bitcoin.app.android.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.softwareverde.bitcoin.app.MainActivity;
import com.softwareverde.bitcoin.app.R;
import com.softwareverde.bitcoin.app.android.priceindexer.BitcoinDotComPriceIndexer;
import com.softwareverde.bitcoin.app.android.store.AndroidKeyManager;
import com.softwareverde.bitcoin.app.android.store.SharedPreferencesKeyValueStore;
import com.softwareverde.bitcoin.app.database.VerdeWalletDatabase;
import com.softwareverde.bitcoin.app.lib.BitcoinVerde;
import com.softwareverde.bitcoin.app.lib.NewBlockHeaderCallback;
import com.softwareverde.bitcoin.server.configuration.NodeProperties;
import com.softwareverde.bitcoin.server.database.Database;
import com.softwareverde.bitcoin.server.database.DatabaseConnection;
import com.softwareverde.bitcoin.server.module.node.manager.BitcoinNodeManager;
import com.softwareverde.bitcoin.server.module.spv.SpvResourceLoader;
import com.softwareverde.bitcoin.server.node.BitcoinNode;
import com.softwareverde.bitcoin.transaction.Transaction;
import com.softwareverde.constable.list.immutable.ImmutableList;
import com.softwareverde.constable.list.immutable.ImmutableListBuilder;
import com.softwareverde.database.DatabaseException;
import com.softwareverde.database.android.sqlite.AndroidSqliteDatabase;
import com.softwareverde.database.row.Row;
import com.softwareverde.logging.LogLevel;
import com.softwareverde.logging.Logger;
import com.softwareverde.logging.log.AnnotatedLog;
import com.softwareverde.logging.log.SystemLog;
import com.softwareverde.network.p2p.node.NodeConnection;
import com.softwareverde.util.StringUtil;
import com.softwareverde.util.Util;
import com.softwareverde.util.type.time.SystemTime;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

// TODO: Prevent service from downgrading until RebuildingIndexes is also complete...
// TODO: Minimizing app during block-header download temporarily freezes until 2k headers loaded...
// TODO: Resuming during block-header batch temporarily freezes...
// TODO: AndroidService is downgraded before indexes are rebuilt...?
// TODO: We are banning BitcoinVerde?  Also check: is BitcoinVerde banning us?

public class BitcoinVerdeService extends Service {
    protected static final String STOP_SERVICE_ACTION = "STOP_SERVICE";

    public class Binder extends android.os.Binder {
        public BitcoinVerde getBitcoinVerdeInstance() {
            return BitcoinVerdeService.this.getBitcoinVerde();
        }
    }

    protected final SystemTime _systemTime = new SystemTime();
    protected final String NOTIFICATION_CHANNEL_ID = "com.softwareverde.bitcoin.app.android.service";
    protected final Integer _startMode = START_NOT_STICKY;
    protected final Binder _binder = new Binder();
    protected final Boolean _allowRebind = false; // Indicates whether onRebind should be used...
    protected final AtomicInteger _activeBindCount = new AtomicInteger(0);

    protected final Integer _initProgressNotificationId = 1;
    protected final Integer _newTransactionNotificationId = 2;
    protected NotificationCompat.Builder _initProgressNotificationBuilder;
    protected NotificationCompat.Builder _newTransactionNotificationBuilder;
    protected BitcoinVerde _bitcoinVerde;

    protected final NewBlockHeaderCallback _newBlockHeaderCallback = new NewBlockHeaderCallback() {
        @Override
        public void newBlockHeight(final Long blockHeight) {
            final BitcoinVerde bitcoinVerde = _bitcoinVerde;
            if (bitcoinVerde == null) { return; }

            final Float percentComplete = bitcoinVerde.getSynchronizationPercent();

            final String percentString = StringUtil.formatPercent(percentComplete * 100F, true);

            final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(BitcoinVerdeService.this);
            notificationManager.notify(_initProgressNotificationId, _createBlockchainSyncNotification(percentString));

            if (bitcoinVerde.isSynchronizationComplete()) {
                _downgradeToBoundService();
            }
        }
    };

    protected Notification _createBlockchainSyncNotification(final String percentString) {
        if (_initProgressNotificationBuilder == null) {
            final Context context = this;

            final PendingIntent stopPendingIntent;
            {
                final Intent stopSelf = new Intent(context, BitcoinVerdeService.class);
                stopSelf.setAction(STOP_SERVICE_ACTION);
                stopPendingIntent = PendingIntent.getService(context, 1, stopSelf, PendingIntent.FLAG_CANCEL_CURRENT);
            }

            final PendingIntent mainPendingIntent = PendingIntent.getActivity(context, 1, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

            final NotificationCompat.Action stopAction = new NotificationCompat.Action.Builder(
                R.drawable.cancel,
                "Cancel",
                stopPendingIntent
            ).build();

            _initProgressNotificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setOnlyAlertOnce(true)
                .setContentTitle("Syncing Blockchain")
                .setSmallIcon(R.drawable.ic_dashboard_black_24dp)
                .setContentIntent(mainPendingIntent)
                .addAction(stopAction)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true)
            ;
        }

        _initProgressNotificationBuilder.setContentText(percentString);
        return _initProgressNotificationBuilder.build();
    }

    protected Notification _createNewTransactionNotification(final Transaction transaction) {
        if (_newTransactionNotificationBuilder == null) {
            final Context context = this;

            final PendingIntent mainPendingIntent = PendingIntent.getActivity(context, _newTransactionNotificationId, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

            final NotificationCompat.Action stopAction = new NotificationCompat.Action.Builder(
                    R.drawable.chevron_right,
                    "Open",
                    mainPendingIntent
            ).build();

            _newTransactionNotificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setOnlyAlertOnce(false)
                .setContentTitle("New Transaction")
                .setSmallIcon(R.drawable.send_icon)
                .setContentIntent(mainPendingIntent)
                .addAction(stopAction)
                .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setAutoCancel(true)
                .setOngoing(false)
            ;
        }

        _newTransactionNotificationBuilder.setContentText(transaction.getHash().toString());
        return _newTransactionNotificationBuilder.build();
    }

    protected void _initNotificationChannel(final Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { return; }

        final NotificationManager notificationManager = (NotificationManager) (context.getSystemService(Context.NOTIFICATION_SERVICE));
        if (notificationManager == null) { return; }

        final NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "BitcoinVerdeService", NotificationManager.IMPORTANCE_MIN);
        notificationChannel.setDescription("Bitcoin Verde Service");
        notificationChannel.setSound(null, null);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);

        notificationManager.createNotificationChannel(notificationChannel);
    }

    protected void _dismissNotification() {
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(BitcoinVerdeService.this);
        notificationManager.cancel(_initProgressNotificationId);
    }

    protected void _downgradeToBoundService() {
        this.stopForeground(true);
        _dismissNotification();
    }

    private Database _createDatabase() {
        final Context applicationContext = this.getApplicationContext();
        final Resources resources = applicationContext.getResources();

        final String databaseDataDirectory = (applicationContext.getFilesDir().getAbsolutePath() + "/database");

        AndroidSqliteDatabase sqliteDatabase = null;
        for (int i = 0; i < 2; ++i) {
            sqliteDatabase = new AndroidSqliteDatabase(applicationContext, "bitcoin", 1);

            try (final DatabaseConnection databaseConnection = sqliteDatabase.newConnection()) {
                if (! sqliteDatabase.shouldBeCreated()) {
                    continue;
                }

                if (_isDatabaseInitialized(databaseConnection)) {
                    break;
                }
                else {
                    final String initSql = SpvResourceLoader.getResource(SpvResourceLoader.INIT_SQL_SQLITE);
                    BitcoinVerde.runSqlInitFile(databaseConnection, initSql);
                    break;
                }
            }
            catch (final Exception exception) {
                exception.printStackTrace();
                BitcoinVerdeService.deleteRecursive(new File(databaseDataDirectory));
                sqliteDatabase = null;
            }
        }

        if (sqliteDatabase == null) {
            throw new RuntimeException("Error initializing database.");
        }

        return new VerdeWalletDatabase(sqliteDatabase);
    }

    protected static void deleteRecursive(final File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (final File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }

        fileOrDirectory.delete();
    }

    protected Boolean _isDatabaseInitialized(final DatabaseConnection databaseConnection) throws DatabaseException {
        final java.util.List<Row> rows = databaseConnection.query("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'metadata'", null);
        return (! rows.isEmpty());
    }

    @Override
    public void onCreate() {
        final Context applicationContext = this.getApplicationContext();
        final Resources resources = applicationContext.getResources();

        _initNotificationChannel(this);

        final Database database = _createDatabase();

        Logger.setLog(AnnotatedLog.getInstance());
        Logger.setLogLevel(LogLevel.ON);
        Logger.setLogLevel("com.softwareverde.util", LogLevel.ERROR);
        Logger.setLogLevel("com.softwareverde.network", LogLevel.INFO);
        Logger.setLogLevel("com.softwareverde.async.lock", LogLevel.WARN);
        // Logger.setLogLevel(BitcoinNodeManager.class, LogLevel.WARN);
        // Logger.setLogLevel(NodeConnection.class, LogLevel.WARN);
        // Logger.setLogLevel(BitcoinNode.class, LogLevel.WARN);

        final BitcoinVerde.InitData initData = new BitcoinVerde.InitData();
        try {
            initData.database = database;
            initData.bootstrapHeaders = SpvResourceLoader.getResourceAsStream(SpvResourceLoader.BOOTSTRAP_HEADERS);
            initData.keyStore = new AndroidKeyManager(applicationContext);
            initData.keyValueStore = new SharedPreferencesKeyValueStore(applicationContext, "wallet_preferences");
            initData.priceIndexer = new BitcoinDotComPriceIndexer();
            initData.shouldOnlyConnectToSeedNodes = false;
            initData.seedNodes = new ImmutableList<NodeProperties>(
                new NodeProperties("bitcoinverde.org", 8333),
                new NodeProperties("btc.softwareverde.com", 8333)
            );
        }
        catch (Exception exception) {
            throw new RuntimeException("Problem creating initData", exception);
        }

        { // Load seed phrase words...
            final String seedWords = SpvResourceLoader.getResource("/seed_words/seed_words_english.txt");
            final ImmutableListBuilder<String> seedWordsBuilder = new ImmutableListBuilder<String>(2048);
            for (final String seedWord : seedWords.split("\n")) {
                seedWordsBuilder.add(seedWord.trim());
            }
            initData.seedPhraseWords = seedWordsBuilder.build();
        }

        BitcoinVerde.init(initData);
        _bitcoinVerde = BitcoinVerde.getInstance();
        if (_bitcoinVerde == null) { throw new RuntimeException("Error initializing library."); }

        _bitcoinVerde.setNewTransactionCallback(new BitcoinVerde.NewTransactionCallback() {
            @Override
            public void onNewTransaction(final Transaction transaction) {
                Logger.debug("Tx: " + transaction.getHash());

                try {
                    final NotificationManager notificationManager = (NotificationManager) BitcoinVerdeService.this.getSystemService(Service.NOTIFICATION_SERVICE);
                    if (notificationManager == null) { return; }

                    final Notification notification = _createNewTransactionNotification(transaction);
                    notificationManager.notify(_newTransactionNotificationId, notification);
                }
                catch (final Exception exception) {
                    Logger.error(exception);
                }
            }
        });
        _bitcoinVerde.addNewBlockHeaderCallback(_newBlockHeaderCallback);
        _bitcoinVerde.setOnSynchronizationComplete(new Runnable() {
            @Override
            public void run() {
                _downgradeToBoundService();
            }
        });
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        // If the STOP_SERVICE_ACTION is received stop the service.
        if (Util.areEqual(STOP_SERVICE_ACTION, intent.getAction())) {
            MainActivity.closeApplication(this);
            this.stopSelf();
            return _startMode;
        }

        // Once synchronization is complete then downgrade the service to a boundService that closes when the activity finishes.
        if (_bitcoinVerde.isSynchronizationComplete()) {
            _downgradeToBoundService();
            return _startMode;
        }

        this.startForeground(_initProgressNotificationId, _createBlockchainSyncNotification(""));
        return _startMode;
    }

    @Override
    public Binder onBind(final Intent intent) {
        if (Util.areEqual(STOP_SERVICE_ACTION, intent.getAction())) {
            _downgradeToBoundService();
            return null;
        }

        _activeBindCount.incrementAndGet();
        return _binder;
    }

    /** Called when all clients have unbound with unbindService() */
    @Override
    public boolean onUnbind(final Intent intent) {
        _activeBindCount.decrementAndGet();
        if ( (_activeBindCount.get() == 0) && (_bitcoinVerde.isSynchronizationComplete())) {
            _downgradeToBoundService();
        }

        return _allowRebind;
    }

    /** Called when a client is binding to the service with bindService()*/
    @Override
    public void onRebind(final Intent intent) {
        // Not used...
    }

    /** Called when The service is no longer used and is being destroyed */
    @Override
    public void onDestroy() {
        final BitcoinVerde bitcoinVerde = _bitcoinVerde;
        if (bitcoinVerde != null) {
            bitcoinVerde.shutdown();
        }

        _bitcoinVerde = null;

        _dismissNotification();
    }

    public BitcoinVerde getBitcoinVerde() {
        return _bitcoinVerde;
    }
}