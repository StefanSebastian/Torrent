package com.torrent.service;

import com.torrent.gen.Torr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.Socket;

@Service
public class SenderService {

    private static Logger LOG = LoggerFactory
            .getLogger(SenderService.class);

    public Torr.Message sendMessage(Torr.Message message, String ip, int port) throws IOException {
        byte[] m = message.toByteArray();
        int len = m.length; // 32-bit integer

        BufferedOutputStream outBuffered = null;
        BufferedInputStream inBuffered = null;
        DataOutputStream out = null;
        DataInputStream in = null;
        Socket clientSocket = null;
        Torr.Message response = null;
        try {
            clientSocket = new Socket(ip, port);
            outBuffered = new BufferedOutputStream(clientSocket.getOutputStream());
            out = new DataOutputStream(outBuffered);
            out.writeInt(len);
            out.write(m);
            out.flush();
            LOG.info("Sent message " + message.toString());

            inBuffered = new BufferedInputStream(clientSocket.getInputStream());
            in = new DataInputStream(inBuffered);
            int length = in.readInt();
            byte[] mb = new byte[length];
            in.readFully(mb, 0, mb.length); // read the message
            response = Torr.Message.parseFrom(mb);
            LOG.info("Received message " + response.toString());
        } finally {
            if (out != null) { out.close(); }
            if (in != null) { in.close(); }
            if (inBuffered != null) { inBuffered.close(); }
            if (outBuffered != null) { outBuffered.close(); }
            if (clientSocket != null) { clientSocket.close(); }
        }
        return response;
    }

}
