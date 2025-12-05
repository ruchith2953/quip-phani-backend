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

    // AEM urls to extract and send back data
    public static final String UPDATE_DATA_TO_AEM="http://34.224.16.46:4502/bin/quip/v1/component-update";
    public static final String UPDATE_SEO_DATA_TO_AEM = "http://34.224.16.46:4502/bin/quip/v1/seo-update";
    public static final String AEM_XF_URL="http://34.224.16.46:4502/bin/quip/v1/xf-extract";
    public static final String UPDATE_XF_DATA_TO_AEM="http://34.224.16.46:4502/bin/quip/v1/xf-update";

    // data field names
    public static final String FIELD_COMPONENTS_DATA="components";
    public static final String FIELD_XF_DATA="xfData";
    public static final String FIELD_FORMS_DATA="formsData";
    public static final String FIELD_PAGE_DATA ="pageData";

    // update xf data API
    public  static final String UPDATE_XF_DATA_API="http://100.24.248.226:9091/component/updateMasterJsonXFData";
}
