package com.torrent.service;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class FileStorage {

    private Map<String, byte[]> files = new HashMap<>();

    public void store(String name, byte[] data){
        files.put(name, data);
    }
}
