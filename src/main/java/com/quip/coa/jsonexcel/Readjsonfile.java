package com.quip.coa.jsonexcel;

import com.quip.coa.utilities.Constants;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class Readjsonfile {

	private static final Logger log = LoggerFactory.getLogger(Readjsonfile.class);

	public Map<String, Object> retrieveComponentProperties(Map<String, Map<String, Object>> components,
														   Document resultDocument) {

		log.info("Starting retrieveComponentProperties for component document: {}", resultDocument);

		List<Map<String, Object>> componentPropertiesList = new ArrayList<>();
		Map<String, Object> mergedMap = new HashMap<>();

		String compNameKey = resultDocument.get("componentName").toString();
//		String[] compNameArr = compNameWithSlash.split("\\|");
//		String compNameKey = compNameArr[0].trim();

		log.info("Extracted component name key: {}", compNameKey);

		Map<String, Object> componentProperties = components.get(compNameKey);

		if (componentProperties == null) {
			log.warn("No component properties found for key '{}'", compNameKey);
		} else {
			log.info("Component properties found for '{}'", compNameKey);

			String propertyKeyToRetrieve = "mappings";
			Object propertyValue = componentProperties.get(propertyKeyToRetrieve);

			if (propertyValue instanceof List) {
				componentPropertiesList = (List<Map<String, Object>>) propertyValue;
				log.info("Fetched {} mapping entries for component '{}'", componentPropertiesList.size(), compNameKey);
			} else {
				log.warn("Property '{}' for component '{}' is not a List", propertyKeyToRetrieve, compNameKey);
			}
		}

		int propertyListSize = componentPropertiesList.size();

		if (propertyListSize > 1) {
			log.info("Multiple ({}) component mappings found. Resolving best match…", propertyListSize);

			for (Map<String, Object> map : componentPropertiesList) {

				log.debug("Evaluating mapping: {}", map);

				boolean allKeysMatch = true;
				int emptyFieldCount = 0;

				for (String key : map.keySet()) {
					String subKey = map.get(key).toString();

					if (subKey.isEmpty()) {
						emptyFieldCount++;
						continue;
					}

					String resultDocKey = getComponentDocumentKey(resultDocument, subKey);

					if (resultDocKey == null || !resultDocument.containsKey(resultDocKey)) {
						allKeysMatch = false;
						log.debug("Key mismatch: expected '{}', found '{}'", subKey, resultDocKey);
						break;
					}
				}

				if (emptyFieldCount == map.size() - Constants.MANDATORY_FIELDS_COUNT) {
					log.debug("Skipping mapping due to empty mandatory fields match. Empty count = {}", emptyFieldCount);
					continue;
				}

				if (allKeysMatch) {
					log.info("Match found. Merging map: {}", map);
					mergedMap.putAll(map);
					break;
				}
			}
		} else {
			log.info("Only one component mapping found. Using directly.");

			for (Map<String, Object> map : componentPropertiesList) {
				log.debug("Merging single mapping: {}", map);
				mergedMap.putAll(map);
			}
		}

		log.info("Final merged component properties: {}", mergedMap);
		return mergedMap;
	}

	private String getComponentDocumentKey(Document resultDocument, String key) {
		for (String componentDocumentKey : resultDocument.keySet()) {

			String aemKeyName = componentDocumentKey.contains("|")
					? componentDocumentKey.substring(0, componentDocumentKey.indexOf("|"))
					: componentDocumentKey;

			if (StringUtils.equalsIgnoreCase(aemKeyName.trim(), key)) {
				log.debug("Matched document key '{}' for expected key '{}'", componentDocumentKey, key);
				return componentDocumentKey;
			}
		}

		log.debug("No matching key found in resultDocument for '{}'", key);
		return null;
	}
}
