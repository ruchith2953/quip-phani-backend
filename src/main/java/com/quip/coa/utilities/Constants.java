package com.quip.coa.utilities;

public class Constants {

    // static/mandatory fields count that are same for all the components in the mapper file
    // example- componentPath, recordType, inventoryUrl, path. these are common for all components
    public static final int MANDATORY_FIELDS_COUNT=4;

    // mongo connection uri
    public static final String MONGO_CONNECTION_URI = "mongodb://admin:admin@34.194.175.59:27017/?authSource=admin";

    // component field names
    public static final String COMPONENT_PATH_ATTRIBUTE_KEY="componentPath|ComponentPath";
    public static final String INVENTORY_URL_ATTRIBUTE_KEY="inventoryUrl|InventoryURL";
    public static final String PATH_ATTRIBUTE_KEY="path|Path";
    public static final String CLIENT_AUTHOR_FIELD = "-author";

    // number of versions to keep a track of previous updated data
    public static final long NUMBER_OF_VERSIONS=5;

    // component extract base url
    public static final String COMPONENT_EXTRACT_BASE_URL ="http://34.224.16.46:4502/bin/quip/v1/component-extract?siteUrl=%s&mapping=%s";
    // AEM urls to extract and send back data
    public static final String UPDATE_DATA_TO_AEM="http://34.224.16.46:4502/bin/quip/v1/component-update";

    // data field names
    public static final String FIELD_COMPONENTS_DATA="components";

    public static final String AUTHOR_DOMAIN_DB ="author-domains";
    public static final String AUTHOR_DOMAIN_URLS_COLLECTION ="domain_urls";
    public static final String EMPTY_VALUES="1001"; // null or empty checks
    public static final String DATA_NOT_FOUND="1002"; // no content or data not found checks [operations based on DB]
    public static final String CREATION_FAILED="1003";// creation check [operation based on DB]
    public static final String INVALID_DATA="1004"; // data validation check
    public static final String EMPTY_FIELDS ="1005" ; // input fields checks

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String CONTENT_EXTRACT_HEADER_FIELD = "mapping";
    public static final String CONTENT_EXTRACT_HEADER_VALUE = "content-extract";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String ACCEPT_JSON = "application/json";
    public static final String ACTIVITY_TYPE = "aem_data_extract";
    public static final String COMPONENT = "component";


    // Status
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_ERROR_CODE = "errorCode";
    public static final String FIELD_ERROR_MESSAGE = "errorMessage";
    public static final String FIELD_MESSAGE = "message";
    public static final String FIELD_ERROR_RESPONSE = "errorResponse";
    public static final String FIELD_RESPONSE = "response";
    public static final String STATUS_SUCCESS = "Success";
    public static final String STATUS_FAILED = "failed";

    // MongoDB Collections
    public static final String TENANT_CONFIG_COLLECTION = "tenantConfig";
    public static final String MASTER_JSON_COLLECTION = "masterJson";
    public static final String ACTIVITY_INFO_COLLECTION = "activityInfo";

    public static final String INVALID_COMPONENT = "invalid_component";
    public static final String AEMFORM_DOCUMENTS = "aemform_documents";
    public static final String MASTER_JSON = "masterJson";
    public static final String VERSION_COLLECTION = "versionCollection";
}
