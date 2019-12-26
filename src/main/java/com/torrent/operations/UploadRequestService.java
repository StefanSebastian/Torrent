package com.torrent.operations;

import com.torrent.Config;
import com.torrent.gen.Torr;
import com.torrent.service.FileStorage;
import com.torrent.service.SenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UploadRequestService {

    private static Logger LOG = LoggerFactory.getLogger(UploadRequestService.class);

    @Autowired
    private SenderService sender;

    @Autowired
    private Config config;

    @Autowired
    private FileStorage storage;

    public Torr.UploadResponse handle(Torr.UploadRequest request) {
        if (request.getFilename() == null || request.getFilename().isEmpty()) {
            return getResponse(Torr.Status.MESSAGE_ERROR, null);
        }

        Torr.FileInfo fileInfo = storage.store(request.getFilename(), request.getData().toByteArray());

        return getResponse(Torr.Status.SUCCESS, fileInfo);
    }

    private Torr.UploadResponse getResponse(Torr.Status status, Torr.FileInfo fileInfo) {
        Torr.UploadResponse response = Torr.UploadResponse.getDefaultInstance()
                .newBuilderForType()
                .setStatus(status)
                .setFileInfo(fileInfo)
                .build();
        return response;
    }
}
