package com.quip.coa.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import com.quip.coa.dbhelper.MongoClientSingleton;
import com.quip.coa.helper.Utility;
import com.quip.coa.service.*;
import com.quip.coa.utilities.Constants;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.bson.Document;
import org.bson.types.ObjectId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/content")
public class ComponentController {

	@Autowired
	private TenantConfigService tenantConfigService;
	@Autowired
	private AEMDataConsumer aemDataConsumer;
	@Autowired
	private ActivityTracking activityTracking;
	@Autowired
	private Utility utility;
	@Autowired
	private ExportComponentsDataToJsonService exportComponentsDataToJsonService;
	@Autowired
	private UpdateComponentDataService updateComponentDataService;
	@Autowired
	private SendDataBackToAEMService sendDataBackToAEMService;
	@Autowired
	private MasterJSONComponentDataService masterJSONComponentDataService;
	@Autowired
	private DataVersionService dataVersionService;
	@Autowired
	private ConvertMappingFileToJson convertMappingFileToJson;
	@Autowired
	private DomainService domainService;

	private static final ObjectMapper mapper = new ObjectMapper();

	// create a domain
	@PostMapping(value = "/createDomain", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ObjectNode> createDomain(@RequestPart("file") MultipartFile file, @RequestPart("domainName") String domainName, @RequestPart("domainUrl") String domainUrl) throws IOException {

		Map<String, Object> response = new LinkedHashMap<>();

		if (domainName==null ){
			response.put("status", "Failed");
			response.put("errorCode", Constants.EMPTY_FIELDS);
			response.put("errorMessage", "domainName field is mandatory.");
			return new ResponseEntity<>(mapper.convertValue(response, ObjectNode.class), HttpStatus.OK);
		}

		if (domainName.trim().isEmpty()) {
			response.put("status", "Failed");
			response.put("errorCode", Constants.EMPTY_VALUES);
			response.put("errorMessage", "Data Invalid. domainName cannot be empty.");
			return new ResponseEntity<>(mapper.convertValue(response, ObjectNode.class), HttpStatus.OK);
		}

		if (domainUrl==null ){
			response.put("status", "Failed");
			response.put("errorCode", Constants.EMPTY_FIELDS);
			response.put("errorMessage", "domainUrl field is mandatory.");
			return new ResponseEntity<>(mapper.convertValue(response, ObjectNode.class), HttpStatus.OK);
		}

		UrlValidator urlValidator = new UrlValidator();
		if (domainUrl.trim().isEmpty()) {
			response.put("status", "Failed");
			response.put("errorCode", Constants.EMPTY_VALUES);
			response.put("errorMessage", "Data Invalid. domainUrl cannot be empty.");
			return new ResponseEntity<>(mapper.convertValue(response, ObjectNode.class), HttpStatus.OK);
		}

		if (!(urlValidator.isValid(domainUrl))) {
			response.put("status", "Failed");
			response.put("errorCode", Constants.INVALID_DATA);
			response.put("errorMessage", "Data Invalid. Please enter valid domainUrl.");
			return new ResponseEntity<>(mapper.convertValue(response, ObjectNode.class), HttpStatus.OK);
		}

		String domainAuthor=utility.getClientName(domainUrl);

		Map<String,Object> mapperResponse=convertMappingFileToJson.convertMappingFileToJson(file.getInputStream(), domainAuthor);
		Document jsonData=mapper.convertValue(mapperResponse, Document.class);
		MongoCollection<Document> tenantConfigCollection=MongoClientSingleton.getClient().getDatabase(domainAuthor).getCollection("tenantConfig");
		Document mappingFileDocument=tenantConfigCollection.find().first();
		if (mappingFileDocument==null){
			tenantConfigCollection.insertOne(jsonData);
		}
		else {
			String id=mappingFileDocument.get("_id").toString();
			tenantConfigCollection.updateOne(new Document("_id",id), new Document("$set",jsonData));
		}

		Map<String, String> domainData = new HashMap<>();
		domainData.put("domainName", domainName);
		domainData.put("domainUrl", domainUrl);

		String id = domainService.postDomain(domainData);
		    if (id == null) {
			response.put("status", "Failed");
			response.put("errorCode", Constants.CREATION_FAILED);
			response.put("errorMessage", "Domain creation is failed as domainName or domainUrl already exists. Try using another values.");
			return new ResponseEntity<>(mapper.convertValue(response, ObjectNode.class), HttpStatus.OK);
		    }
		response.put("status", "Success");
		response.put("response", "Domain created successfully. Domain can be accessed by Id: "+id);
		return new ResponseEntity<>(mapper.convertValue(response, ObjectNode.class), HttpStatus.OK);
	}

	// fetch all the domains from the database
	@GetMapping("/getAllDomains")
	public ResponseEntity<ObjectNode> getAllDomains() {
		Map<String, String> response = new LinkedHashMap<>();

		List<Document> domains = domainService.getAllDomains();
		if ((domains.isEmpty())) {
			response.put("status", "Failed");
			response.put("errorCode", Constants.DATA_NOT_FOUND);
			response.put("errorMessage", "No domains available in the collection.");
			return new ResponseEntity<>(mapper.convertValue(response, ObjectNode.class), HttpStatus.OK);
		}
		return new ResponseEntity<>(mapper.convertValue(new Document("domains", domains), ObjectNode.class), HttpStatus.OK);
	}

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
	public JsonNode returnComponentsToJSON(@RequestParam String userName, @RequestParam String domainUrl) throws Exception {
		return mapper.convertValue(exportComponentsDataToJsonService.exportToJSON(userName,domainUrl),JsonNode.class);
	}

	@PutMapping("/updateComponent")
	public ResponseEntity<Map<String, String>> updateComponent(@RequestBody JsonNode document) {
		Map<String, String> response = new LinkedHashMap<>();
		JsonNode updateDocumentNode = document.get("updateDocument");

		Map<String, String> updateDocument = mapper.convertValue(updateDocumentNode, Map.class);

		String domainUrl = document.get("domainUrl").asText();
		String userName = document.get("userName").asText();
		String id = updateDocument.get("id");

		if (id == null || id.isEmpty()) {
			response.put("status", "failed");
			response.put("message", "Document id should not be empty or null, please provide a valid id");
			return new ResponseEntity<>(response, HttpStatus.OK);
		}

		String domainName=utility.getClientName(domainUrl);
		try {
			response = updateComponentDataService.updateComponent(domainName, updateDocument, userName);
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

	@GetMapping("/reviewChanges")
	public JsonNode reviewChanges(@RequestParam String domainUrl) {
		String domainName=utility.getClientName(domainUrl);
		return mapper.convertValue(dataVersionService.displayData(domainName), JsonNode.class);
	}

	// mongo versioning
	@PostMapping("/revertBackData")
	public JsonNode revertBackData(@RequestBody JsonNode document) {
		String currentDocumentId=document.get("currentDocumentId").asText();
		Document filterById = new Document("_id", new ObjectId(currentDocumentId));
		String documentId=document.get("documentId").asText();
		String clientName=document.get("clientName").asText();
		return mapper.convertValue(dataVersionService.revertBack(filterById,documentId, clientName), JsonNode.class);
	}

	// create tenantConfig
	@PostMapping(path = "/convertMappingFileToJson", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
	public Map<String,Object> convertMappingFileToJson(@RequestParam("file") MultipartFile file, @RequestParam String domainUrl) {
		Map<String,Object> response;
		try {
			domainUrl=utility.getClientName(domainUrl);

			response=convertMappingFileToJson.convertMappingFileToJson(file.getInputStream(), domainUrl);
			Document jsonData=mapper.convertValue(response, Document.class);
			MongoCollection<Document> tenantConfigCollection=MongoClientSingleton.getClient().getDatabase(domainUrl).getCollection("tenantConfig");
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

		// fetching for components
		Map<String,Object> componentDataResponse=mapper.convertValue(sendDataBackToAEMService.connectToAEM(mapper.convertValue(masterJSONComponentDataService.convertMasterJsonData(domainName), JsonNode.class),domainName,domainUrl,userName), new TypeReference<Map<String, Object>>() {});

		response.put("response", componentDataResponse);
		return mapper.convertValue(response, JsonNode.class);
	}

	@PutMapping("/updateTenantConfig")
	public ResponseEntity<JsonNode> updateTenantConfig(@RequestBody JsonNode requestJson, @RequestParam String clientUrl) {
		Map<String, Object> responseMap = new HashMap<>();

		String clientName=utility.getClientName(clientUrl);
		JsonNode response = null;
		try {
			Map<String, Object> request = mapper.treeToValue(requestJson, Map.class);
			UpdateResult result = tenantConfigService.updateTenantConfig(request,clientName);
			responseMap.put("result", result);
		} catch (Exception ex) {
			responseMap.put("error", ex.getMessage());
			response = mapper.convertValue(responseMap, JsonNode.class);
			return ResponseEntity.internalServerError().body(response);
		}
		response = mapper.convertValue(responseMap, JsonNode.class);
		return ResponseEntity.ok(response);
	}
}