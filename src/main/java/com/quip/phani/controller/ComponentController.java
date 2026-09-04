package com.quip.phani.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.result.DeleteResult;
import com.quip.phani.mongoUtility.MongoUtility;
import com.quip.phani.service.*;
import com.quip.phani.utilities.Constants;
import org.apache.commons.lang3.StringUtils;
import com.quip.phani.helper.Utility;
import org.apache.commons.validator.routines.UrlValidator;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/content")
public class ComponentController {

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
    private SendDataToAEM sendDataToAEM;
    @Autowired
    private MasterJSONComponentData masterJSONComponentData;
    @Autowired
    private DataVersionService dataVersionService;
    @Autowired
    private ConvertMappingFileToJsonV2 convertMappingFileToJson;
    @Autowired
    private DomainService domainService;
    @Autowired
    private MongoUtility mongoUtility;
    @Autowired
    private ExportPageProperties exportPageProperties;
    @Autowired
    private AemFormService aemFormService;
    @Autowired
    private ReturnAemFormsData returnAemFormsData;


    private static final ObjectMapper mapper = new ObjectMapper();

    // create a domain
    @PostMapping("/createDomain")
    public ResponseEntity<ObjectNode> createDomain(@RequestParam String domainUrl) {
        Map<String, Object> response = new LinkedHashMap<>();

        if (domainUrl == null || domainUrl.trim().isEmpty()) {
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_MESSAGE, "domainUrl field is mandatory.");
            return new ResponseEntity<>(mapper.convertValue(response, ObjectNode.class), HttpStatus.OK);
        }

        UrlValidator urlValidator = new UrlValidator();

