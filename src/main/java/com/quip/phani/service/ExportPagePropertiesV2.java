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
public class ExportPagePropertiesV2 {

    @Autowired
    private Utility utility;
    @Autowired
    private ReadJsonFile readjsonfile;

    private static final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> exportToJSON(String userName, String domainUrl) throws Exception {

        // extract clientName from URL
        String clientName = utility.getClientName(domainUrl);

        // fetching tenantConfig from database
        Document tenantConfigDoc = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("tenantConfig")
                .find().first();
        Map<String, Object> outerData = new LinkedHashMap<>();
        List<Map<String, Object>> innerList = new ArrayList<>();

        if (tenantConfigDoc!=null){
            JsonNode tenantConfigComponentsData=mapper.convertValue(tenantConfigDoc.get("pageData"), JsonNode.class);
            // consists mappings for all components
            Map<String, Map<String, Object>> components = mapper.convertValue(tenantConfigComponentsData, new TypeReference<Map<String, Map<String, Object>>>() {
            });

            // getting components data from component collection
            MongoCollection<Document> componentsCollectionData = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("component");

            // getting masterJson doc
            Document masterJsonData = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("masterJson")
                    .find().first();

            if (masterJsonData != null) {

                // getting page properties list of doc id's from masterJson collection
                JsonNode pageData = mapper.convertValue(masterJsonData.get("components"), JsonNode.class);
                List<String> fullWidthComponentPageList = mapper.convertValue(pageData.get("full-width-content-page"), new TypeReference<List<String>>() {
                });

                // iterating over page properties list
                for (String documentId : fullWidthComponentPageList) {
                    // getting component from component collection
                    Document component = componentsCollectionData.find(new Document("_id", new ObjectId(documentId))).first();

                    if (component != null && component.get("componentName") != null) {
                        System.out.println(component.get("componentName"));
                        // getting page properties mapping
                        Map<String, Object> componentProperties = readjsonfile.retrieveComponentProperties(components, component);
                        if (componentProperties.isEmpty()) {
                            continue;
                        }

                        Map<String, Object> innerData = new LinkedHashMap<>();
                        innerData.put("documentId",documentId);

                        // now using componentProperties will have to check mappings and add data
                        for (String key: componentProperties.keySet()){
                            String matchKey=getComponentDocumentKey(component,componentProperties.get(key).toString());
                            innerData.put(key, component.get(matchKey));
                        }

                        innerList.add(innerData);
                    }
                }
            }
        }
        outerData.put(Constants.FIELD_PAGE_DATA, innerList);
        return outerData;
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
}