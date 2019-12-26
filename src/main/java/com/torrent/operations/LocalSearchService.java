package com.torrent.operations;

import com.torrent.gen.Torr;
import com.torrent.service.FileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
public class LocalSearchService {

    private static Logger LOG = LoggerFactory.getLogger(LocalSearchService.class);

    @Autowired
    private FileStorage storage;

    public Torr.LocalSearchResponse handle(Torr.LocalSearchRequest request) {
        String regex = request.getRegex();
        if (regex == null) {
            return getResponse(Torr.Status.MESSAGE_ERROR);
        }

        try {
            List<Torr.FileInfo> result = getMatches(regex);
            return getResponse(Torr.Status.SUCCESS, result);
        } catch (PatternSyntaxException exc) {
            return getResponse(Torr.Status.MESSAGE_ERROR);
        }
    }

    private List<Torr.FileInfo> getMatches(String regex) {
        Map<String, Torr.FileInfo> files = storage.getFiles();
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

    private Torr.LocalSearchResponse getResponse(Torr.Status status) {
        return Torr.LocalSearchResponse.getDefaultInstance()
                .newBuilderForType()
                .setStatus(status)
                .build();
    }

    private Torr.LocalSearchResponse getResponse(Torr.Status status, List<Torr.FileInfo> matches) {
        return Torr.LocalSearchResponse.getDefaultInstance()
                .newBuilderForType()
                .setStatus(status)
                .addAllFileInfo(matches)
                .build();
    }
}
