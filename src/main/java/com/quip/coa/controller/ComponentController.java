package com.quip.coa.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import com.quip.coa.dbConfig.MongoClientSingleton;
import com.quip.coa.model.ComponentDocument;
import com.quip.coa.utilities.MongoUtility;
import com.quip.coa.utilities.Utility;
import com.quip.coa.service.*;
import com.quip.coa.utilities.Constants;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.bson.Document;
import org.bson.types.ObjectId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/content")
public class ComponentController {

    private static final Logger log = LoggerFactory.getLogger(ComponentController.class);
    @Autowired
    private AEMDataConsumer aemDataConsumer;
    @Autowired
    private ActivityTracking activityTracking;
    @Autowired
    private Utility utility;
    @Autowired
    private SendDataBackToAEMService sendDataBackToAEMService;
    @Autowired
    private DataVersionService dataVersionService;
    @Autowired
    private DomainService domainService;
    @Autowired
    private AemService service;

    private static final ObjectMapper mapper = new ObjectMapper();

    // create a domain
    @PostMapping("/createDomain")
    public ResponseEntity<ObjectNode> createDomain(@RequestParam("domainUrl") String domainUrl) {

        Map<String, Object> response = new LinkedHashMap<>();

        if (domainUrl == null) {
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_ERROR_CODE, Constants.EMPTY_FIELDS);
            response.put(Constants.FIELD_MESSAGE, "domainUrl field is mandatory.");
            return new ResponseEntity<>(mapper.convertValue(response, ObjectNode.class), HttpStatus.OK);
        }

