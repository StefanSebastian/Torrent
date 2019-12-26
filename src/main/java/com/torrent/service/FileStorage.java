package com.torrent.service;

import com.google.protobuf.ByteString;
import com.torrent.gen.Torr;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FileStorage {

    private Map<String, Torr.FileInfo> files = new HashMap<>();
    private Map<String, byte[]> dataStore = new HashMap<>();

    public Torr.FileInfo getByHash(byte[] fileHash) {
        Torr.FileInfo fileInfo = null;
        for (Torr.FileInfo file : files.values()) {
            if (Arrays.equals(fileHash, file.getHash().toByteArray())) {
                fileInfo = file;
                break;
            }
        }
        return fileInfo;
    }

    public List<Torr.FileInfo> getMatches(String regex) {
        List<Torr.FileInfo> result = new LinkedList<>();
        Pattern pattern = Pattern.compile(regex);
        for (String key : files.keySet()) {
            Matcher matcher = pattern.matcher(key);
            if (matcher.matches()) {
                result.add(files.get(key));
            }
        }
        return result;
    }

    public Torr.FileInfo store(String name, byte[] data){
        Torr.FileInfo fileInfo = Torr.FileInfo.getDefaultInstance()
                .newBuilderForType()
                .setFilename(name)
                .setHash(getConvertedMd5(data))
                .setSize(data.length)
                .addAllChunks(getChunks(data))
                .build();
        files.put(name, fileInfo);
        dataStore.put(name, data);
        return fileInfo;
    }

    public byte[] getFileContent(String fileName) {
        return dataStore.get(fileName);
    }

    public void storeInfo(Torr.FileInfo fileInfo) {
        files.put(fileInfo.getFilename(), fileInfo);
    }

    public boolean isStored(String fileName) {
        return dataStore.containsKey(fileName);
    }


    private byte[] getMd5(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(content);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private ByteString getConvertedMd5(byte[] content) {
        byte[] md5 = getMd5(content);
        if (md5 == null) {
            return null;
        }
        return ByteString.copyFrom(md5);
    }

    private List<Torr.ChunkInfo> getChunks(byte[] data) {
        List<Torr.ChunkInfo> chunks = new LinkedList<>();

        int index = 0;
        int size = 1024;
        while (index * size < data.length) {
            int localsize = size;
            if (index * size + size > data.length) {
                localsize = data.length - index * size;
            }
            byte[] subarr = Arrays.copyOfRange(data, index * size, index * size + localsize);
            Torr.ChunkInfo chunkInfo = Torr.ChunkInfo.getDefaultInstance()
                    .newBuilderForType()
                    .setIndex(index)
                    .setSize(localsize)
                    .setHash(getConvertedMd5(subarr))
                    .build();
            chunks.add(chunkInfo);
            index += 1;
        }

        return chunks;
    }
}
