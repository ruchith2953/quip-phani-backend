package com.quip.coa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.result.UpdateResult;
import com.quip.coa.dbhelper.MongoClientSingleton;
import org.bson.Document;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TenantConfigService {

	private final ObjectMapper mapper = new ObjectMapper();

	public UpdateResult updateTenantConfig(Map<String, Object> configdoc, String clientName){
		String refNum = configdoc.get("refNum").toString();
		Document query = new Document();
		query.put("refNum", refNum);

		Document update = mapper.convertValue(configdoc, Document.class);
		Document setDoc = new Document();
		setDoc.put("$set", update);

		UpdateResult result = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("tenantConfig").updateOne(query, setDoc);
		return result;
	}
}