        UrlValidator urlValidator = new UrlValidator();
        if (domainUrl.trim().isEmpty()) {
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_ERROR_CODE, Constants.EMPTY_VALUES);
            response.put(Constants.FIELD_MESSAGE, "Data Invalid. domainUrl cannot be empty.");
            return new ResponseEntity<>(mapper.convertValue(response, ObjectNode.class), HttpStatus.OK);
        }

        if (!(urlValidator.isValid(domainUrl))) {
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_ERROR_CODE, Constants.INVALID_DATA);
            response.put(Constants.FIELD_MESSAGE, "Data Invalid. Please enter valid domainUrl.");
            return new ResponseEntity<>(mapper.convertValue(response, ObjectNode.class), HttpStatus.OK);
        }

        String domainName = utility.getClientName(domainUrl);

        String id = domainService.postDomain(domainName,domainUrl);

        if (id == null) {
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_ERROR_CODE, Constants.CREATION_FAILED);
            response.put(Constants.FIELD_MESSAGE, "Domain creation is failed as domainUrl already exists. Try using another values.");
            return new ResponseEntity<>(mapper.convertValue(response, ObjectNode.class), HttpStatus.OK);
        }
        response.put(Constants.FIELD_STATUS, Constants.STATUS_SUCCESS);
        response.put(Constants.FIELD_RESPONSE, "Domain created successfully. Domain can be accessed by Id: " + id);
        return new ResponseEntity<>(mapper.convertValue(response, ObjectNode.class), HttpStatus.OK);
    }

    // fetch all the domains from the database
    @GetMapping("/getAllDomains")
    public ResponseEntity<ObjectNode> getAllDomains() {
        Map<String, String> response = new LinkedHashMap<>();

        List<Document> domains = domainService.getAllDomains();
        if ((domains.isEmpty())) {
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_ERROR_CODE, Constants.DATA_NOT_FOUND);
            response.put(Constants.FIELD_MESSAGE, "No domains available in the collection.");
            return new ResponseEntity<>(mapper.convertValue(response, ObjectNode.class), HttpStatus.OK);
        }
        return new ResponseEntity<>(mapper.convertValue(new Document("domains", domains), ObjectNode.class), HttpStatus.OK);
    }

    @PutMapping("/updateDomain")
    public ResponseEntity<Map<String, String>> updateDomainDetails(@RequestBody Map<String, String> request) {
        Map<String, String> response = new HashMap<>();
        String id = request.get("id");

        if (id == null || id.trim().isEmpty()) {
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_MESSAGE, "Domain id is mandatory");
            return ResponseEntity.badRequest().body(response);
        }

        if (!ObjectId.isValid(id)) {
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_MESSAGE, "Invalid domain id");
            return ResponseEntity.badRequest().body(response);
        }

        String domainName = request.get("domainName");
        String domainUrl = request.get("domainUrl");

        if ((domainName == null || domainName.trim().isEmpty()) && (domainUrl == null || domainUrl.trim().isEmpty())) {
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_MESSAGE,"Either domainName or domainUrl must be provided");
            return ResponseEntity.badRequest().body(response);
        }
        if (domainName != null) {
            domainName = domainName.trim();
        }
        if (domainUrl != null) {
            domainUrl = domainUrl.trim();
        }
        response = domainService.updateDomain(id, domainName, domainUrl);
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/deleteDomain/{id}")
    public ResponseEntity<Map<String, String>> deleteDomain(@PathVariable String id) {
        DeleteResult deleteResult = domainService.deleteDomain(id);
        if (deleteResult.getDeletedCount() > 0) {
            return ResponseEntity.ok(Map.of(
                    Constants.FIELD_STATUS, Constants.STATUS_SUCCESS,
                    Constants.FIELD_MESSAGE, "Domain deleted successfully"
            ));
        }
        return ResponseEntity.status(404).body(Map.of(
                Constants.FIELD_STATUS, Constants.STATUS_FAILED,
                Constants.FIELD_MESSAGE, "Domain not found"
        ));
    }

    @GetMapping("/ingestAemData")
    public Map<String, Object> ingestAemData(@RequestParam String userEmail, @RequestParam String domainUrl, @RequestParam String domainPath) {

        String domainName = utility.getClientName(domainUrl);

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> activityStatusMap = activityTracking.initAemExtract(userEmail, domainUrl, domainName);
        try {
            boolean initAem = (boolean) activityStatusMap.get("initAEM");
            if (!initAem) {
                result.put(Constants.FIELD_STATUS, Constants.STATUS_SUCCESS);
                result.put(Constants.FIELD_MESSAGE, activityStatusMap.get(Constants.FIELD_MESSAGE).toString());
                return result;
            } else {
                aemDataConsumer.processAEMData(domainUrl, userEmail, domainName);

                result.put(Constants.FIELD_MESSAGE, "data ingested in QUIP");
                result.put(Constants.FIELD_STATUS, Constants.STATUS_SUCCESS);
            }
        } catch (Exception exception) {
            result.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            result.put(Constants.FIELD_MESSAGE, "Failed to ingest data: " + exception.getClass().getName());
            return result;
        }
        return result;
    }

    @GetMapping("/components")
    public List<ComponentDocument> getAllComponentsData(@RequestParam String domainPath, @RequestParam String domainUrl) throws JsonProcessingException {

        String domainName = utility.getClientName(domainUrl);
        domainPath = "/content/" + domainName + utility.getPagePath(domainPath);
        return service.getAll(domainPath, domainName);
    }

    @PutMapping("/updateComponent")
    public void updateComponent(@RequestBody Map<String, Object> body) {
        String componentPath = body.get("componentPath").toString();
        log.info("COmpo: {}", componentPath);
        String domainName=utility.getClientName(body.get("domainUrl").toString());
        Document document = MongoUtility.getDocumentByPath(componentPath,domainName);

        document.put("rawProps", body.get("rawProps"));

        MongoUtility.updateDocument(domainName, componentPath, document);
    }

    @GetMapping("/reviewChanges")
    public JsonNode reviewChanges(@RequestParam String domainUrl) {
        String domainName = utility.getClientName(domainUrl);
        return mapper.convertValue(dataVersionService.displayData(domainName), JsonNode.class);
    }

    // mongo versioning
    @PostMapping("/revertBackData")
    public JsonNode revertBackData(@RequestBody JsonNode document) {
        String currentDocumentId = document.get("currentDocumentId").asText();
        Document filterById = new Document("_id", new ObjectId(currentDocumentId));
        String documentId = document.get("documentId").asText();
        String clientName = document.get("clientName").asText();
        return mapper.convertValue(dataVersionService.revertBack(filterById, documentId, clientName), JsonNode.class);
    }

	// send data back to AEM for components
	@GetMapping("/updateAEM")
	public JsonNode updateAEM(@RequestParam String domainUrl,@RequestParam String userEmail) throws JsonProcessingException {
		Map<String,Object> response=new LinkedHashMap<>();
		String domainName=utility.getClientName(domainUrl);
		Document query=new Document();
		query.put("activityType","component");

		// extract the activity collection
		MongoCollection<Document> userActivity=MongoClientSingleton.getClient().getDatabase(domainName).getCollection(Constants.ACTIVITY_INFO_COLLECTION);

		// check activity for component ingestion and check if activity is in progress
		Document componentActivity=userActivity.find(query).sort(new Document("_id", -1)).first();
		if (componentActivity==null || !componentActivity.get("activityCycle").equals("inprogress")){
			response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
			response.put(Constants.FIELD_MESSAGE, "No data found for domain '" + domainName + "'. Please Ingest data");
			return mapper.convertValue(response, JsonNode.class);
		}

		// check if activity is in progress or not
		if (!componentActivity.get("activityCycle").equals("inprogress")){
			response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
			response.put(Constants.FIELD_MESSAGE, "Activity is not in progress for domain '" + domainName + "'. Please Ingest data");
			return mapper.convertValue(response, JsonNode.class);
		}

		// check if userName in activity is as same as current user
		if (!StringUtils.equals(componentActivity.get("userName").toString(),(userEmail))){
			response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
			response.put(Constants.FIELD_MESSAGE, "The domain '" + domainName + "' is owned by '" + componentActivity.get("userName").toString() + "'. Kindly reach out to the domain owner for further assistance");
			response.put(Constants.FIELD_RESPONSE, userEmail+": you do not have permission to update this domain");
			return mapper.convertValue(response, JsonNode.class);
		}

		// fetching for components
        // fetch metadata
        JsonNode componentsData= mapper.convertValue(service.reStructureComponentData(service.reconstruct(domainName), domainName), JsonNode.class);
        log.info("{}", componentsData);
		Map<String,Object> componentDataResponse=mapper.convertValue(sendDataBackToAEMService.connectToAEM(componentsData,domainName,domainUrl,userEmail), new TypeReference<Map<String, Object>>() {});

		response.put(Constants.FIELD_RESPONSE, componentDataResponse);
		return mapper.convertValue(response, JsonNode.class);
	}
}