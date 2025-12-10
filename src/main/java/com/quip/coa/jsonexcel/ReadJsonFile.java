package com.quip.coa.jsonexcel;

import com.quip.coa.utilities.Constants;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReadJsonFile {

	public Map<String, Object> retrieveComponentProperties(Map<String, Map<String, Object>> components,Document resultDocument) {

		List<Map<String, Object>> componentPropertiesList = new ArrayList<>();
		Map<String, Object> mergedMap = new HashMap<>();

		String compNameKey = resultDocument.get("componentName").toString();

		Map<String, Object> componentProperties = components.get(compNameKey);

		if (componentProperties != null) {
			String propertyKeyToRetrieve = "mappings";
			Object propertyValue = componentProperties.get(propertyKeyToRetrieve);

			if (propertyValue instanceof List) {
				componentPropertiesList = (List<Map<String, Object>>) propertyValue;
			}
		}

		if (componentPropertiesList.size() > 1) {
			for (Map<String, Object> map : componentPropertiesList) {

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
						break;
					}
				}
				if (emptyFieldCount == map.size() - Constants.MANDATORY_FIELDS_COUNT) {
					continue;
				}
				if (allKeysMatch) {
					mergedMap.putAll(map);
					break;
				}
			}
		} else {
			for (Map<String, Object> map : componentPropertiesList) {
				mergedMap.putAll(map);
			}
		}
		return mergedMap;
	}

	private String getComponentDocumentKey(Document resultDocument, String key) {
		for (String componentDocumentKey : resultDocument.keySet()) {

			String aemKeyName = componentDocumentKey.contains("|")
					? componentDocumentKey.substring(0, componentDocumentKey.indexOf("|"))
					: componentDocumentKey;

			if (StringUtils.equalsIgnoreCase(aemKeyName.trim(), key)) {
				return componentDocumentKey;
			}
		}
		return null;
	}
}
