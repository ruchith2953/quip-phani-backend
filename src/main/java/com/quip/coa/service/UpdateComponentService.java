package com.quip.coa.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.Projections;
import com.quip.coa.jsonexcel.ReadJsonFile;
import com.quip.coa.mongoUtility.MongoUtility;
import com.quip.coa.utilities.Constants;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class UpdateComponentService {

    @Autowired
    private ReadJsonFile readjsonfile;
    @Autowired
    private DataVersionService dataVersionService;
    @Autowired
    private MongoUtility mongoUtility;
   @Autowired
    private ObjectMapper objectMapper;

    public Map<String, String> updateComponent(String clientName, Map<String, String> document, String userName){
        Map<String, String> response = new LinkedHashMap<>();

        // user restriction
        Document activity = mongoUtility.findFirstSorted(clientName, Constants.ACTIVITY_INFO_COLLECTION,new Document("_id", -1));
        userName=userName.contains("@")? userName.substring(0,userName.indexOf("@")): userName;
        if (activity==null || !Objects.equals(activity.get("userName"),userName)){
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_MESSAGE, "The domain '" + clientName + "' is owned by '" + activity.get("userName").toString() + "'. Kindly reach out to the domain owner for further assistance");
            response.put(Constants.RESPONSE_FAILED, userName+": you do not have permission to update this domain");
            return response;
        }

        Document tenantConfigDoc = mongoUtility.getFirstDocument(clientName,Constants.TENANT_CONFIG_COLLECTION);
        if (tenantConfigDoc==null){
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_MESSAGE, "No Master Mapping Data found for client: "+clientName);
            return response;
        }

        ObjectId id = new ObjectId(document.get("id"));
        Document updateDocument = new Document(document);
        updateDocument.remove("id");

        Document componentDocument= mongoUtility.getDocByQueryAndProjection(clientName,Constants.COMPONENT_COLLECTION,new Document("_id", id),Projections.exclude("_id"));

        if (componentDocument!=null){
            String componentName=componentDocument.get("componentName").toString();

            JsonNode tenantConfigComponent=objectMapper.convertValue(tenantConfigDoc.get("components"), JsonNode.class);

            Map<String, Map<String, Object>> components = objectMapper.convertValue(tenantConfigComponent, new TypeReference<Map<String, Map<String, Object>>>() {
            });

            Map<String, Object> componentPropertiesList = readjsonfile.retrieveComponentProperties(components, componentDocument);

            JsonNode mappingData=objectMapper.convertValue(componentPropertiesList,JsonNode.class);

            if(mappingData==null){
                response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
                response.put(Constants.FIELD_MESSAGE, "No Mapping Values found for component: " + componentName);
                return response;
            }

            Document toBeUpdated=new Document();

            for (String key: updateDocument.keySet()){
                String mappingValue=mappingData.get(key).asText();
                String getComponentKey=getComponentDocumentKey(componentDocument,mappingValue);
                if (getComponentKey!=null){
                    toBeUpdated.put(getComponentKey,updateDocument.get(key));
                }
            }
            toBeUpdated.put("modified","true");

            // mongo versioning
            componentDocument.put("id",document.get("id"));
            componentDocument.put("type","components");
            long result = mongoUtility.updateDocument(clientName,Constants.COMPONENT_COLLECTION,new Document("_id", id),toBeUpdated).getModifiedCount();
            if (result > 0) {
                dataVersionService.updateVersionData(componentDocument,clientName,userName);
                response.put(Constants.FIELD_STATUS, Constants.STATUS_SUCCESS);
                response.put(Constants.FIELD_MESSAGE, "Document updated successfully with the id: " + id);
                return response;
            }
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_MESSAGE, "No field got updated");
            return response;
        }
        response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
        response.put(Constants.FIELD_MESSAGE, "No document matched with the id: " + id);
        return response;
    }

    private String getComponentDocumentKey(Document resultDocument, String key) {
        for (String componentDocumentKey : resultDocument.keySet()) {
            String aemKeyName=componentDocumentKey.contains("|")? componentDocumentKey.substring(0,componentDocumentKey.indexOf("|")): componentDocumentKey;

            if (StringUtils.equalsIgnoreCase(aemKeyName.trim(),key) ) {
                return componentDocumentKey;
            }
        }
        return null;
    }
}