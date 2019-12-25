package com.torrent.service;

import com.torrent.Config;
import com.torrent.gen.Torr;
import com.torrent.operations.UploadRequestService;
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

@Component
public class Server {

    private static Logger LOG = LoggerFactory
            .getLogger(Server.class);

    @Autowired
    private Config config;

    @Autowired
    private UploadRequestService uploadRequestService;

    //initialize socket and input stream
    private Socket socket = null;
    private ServerSocket server = null;
    private DataInputStream in = null;
    private DataOutputStream out = null;

    @EventListener(ApplicationReadyEvent.class)
    public void start() throws IOException {
        try {
            server = new ServerSocket(config.getNodePort());
            LOG.info("Server started ...");

            while (true) {
                socket = server.accept();
                LOG.info("Client accepted");

                in = new DataInputStream(socket.getInputStream());
                int length = in.readInt();
                byte[] mb = new byte[length];
                in.readFully(mb, 0, mb.length); // read the message
                Torr.Message request = Torr.Message.parseFrom(mb);
                LOG.info("Received message " + request.toString());

                handleMessage(request);

                closeResources();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeResources();
            if (server != null) { server.close(); }
        }
    }

    private void closeResources() throws IOException {
        if (socket != null) { socket.close(); }
        if (in != null) { in.close(); }
        if (out != null) { out.close(); }
    }

    protected void handleMessage(Torr.Message message) {
        if (message.getType() == Torr.Message.Type.UPLOAD_REQUEST) {
            Torr.UploadResponse response = uploadRequestService.handle(message.getUploadRequest());
        }
    }
}
