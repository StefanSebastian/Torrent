package com.torrent.operations;

import com.torrent.Config;
import com.torrent.gen.Torr;
import com.torrent.service.FileStorage;
import com.torrent.service.SenderService;
import com.torrent.service.StoredChunkInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

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

    @Autowired
    private SenderService senderService;

    class ChunkRequestStatus {
        Torr.ChunkResponse finalResponse;
        List<LoggedChunkResponse> loggedResponses = new LinkedList<>();
    }

    class LoggedChunkResponse {
        Torr.NodeId nodeId;
        Torr.ChunkResponse chunkResponse;
        Torr.ChunkInfo chunkInfo;

        LoggedChunkResponse(Torr.NodeId nodeId, Torr.ChunkResponse chunkResponse, Torr.ChunkInfo chunkInfo) {
            this.nodeId = nodeId;
            this.chunkResponse = chunkResponse;
            this.chunkInfo = chunkInfo;
        }
    }

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

        // ask for chunks
        List<ChunkRequestStatus> opRes = chunkRequestParallel(fileInfo, response);

        // try to rebuild file
        boolean allFound = rebuildFile(fileInfo, opRes);

        // build final response
        return getResponse(opRes, allFound);
    }

    private Torr.ReplicateResponse getResponse(Torr.Status status) {
        return Torr.ReplicateResponse.newBuilder()
                .setStatus(status)
                .build();
    }

    private Torr.ReplicateResponse getResponse(List<ChunkRequestStatus> resp, boolean allFound) {
        List<Torr.NodeReplicationStatus> statuses = new LinkedList<>();
        Torr.ReplicateResponse.Builder builder = Torr.ReplicateResponse.newBuilder();
        builder.setStatus(allFound ? Torr.Status.SUCCESS : Torr.Status.UNABLE_TO_COMPLETE);

        for (ChunkRequestStatus status : resp) {
            for (LoggedChunkResponse response : status.loggedResponses) {
                Torr.NodeReplicationStatus nodeSt = Torr.NodeReplicationStatus.newBuilder()
                        .setNode(response.nodeId)
                        .setChunkIndex(response.chunkInfo.getIndex())
                        .setStatus(response.chunkResponse.getStatus())
                        .build();
                statuses.add(nodeSt);
            }
        }

        builder.addAllNodeStatusList(statuses);

        return builder.build();
    }

    private boolean rebuildFile(Torr.FileInfo fileInfo, List<ChunkRequestStatus> responses) {
        byte[] data = new byte[fileInfo.getSize()];
        boolean allChunksFound = true;
        List<StoredChunkInfo> toStore = new LinkedList<>();
        for (int i = 0; i < responses.size(); i++) {
            Torr.ChunkInfo info = fileInfo.getChunksList().get(i);
            Torr.ChunkResponse response = responses.get(i).finalResponse;
            if (response.getStatus() != Torr.Status.SUCCESS) { // couldnt find
                allChunksFound = false;
            } else { // case of success
                byte[] responseData = response.getData().toByteArray();
                if (allChunksFound) { // while true rebuild file
                    for (int c = i * 1024; c < i * 1024 + info.getSize(); c++) {
                        data[c] = responseData[c - i * 1024];
                    }
                }

                StoredChunkInfo storedChunkInfo = new StoredChunkInfo(info, responseData);
                toStore.add(storedChunkInfo);
            }
        }

        if (allChunksFound) {
            storage.store(fileInfo.getFilename(), data);
        } else {
            storage.storePartialChunks(fileInfo.getFilename(), toStore);
        }

        return allChunksFound;
    }

    // concurrency here
    private ExecutorService executor = Executors.newFixedThreadPool(8);

    private List<ChunkRequestStatus> chunkRequestParallel(Torr.FileInfo fileInfo, Torr.SubnetResponse subnet) {
        List<ChunkRequestStatus> operationResult = new LinkedList<>();
        List<Callable<ChunkRequestStatus>> callables = new LinkedList<>();

        // task for each chunk ; try on all nodes
        for (Torr.ChunkInfo chunkInfo : fileInfo.getChunksList()) {
            callables.add(getRemoteChunk(subnet, chunkInfo, fileInfo));
        }

        try {
            List<Future<ChunkRequestStatus>> futures = executor.invokeAll(callables);

            for (Future<ChunkRequestStatus> future : futures) {
                ChunkRequestStatus chunkResponse = future.get();
                operationResult.add(chunkResponse);
            }
        } catch (InterruptedException | ExecutionException exc) {
            LOG.info("Execution was interrupted");
        }

        return operationResult;
    }

    /**
     * Try to grab remote chunk
     */
    private Callable<ChunkRequestStatus> getRemoteChunk(Torr.SubnetResponse subnet, Torr.ChunkInfo info, Torr.FileInfo fileInfo) {
        return () -> {
            ChunkRequestStatus status = new ChunkRequestStatus();
            List<LoggedChunkResponse> loggedResponses = new LinkedList<>();

            Torr.ChunkResponse.Builder builder = Torr.ChunkResponse.newBuilder();
            builder.setStatus(Torr.Status.UNABLE_TO_COMPLETE);

            for (Torr.NodeId nodeId : subnet.getNodesList()) {
                if (nodeId.getOwner().equals(config.getNodeOwner()) && nodeId.getIndex() == config.getNodeIndex()) {
                    continue; // skip auto query
                }
                try {
                    Torr.Message message = senderService.sendMessage(
                            getChunkRequestMessage(info, fileInfo), nodeId.getHost(), nodeId.getPort());
                    Torr.ChunkResponse response = message.getChunkResponse();

                    // log resp
                    loggedResponses.add(new LoggedChunkResponse(nodeId, response, info));
                    if (response.getStatus() == Torr.Status.SUCCESS) {
                        builder.setStatus(Torr.Status.SUCCESS);
                        builder.setData(response.getData());
                        break;
                    }
                } catch (IOException exc) {
                    LOG.info("Can't contact node");
                }
            }

            status.finalResponse = builder.build();
            status.loggedResponses = loggedResponses;
            return status;
        };
    }

    private Torr.Message getChunkRequestMessage(Torr.ChunkInfo chunkInfo, Torr.FileInfo fileInfo) {
        Torr.ChunkRequest request = Torr.ChunkRequest.newBuilder()
                .setChunkIndex(chunkInfo.getIndex())
                .setFileHash(fileInfo.getHash())
                .build();
        return Torr.Message.newBuilder()
                .setType(Torr.Message.Type.CHUNK_REQUEST)
                .setChunkRequest(request)
                .build();
    }

}
