package com.quip.coa.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.quip.coa.dbhelper.MongoClientSingleton;
import com.quip.coa.service.*;
import org.apache.commons.lang3.StringUtils;
import com.quip.coa.helper.Utility;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/component")
public class ComponentController {

	private static final Logger log = LoggerFactory.getLogger(ComponentController.class);
	@Autowired
	private AEMDataConsumer aemDataConsumer;
	@Autowired
	private ActivityTracking activityTracking;
	@Autowired
	private Utility utility;
	@Autowired
	private ExportComponentsToJSON exportComponentsToJSON;
	@Autowired
	private UpdateComponentService updateComponentService;
	@Autowired
	private SendDataToAEMV2 sendDataToAEMV2;
	@Autowired
	private MasterJSONComponentDataV2 masterJSONComponentDataV2;
	@Autowired
	private DataVersionService dataVersionService;
	@Autowired
	private ConvertMappingFileToJsonV2 convertMappingFileToJsonV2;

	private static final ObjectMapper mapper = new ObjectMapper();

	@GetMapping("/ingestAemData")
	public Map<String, Object> ingestAemData(@RequestParam String userName, @RequestParam String domainUrl) {

		String domainName=utility.getClientName(domainUrl);

		Map<String, Object> result = new HashMap<>();
		Map<String, Object> activityStatusMap = activityTracking.initAemExtract(userName, domainUrl,domainName);
		try {
			boolean initAem = (boolean) activityStatusMap.get("initAEM");
			if (!initAem) {
				result.put("message", activityStatusMap.get("message").toString());
				result.put("result", Collections.EMPTY_MAP);
				return result;
			} else {
				Map<String, Object> aemData = aemDataConsumer.processAEMData(domainUrl, userName, domainName);
				Document masterJson = mapper.convertValue(aemData, Document.class);
				// adding user email to AEM meta data
				Document metadata= mapper.convertValue(aemData.get("metadata"),Document.class);
				metadata.put("user_email",userName);
				masterJson.put("metadata",metadata);
				MongoClientSingleton.getClient().getDatabase(domainName).getCollection("masterJson").insertOne(masterJson);
				result.put("message", "data ingested in QUIP");
				result.put("result", aemData);
				activityTracking.updateActivity(userName, domainUrl, "aem_data_ingestion",domainName, "component");
			}
		} catch (Exception exception) {
			result.put("status","failed");
			result.put("message", "Failed to ingest data: "+ exception.getClass().getName());
			result.put("result", Collections.emptyMap());
			result.put("response",exception.getMessage());
			return result;
		}
		return result;
	}

	@GetMapping("/exportComponentsToJSON")
	public JsonNode returnComponentsToJSON(@RequestParam String userName, @RequestParam String domainUrl, @RequestParam String domainPath) throws Exception {
		String domainName = utility.getClientName(domainUrl);
		domainPath = "/content/" + domainName + utility.getPagePath(domainPath);
		log.info("dp: {}", domainPath);
		return mapper.convertValue(exportComponentsToJSON.exportToJSON(userName,domainUrl,domainPath),JsonNode.class);
	}

