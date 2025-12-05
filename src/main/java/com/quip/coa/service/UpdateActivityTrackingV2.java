package com.quip.coa.service;

import com.mongodb.client.MongoCollection;
import com.quip.coa.dbhelper.MongoClientSingleton;
import org.bson.Document;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Service
public class UpdateActivityTrackingV2 {

    public boolean updateActivity(String userName, String domain, String activityName, String clientName, String activityType) {
    	//
        MongoCollection<Document> activityCollection = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("activityInfo");
        Document query = new Document();
        query.append("activityType", activityType);
        query.append("userName", userName);
        Document activity = activityCollection.find(query).sort(new Document("_id", -1)).first();
        if (activity != null && Objects.equals(activity.get("userName"), userName)) {
            Document updateDoc = new Document();
            updateDoc.put("activityName", activityName);
            updateDoc.put("updatedDate",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
            if (Objects.equals(activityName,"aem_update_success")) {
                updateDoc.put("activityCycle", "done");
            }
            Document update = new Document("$set", updateDoc);
            activityCollection.updateOne(activity, update);
            return true;
        }
        return false;
    }
}