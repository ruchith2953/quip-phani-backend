package com.quip.coa.utilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.net.URISyntaxException;
import java.util.Map;

@Service
public class HttpUtilities {

    private static final Logger logger = LoggerFactory.getLogger(HttpUtilities.class);

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CloseableHttpClient httpClient;

    // ---- HTTP GET ----
    public JsonNode executeGetRequest(HttpGet httpGet) throws URISyntaxException {
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            return objectMapper.readTree(response.getEntity().getContent());
        } catch (Exception e) {
            logger.error("Error executing GET request to {}: {}", httpGet.getURI(), e.getMessage());
            return null;
        }
    }

    public JsonNode httpGetResponse(String connectionUrl, Map<String, String> headers) {
        try {
            HttpGet httpGet = new HttpGet(connectionUrl);
            headers.forEach(httpGet::setHeader);
            return executeGetRequest(httpGet);
        } catch (Exception e) {
            logger.error("Error creating GET request for URL {}: {}", connectionUrl, e.getMessage());
            return null;
        }
    }

    // ---- HTTP POST ----
    public JsonNode executePostRequest(HttpPost httpPost) throws URISyntaxException {
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            return objectMapper.readTree(response.getEntity().getContent());
        } catch (Exception e) {
            logger.error("Error executing POST request to {}: {}", httpPost.getURI(), e.getMessage());
            return null;
        }
    }

    public JsonNode httpPostResponse(String connectionUrl, String jsonDataString, Map<String, String> headers) {
        try {
            HttpPost httpPost = new HttpPost(connectionUrl);
            httpPost.setEntity(new StringEntity(jsonDataString));
            headers.forEach(httpPost::setHeader);
            return executePostRequest(httpPost);
        } catch (Exception e) {
            logger.error("Error creating POST request for URL {}: {}", connectionUrl, e.getMessage());
            return null;
        }
    }
}
