package com.torrent;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "torrent")
public class Config {

    private String nodeOwner;
    private int nodePort;
    private int nodeIndex;

    private String hubIp;
    private int hubPort;

    public String getNodeOwner() {
        return nodeOwner;
    }

    public int getNodePort() {
        return nodePort;
    }

    public int getNodeIndex() {
        return nodeIndex;
    }

    public String getHubIp() {
        return hubIp;
    }

    public int getHubPort() {
        return hubPort;
    }

    public void setNodeOwner(String nodeOwner) {
        this.nodeOwner = nodeOwner;
    }

    public void setNodePort(int nodePort) {
        this.nodePort = nodePort;
    }

    public void setNodeIndex(int nodeIndex) {
        this.nodeIndex = nodeIndex;
    }

    public void setHubIp(String hubIp) {
        this.hubIp = hubIp;
    }

    public void setHubPort(int hubPort) {
        this.hubPort = hubPort;
    }
}