	@PutMapping("/updateComponent")
	public ResponseEntity<Map<String, String>> updateComponent(@RequestBody JsonNode document) {
		Map<String, String> response = new LinkedHashMap<>();
		JsonNode updateDocumentNode = document.get("updateDocument");

		Map<String, String> updateDocument = mapper.convertValue(updateDocumentNode, Map.class);

		String domainName = document.get("domainUrl").asText();
		domainName=utility.getClientName(domainName);
		String userName = document.get("userName").asText();
		String id = updateDocument.get("id");

		if (id == null || id.isEmpty()) {
			response.put("status", "failed");
			response.put("message", "Document id should not be empty or null, please provide a valid id");
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
		try {
			response = updateComponentService.updateComponent(domainName, updateDocument, userName);
		} catch (Exception e) {
			if (e.getMessage().equalsIgnoreCase("state should be: hexString has 24 characters")) {
				response.put("status", "failed");
				response.put("message", "document id should be valid ");
			} else {
				response.put("status", "failed");
				response.put("message", "exception occurred:" + e.getMessage());
			}
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	// mongo versioning
	@PostMapping("/revertBackData")
	public JsonNode revertBackData(@RequestBody JsonNode document) {
		String currentDocumentId=document.get("currentDocumentId").asText();
		Document filterById = new Document("_id", new ObjectId(currentDocumentId));
		String documentId=document.get("documentId").asText();
		String domainUrl=document.get("domainUrl").asText();
		String domainName=utility.getClientName(domainUrl);
		return mapper.convertValue(dataVersionService.revertBack(filterById,documentId, domainName), JsonNode.class);
	}

	@GetMapping("/reviewChanges")
	public JsonNode reviewChanges(@RequestParam String domainUrl) {
		String domainName=utility.getClientName(domainUrl);
		return mapper.convertValue(dataVersionService.displayData(domainName), JsonNode.class);
	}

	// create tenantConfig
	@PostMapping(path = "/convertMappingFileToJsonV2", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
	public Map<String,Object> convertMappingFileToJsonV2(@RequestParam("file") MultipartFile file, @RequestParam String domain) {
		Map<String,Object> response;
		try {

			response=convertMappingFileToJsonV2.convertMappingFileToJson(file.getInputStream(), domain);
			Document jsonData=mapper.convertValue(response, Document.class);
			log.info(String.valueOf(jsonData));
			MongoCollection<Document> tenantConfigCollection=MongoClientSingleton.getClient().getDatabase(domain).getCollection("tenantConfig");
			Document mappingFileDocument=tenantConfigCollection.find().first();
			if (mappingFileDocument==null){
				tenantConfigCollection.insertOne(jsonData);
			}
			else {
				String id=mappingFileDocument.get("_id").toString();
				tenantConfigCollection.updateOne(new Document("_id",id), new Document("$set",jsonData));
			}
			return response;
		} catch (Exception e) {
			response=new HashMap<>();
			response.put("status","failed");
			response.put("message","failed to update master mapping data");
			response.put("errorResponse",e.getMessage());
			return response;
		}
	}

	// send data back to AEM for components,xf,page and form
	@GetMapping("/updateAEM")
	public JsonNode updateAEM(@RequestParam String domainUrl,@RequestParam String userName) {
		Map<String,Object> response=new LinkedHashMap<>();
		String domainName=utility.getClientName(domainUrl);
		Document query=new Document();
		query.put("activityType","component");

		// extract the activity collection
		MongoCollection<Document> userActivity=MongoClientSingleton.getClient().getDatabase(domainName).getCollection("activityInfo");

		// check activity for component ingestion and check if activity is in progress
		Document componentActivity=userActivity.find(query).sort(new Document("_id", -1)).first();
		if (componentActivity==null || !componentActivity.get("activityCycle").equals("inprogress")){
			response.put("status", "failed");
			response.put("message", "No data found for domain '" + domainName + "'. Please Ingest data");
			return mapper.convertValue(response, JsonNode.class);
		}

		// check if activity is in progress or not
		if (!componentActivity.get("activityCycle").equals("inprogress")){
			response.put("status", "failed");
			response.put("message", "Activity is not in progress for domain '" + domainName + "'. Please Ingest data");
			return mapper.convertValue(response, JsonNode.class);
		}

		// check if userName in activity is as same as current user
		if (!StringUtils.equals(componentActivity.get("userName").toString(),(userName))){
			response.put("status", "failed");
			response.put("message", "The domain '" + domainName + "' is owned by '" + componentActivity.get("userName").toString() + "'. Kindly reach out to the domain owner for further assistance");
			response.put("response", userName+": you do not have permission to update this domain");
			return mapper.convertValue(response, JsonNode.class);
		}

		// fetching for components,page and forms
		Map<String,Object> componentDataResponse=mapper.convertValue(sendDataToAEMV2.connectToAEM(mapper.convertValue(masterJSONComponentDataV2.convertMasterJsonData(domainName), JsonNode.class),domainName,domainUrl,userName), new TypeReference<Map<String, Object>>() {});

		response.put("response", componentDataResponse);
		return mapper.convertValue(response, JsonNode.class);
	}
}