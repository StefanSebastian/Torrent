package com.torrent.service;

import com.google.protobuf.InvalidProtocolBufferException;
import com.torrent.Config;
import com.torrent.gen.Torr;
import com.torrent.operations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class Server {

    private static Logger LOG = LoggerFactory
            .getLogger(Server.class);

    @Autowired
    private Config config;

    @Autowired
    private UploadRequestService uploadRequestService;

    @Autowired
    private ReplicateRequestService replicateRequestService;

    @Autowired
    private LocalSearchService localSearchService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private DownloadRequestService downloadRequestService;

    @Autowired
    private ChunkRequestService chunkRequestService;

    //initialize socket and input stream
    private ServerSocket server = null;

    private ExecutorService executorService = Executors.newCachedThreadPool();

    @EventListener(ApplicationReadyEvent.class)
    public void start() throws IOException {
        try {
            server = new ServerSocket(config.getNodePort());
            LOG.info("Server started ...");

            while (true) {
                Socket socket = server.accept();
                LOG.info("Client accepted");

                // start new task
                executorService.submit(getClientExecutor(socket));

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            if (server != null) { server.close(); }
        }
    }

    private Callable<Boolean> getClientExecutor(Socket socket) {
        return () -> {
            DataInputStream in = null;
            DataOutputStream out = null;

            try {
                in = new DataInputStream(socket.getInputStream());
                int length = in.readInt();
                byte[] mb = new byte[length];
                in.readFully(mb, 0, mb.length); // read the message
                Torr.Message request = Torr.Message.parseFrom(mb);
                LOG.info("Received message " + request.toString());

                handleMessage(request, out, socket);
                return true;
            } catch (IOException exc) {
                LOG.info("Could not read message");
                return false;
            } finally {
                if (in != null) { in.close(); }
                if (out != null) { out.close(); }
                if (socket != null) { socket.close(); }
            }
        };
    }


    private void handleMessage(Torr.Message message, DataOutputStream out, Socket socket) {
        Torr.Message reply = null;

        if (message.getType() == Torr.Message.Type.UPLOAD_REQUEST) {
            Torr.UploadResponse response = uploadRequestService.handle(message.getUploadRequest());
            reply = Torr.Message.getDefaultInstance()
                    .newBuilderForType()
                    .setType(Torr.Message.Type.UPLOAD_RESPONSE)
                    .setUploadResponse(response)
                    .build();
        }
        if (message.getType() == Torr.Message.Type.REPLICATE_REQUEST) {
            Torr.ReplicateResponse response = replicateRequestService.handle(message.getReplicateRequest());
            reply = Torr.Message.getDefaultInstance()
                    .newBuilderForType()
                    .setType(Torr.Message.Type.REPLICATE_RESPONSE)
                    .setReplicateResponse(response)
                    .build();
        }
        if (message.getType() == Torr.Message.Type.LOCAL_SEARCH_REQUEST) {
            Torr.LocalSearchResponse response = localSearchService.handle(message.getLocalSearchRequest());
            reply = Torr.Message.getDefaultInstance()
                    .newBuilderForType()
                    .setType(Torr.Message.Type.LOCAL_SEARCH_RESPONSE)
                    .setLocalSearchResponse(response)
                    .build();
        }
        if (message.getType() == Torr.Message.Type.SEARCH_REQUEST) {
            Torr.SearchResponse response = searchService.handle(message.getSearchRequest());
            reply = Torr.Message.getDefaultInstance()
                    .newBuilderForType()
                    .setType(Torr.Message.Type.SEARCH_RESPONSE)
                    .setSearchResponse(response)
                    .build();
        }
        if (message.getType() == Torr.Message.Type.DOWNLOAD_REQUEST) {
            Torr.DownloadResponse response = downloadRequestService.handle(message.getDownloadRequest());
            reply = Torr.Message.newBuilder()
                    .setType(Torr.Message.Type.DOWNLOAD_RESPONSE)
                    .setDownloadResponse(response)
                    .build();
        }
        if (message.getType() == Torr.Message.Type.CHUNK_REQUEST) {
            Torr.ChunkResponse response = chunkRequestService.handle(message.getChunkRequest());
            reply = Torr.Message.newBuilder()
                    .setType(Torr.Message.Type.CHUNK_RESPONSE)
                    .setChunkResponse(response)
                    .build();
        }

        try {
            sendReply(reply, out, socket);
        } catch (IOException exc) {
            LOG.info("Could not send reply " + exc.getMessage());
        }
    }

    private void sendReply(Torr.Message message, DataOutputStream out, Socket socket) throws IOException {
        byte[] m = message.toByteArray();
        out = new DataOutputStream(socket.getOutputStream());
        out.writeInt(m.length);
        out.write(m);
        LOG.info("Sent message " + message.toString());
    }
}
