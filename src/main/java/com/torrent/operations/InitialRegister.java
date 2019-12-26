package com.torrent.operations;

import com.torrent.Config;
import com.torrent.gen.Torr;
import com.torrent.service.SenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class InitialRegister {

    private static Logger LOG = LoggerFactory
            .getLogger(InitialRegister.class);

    @Autowired
    private SenderService sender;

    @Autowired
    private Config config;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try {
            Torr.Message message = sender.sendMessage(getRegisterMessage(), config.getHubIp(), config.getHubPort());
            Torr.RegistrationResponse response = message.getRegistrationResponse();
            if (response.getStatus() != Torr.Status.SUCCESS) {
                LOG.info("Status for register : " + response.getStatus());
                throw new RuntimeException("Application could not start");
            }
        } catch (IOException exc) {
            LOG.info("Could not send register message. " + exc.getMessage(), exc);
            throw new RuntimeException("Application could not start");
        }
    }

    private Torr.Message getRegisterMessage() {
        Torr.RegistrationRequest request = Torr.RegistrationRequest.getDefaultInstance()
                .newBuilderForType()
                .setIndex(config.getNodeIndex())
                .setOwner(config.getNodeOwner())
                .setPort(config.getNodePort())
                .build();
        return Torr.Message.newBuilder()
                .setType(Torr.Message.Type.REGISTRATION_REQUEST)
                .setRegistrationRequest(request).build();
    }
}
