package com.torrent;

import com.torrent.gen.Torr;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Main {
    public static void main(String[] args) throws Exception{
        Torr.RegistrationRequest request = Torr.RegistrationRequest.getDefaultInstance()
                .newBuilderForType()
                .setIndex(3)
                .setOwner("Test")
                .setPort(5006)
                .build();

        Torr.Message message = Torr.Message.newBuilder().setType(Torr.Message.Type.REGISTRATION_REQUEST).setRegistrationRequest(request).build();
        byte[] m = message.toByteArray();
        int len = m.length; // 32-bit integer

        ByteBuffer buffer = ByteBuffer.allocate(1*Integer.BYTES);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(len);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.rewind();

        Socket clientSocket = new Socket("localhost", 5000);
        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

        System.out.println("Sending");
        out.writeInt(len);
        out.write(m);

        System.out.println("Sent");

        DataInputStream dIn = new DataInputStream(clientSocket.getInputStream());

        System.out.println("Receiving");
        int length = dIn.readInt();                    // read length of incoming message
        if(length>0) {
            byte[] mb = new byte[length];
            dIn.readFully(mb, 0, mb.length); // read the message
        }
        System.out.println("Received");

    }

}
