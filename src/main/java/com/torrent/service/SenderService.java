package com.torrent.service;

import com.torrent.Config;
import com.torrent.gen.Torr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

@Service
public class SenderService {

    private static Logger LOG = LoggerFactory
            .getLogger(SenderService.class);

    public Torr.Message sendMessage(Torr.Message message, String ip, int port) throws IOException {
        byte[] m = message.toByteArray();
        int len = m.length; // 32-bit integer

        DataOutputStream out = null;
        DataInputStream dIn = null;
        Socket clientSocket = null;
        Torr.Message response = null;
        try {
            clientSocket = new Socket(ip, port);
            out = new DataOutputStream(clientSocket.getOutputStream());
            out.writeInt(len);
            out.write(m);
            LOG.info("Sent message " + message.toString());

            dIn = new DataInputStream(clientSocket.getInputStream());
            int length = dIn.readInt();
            byte[] mb = new byte[length];
            dIn.readFully(mb, 0, mb.length); // read the message
            response = Torr.Message.parseFrom(mb);
            LOG.info("Received message " + response.toString());
        } finally {
            if (out != null) {
                out.close();
            }
            if (dIn != null) {
                dIn.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
        }
        return response;
    }

}
