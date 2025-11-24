package com.quip.coa.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCursor;
import com.quip.coa.dbConfig.MongoClientSingleton;
import com.quip.coa.utilities.Utility;
import com.quip.coa.jsonexcel.TenantConfigJsonParserService;
import com.quip.coa.utilities.Constants;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ExportComponentsDataToJsonService {

	@Autowired
	private Utility utility;
	@Autowired
	private TenantConfigJsonParserService tenantConfigJsonParserService;

	private static final ObjectMapper mapper = new ObjectMapper();

	public Map<String, Object> exportToJSON(String userName, String domainUrl) throws Exception {

		// extract clientName from URL
		String clientName = utility.getClientName(domainUrl);

		Document tenantConfigDoc = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("tenantConfig")
				.find().first();

		Map<String, Object> outerData = new LinkedHashMap<>();
		List<Map<String, Object>> innerList = new ArrayList<>();

		if (tenantConfigDoc != null) {
			JsonNode tenantConfigCollectionData = mapper.convertValue(tenantConfigDoc, JsonNode.class);
			JsonNode tenantConfigColumnMapData = tenantConfigCollectionData.get("columnsMap");

			Iterator<String> tenantConfigColumnMapDataKeys = tenantConfigColumnMapData.fieldNames();
			Set<String> columnKeys = new HashSet<>();

			while (tenantConfigColumnMapDataKeys.hasNext()) {
				columnKeys.add(tenantConfigColumnMapDataKeys.next());
			}

			columnKeys.remove("authorableSet");
			columnKeys.remove("filter");

			JsonNode tenantConfigComponentsData = tenantConfigCollectionData.get("components");

			Map<String, Map<String, Object>> components = mapper.convertValue(tenantConfigComponentsData, new TypeReference<Map<String, Map<String, Object>>>() {
			});

			MongoCursor<Document> componentCollectionData = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("component").find().iterator();

			while (componentCollectionData.hasNext()) {
				Document componentDocument = componentCollectionData.next();
				String documentId=componentDocument.getObjectId("_id").toHexString();

				// check if component name is page property
				if (StringUtils.equals(componentDocument.get("componentName").toString(),"full-width-content-page")){
					continue;
				}

				if (componentDocument.get("componentName") != null) {
					Map<String, Object> componentPropertiesList = tenantConfigJsonParserService.retrieveComponentProperties(components, componentDocument);
					if(componentPropertiesList.isEmpty()){
						continue;
					}
					Map<String, Object> innerData = new LinkedHashMap<>();
					innerData.put("documentId",documentId);

					for (String key : columnKeys) {
						String cellValue = "";
						String k;
						if (componentPropertiesList.get(key) != null && StringUtils.isNotBlank(componentPropertiesList.get(key).toString())) {
							k = componentPropertiesList.get(key).toString();
							String resultDocKey = getComponentDocumentKey(componentDocument, k);
							if (null!=resultDocKey  &&  null!= componentDocument.get(resultDocKey)) {
								cellValue = componentDocument.get(resultDocKey).toString();
							}
							else {
								cellValue = "Property is missing";
							}
						}else {
							cellValue = "Property is missing";
						}
						if (StringUtils.equalsIgnoreCase(key, "componentName")) {
							cellValue = componentDocument.get(key) != null ? componentDocument.get(key).toString() : StringUtils.EMPTY;
						}
						innerData.put(key, cellValue);
					}
					innerList.add(innerData);
				}
			}
		}
		outerData.put(Constants.FIELD_COMPONENTS_DATA, innerList);
		return outerData;
	}

	private String getComponentDocumentKey(Document resultDocument, String key) {
		for (String componentDocumentKey : resultDocument.keySet()) {
			String aemKeyName=componentDocumentKey.contains("|")? componentDocumentKey.substring(0,componentDocumentKey.indexOf("|")): componentDocumentKey;

			if (StringUtils.equalsIgnoreCase(aemKeyName.trim(),key) ) {
				return componentDocumentKey;
			}
		}
		return null;
	}
}