package com.quip.phani.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.quip.phani.dbhelper.MongoClientSingleton;
import org.bson.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AemFormService {

    private ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> aemformData(Map<String, Object> aemData, String clientName) {
        Map<String, Object> formsData = mapper.convertValue(aemData.get("forms"), Map.class);
        List<String> aemFormIds = new ArrayList<>(); // List to store form IDs

        if (formsData != null && formsData.containsKey("aemform")) {
            MongoCollection<Document> formsCollection = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("aemform_documents");

            List<Map<String, Object>> aemFormList = mapper.convertValue(formsData.get("aemform"), ArrayList.class);
            if (!aemFormList.isEmpty()) {
                for (Map<String, Object> formData : aemFormList) {
                    Document document = mapper.convertValue(formData, Document.class);
                    document.put("componentName", "aemform");//adding component Name field in every document.
                    String id = formsCollection.insertOne(document).getInsertedId().asObjectId().getValue().toString();
                    aemFormIds.add(id);
                }
            }
        }
        Map<String, Object> aemformIdsData = new HashMap<>();
        aemformIdsData.put("aemform", aemFormIds);
        aemData.put("forms", aemformIdsData);

        return aemData;
    }

}
