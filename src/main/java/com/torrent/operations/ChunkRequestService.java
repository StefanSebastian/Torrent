package com.torrent.operations;

import com.google.protobuf.ByteString;
import com.torrent.gen.Torr;
import com.torrent.service.FileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ChunkRequestService {

    private static Logger LOG = LoggerFactory.getLogger(ChunkRequestService.class);

    @Autowired
    private FileStorage storage;

    public Torr.ChunkResponse handle(Torr.ChunkRequest request) {
        if (request.getFileHash() == null) {
            return getResponse(Torr.Status.MESSAGE_ERROR);
        }
        byte[] fileHash = request.getFileHash().toByteArray();
        if (fileHash == null || fileHash.length != 16) {
            return getResponse(Torr.Status.MESSAGE_ERROR);
        }
        int index = request.getChunkIndex();
        if (index < 0) {
            return getResponse(Torr.Status.MESSAGE_ERROR);
        }

        Torr.FileInfo fileInfo = storage.getByHash(fileHash);
        if (fileInfo == null) {
            return getResponse(Torr.Status.UNABLE_TO_COMPLETE);
        }

        byte[] chunkData = storage.getChunkByIndex(fileInfo.getFilename(), index);
        if (chunkData == null) {
            return getResponse(Torr.Status.UNABLE_TO_COMPLETE);
        }

        return getResponse(ByteString.copyFrom(chunkData));
    }

    private Torr.ChunkResponse getResponse(Torr.Status status) {
        return Torr.ChunkResponse.newBuilder()
                .setStatus(status)
                .build();
    }

    private Torr.ChunkResponse getResponse(ByteString data) {
        return Torr.ChunkResponse.newBuilder()
                .setStatus(Torr.Status.SUCCESS)
                .setData(data)
                .build();
    }

}
