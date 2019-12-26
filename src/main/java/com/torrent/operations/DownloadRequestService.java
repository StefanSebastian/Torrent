package com.torrent.operations;

import com.google.protobuf.ByteString;
import com.torrent.gen.Torr;
import com.torrent.service.FileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

@Component
public class DownloadRequestService {

    private static Logger LOG = LoggerFactory.getLogger(DownloadRequestService.class);

    @Autowired
    private FileStorage storage;

    public Torr.DownloadResponse handle(Torr.DownloadRequest request) {
        byte[] fileHash = request.getFileHash().toByteArray();
        if (fileHash == null || fileHash.length != 16) {
            return getResponse(Torr.Status.MESSAGE_ERROR);
        }

        Torr.FileInfo fileInfo = storage.getByHash(fileHash);
        if (fileInfo == null) {
            return getResponse(Torr.Status.UNABLE_TO_COMPLETE);
        }
        byte[] content = storage.getFileContent(fileInfo.getFilename());
        if (content == null) {
            return getResponse(Torr.Status.UNABLE_TO_COMPLETE);
        }

        return getResponse(ByteString.copyFrom(content));
    }

    private Torr.DownloadResponse getResponse(Torr.Status status) {
        return Torr.DownloadResponse.newBuilder()
                .setStatus(status)
                .build();
    }

    private Torr.DownloadResponse getResponse(ByteString data) {
        return Torr.DownloadResponse.newBuilder()
                .setStatus(Torr.Status.SUCCESS)
                .setData(data)
                .build();
    }
}
