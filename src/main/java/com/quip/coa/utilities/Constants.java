package com.quip.coa.utilities;

public class Constants {
    // mongo connection uri
    public static final String MONGO_CONNECTION_URI = "mongodb://admin:Admin123@134.33.246.207:27017/?authSource=admin";

    // number of versions to keep a track of previous updated data
    public static final long NUMBER_OF_VERSIONS=5;

    // component extract base url
    public static final String COMPONENT_EXTRACT_BASE_URL ="http://34.224.16.46:4502/bin/quip/v1/component-extract?siteUrl=%s&mapping=%s";
    // AEM urls to extract and send back data
    public static final String UPDATE_DATA_TO_AEM="http://34.224.16.46:4502/bin/quip/v1/component-update";

    public static final String AUTHOR_DOMAIN_DB ="author-domains";
    public static final String AUTHOR_DOMAIN_URLS_COLLECTION ="domain_urls";
    public static final String EMPTY_VALUES="1001"; // null or empty checks
    public static final String DATA_NOT_FOUND="1002"; // no content or data not found checks [operations based on DB]
    public static final String CREATION_FAILED="1003";// creation check [operation based on DB]
    public static final String INVALID_DATA="1004"; // data validation check
    public static final String EMPTY_FIELDS ="1005" ; // input fields checks

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String ACCEPT_JSON = "application/json";
    public static final String ACTIVITY_TYPE = "aem_data_extract";
    public static final String COMPONENT = "component";

    public static final String COMPONENTS_COLLECTION ="components";
    public static final String METADATA_COLLECTION ="metadata";

    public static final String COMPONENT_EXTRACT_MAPPING ="content-extract";

    // Status
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_ERROR_CODE = "errorCode";
    public static final String FIELD_MESSAGE = "message";
    public static final String FIELD_RESPONSE = "response";
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAILED = "failed";

    public static final String ACTIVITY_INFO_COLLECTION = "activityInfo";
}
