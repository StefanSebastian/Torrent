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
public class ReplicateRequestService {

    private static Logger LOG = LoggerFactory.getLogger(ReplicateRequestService.class);

    @Autowired
    private SenderService sender;

    @Autowired
    private Config config;

    @Autowired
    private FileStorage storage;

    @Autowired
    private SubnetRequestService subnetRequestService;

    public Torr.ReplicateResponse handle(Torr.ReplicateRequest request) {
        LOG.info("Replicate request handler");

        Torr.FileInfo fileInfo = request.getFileInfo();
        // invalid file
        if (fileInfo == null || fileInfo.getFilename() == null || fileInfo.getFilename().isEmpty()) {
            return getResponse(Torr.Status.MESSAGE_ERROR);
        }
        LOG.info("For file " + fileInfo.getFilename());

        // file already stored
        if (storage.isStored(fileInfo.getFilename())){
            LOG.info("File " + fileInfo.getFilename() + " was already stored");
            return getResponse(Torr.Status.SUCCESS);
        }

        // file not stored, must get chunks from nodes
        Torr.SubnetResponse response = subnetRequestService.perform(request.getSubnetId());
        if (response == null || response.getStatus() != Torr.Status.SUCCESS) {
            return getResponse(Torr.Status.PROCESSING_ERROR);
        }
        LOG.info("Performed subnet request : " + response);

        // TODO
        storage.storeInfo(fileInfo);

        return getResponse(Torr.Status.SUCCESS);
    }

    private Torr.ReplicateResponse getResponse(Torr.Status status) {
        Torr.ReplicateResponse response = Torr.ReplicateResponse.getDefaultInstance()
                .newBuilderForType()
                .setStatus(status)
                .build();
        return response;
    }

}
