package com.quip.phani.jsonexcel;

import com.quip.phani.utilities.Constants;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReadJsonFile {

    public Map<String, Object> retrieveComponentProperties(Map<String, Map<String, Object>> components, Document resultDocument) {
        List<Map<String, Object>> componentPropertiesList = new ArrayList<>();
        Map<String, Object> mergedMap = new HashMap<>();
        String compNameWithSlash = resultDocument.get("componentName").toString();
        String[] compNameArr = compNameWithSlash.split("\\|");
        String compNameKey = compNameArr[0].trim();
        //System.out.println("compNameKey: " + compNameKey);
        Map<String, Object> componentProperties = components.get(compNameKey);

        if (componentProperties != null) {
            String propertyKeyToRetrieve = "mappings";
            // Replace with the key you want to retrieve
            Object propertyValue = componentProperties.get(propertyKeyToRetrieve);
            if (propertyValue instanceof List) {
                componentPropertiesList = (List<Map<String, Object>>) propertyValue;
            }
        }
        int propertyListSize = componentPropertiesList.size();
        if (propertyListSize > 1) {
            // Iterate through the list of maps
            for (Map<String, Object> map : componentPropertiesList) {
                boolean allKeysMatch = true;
                // Check if all keys in the 'item' map match the values in the 'map'

                // empty fields count
                int emptyFieldCount = 0;

                for (String key : map.keySet()) {
                    String subKey = map.get(key).toString();
                    if (subKey.isEmpty()) {
                        emptyFieldCount++;
                        continue;
                    }
                    String resultDocKey = getComponentDocumentKey(resultDocument, subKey);
                    if (!resultDocument.containsKey(resultDocKey)) {
                        allKeysMatch = false;
                        break;
                    }
                }
                // now check if emptyFieldCount will match with the number of component properties map
                // remove the common/mandatory fields count from component properties map
                if (emptyFieldCount == map.size() - Constants.MANDATORY_FIELDS_COUNT) {
                    // we can make allKeysMatch to false or just skip iteration
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
            String aemKeyName = componentDocumentKey.contains("|") ? componentDocumentKey.substring(0, componentDocumentKey.indexOf("|")) : componentDocumentKey;

            if (StringUtils.equalsIgnoreCase(aemKeyName.trim(), key)) {
                return componentDocumentKey;
            }
        }
        return null;
    }
}
