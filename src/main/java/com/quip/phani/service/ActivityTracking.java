package com.quip.phani.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.quip.phani.model.Activity;
import com.quip.phani.mongoUtility.MongoUtility;
import com.quip.phani.utilities.Constants;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

@Service
public class ActivityTracking {

	@Autowired
	private MongoUtility mongoUtility;

	public void addActivity(String userName, String domain, String activityName,String clientName, String activityType) {
		MongoCollection<Document> activityCollection = mongoUtility.getCollection(clientName,Constants.ACTIVITY_INFO_COLLECTION);
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
		MongoCollection<Document> activityCollection = mongoUtility.getCollection(clientName,Constants.ACTIVITY_INFO_COLLECTION);
		Document query = new Document();
        query.append("activityType", activityType);
        query.append("userName", userName);
		Document activity = activityCollection.find(query).first();
		if (activity != null) {
			Document updateDoc = new Document();
			updateDoc.put("activityName", activityName);
			updateDoc.put("updatedDate",LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
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

	public HashMap<String, Object> initAemExtract(String userName, String domainUrl, String clientName) {
		try {
			HashMap<String, Object> result = new HashMap<String, Object>();
			Document activity_query = new Document();
			activity_query.put("domain", domainUrl);
			activity_query.put("activityType", "component");
			Document activityDoc = mongoUtility.getDocByQueryProjectionAndSort(clientName,Constants.ACTIVITY_INFO_COLLECTION,activity_query,new Document("_id", 0),new Document("_id", -1));
			ObjectMapper objectMapper = new ObjectMapper();
			Activity activity = objectMapper.convertValue(activityDoc, Activity.class);
			if (activity == null) {
				result.put("initAEM", true);
				result.put("message", "you can initiate AEM Data Ingestion");
			} else {
				if (activity.getUserName().equals(userName)) {
					if ("done".equals(activity.getActivityCycle())) {
						result.put("initAEM", true);
						result.put("message", "you can initiate AEM Data Ingestion");
					} else {
						result.put("initAEM", false);
						result.put("message", "Sorry, You have already initiated QUIP Ingestion, now you are at activity " + activity.getActivityName());
					}
				} else {
					if ("done".equals(activity.getActivityCycle())) {
						result.put("initAEM", true);
						result.put("message", "you can initiate AEM Data Ingestion");
					} else {
						result.put("initAEM", false);
						result.put("message", "Sorry , QUIP Ingestion is already initiated, please check with " + activity.getUserName());
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