        if (!(urlValidator.isValid(domainUrl))) {
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_MESSAGE, "Data Invalid. Please enter valid domainUrl.");
            return new ResponseEntity<>(mapper.convertValue(response, ObjectNode.class), HttpStatus.OK);
        }

        String domainName = utility.getClientName(domainUrl);

        String id = domainService.postDomain(domainName, domainUrl);
        if (id == null) {
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_MESSAGE, "Domain creation is failed as domainUrl already exists. Try with other values.");
            return new ResponseEntity<>(mapper.convertValue(response, ObjectNode.class), HttpStatus.OK);
        }
        response.put(Constants.FIELD_STATUS, Constants.STATUS_SUCCESS);
        response.put(Constants.FIELD_MESSAGE, "Domain created successfully. Domain can be accessed by Id: " + id);
        return new ResponseEntity<>(mapper.convertValue(response, ObjectNode.class), HttpStatus.OK);
    }

    // fetch all the domains from the database
    @GetMapping("/getAllDomains")
    public ResponseEntity<ObjectNode> getAllDomains() {
        Map<String, String> response = new LinkedHashMap<>();

        List<Document> domains = domainService.getAllDomains();
        if ((domains.isEmpty())) {
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_MESSAGE, "No domains available in the collection.");
            return new ResponseEntity<>(mapper.convertValue(response, ObjectNode.class), HttpStatus.OK);
        }
        return new ResponseEntity<>(mapper.convertValue(new Document("domains", domains), ObjectNode.class), HttpStatus.OK);
    }

    @PutMapping("/updateDomain")
    public ResponseEntity<Map<String, String>> updateDomain(@RequestBody Map<String, String> request) {
        Map<String, String> response = new HashMap<>();
        String id = request.get("id");

        if (id == null || id.trim().isEmpty()) {
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_MESSAGE, "Domain id is mandatory");
            return ResponseEntity.badRequest().body(response);
        }

        String domainName = request.get("domainName");
        String domainUrl = request.get("domainUrl");

        if ((domainName == null || domainName.trim().isEmpty()) && (domainUrl == null || domainUrl.trim().isEmpty())) {
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_MESSAGE, "Either domainName or domainUrl must be provided");
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
            return ResponseEntity.ok(Map.of(Constants.FIELD_STATUS, Constants.STATUS_SUCCESS, Constants.FIELD_MESSAGE, "Domain deleted successfully"));
        }
        return ResponseEntity.status(404).body(Map.of(Constants.FIELD_STATUS, Constants.STATUS_FAILED, Constants.FIELD_MESSAGE, "Domain not found"));
    }

    @GetMapping("/ingestAemData")
    public Map<String, Object> ingestAemData(@RequestParam String userEmail, @RequestParam String domainUrl) {
        String domainName = utility.getClientName(domainUrl);

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> activityStatusMap = activityTracking.initAemExtract(userEmail, domainUrl, domainName);
        try {
            boolean initAem = (boolean) activityStatusMap.get("initAEM");
            if (!initAem) {
                result.put(Constants.FIELD_MESSAGE, activityStatusMap.get("message").toString());
                result.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
                result.put("result", Collections.EMPTY_MAP);
                return result;
            } else {
                Map<String, Object> aemData = aemDataConsumer.processAEMData(domainUrl, userEmail, domainName);
                aemData = aemFormService.aemformData(aemData, domainName);
                Document masterJson = mapper.convertValue(aemData, Document.class);
                // adding user email to AEM meta data
                Document metadata = mapper.convertValue(aemData.get("metadata"), Document.class);
                metadata.put("user_email", userEmail);
                masterJson.put("metadata", metadata);
                mongoUtility.insertDocument(domainName, Constants.MASTER_JSON_COLLECTION, masterJson);
                result.put(Constants.FIELD_MESSAGE, "data ingested in QUIP");
                result.put(Constants.FIELD_STATUS, Constants.STATUS_SUCCESS);
                result.put("result", aemData);
                activityTracking.updateActivity(userEmail, domainUrl, "aem_data_ingestion", domainName, "component");
            }
        } catch (Exception exception) {
            result.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            result.put(Constants.FIELD_MESSAGE, "Failed to ingest data: " + exception.getClass().getName());
            result.put("result", Collections.emptyMap());
            return result;
        }
        return result;
    }

    @GetMapping("/exportComponentsToJSON")
    public JsonNode returnComponentsToJSON(@RequestParam String domainUrl, @RequestParam String userName) throws Exception {
        return mapper.convertValue(exportComponentsToJSON.exportToJSON(userName, domainUrl), JsonNode.class);
    }

    @PutMapping("/updateComponent")
    public ResponseEntity<Map<String, String>> updateComponent(@RequestBody JsonNode document) {
        Map<String, String> response = new LinkedHashMap<>();
        JsonNode updateDocumentNode = document.get("updateDocument");

        Map<String, String> updateDocument = mapper.convertValue(updateDocumentNode, Map.class);

        String domainName = document.get("domainUrl").asText();
        domainName = utility.getClientName(domainName);
        String userName = document.get("userName").asText();
        String id = updateDocument.get("id");

        if (id == null || id.isEmpty()) {
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_MESSAGE, "Document id should not be empty or null, please provide a valid id");
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        try {
            response = updateComponentService.updateComponent(domainName, updateDocument, userName);
        } catch (Exception e) {
            if (e.getMessage().equalsIgnoreCase("state should be: hexString has 24 characters")) {
                response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
                response.put(Constants.FIELD_MESSAGE, "document id should be valid ");
            } else {
                response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
                response.put(Constants.FIELD_MESSAGE, "exception occurred:" + e.getMessage());
            }
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // mongo versioning
    @PostMapping("/revertBackData")
    public JsonNode revertBackData(@RequestBody JsonNode document) {
        String currentDocumentId = document.get("currentDocumentId").asText();
        Document filterById = new Document("_id", new ObjectId(currentDocumentId));
        String documentId = document.get("documentId").asText();
        String domainUrl = document.get("domainUrl").asText();
        String domainName = utility.getClientName(domainUrl);
        return mapper.convertValue(dataVersionService.revertBack(filterById, documentId, domainName), JsonNode.class);
    }

    @GetMapping("/reviewChanges")
    public JsonNode reviewChanges(@RequestParam String domainUrl) {
        String domainName = utility.getClientName(domainUrl);
        return mapper.convertValue(dataVersionService.displayData(domainName), JsonNode.class);
    }

    // create tenantConfig
    @PostMapping(path = "/convertMappingFileToJson", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public Map<String, Object> convertMappingFileToJson(@RequestParam("file") MultipartFile file, @RequestParam String domain) {
        Map<String, Object> response;
        try {
            response = convertMappingFileToJson.convertMappingFileToJson(file.getInputStream(), domain);
            Document jsonData = mapper.convertValue(response, Document.class);
            Document mappingFileDocument = mongoUtility.getFirstDocument(domain, Constants.TENANT_CONFIG_COLLECTION);
            if (mappingFileDocument == null) {
                mongoUtility.insertDocument(domain, Constants.TENANT_CONFIG_COLLECTION, jsonData);
            } else {
                String id = mappingFileDocument.get("_id").toString();
                mongoUtility.updateDocument(domain, Constants.TENANT_CONFIG_COLLECTION, new Document("_id", id), jsonData);
            }
            return response;
        } catch (Exception e) {
            response = new HashMap<>();
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_MESSAGE, "failed to update master mapping data");
            response.put(Constants.RESPONSE_FAILED, e.getMessage());
            return response;
        }
    }

    // send data back to AEM for components,xf,page and form
    @GetMapping("/updateAEM")
    public JsonNode updateAEM(@RequestParam String domainUrl, @RequestParam String userName) {
        Map<String, Object> response = new LinkedHashMap<>();
        String domainName = utility.getClientName(domainUrl);
        Document query = new Document();
        query.put("activityType", "component");

        // check activity for component ingestion and check if activity is in progress
        Document componentActivity = mongoUtility.getFirstDocByQueryAndSort(domainName, Constants.ACTIVITY_INFO_COLLECTION, query, new Document("_id", -1));
        if (componentActivity == null || !componentActivity.get("activityCycle").equals("inprogress")) {
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_MESSAGE, "No data found for domain '" + domainName + "'. Please Ingest data");
            return mapper.convertValue(response, JsonNode.class);
        }

        // check if activity is in progress or not
        if (!componentActivity.get("activityCycle").equals("inprogress")) {
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_MESSAGE, "Activity is not in progress for domain '" + domainName + "'. Please Ingest data");
            return mapper.convertValue(response, JsonNode.class);
        }

        // check if userName in activity is as same as current user
        if (!StringUtils.equals(componentActivity.get("userName").toString(), (userName))) {
            response.put(Constants.FIELD_STATUS, Constants.STATUS_FAILED);
            response.put(Constants.FIELD_MESSAGE, "The domain '" + domainName + "' is owned by '" + componentActivity.get("userName").toString() + "'. Kindly reach out to the domain owner for further assistance");
            response.put(Constants.RESPONSE_FAILED, userName + ": you do not have permission to update this domain");
            return mapper.convertValue(response, JsonNode.class);
        }

        // fetching for components,page and forms
        Map<String, Object> componentDataResponse = mapper.convertValue(sendDataToAEM.connectToAEM(mapper.convertValue(masterJSONComponentData.convertMasterJsonData(domainName), JsonNode.class), domainName, domainUrl, userName), new TypeReference<Map<String, Object>>() {
        });

        response.put(Constants.RESPONSE_FAILED, componentDataResponse);
        return mapper.convertValue(response, JsonNode.class);
    }

    // export page data
    @GetMapping("/exportPageProperties")
    public JsonNode returnPageProperties(@RequestParam String userName, @RequestParam String domainUrl) throws Exception {
        return mapper.convertValue(exportPageProperties.exportToJSON(userName, domainUrl), JsonNode.class);
    }

    @GetMapping("/getAemForms")
    public Map<String, Object> getAemForms(@RequestParam String domainUrl) throws Exception {
        return returnAemFormsData.exportFormsToJSON(domainUrl);
    }
}