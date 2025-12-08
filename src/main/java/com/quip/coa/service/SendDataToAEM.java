package com.quip.coa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quip.coa.mongoUtility.MongoUtility;
import com.quip.coa.utilities.Constants;
import com.quip.coa.utilities.HttpUtilities;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class SendDataToAEM {

    public static final ObjectMapper objectMapper=new ObjectMapper();
    @Autowired
    private Environment environment;
    @Autowired
    private UpdateActivityTracking updateActivityTracking;
    @Autowired
    private MongoUtility mongoUtility;
    @Autowired
    private HttpUtilities httpUtilities;

    public JsonNode connectToAEM(JsonNode componentsData,String clientName,String domain,String userName){
        Map<String,String> response=new HashMap<>();
        try{
            String componentDataString=objectMapper.writeValueAsString(componentsData);
            String credentials = environment.getProperty("AEM_PAGE_USERNAME")+ ":" + environment.getProperty("AEM_PAGE_PASSWORD");
            String encodedAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            Map<String,String> headersMap= new HashMap<>();
            headersMap.put("Content-Type", "application/json");
            headersMap.put("Authorization", "Basic " + encodedAuth);
            JsonNode data= httpUtilities.httpPostResponse(Constants.UPDATE_DATA_TO_AEM,componentDataString,headersMap);
               if (data == null){
                response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
                response.put(Constants.FIELD_MESSAGE, "Update aem response is null");
                return objectMapper.convertValue(response, JsonNode.class);
               }

                String activityName="aem_update_success";
                if (updateActivityTracking.updateActivity(userName,domain,activityName,clientName,"component")){
                    //after sending data to aem delete data from db
                    mongoUtility.deleteMany(clientName,Constants.COMPONENT_COLLECTION,new Document());
                    mongoUtility.deleteMany(clientName,Constants.MASTER_JSON_COLLECTION,new Document());
                    mongoUtility.deleteMany(clientName,Constants.VERSION_COLLECTION,new Document());

                    return objectMapper.convertValue(data,JsonNode.class);
                }
                response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
                response.put(Constants.FIELD_MESSAGE, "No activity found for user: "+userName);
                return objectMapper.convertValue(response, JsonNode.class);
        }
        catch (Exception exception){
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_MESSAGE,"couldn't to connect and post data to update page.");
            return objectMapper.convertValue(response, JsonNode.class);
        }
    }
}