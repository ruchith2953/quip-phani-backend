package com.quip.coa.model;

public class ComponentData {
    private String id;
    private String componentType;
    private String componentPath;
    private String html;

    public ComponentData() {}

    public ComponentData(String id, String componentType, String componentPath, String html) {
        this.id = id;
        this.componentType = componentType;
        this.componentPath = componentPath;
        this.html = html;
    }

    public String getId() { return id; }
    public String getComponentType() { return componentType; }
    public String getComponentPath() { return componentPath; }
    public String getHtml() { return html; }

    public void setId(String id) { this.id = id; }
    public void setComponentType(String type) { this.componentType = type; }
    public void setComponentPath(String path) { this.componentPath = path; }
    public void setHtml(String html) { this.html = html; }
}
