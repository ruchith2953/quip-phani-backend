package com.quip.coa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoDatabase;
import com.quip.coa.dbConfig.MongoClientSingleton;
import com.quip.coa.utilities.Constants;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class SendDataBackToAEMService {

    public static final ObjectMapper objectMapper=new ObjectMapper();
    @Autowired
    private Environment environment;
    @Autowired
    private UpdateActivityTracking updateActivityTracking;

    public JsonNode connectToAEM(JsonNode componentsData,String domainName,String domainUrl,String userEmail){
        Map<String,String> response=new HashMap<>();
        try{
            String componentDataString=objectMapper.writeValueAsString(componentsData);
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost postRequest = new HttpPost(Constants.UPDATE_DATA_TO_AEM);
            String credentials = environment.getProperty("AEM_PAGE_USERNAME")+ ":" + environment.getProperty("AEM_PAGE_PASSWORD");
            String encodedAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            postRequest.setHeader(Constants.AUTHORIZATION_HEADER, "Basic " + encodedAuth);
            postRequest.setHeader(Constants.CONTENT_TYPE, Constants.ACCEPT_JSON);
            StringEntity entity = new StringEntity(componentDataString);
            postRequest.setEntity(entity);

            try (CloseableHttpResponse closeableHttpResponse = httpClient.execute(postRequest)) {
                String responseString = EntityUtils.toString(closeableHttpResponse.getEntity());
                JsonNode data=objectMapper.readTree(responseString);

                String activityName="aem_update_success";
                if (updateActivityTracking.updateActivity(userEmail,domainUrl,activityName,domainName,Constants.COMPONENT)){
                    //after sending data to aem delete it
                    MongoDatabase mongoDatabase = MongoClientSingleton.getClient().getDatabase(domainName);

                    String[] collections = {
                            Constants.COMPONENTS_COLLECTION,
                            Constants.METADATA_COLLECTION
                    };

                    for (String collectionName : collections) {
                        mongoDatabase.getCollection(collectionName).deleteMany(new Document());
                    }
                    return objectMapper.convertValue(data,JsonNode.class);
                }
                response.put("status","failed");
                response.put("message","No activity found for user: "+userEmail);
                return objectMapper.convertValue(response, JsonNode.class);
            }
            //op stream bytes
            catch (Exception exception){
                response.put("status","failed");
                response.put("message",exception.getClass().getName());
                response.put("response",exception.getMessage());
                return objectMapper.convertValue(response, JsonNode.class);
            }
        }
        catch (Exception exception){
            response.put("status","failed");
            response.put("message","couldn't to connect and post data to update page.");
            response.put("response",exception.getMessage());
            return objectMapper.convertValue(response, JsonNode.class);
        }
    }
}