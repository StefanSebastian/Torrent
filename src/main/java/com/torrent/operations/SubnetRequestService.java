package com.torrent.operations;

import com.torrent.Config;
import com.torrent.gen.Torr;
import com.torrent.service.SenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SubnetRequestService {

    private static Logger LOG = LoggerFactory.getLogger(SubnetRequestService.class);

    @Autowired
    private SenderService sender;

    @Autowired
    private Config config;

    // TODO : refactor
    //@EventListener(ApplicationReadyEvent.class)
    public void test() {
        try {
            Torr.Message message = sender.sendMessage(getSubnetRequestMessage());
            Torr.SubnetResponse response = message.getSubnetResponse();
            if (response.getStatus() != Torr.Status.SUCCESS) {
                LOG.info("Status " + response.getStatus());
                LOG.info("Error " + response.getErrorMessage());
            }
        } catch (IOException exc) {
            LOG.info("Could not send subnet request");
        }
    }

    private Torr.Message getSubnetRequestMessage() {
        Torr.SubnetRequest request = Torr.SubnetRequest.getDefaultInstance()
                .newBuilderForType()
                .setSubnetId(1)
                .build();
        return Torr.Message.newBuilder()
                .setType(Torr.Message.Type.SUBNET_REQUEST)
                .setSubnetRequest(request)
                .build();
    }
}
