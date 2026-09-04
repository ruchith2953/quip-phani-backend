package com.quip.phani.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.quip.phani.dbhelper.MongoClientSingleton;
import com.quip.phani.helper.Utility;
import com.quip.phani.jsonexcel.ReadJsonFile;
import com.quip.phani.utilities.Constants;
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
public class ReturnAemFormsData {

    @Autowired
    private Utility utility;
    @Autowired
    private ReadJsonFile readjsonfile;

    private static final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> exportFormsToJSON(String domainUrl) throws Exception {

        // extract clientName from URL
        String clientName = utility.getClientName(domainUrl);

        // fetching tenantConfig from database
        Document tenantConfigDoc = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("tenantConfig").find().first();
        Map<String, Object> outerData = new LinkedHashMap<>();
        List<Map<String, Object>> innerList = new ArrayList<>();

        if (tenantConfigDoc!=null){
            JsonNode tenantConfigFormsData=mapper.convertValue(tenantConfigDoc.get("formsData"), JsonNode.class);
            // consists mappings for all components
            Map<String, Map<String, Object>> forms = mapper.convertValue(tenantConfigFormsData, new TypeReference<Map<String, Map<String, Object>>>() {
            });

            // getting forms data from aemform_documents collection
            MongoCollection<Document> aemFormCollectionData = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("aemform_documents");

            // getting masterJson doc
            Document masterJsonData = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("masterJson").find().first();

            if (masterJsonData != null) {
                // getting aemform list of doc id's from masterJson collection
                JsonNode formsData = mapper.convertValue(masterJsonData.get("forms"), JsonNode.class);
                List<String> aemformList = mapper.convertValue(formsData.get("aemform"), new TypeReference<List<String>>() {
                });

                // iterating over aemform list
                for (String documentId : aemformList) {
                    // getting formDoc from aemform_documents collection
                    Document formDoc = aemFormCollectionData.find(new Document("_id", new ObjectId(documentId))).first();

                    if (formDoc != null) {
                        // getting forms data mapping
                        Map<String, Object> formsMappings = readjsonfile.retrieveComponentProperties(forms, formDoc);
                        if (formsMappings.isEmpty()) {
                            continue;
                        }

                        Map<String, Object> innerData = new LinkedHashMap<>();
                        innerData.put("documentId",documentId);

                        // now using formsMappings will have to check mappings and add data
                        for (String key: formsMappings.keySet()){
                            String matchKey=getFormDocumentKey(formDoc,formsMappings.get(key).toString());
                            innerData.put(key, formDoc.get(matchKey));
                        }

                        innerList.add(innerData);
                    }
                }
            }
        }
        outerData.put(Constants.FIELD_FORMS_DATA, innerList);
        return outerData;
    }

    private String getFormDocumentKey(Document resultDocument, String key) {
        for (String formDocumentKey : resultDocument.keySet()) {
            String aemKeyName=formDocumentKey.contains("|")? formDocumentKey.substring(0,formDocumentKey.indexOf("|")): formDocumentKey;
            if (StringUtils.equalsIgnoreCase(aemKeyName.trim(),key)) {
                return formDocumentKey;
            }
        }
        return null;
    }
}