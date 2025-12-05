package com.quip.coa.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.quip.coa.dbhelper.MongoClientSingleton;
import com.quip.coa.model.Activity;
import org.bson.Document;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

@Service
public class ActivityTracking {
	private static final String ACTIVITY_INFO_COLLECTION = "activityInfo";

	//private MongoClient mongoClient;
	//private MongoDatabase database;
	private MongoCollection<Document> activityCollection;

	public void addActivity(String userName, String domain, String activityName,String clientName, String activityType) {
		activityCollection = MongoClientSingleton.getClient().getDatabase(clientName).getCollection(ACTIVITY_INFO_COLLECTION);
		LocalDateTime currentDate = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
		String date = currentDate.format(formatter);

		Activity activity = new Activity();
		activity.setUserName(userName);
		activity.setDomain(domain);
		activity.setActivityName(activityName);
		activity.setCreatedDate(date);
		activity.setUpdatedDate(date);
		activity.setActivityCycle("inprogress");
		activity.setActivityType(activityType);
		ObjectMapper objectMapper = new ObjectMapper();
		Document activityDoc = objectMapper.convertValue(activity, Document.class);
		activityCollection.insertOne(activityDoc);
	}

	public void updateActivity(String userName, String domain, String activityName, String clientName, String activityType) throws JsonProcessingException {
		activityCollection = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("activityInfo");
		Document query = new Document();
        query.append("activityType", activityType);
        query.append("userName", userName);
		Document activity = activityCollection.find(query).first();
		if (activity != null) {
			Document updateDoc = new Document();
			updateDoc.put("activityName", activityName);
			updateDoc.put("updatedDate",
					LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
			if (activityName.equals("aem_update_success")) {
				updateDoc.put("activityCycle", "done");
			}
			Document update = new Document("$set", updateDoc);
			activityCollection.updateOne(query, update);
		} else {
			Activity newActivity = new Activity();
			newActivity.setUserName(userName);
			newActivity.setDomain(domain);
			newActivity.setActivityName(activityName);
			newActivity.setActivityType(activityType);
			LocalDateTime currentDate = LocalDateTime.now();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
			String date = currentDate.format(formatter);
			newActivity.setCreatedDate(date);
			newActivity.setUpdatedDate(date);
			newActivity.setActivityCycle("inprogress");
			ObjectMapper objectMapper = new ObjectMapper();
			String jsonString = objectMapper.writeValueAsString(newActivity);
			Document activityDocument = Document.parse(jsonString);
			activityCollection.insertOne(activityDocument);
		}
	}

	public HashMap<String, Object> initAemExtract(String userName, String domain, String clientName) {
		activityCollection = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("activityInfo");
		try {
			HashMap<String, Object> result = new HashMap<String, Object>();
			Document activity_query = new Document();
			activity_query.put("domain", domain);
			activity_query.put("activityType", "component");
			Document activityDoc = activityCollection.find(activity_query).sort(new Document("_id", -1))
					.projection(new Document("_id", 0)).first();
			ObjectMapper objectMapper = new ObjectMapper();
			Activity activity = objectMapper.convertValue(activityDoc, Activity.class);
			if (activity == null) {
				result.put("initAEM", true);
				result.put("message", "you can intiate AEM Data injestion");
			} else {
				if (activity.getUserName().equals(userName)) {
					if ("done".equals(activity.getActivityCycle())) {
						result.put("initAEM", true);
						result.put("message", "you can intiate AEM Data injestion");
					} else {
						result.put("initAEM", false);
						result.put("message", "Sorry, You have already initiated QUIP Ingestion, now you are at activity " + activity.getActivityName());
					}
				} else {
					if ("done".equals(activity.getActivityCycle())) {
						result.put("initAEM", true);
						result.put("message", "you can intiate AEM Data injestion");
					} else {
						result.put("initAEM", false);
						result.put("message", "Sorry , QUIP Injestion is already initiated, please check with " + activity.getUserName());
					}
				}
			}
			return result;
		} catch (Exception e) {
			HashMap<String, Object> errResult = new HashMap<String, Object>();
			errResult.put("initAEM", false);
			errResult.put("message", "An error occurred - " + e.getMessage());
			return errResult;
		}

	}
}