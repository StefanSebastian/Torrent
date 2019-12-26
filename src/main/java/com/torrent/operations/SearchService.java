package com.torrent.operations;

import com.torrent.Config;
import com.torrent.gen.Torr;
import com.torrent.service.FileStorage;
import com.torrent.service.SenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.IIOException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
public class SearchService {

    private static Logger LOG = LoggerFactory.getLogger(SearchService.class);

    @Autowired
    private Config config;

    @Autowired
    private SenderService sender;

    @Autowired
    private SubnetRequestService subnetRequestService;

    @Autowired
    private FileStorage storage;

    @Autowired
    private SenderService senderService;

    public Torr.SearchResponse handle(Torr.SearchRequest request) {
        String regex = request.getRegex();
        Pattern pattern = compileRegex(regex);
        if (pattern == null) {
            return getResponse(Torr.Status.MESSAGE_ERROR);
        }

        Torr.SubnetResponse response = subnetRequestService.perform(request.getSubnetId());
        if (response == null || response.getStatus() != Torr.Status.SUCCESS) {
            return getResponse(Torr.Status.PROCESSING_ERROR);
        }

        List<Torr.NodeSearchResult> res = executeParallelSearch(regex, response);

        return getResponse(Torr.Status.SUCCESS, res);
    }

    private Pattern compileRegex(String regex) {
        if (regex == null) {
            return null;
        }
        try {
            return Pattern.compile(regex);
        } catch (PatternSyntaxException exc) {
            return null;
        }
    }

    private Torr.SearchResponse getResponse(Torr.Status status) {
        return Torr.SearchResponse.newBuilder()
                .setStatus(status)
                .build();
    }

    private Torr.SearchResponse getResponse(Torr.Status status, List<Torr.NodeSearchResult> res) {
        return Torr.SearchResponse.newBuilder()
                .setStatus(status)
                .addAllResults(res)
                .build();
    }

    // concurrency here
    private ExecutorService executor = Executors.newFixedThreadPool(8);

    private List<Torr.NodeSearchResult> executeParallelSearch(String regex, Torr.SubnetResponse subnet) {
        List<Torr.NodeSearchResult> operationResult = new LinkedList<>();
        List<Callable<Torr.NodeSearchResult>> callables = new ArrayList<>();

        for (Torr.NodeId nodeId : subnet.getNodesList()) {
            if (nodeId.getIndex() == config.getNodeIndex() &&
                    nodeId.getOwner().equals(config.getNodeOwner())) {
                callables.add(getLocalSearchResult(regex, nodeId));
            } else {
                callables.add(getRemoteSearchResult(regex, nodeId));
            }
        }

        try {
            List<Future<Torr.NodeSearchResult>> futureList = executor.invokeAll(callables);

            for (Future<Torr.NodeSearchResult> resultFuture : futureList) {
                Torr.NodeSearchResult result = resultFuture.get();
                operationResult.add(result);
            }
        } catch (InterruptedException | ExecutionException ex) {
            LOG.info("Executor was interrupted!");
        }

        return operationResult;
    }

    private Callable<Torr.NodeSearchResult> getRemoteSearchResult(String regex, Torr.NodeId nodeId) {
        return () -> {
            Torr.NodeSearchResult.Builder builder = Torr.NodeSearchResult.newBuilder();
            builder.setNode(nodeId);
            try {
                Torr.Message message = senderService.sendMessage(
                        getNodeSearchMessage(regex), nodeId.getHost(), nodeId.getPort());
                Torr.LocalSearchResponse response = message.getLocalSearchResponse();
                if (response.getStatus() != Torr.Status.SUCCESS) {
                    LOG.info("Remote search result status : " + response.getStatus() + " ; " + response.getErrorMessage());
                } else {
                    builder.addAllFiles(response.getFileInfoList());
                }
                builder.setStatus(response.getStatus());
            } catch (IOException exc) {
                LOG.info("Could not send remote search message");
                builder.setStatus(Torr.Status.NETWORK_ERROR);
            }
            return builder.build();
        };
    }

    private Torr.Message getNodeSearchMessage(String regex) {
        Torr.LocalSearchRequest request = Torr.LocalSearchRequest
                .newBuilder()
                .setRegex(regex)
                .build();
        return Torr.Message.newBuilder()
                .setType(Torr.Message.Type.LOCAL_SEARCH_REQUEST)
                .setLocalSearchRequest(request)
                .build();
    }

    private Callable<Torr.NodeSearchResult> getLocalSearchResult(String regex, Torr.NodeId nodeId) {
        return () -> {
            Map<String, Torr.FileInfo> files = storage.getFiles();
            List<Torr.FileInfo> foundFiles = new LinkedList<>();
            Pattern pattern = Pattern.compile(regex);
            for (String key : files.keySet()) {
                Matcher matcher = pattern.matcher(key);
                if (matcher.matches()) {
                    foundFiles.add(files.get(key));
                }
            }
            return Torr.NodeSearchResult
                    .newBuilder()
                    .setNode(nodeId)
                    .setStatus(Torr.Status.SUCCESS)
                    .addAllFiles(foundFiles)
                    .build();
        };
    }
}
