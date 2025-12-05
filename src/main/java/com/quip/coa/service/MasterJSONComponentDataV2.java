package com.quip.coa.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Projections;
import com.quip.coa.dbhelper.MongoClientSingleton;
import com.quip.coa.helper.FetchCompPropFromTenantConfig;
import com.quip.coa.helper.Utility;
import com.quip.coa.jsonexcel.Readjsonfile;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MasterJSONComponentDataV2 {

    @Autowired
    private AEMDataConsumer aemDataConsumer;
    @Autowired
    private Utility utility;
    @Autowired
    private FetchCompPropFromTenantConfig fetchCompPropFromTenantConfig;
    @Autowired
    private Readjsonfile readjsonfile;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    // modified child flag component
    private boolean flag=false;

    public Map<String,Object> convertMasterJsonData(String clientName) {
        Document masterJsonData = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("masterJson")
                .find().first();

        if (masterJsonData==null){
            Map<String,Object> masterJsonComponentsData=new HashMap<>();
            masterJsonComponentsData.put("status","failed");
            masterJsonComponentsData.put("message","Data is not ingested. Please ingest the data.");
            return masterJsonComponentsData;
        }
        masterJsonData.remove("_id");

        MongoCollection<Document> componentsCollectionData = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("component");

        MongoCollection<Document> invalidComponentsCollectionData = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("invalid_component");

        Map<String, List<Document>> componentsData = new HashMap<>();

        Map<String,Object> components=objectMapper.convertValue(masterJsonData.get("components"), new TypeReference<Map<String, Object>>() {});

        for(String componentKey: components.keySet()) {
            ArrayList<String> componentData=objectMapper.convertValue(components.get(componentKey), new TypeReference<ArrayList<String>>() {});
            List<Document> componentsList = new ArrayList<>();

            for (String objectId : componentData) {
                // query to fetch data from database
                Document component = componentsCollectionData.find(new Document("_id", new ObjectId(objectId))).projection(Projections.exclude("_id", "recordType")).first();

                if (component != null) {
                    List<Document> processedComponentData = processComponent(component, clientName, componentsCollectionData);
                    componentsList.addAll(processedComponentData);
                } else {
                    Document invalidComponent = invalidComponentsCollectionData.find(new Document("_id", new ObjectId(objectId))).projection(Projections.exclude("_id", "recordType")).first();
                    if (invalidComponent != null) {
                        List<Document> processedComponentData = processComponent(invalidComponent, clientName, componentsCollectionData);
                        componentsList.addAll(processedComponentData);
                    }
                }
            }
            componentsData.put(componentKey, componentsList);
        }

        MongoCollection<Document> aemformCollectionData = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("aemform_documents");
        Map<String, List<Document>> formsData = new HashMap<>();
        Map<String, Object> forms = objectMapper.convertValue(masterJsonData.get("forms"), new TypeReference<Map<String, Object>>() {
        });

        List<Document> formList = new ArrayList<>();
        ArrayList<String> formIDList = objectMapper.convertValue(forms.get("aemform"), new TypeReference<ArrayList<String>>() {
        });

        for (String objectId : formIDList) {
            // query to fetch data from database
            Document formDoc = aemformCollectionData.find(new Document("_id", new ObjectId(objectId))).projection(Projections.exclude("_id")).first();
            if (formDoc != null) {
                formList.addAll(processComponent(formDoc, clientName, aemformCollectionData));// processing the forms document
            }
        }
        formsData.put("aemform", formList);

        masterJsonData.put("forms", formsData);
        masterJsonData.put("components", componentsData);

        // if in case want to store the response to database for backup
        //MongoClientSingleton.getClient().getDatabase(clientName).getCollection("masterJsonComponentsData").insertOne(new Document("components", componentsData));
        return masterJsonData;
    }


    private List<Document> processComponent(Document component, String clientName, MongoCollection<Document> componentsCollectionData) {
        // to store the processed component
        List<Document> processedComponents = new ArrayList<>();

        // processed component
        Document processedComponent = new Document(component);

        // Check for child component
        List<String> childComponentsList = childComponentsList(component, clientName);

        // if child exists
        if (!childComponentsList.isEmpty()) {
            // to store child documents
            List<Document> childDocuments = new ArrayList<>();

            // iterate over each child component key
            for (String childComponentKey : childComponentsList) {

                // get the key that will match with key in component
                String childComponentMatchKey = getResultDocKey(component, childComponentKey);

                // check if component has that key
                if (component.containsKey(childComponentMatchKey)) {

                    // get doc array of child components
                    Object componentDataObject = component.get(childComponentMatchKey);
                    ArrayNode childComponentDocumentIds = objectMapper.convertValue(componentDataObject, ArrayNode.class);

                    // iterate over each doc id from child components docs
                    for (int i = 0; i < childComponentDocumentIds.size(); i++) {
                        String childComponentDocumentId = childComponentDocumentIds.get(i).asText();
                        // retrieve the child component
                        Document childComponentData = componentsCollectionData.find(new Document("_id", new ObjectId(childComponentDocumentId))).projection(Projections.exclude("_id","recordType","path|Path")).first();

                        if (childComponentData != null) {

                            // check if child data is updated or not
                            if (childComponentData.containsKey("modified") && Objects.equals(childComponentData.get("modified").toString(),"true")){
                                flag=true;
                                childComponentData.remove("modified");
                            }

                            // recursive method to check and fetch if component has sub child/child of sub child... and so on
                            List<Document> processedChildDocuments = processComponent(childComponentData, clientName, componentsCollectionData);
                            // adding if sub children are present
                            for(Document subChildDoc: processedChildDocuments){
                                subChildDoc.remove("componentName");
                            }

                            childComponentData.remove("componentName");
                            childDocuments.addAll(processedChildDocuments);
                        }
                    }

                    // add the processed child documents to the parent component
                    if (!childDocuments.isEmpty()) {
                        processedComponent.put(childComponentMatchKey, childDocuments);
                    }
                }
            }
        }

        // if no child and parent is updated
        if (processedComponent.containsKey("modified") && Objects.equals(processedComponent.get("modified").toString(),"true")){
            flag=true;
            processedComponent.remove("modified");
        }
        // updating the root component if root/ child/ sub child or so on... is modified
        if (flag){
            if (processedComponent.containsKey("changed|Changed")){
                processedComponent.put("changed|Changed","true");
                flag=false;
            }
        }

        processedComponent.remove("componentName");
        // add the processed component to the list
        processedComponents.add(processedComponent);
        return processedComponents;
    }

    public List<String> childComponentsList(Document component,String clientName){
        String compName=component.getString("componentName");
        String compKeyAemName = compName.contains("|")? compName.substring(0, compName.indexOf("|")):compName;
        return  aemDataConsumer.getCompChildList(compKeyAemName,clientName);
    }


    public String getResultDocKey(Document resultDocument, String key) {
        String matchKey = null;
        if (StringUtils.isNotEmpty(key)) {
            for (String resultDocKey : resultDocument.keySet()) {
                if (StringUtils.containsIgnoreCase(resultDocKey, key)) {
                    matchKey = resultDocKey;
                    break;
                }
            }
        }
        return matchKey;
    }
}
