package com.quip.coa.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Document(collection = "components")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ComponentDocument {
    @Id
    public String id;

    public String componentType;
    public String identifier;
    public String componentKeyRaw;

    public String path;
    public String componentPath;

    @Field("rawProps")
    public Map<String, Object> rawProps = new LinkedHashMap<>();

    public void setRawProps(Map<String, Object> updatedRaw) {
        if (updatedRaw == null) return;

        this.rawProps.clear();
        this.rawProps.putAll(updatedRaw);
    }
}
