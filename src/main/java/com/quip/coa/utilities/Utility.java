package com.quip.coa.utilities;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Component
public class Utility {
    public String getClientName(String domain) {
        return domain.substring(8).split("\\.")[0];
    }

    public String getPagePath(String url) {
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            URI uri = new URI(url);
            return uri.getPath();
        } catch (URISyntaxException e) {
            return null;
        }
    }

}