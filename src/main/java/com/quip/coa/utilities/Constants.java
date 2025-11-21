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

    // number of versions to keep a track of previous updated data
    public static final long NUMBER_OF_VERSIONS=5;

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

}
