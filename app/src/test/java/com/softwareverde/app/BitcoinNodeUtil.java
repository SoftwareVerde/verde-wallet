package com.softwareverde.app;

import com.softwareverde.bitcoin.server.message.type.node.feature.LocalNodeFeatures;
import com.softwareverde.bitcoin.server.message.type.node.feature.NodeFeatures;
import com.softwareverde.bitcoin.server.message.type.query.response.hash.InventoryItem;
import com.softwareverde.bitcoin.server.message.type.query.response.hash.InventoryItemType;
import com.softwareverde.bitcoin.server.node.BitcoinNode;
import com.softwareverde.bitcoin.server.node.RequestId;
import com.softwareverde.bitcoin.transaction.Transaction;
import com.softwareverde.concurrent.pool.cached.CachedThreadPool;
import com.softwareverde.constable.list.List;
import com.softwareverde.constable.list.mutable.MutableList;
import com.softwareverde.cryptography.hash.sha256.Sha256Hash;
import com.softwareverde.network.p2p.node.Node;
import com.softwareverde.util.Util;

public class BitcoinNodeUtil {
    protected BitcoinNodeUtil() { }

    public static List<Transaction> getTransactions(final List<Sha256Hash> transactionHashes) throws Exception {
        final MutableList<Transaction> transactions = new MutableList<Transaction>();

        final CachedThreadPool mainThreadPool = new CachedThreadPool(1, 5000L);
        mainThreadPool.start();

        final LocalNodeFeatures localNodeFeatures = new LocalNodeFeatures() {
            @Override
            public NodeFeatures getNodeFeatures() {
                final NodeFeatures nodeFeatures = new NodeFeatures();
                nodeFeatures.enableFeature(NodeFeatures.Feature.BITCOIN_CASH_ENABLED);
                nodeFeatures.enableFeature(NodeFeatures.Feature.BLOOM_CONNECTIONS_ENABLED);
                return nodeFeatures;
            }
        };

        final Object pin = new Object();
        final BitcoinNode bitcoinNode = new BitcoinNode("bitcoinverde.org", 8333, mainThreadPool, localNodeFeatures);
        bitcoinNode.setDisconnectedCallback(new Node.DisconnectedCallback() {
            @Override
            public void onNodeDisconnected() {
                synchronized (pin) {
                    pin.notifyAll();
                }
            }
        });
        bitcoinNode.setHandshakeCompleteCallback(new Node.HandshakeCompleteCallback() {
            @Override
            public void onHandshakeComplete() {
                bitcoinNode.requestTransactions(transactionHashes, new BitcoinNode.DownloadTransactionCallback() {
                    @Override
                    public void onResult(final RequestId requestId, final BitcoinNode bitcoinNode, final Transaction transaction) {
                        transactions.add(transaction);
                        System.out.println("Downloaded Transaction: " + transaction.getHash());

                        if (transactions.getSize() >= transactionHashes.getSize()) {
                            synchronized (pin) {
                                pin.notifyAll();
                            }
                        }
                    }
                });
            }
        });

        bitcoinNode.connect();
        bitcoinNode.handshake();

        synchronized (pin) {
            pin.wait(30000L);
        }

        Thread.sleep(2500L);

        bitcoinNode.disconnect();

        synchronized (pin) {
            pin.wait(500L);
        }

        mainThreadPool.stop();

        return transactions;
    }

    public static void broadcastTransaction(final Transaction transaction) throws Exception {
        final CachedThreadPool mainThreadPool = new CachedThreadPool(1, 5000L);
        final LocalNodeFeatures localNodeFeatures = new LocalNodeFeatures() {
            @Override
            public NodeFeatures getNodeFeatures() {
                final NodeFeatures nodeFeatures = new NodeFeatures();
                nodeFeatures.enableFeature(NodeFeatures.Feature.BITCOIN_CASH_ENABLED);
                nodeFeatures.enableFeature(NodeFeatures.Feature.BLOOM_CONNECTIONS_ENABLED);
                return nodeFeatures;
            }
        };

        final Object pin = new Object();
        final BitcoinNode bitcoinNode = new BitcoinNode("btc.softwareverde.com", 8333, mainThreadPool, localNodeFeatures);
        bitcoinNode.setDisconnectedCallback(new Node.DisconnectedCallback() {
            @Override
            public void onNodeDisconnected() {
                synchronized (pin) {
                    pin.notifyAll();
                }
            }
        });
        bitcoinNode.setHandshakeCompleteCallback(new Node.HandshakeCompleteCallback() {
            @Override
            public void onHandshakeComplete() {
                final MutableList<Sha256Hash> transactionHashes = new MutableList<Sha256Hash>(1);
                transactionHashes.add(transaction.getHash());
                bitcoinNode.transmitTransactionHashes(transactionHashes);
            }
        });
        bitcoinNode.setRequestDataHandler(new BitcoinNode.RequestDataHandler() {
            @Override
            public void run(final BitcoinNode bitcoinNode, final List<InventoryItem> dataHashes) {
                boolean transactionWasBroadcasted = false;
                for (final InventoryItem inventoryItem : dataHashes) {
                    if (inventoryItem.getItemType() != InventoryItemType.TRANSACTION) { continue; }

                    if (Util.areEqual(transaction.getHash(), inventoryItem.getItemHash())) {
                        bitcoinNode.transmitTransaction(transaction);
                        transactionWasBroadcasted = true;
                    }
                }

                if (transactionWasBroadcasted) {
                    synchronized (pin) {
                        pin.notifyAll();
                    }
                }
            }
        });
        bitcoinNode.connect();
        bitcoinNode.handshake();

        synchronized (pin) {
            pin.wait(30000L);
        }

        Thread.sleep(5000L);

        bitcoinNode.disconnect();

        synchronized (pin) {
            pin.wait(500L);
        }
    }
}
