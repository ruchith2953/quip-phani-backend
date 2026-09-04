package com.quip.phani.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.quip.phani.utilities.Constants;
import com.quip.phani.utilities.HttpUtilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class ComponentExtractJson {
    @Autowired
    ActivityTracking activityTracking;
    @Autowired
    private Environment environment;
    @Autowired
    private HttpUtilities httpUtilities;

    public JsonNode componentExtractJson(String userEmail, String domainUrl, String domainName) {

        Map<String, String> headers = new HashMap<>();
        String credentials = environment.getProperty("AEM_PAGE_USERNAME") + ":" + environment.getProperty("AEM_PAGE_PASSWORD");
        String encodedAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
//        headers.put(Constants.AUTHORIZATION_HEADER, "Basic " + encodedAuth);
        headers.put(Constants.CONTENT_TYPE, Constants.ACCEPT_JSON);
        headers.put("Authorization", "Basic cXVpcC1zZXJ2aWNlLXVzZXI6cXVpcFNlcnZpY2VVc2VyQDM=");

        headers.put("Cookie", "AWSALB=5bGdxsFKy00Tyid1nYR8w0IhA1bfrKwRqYMN+m+kA4xJKPEhwk+10pZOlpLdCX6FurtqWFscrw1IUyxyCEr2iHJ2Xzg73D/ncP9g6ctvPYhVK4w/K7Zqzgi5Oy+a; AWSALBCORS=5bGdxsFKy00Tyid1nYR8w0IhA1bfrKwRqYMN+m+kA4xJKPEhwk+10pZOlpLdCX6FurtqWFscrw1IUyxyCEr2iHJ2Xzg73D/ncP9g6ctvPYhVK4w/K7Zqzgi5Oy+a; cq-authoring-mode=TOUCH");

        String apiUrl = String.format(Constants.CONTENT_EXTRACT_BASE_URL, domainUrl, Constants.CONTENT_EXTRACT_MAPPING);

        // Execute GET request
        JsonNode responseJson = httpUtilities.httpGetResponse(apiUrl, headers);
        if (responseJson == null) {
            throw new RuntimeException("Failed to fetch component JSON from API: " + domainUrl);
        }

        // Track activity
        activityTracking.addActivity(userEmail, domainUrl, Constants.ACTIVITY_TYPE, domainName, Constants.COMPONENT);

        return responseJson;
    }
}