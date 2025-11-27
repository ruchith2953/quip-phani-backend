package com.quip.coa.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.UpdateResult;
import com.quip.coa.config.MongoClientSingleton;
import com.quip.coa.jsonexcel.TenantConfigJsonParserService;
import com.quip.coa.utilities.Constants;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DataVersionService {

    @Autowired
    private TenantConfigJsonParserService readjsonfile;

    private static final ObjectMapper objectMapper=new ObjectMapper();

    public void updateVersionData(Document componentDocument,String clientName, String userName) {
        MongoCollection<Document> versionCollection = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("versionCollection");

        String id=componentDocument.get("id").toString();
        Document filterById=new Document("id",id);
        String type=componentDocument.get("type").toString();
        filterById.append("type",type);

        // after filling db with all versions, to insert one more we override the existing docs
        if (versionCollection.countDocuments(filterById)== Constants.NUMBER_OF_VERSIONS){
            // delete the last version created
            deleteVersion(versionCollection,filterById);
        }

        // adding the new document to versionCollection
        componentDocument.put("user",userName);
        componentDocument.put("timeStamp",System.currentTimeMillis());
        versionCollection.insertOne(componentDocument);
    }

    private void deleteVersion(MongoCollection<Document> versionCollection, Document filterById) {

        // get all document's data sort in ascending order using id
        FindIterable<Document> documentsById=versionCollection.find(filterById).sort(Sorts.ascending("timeStamp"));

        //delete the first data
        Document latestDocument=documentsById.first();
        if (latestDocument==null){
            return;
        }
        versionCollection.deleteOne(latestDocument);
    }

    public Map<String, Object> displayData(String clientName){
        // fetch document from versionCollection and remove id
        MongoCollection<Document> versionCollection = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("versionCollection");
        FindIterable<Document> componentDocuments= versionCollection.find();

        List<Document> documents = componentDocuments.into(new ArrayList<>());
        Map<String, Object> combinedResponse = new LinkedHashMap<>();

        // iterate over doc and put doc id as key  and response document
        for (Document document: documents){
            String currentDocumentId=document.get("_id").toString();
            String id=document.get("id").toString();
            String type=document.get("type").toString();
            String timeStamp=document.get("timeStamp").toString();
            document.remove("id");

            // get mapping for the document and add to that
            Document innerData = new Document();

            // adding few fields manually to documents
            innerData.put("currentDocumentId",currentDocumentId);
            innerData.put("documentId",id);
            innerData.put("timeStamp",timeStamp);

            // remove document _id from document
            document.remove("_id");

            // adding rest all fields using mapping file
            filterDataWithMappings(clientName, document,type,innerData);

            // check if the type exists
            Map<String, List<Document>> typeMap = (Map<String, List<Document>>) combinedResponse.computeIfAbsent(type, k -> new LinkedHashMap<>());

            // if yes then take respective id related to it and add that doc the id
            typeMap.computeIfAbsent(id, k -> new ArrayList<>()).add(innerData);
        }
        return combinedResponse;
    }


    public void filterDataWithMappings(String clientName, Document componentDocument, String type,Document innerData ){
        Map<String, Map<String, Object>> components=getMapping(clientName,type);
        Map<String, Object> componentProperties = readjsonfile.retrieveComponentProperties(components, componentDocument);
        if (componentProperties.isEmpty()) {
            // return app response
            return;
        }

        // now using componentProperties will have to check mappings and add data
        for (String key: componentProperties.keySet()){
            String matchKey=getComponentDocumentKey(componentDocument,componentProperties.get(key).toString());

            if (matchKey!=null){
                innerData.put(key, componentDocument.get(matchKey));
            }
            else {
                innerData.put(key, "Property is missing");
            }
        }
    }

    public Map<String, Map<String, Object>> getMapping(String clientName,String type){
        // fetching tenantConfig from database
        Document tenantConfigDoc = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("tenantConfig")
                .find().first();

        if (tenantConfigDoc!=null) {
            JsonNode tenantConfigComponentsData = objectMapper.convertValue(tenantConfigDoc.get(type), JsonNode.class);
            // consists mappings for all components
            return objectMapper.convertValue(tenantConfigComponentsData, new TypeReference<Map<String, Map<String, Object>>>() {
            });
        }
        return null;
    }

    private String getComponentDocumentKey(Document resultDocument, String key) {
        for (String componentDocumentKey : resultDocument.keySet()) {
            String aemKeyName=componentDocumentKey.contains("|")? componentDocumentKey.substring(0,componentDocumentKey.indexOf("|")): componentDocumentKey;
            if (StringUtils.equalsIgnoreCase(aemKeyName.trim(),key)) {
                return componentDocumentKey;
            }
        }
        return null;
    }

    // revert
    public Map<String,String> revertBack(Document filterById,String documentId, String clientName){

        Map<String, String> response = new LinkedHashMap<>();

        // fetch existing doc from valid component
        ObjectId objectId=new ObjectId(documentId);
        // get xf data
        MongoCollection<Document> componentCollection = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("component");

        // get versionCollection
        MongoCollection<Document> versionCollection = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("versionCollection");

        // fetch document from versionCollection and exclude _id, userName,type and timeStamp
        Document componentDocument= versionCollection.find(filterById).projection(Projections.exclude("_id","user","type","timeStamp")).first();

        if (componentDocument==null){
            response.put("status","failed");
            response.put("message","no previous version available for component id: "+documentId);
            return response;
        }

        // update component document with _id
        componentDocument.put("_id",objectId);
        // remove id
        componentDocument.remove("id");

        UpdateResult result=componentCollection.updateOne(new Document("_id",objectId),new Document("$set",componentDocument));

        if (result.getModifiedCount()>0){
            response.put("status","success");
            response.put("message","previous version updated for component id: "+documentId);
            return response;
        }
        response.put("status","failed");
        response.put("message","couldn't update previous version for component id: "+documentId);
        return response;
    }
}