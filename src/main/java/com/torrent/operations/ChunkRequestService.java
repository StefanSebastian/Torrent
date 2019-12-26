package com.torrent.operations;

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
        return null;
    }
}
