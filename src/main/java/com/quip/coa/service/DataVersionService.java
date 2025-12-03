package com.quip.coa.service;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.UpdateResult;
import com.quip.coa.dbConfig.MongoClientSingleton;
import com.quip.coa.utilities.Constants;
import com.quip.coa.utilities.MongoUtility;
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
     private MongoUtility mongoUtility;

    /** Stores a new version snapshot for a component document.*/
    public void updateVersionData(Document componentDocument, String clientName, String userName) {

        MongoCollection<Document> versionCollection =mongoUtility.getCollection(clientName,Constants.VERSION_COLLECTION);

        String id = componentDocument.get("id").toString();
        String type = componentDocument.get("type").toString();

        Document filter = new Document("id", id).append("type", type);

        // Remove the oldest version if exceeding limit
        long count = versionCollection.countDocuments(filter);
        if (count >= Constants.MAX_VERSIONS) {
            deleteOldestVersion(versionCollection, filter);
        }

        // Snapshot of component
        Document versionDoc = new Document(componentDocument);
        versionDoc.put("user", userName);
        versionDoc.put("timeStamp", System.currentTimeMillis());

        versionCollection.insertOne(versionDoc);
    }

    /** Deletes the oldest version entry based on timestamp. */
    private void deleteOldestVersion(MongoCollection<Document> versionCollection, Document filter) {
        Document oldest =versionCollection.find(filter).sort(Sorts.ascending("timeStamp")).first();

        if (oldest != null) {
            versionCollection.deleteOne(new Document("_id", oldest.getObjectId("_id")));
        }
    }

    /** Returns version history grouped by type and id. */
    public Map<String, Object> displayData(String clientName) {

        MongoCollection<Document> versionCollection = mongoUtility.getCollection(clientName,Constants.VERSION_COLLECTION);

        List<Document> versionDocs =versionCollection.find().into(new ArrayList<>());

        Map<String, Object> response = new LinkedHashMap<>();

        for (Document version : versionDocs) {
            String type = version.getString("type");
            String id = version.getString("id");
            String timeStamp = version.get("timeStamp").toString();

            Map<String, List<Document>> typeGroup = (Map<String, List<Document>>) response.computeIfAbsent(type, k -> new LinkedHashMap<>());

            Document cleaned = new Document(version);

            cleaned.put("currentDocumentId", cleaned.get("_id").toString());
            cleaned.put("documentId", id);
            cleaned.put("timeStamp", timeStamp);

            cleaned.remove("_id");
            cleaned.remove("id");
            cleaned.remove("type");

            typeGroup.computeIfAbsent(id, k -> new ArrayList<>()).add(cleaned);
        }
        return response;
    }

    /** Reverts a component document to a previous version. */
    public Map<String, String> revertBack(Document filterById, String documentId, String clientName) {

        Map<String, String> response = new LinkedHashMap<>();

        ObjectId objectId = new ObjectId(documentId);

        MongoCollection<Document> componentCollection = mongoUtility.getCollection(clientName,Constants.COMPONENTS_COLLECTION);

        Document existing = componentCollection.find(new Document("_id", objectId)).first();

        if (existing == null) {
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_MESSAGE, "Component not found with id: " + documentId);
            return response;
        }

        MongoCollection<Document> versionCollection = mongoUtility.getCollection(clientName,Constants.VERSION_COLLECTION);

        Document versionDoc =versionCollection.find(filterById).projection(Projections.exclude("_id", "user", "type", "timeStamp")).first();

        if (versionDoc == null) {
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_MESSAGE, "Previous version not found for id: " + documentId);
            return response;
        }

        versionDoc.put("_id", objectId);
        versionDoc.remove("id");

        UpdateResult update =componentCollection.updateOne(new Document("_id", objectId),new Document("$set", versionDoc));

        if (update.getModifiedCount() > 0) {
            response.put(Constants.FIELD_STATUS, Constants.STATUS_SUCCESS);
            response.put(Constants.FIELD_MESSAGE, "Component restored to previous version.");
        } else {
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_MESSAGE, "Failed to restore version.");
        }
        return response;
    }
}