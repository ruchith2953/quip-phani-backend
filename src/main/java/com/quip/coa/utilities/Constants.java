package com.quip.coa.utilities;

public class Constants {

    // static/mandatory fields count that are same for all the components in the mapper file
    // example- componentPath, recordType, inventoryUrl, path. these are common for all components
    public static final int MANDATORY_FIELDS_COUNT=4;

    // component field names
    public static final String COMPONENT_PATH_ATTRIBUTE_KEY="componentPath|ComponentPath";
    public static final String INVENTORY_URL_ATTRIBUTE_KEY="inventoryUrl|InventoryURL";
    public static final String PATH_ATTRIBUTE_KEY="path|Path";

    // number of versions to keep a track of previous updated data
    public static final long NUMBER_OF_VERSIONS=5;

    // component extract base url
    public static final String CONTENT_EXTRACT_BASE_URL ="http://34.224.16.46:4502/bin/quip/v1/component-extract?siteUrl=%s&mapping=%s";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String ACCEPT_JSON = "application/json";
    public static final String ACTIVITY_TYPE = "aem_data_extract";
    public static final String CONTENT_EXTRACT_MAPPING ="content-extract";
    public static final String COMPONENT = "component";

    // AEM urls to extract and send back data
    public static final String UPDATE_DATA_TO_AEM="http://34.224.16.46:4502/bin/quip/v1/component-update";

    // mongo connection uri
    public static final String MONGO_CONNECTION_URI = "mongodb://admin:Admin123@134.33.246.207:27017/?authSource=admin";

    public static final String AUTHOR_DOMAIN_DB ="author-domains";

    //Collections
    public static final String AUTHOR_DOMAIN_URLS_COLLECTION ="domain_urls";
    public static final String COMPONENT_COLLECTION ="component";
    public static final String MASTER_JSON_COLLECTION ="masterJson";
    public static final String TENANT_CONFIG_COLLECTION = "tenantConfig";
    public static final String ACTIVITY_INFO_COLLECTION = "activityInfo";
    public static final String VERSION_COLLECTION = "versionCollection";

    // data field names
    public static final String FIELD_COMPONENTS_DATA="components";

    // Status
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_MESSAGE = "message";
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAILED = "failed";
    public static final String RESPONSE_FAILED = "response";
}
