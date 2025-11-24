package com.quip.coa.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Projections;
import com.quip.coa.dbConfig.MongoClientSingleton;
import com.quip.coa.jsonexcel.TenantConfigJsonParserService;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class UpdateComponentDataService {

    @Autowired
    private TenantConfigJsonParserService tenantConfigJsonParserService;
    @Autowired
    private DataVersionService dataVersionService;

    public static final ObjectMapper objectMapper=new ObjectMapper();

    public Map<String, String> updateComponent(String clientName, Map<String, String> document, String userName) throws Exception {
        Map<String, String> response = new LinkedHashMap<>();

        // user restriction
        Document query = new Document("_id", -1);
        Document activity = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("activityInfo").find().sort(query).first();
        userName=userName.contains("@")? userName.substring(0,userName.indexOf("@")): userName;
        if (activity==null || !Objects.equals(activity.get("userName"),userName)){
            response.put("status", "failed");
            response.put("message", "The domain '" + clientName + "' is owned by '" + activity.get("userName").toString() + "'. Kindly reach out to the domain owner for further assistance");
            response.put("response", userName+": you do not have permission to update this domain");
            return response;
        }

        Document toBeUpdated=new Document();

        Document tenantConfigDoc = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("tenantConfig")
                .find().first();

        if (tenantConfigDoc==null){
            response.put("status", "failed");
            response.put("message", "No Master Mapping Data found for client: "+clientName);
            return response;
        }
        MongoCollection<Document> clientCollection = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("component");

        ObjectId id = new ObjectId(document.get("id"));
        Document updateDocument = new Document(document);
        updateDocument.remove("id");

        Document filterById = new Document("_id", id);

        Document componentDocument=clientCollection.find(filterById).projection(Projections.exclude("_id")).first();

        if (componentDocument!=null){
            String componentName=componentDocument.get("componentName").toString();

            JsonNode tenantConfigComponent=objectMapper.convertValue(tenantConfigDoc.get("components"), JsonNode.class);

            Map<String, Map<String, Object>> components = objectMapper.convertValue(tenantConfigComponent, new TypeReference<Map<String, Map<String, Object>>>() {
            });

            Map<String, Object> componentPropertiesList = tenantConfigJsonParserService.retrieveComponentProperties(components, componentDocument);

            JsonNode mappingData=objectMapper.convertValue(componentPropertiesList,JsonNode.class);

            if(mappingData==null){
                response.put("status", "failed");
                response.put("message", "No Mapping Values found for component: " + componentName);
                return response;
            }

            for (String key: updateDocument.keySet()){
                String mappingValue=mappingData.get(key).asText();

                String getComponentKey=getComponentDocumentKey(componentDocument,mappingValue);
                if (getComponentKey!=null){
                    toBeUpdated.put(getComponentKey,updateDocument.get(key));
                }
            }
            toBeUpdated.put("modified","true");
            Document updateDoc = new Document("$set", toBeUpdated);

            // mongo versioning
            componentDocument.put("id",document.get("id"));
            componentDocument.put("type","components");
            dataVersionService.updateVersionData(componentDocument,clientName,userName);

            long result = clientCollection.updateOne(filterById,updateDoc).getModifiedCount();
            if (result > 0) {
                response.put("status", "success");
                response.put("message", "Document updated successfully with the id: " + id);
                return response;
            }
            response.put("status", "failed");
            response.put("message", "No field got updated");
            return response;
        }
        response.put("status", "failed");
        response.put("message", "No document matched with the id: " + id);
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