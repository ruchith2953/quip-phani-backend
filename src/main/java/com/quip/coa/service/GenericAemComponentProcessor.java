package com.quip.coa.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.quip.coa.model.ComponentDocument;

import java.util.*;

public class GenericAemComponentProcessor {

    private final ObjectMapper mapper;

    public GenericAemComponentProcessor() {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        mapper.registerModule(new JavaTimeModule());
    }

    // ========================================================================
    // MAIN PARSER – extracts ALL components from AEM JSON
    // ========================================================================
    public List<ComponentDocument> parseComponentsJson(JsonNode root) {
        List<ComponentDocument> result = new ArrayList<>();

        JsonNode compsNode = root.get("components");
        if (compsNode == null || !compsNode.isObject()) return result;

        Iterator<String> keys = compsNode.fieldNames();

        while (keys.hasNext()) {
            String compKeyRaw = keys.next();
            JsonNode componentsArray = compsNode.get(compKeyRaw);

            if (!componentsArray.isArray()) continue;

            String[] parts = compKeyRaw.split("\\|", 2);
            String type = parts[0];
            String identifier = parts.length > 1 ? parts[1] : "";

            for (JsonNode instanceJson : componentsArray) {
                ComponentDocument doc = parseInstance(type, identifier, compKeyRaw, instanceJson);
                result.add(doc);
            }
        }

        return result;
    }

    // ========================================================================
    // PARSE A SINGLE COMPONENT INSTANCE (FULL RECURSION ENABLED)
    // ========================================================================
    private ComponentDocument parseInstance(String type, String identifier, String rawKey, JsonNode instanceNode) {

        ComponentDocument doc = new ComponentDocument();
        doc.id = UUID.randomUUID().toString();
        doc.componentType = type;
        doc.identifier = identifier;
        doc.componentKeyRaw = rawKey;

        Map<String, Object> clean = new LinkedHashMap<>();
        Map<String, Object> raw = new LinkedHashMap<>();
        Map<String, String> mappings = new LinkedHashMap<>();

        // do recursive extraction
        parseNodeRecursive(instanceNode, clean, raw, mappings);

        doc.cleanProps = clean;
        doc.rawProps = raw;
        doc.fieldMappings = mappings;

        // extract special top-level clean fields
        if (clean.containsKey("path")) {
            doc.path = clean.get("path").toString();
        }

        if (clean.containsKey("componentPath")) {
            doc.componentPath = clean.get("componentPath").toString();
        }

        return doc;
    }

    // ========================================================================
    // RECURSIVE FUNCTION — handles unlimited nesting
    // ========================================================================
    private void parseNodeRecursive(JsonNode node,
                                    Map<String, Object> cleanOut,
                                    Map<String, Object> rawOut,
                                    Map<String, String> mappings) {

        Iterator<String> fieldNames = node.fieldNames();

        while (fieldNames.hasNext()) {
            String rawKey = fieldNames.next();                 // e.g. "itemLabel|Label"
            JsonNode value = node.get(rawKey);

            // cleanKey = left side of '|'
            String cleanKey = rawKey.contains("|")
                    ? rawKey.substring(0, rawKey.indexOf("|"))
                    : rawKey;

            // store mapping clean → raw
            mappings.put(cleanKey, rawKey);

            // CASE 1: OBJECT
            if (value.isObject()) {
                Map<String, Object> cleanChild = new LinkedHashMap<>();
                Map<String, Object> rawChild = new LinkedHashMap<>();

                parseNodeRecursive(value, cleanChild, rawChild, mappings);

                cleanOut.put(cleanKey, cleanChild);
                rawOut.put(rawKey, rawChild);
            }

            // CASE 2: ARRAY
            else if (value.isArray()) {
                List<Object> cleanList = new ArrayList<>();
                List<Object> rawList = new ArrayList<>();

                for (JsonNode child : value) {

                    if (child.isObject()) {
                        Map<String, Object> cleanChild = new LinkedHashMap<>();
                        Map<String, Object> rawChild = new LinkedHashMap<>();

                        parseNodeRecursive(child, cleanChild, rawChild, mappings);

                        cleanList.add(cleanChild);
                        rawList.add(rawChild);
                    } else {
                        Object primitive = convert(child);
                        cleanList.add(primitive);
                        rawList.add(primitive);
                    }
                }

                cleanOut.put(cleanKey, cleanList);
                rawOut.put(rawKey, rawList);
            }

            // CASE 3: PRIMITIVE
            else {
                Object primitive = convert(value);
                cleanOut.put(cleanKey, primitive);
                rawOut.put(rawKey, primitive);
            }
        }
    }

    // ========================================================================
    // Convert JsonNode → clean Java object
    // ========================================================================
    private Object convert(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isTextual()) return node.asText();
        if (node.isBoolean()) return node.asBoolean();
        if (node.isNumber()) return node.numberValue();
        return node.toString();
    }

    // ========================================================================
    // RECONSTRUCT ORIGINAL AEM JSON FROM DB
    // ========================================================================
    public ObjectNode reconstructComponents(List<ComponentDocument> docs) {
        ObjectNode compsNode = mapper.createObjectNode();

        for (ComponentDocument doc : docs) {

            ArrayNode arr = (ArrayNode) compsNode.get(doc.componentKeyRaw);
            if (arr == null) {
                arr = mapper.createArrayNode();
                compsNode.set(doc.componentKeyRaw, arr);
            }

            ObjectNode reconstructedInstance = mapper.createObjectNode();

            // Rebuild rawProps exactly as they were in AEM
            for (Map.Entry<String, Object> entry : doc.rawProps.entrySet()) {
                reconstructedInstance.set(entry.getKey(), mapper.valueToTree(entry.getValue()));
            }

            arr.add(reconstructedInstance);
        }

        return compsNode;
    }
}
