package com.torrent.service;

import com.torrent.gen.Torr;

public class StoredChunkInfo {

    private Torr.ChunkInfo chunkInfo;
    private byte[] chunkData;

    public StoredChunkInfo(Torr.ChunkInfo chunkInfo, byte[] chunkData) {
        this.chunkInfo = chunkInfo;
        this.chunkData = chunkData;
    }

    public Torr.ChunkInfo getChunkInfo() {
        return chunkInfo;
    }

    public void setChunkInfo(Torr.ChunkInfo chunkInfo) {
        this.chunkInfo = chunkInfo;
    }

    public byte[] getChunkData() {
        return chunkData;
    }

    public void setChunkData(byte[] chunkData) {
        this.chunkData = chunkData;
    }
}
