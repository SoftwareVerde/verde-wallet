package com.softwareverde.bitcoin.android.lib;

import com.softwareverde.network.p2p.node.NodeId;

public class Node {
    protected final NodeId _id;
    protected String _userAgent;
    protected String _ip;
    protected Integer _port;
    protected Long _ping;

    public Node(final NodeId nodeId) {
        _id = nodeId;
    }

    public NodeId getId() {
        return _id;
    }

    public String getUserAgent() {
        return _userAgent;
    }

    public String getIp() {
        return _ip;
    }

    public Integer getPort() {
        return _port;
    }

    public Long getPing() {
        return _ping;
    }
}
