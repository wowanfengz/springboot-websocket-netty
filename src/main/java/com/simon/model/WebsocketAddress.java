package com.simon.model;

import java.net.InetSocketAddress;

public class WebsocketAddress {
    private String path;
    private int port;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == null || !(obj instanceof WebsocketAddress))
            return false;
        return this.port==((WebsocketAddress) obj).port;
    }
}
