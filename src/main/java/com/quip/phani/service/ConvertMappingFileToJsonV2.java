package com.quip.phani.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

import com.quip.phani.utilities.Constants;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CaseUtils;
import java.util.ArrayList;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.util.HashMap;
import java.util.Iterator;
import org.springframework.stereotype.Service;

@Service
public class ConvertMappingFileToJsonV2 {
    public static final int PAGE_INTERACTION_ID_COLUMN_NUMBER=5;
    public static final int COMPONENT_INTERACTION_ID_COLUMN_NUMBER=5;

    public Map<String, Object> convertMappingFileToJson(InputStream inputFile, String domain) throws IOException{
        Map<String, Object> mappingJson = new LinkedHashMap<>();
        Map<String, Object> components = new HashMap<>();
        Map<String, Object> pageData = new HashMap<>();
        Map<String, Object> xfData = new HashMap<>();
        Map<String, Object> formsData = new HashMap<>();
        XSSFWorkbook workbook = new XSSFWorkbook(inputFile);

        // for components data
        XSSFSheet componentsSheet = workbook.getSheet("QUIPMasterMapping");
        transformComponentMappingFileToJson(componentsSheet, mappingJson,components);

        // for page properties
        XSSFSheet pageSheet = workbook.getSheet("PageMapping");
        transformPageMappingFileToJson(pageSheet,mappingJson,pageData);

        // for xf data
        XSSFSheet xfSheet = workbook.getSheet("XFMapping");
        transformXFMappingFileToJson(xfSheet,mappingJson,xfData);

        // for forms data
        XSSFSheet formSheet = workbook.getSheet("FormsMapping");
        formsMappingFileToJson(formSheet, mappingJson, formsData);

        workbook.close();
        mappingJson.put(Constants.FIELD_COMPONENTS_DATA, components);
        mappingJson.put(Constants.FIELD_PAGE_DATA, pageData);
        mappingJson.put(Constants.FIELD_XF_DATA, xfData);
        mappingJson.put(Constants.FIELD_FORMS_DATA, formsData);
        mappingJson.put("domain", domain);

        return mappingJson;
    }

    /***************************************************XF Data Methods starts**************************************************/

    public void getXFColumnMaps(Iterator<Cell> cellItr, Map<String, String> columnNamesMap, Map<String, Object> mappingJson){
        while(cellItr.hasNext()) {
            Cell cell = cellItr.next();
            CellType cellType = cell.getCellType();
            if(cellType != CellType.BLANK) {
                String colName = cell.getStringCellValue();
                String updateColumnName= CaseUtils.toCamelCase(colName,false,' ');
                columnNamesMap.put(updateColumnName, colName);
            }
        }
    }

    public void processXFChildComponents(Row row, Map<String, Object> components, Map<String, String> mapping, Map<String, Object> componentMap, List<Map<String, String>> mappingsList , List<String> childList ){

        String interactionIdColumn = row.getCell(PAGE_INTERACTION_ID_COLUMN_NUMBER, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();
        if(interactionIdColumn.contains(".")) {
            String[] interactionIdArray = interactionIdColumn.split("\\.");

            if(interactionIdArray.length == 2) {
                String componentName = interactionIdArray[interactionIdArray.length-2].trim();
                if(components.containsKey(componentName)) {
                    Map<String, Object> component = (Map<String, Object>)components.get(componentName);
                    List<Map<String, String>> mappings = (List<Map<String, String>>) component.get("mappings");
                    mappings.add(mapping);
                }else {
                    mappingsList.add(mapping);
                    componentMap.put("mappings", mappingsList);
                    componentMap.put("child", childList);
                    components.put(componentName, componentMap);
                }
                String parent = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();
                if(StringUtils.isNotEmpty(parent)) {
                    Map<String, Object> component = (Map<String, Object>)components.get(parent);
                    List<String> child = (List<String>) component.get("child");
                    if (!child.contains(componentName)){
                        child.add(componentName);
                    }
                }
            }

            if(interactionIdArray.length >= 3) {
                String compName = interactionIdArray[interactionIdArray.length-2].trim();
                if(components.containsKey(compName)) {
                    Map<String, Object> component = (Map<String, Object>)components.get(compName);
                    List<Map<String, String>> mappings = (List<Map<String, String>>) component.get("mappings");
                    mappings.add(mapping);
                }else {
                    mappingsList.add(mapping);
                    componentMap.put("mappings", mappingsList);
                    componentMap.put("child", childList);
                    components.put(compName, componentMap);
                }
                String parent = interactionIdArray[interactionIdArray.length-3].trim();
                if(StringUtils.isNotEmpty(parent)) {
                    Map<String, Object> component = (Map<String, Object>)components.get(parent);
                    List<String> child = (List<String>) component.get("child");
                    if (!child.contains(compName)){
                        child.add(compName);
                    }
                }
            }
        }
    }

    private void transformXFMappingFileToJson(XSSFSheet xfSheet, Map<String, Object> mappingJson, Map<String, Object> xfData) {
        Map<String, String> columnNamesMap = new LinkedHashMap<>();

        Iterator<Row> rowIterator = xfSheet.rowIterator();
        int rowIndex = 0;

        while(rowIterator.hasNext()) {
            boolean rowContainsDot = false;
            Row row = rowIterator.next();

            if(rowIndex == 0) {
                Iterator<Cell> cellItr = row.cellIterator();
                // column maps to mappingJson
                getXFColumnMaps(cellItr, columnNamesMap, mappingJson);
                rowIndex ++;
                continue;
            }

            String[] colNamesList = new String[columnNamesMap.keySet().size()];
            columnNamesMap.keySet().toArray(colNamesList);

            Map<String, String> mapping = new HashMap<>();

            for(int columnNumber=0; columnNumber<row.getLastCellNum(); columnNumber++) {
                if(columnNumber == 0){
                    mapping.put(colNamesList[columnNumber], "componentName");
                    continue;
                }
                Cell cell = row.getCell(columnNumber, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                String colValue = cell.getStringCellValue();
                if(StringUtils.isNotEmpty(colValue) && colValue.contains(".")) {
                    String[] dotSplit = colValue.split("\\.");
                    mapping.put(colNamesList[columnNumber], dotSplit[dotSplit.length-1]);
                    rowContainsDot = true;
                }
                else {
                    mapping.put(colNamesList[columnNumber], colValue);
                }
            }

            Map<String, Object> componentMap = new HashMap<>();
            List<Map<String, String>> mappingsList = new ArrayList<>();
            List<String> childList = new ArrayList<>();

            // if row contains dot then we process child doc
            if(rowContainsDot) {
                processXFChildComponents(row,xfData,mapping, componentMap,mappingsList,childList);
            }
            else {
                String parentComponentName= row.getCell(0).getStringCellValue();
                if(xfData.containsKey(parentComponentName)) {
                    Map<String, Object> component = (Map<String, Object>)xfData.get(parentComponentName);
                    List<Map<String, String>> mappings = (List<Map<String, String>>) component.get("mappings");
                    mappings.add(mapping);
                }else {
                    mappingsList.add(mapping);
                    componentMap.put("mappings", mappingsList);
                    componentMap.put("child", childList);
                    xfData.put(parentComponentName, componentMap);
                }
            }
            rowIndex ++;
        }
    }

    /**********************************************XF Data Methods end*******************************************************/

    /**********************************************Components Data Methods Starts*******************************************************/

    public void getComponentColumnMaps(Iterator<Cell> cellItr, Map<String, String> columnNamesMap, Map<String, Object> mappingJson){
        while(cellItr.hasNext()) {
            Cell cell = cellItr.next();
            CellType cellType = cell.getCellType();
            if(cellType != CellType.BLANK) {
                String colName = cell.getStringCellValue();
                String updateColumnName= CaseUtils.toCamelCase(colName,false,' ');
                columnNamesMap.put(updateColumnName, colName);
            }
        }
        mappingJson.put("columnsMap", columnNamesMap);
    }

    public void processComponentChildComponents(Row row, Map<String, Object> components, Map<String, String> mapping, Map<String, Object> componentMap, List<Map<String, String>> mappingsList , List<String> childList ){

        String interactionIdColumn = row.getCell(COMPONENT_INTERACTION_ID_COLUMN_NUMBER, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();
        if(interactionIdColumn.contains(".")) {
            String[] interactionIdArray = interactionIdColumn.split("\\.");

            if(interactionIdArray.length == 2) {
                String componentName = interactionIdArray[interactionIdArray.length-2].trim();
                if(components.containsKey(componentName)) {
                    Map<String, Object> component = (Map<String, Object>)components.get(componentName);
                    List<Map<String, String>> mappings = (List<Map<String, String>>) component.get("mappings");
                    mappings.add(mapping);
                }else {
                    mappingsList.add(mapping);
                    componentMap.put("mappings", mappingsList);
                    componentMap.put("child", childList);
                    components.put(componentName, componentMap);
                }
                String parent = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();
                if(StringUtils.isNotEmpty(parent)) {
                    Map<String, Object> component = (Map<String, Object>)components.get(parent);
                    List<String> child = (List<String>) component.get("child");
                    if (!child.contains(componentName)){
                        child.add(componentName);
                    }
                }
            }

            if(interactionIdArray.length >= 3) {
                String compName = interactionIdArray[interactionIdArray.length-2].trim();
                if(components.containsKey(compName)) {
                    Map<String, Object> component = (Map<String, Object>)components.get(compName);
                    List<Map<String, String>> mappings = (List<Map<String, String>>) component.get("mappings");
                    mappings.add(mapping);
                }else {
                    mappingsList.add(mapping);
                    componentMap.put("mappings", mappingsList);
                    componentMap.put("child", childList);
                    components.put(compName, componentMap);
                }
                String parent = interactionIdArray[interactionIdArray.length-3].trim();
                if(StringUtils.isNotEmpty(parent)) {
                    Map<String, Object> component = (Map<String, Object>)components.get(parent);
                    List<String> child = (List<String>) component.get("child");
                    if (!child.contains(compName)){
                        child.add(compName);
                    }
                }
            }
        }
    }

    public void transformComponentMappingFileToJson(XSSFSheet sheet,Map<String, Object> mappingJson,Map<String, Object> components) throws IOException{
        Map<String, String> columnNamesMap = new LinkedHashMap<>();

        Iterator<Row> rowIterator = sheet.rowIterator();
        int rowIndex = 0;

        while(rowIterator.hasNext()) {
            boolean rowContainsDot = false;
            Row row = rowIterator.next();

            if(rowIndex == 0) {
                Iterator<Cell> cellItr = row.cellIterator();
                // column maps to mappingJson
                getComponentColumnMaps(cellItr, columnNamesMap, mappingJson);
                rowIndex ++;
                continue;
            }

            String[] colNamesList = new String[columnNamesMap.keySet().size()];
            columnNamesMap.keySet().toArray(colNamesList);

            Map<String, String> mapping = new HashMap<>();

            for(int columnNumber=0; columnNumber<row.getLastCellNum(); columnNumber++) {
                if(columnNumber == 0 || columnNumber == 1){
                    continue;
                }
                Cell cell = row.getCell(columnNumber, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                String colValue = cell.getStringCellValue();
                if(StringUtils.isNotEmpty(colValue) && colValue.contains(".")) {
                    String[] dotSplit = colValue.split("\\.");
                    mapping.put(colNamesList[columnNumber], dotSplit[dotSplit.length-1]);
                    rowContainsDot = true;
                }
                else {
                    mapping.put(colNamesList[columnNumber], colValue);
                }
            }

            Map<String, Object> componentMap = new HashMap<>();
            List<Map<String, String>> mappingsList = new ArrayList<>();
            List<String> childList = new ArrayList<>();

            // if row contains dot then we process child doc
            if(rowContainsDot) {
                processComponentChildComponents(row,components,mapping, componentMap,mappingsList,childList);
            }
            else {
                String parentComponentName= row.getCell(0).getStringCellValue();
                if(components.containsKey(parentComponentName)) {
                    Map<String, Object> component = (Map<String, Object>)components.get(parentComponentName);
                    List<Map<String, String>> mappings = (List<Map<String, String>>) component.get("mappings");
                    mappings.add(mapping);
                }else {
                    mappingsList.add(mapping);
                    componentMap.put("mappings", mappingsList);
                    componentMap.put("child", childList);
                    components.put(parentComponentName, componentMap);
                }
            }
            rowIndex ++;
        }
    }
    /**********************************************Components Data Methods end*******************************************************/

    /**********************************************Page Data Methods Starts*******************************************************/

    public void transformPageMappingFileToJson (XSSFSheet sheet,Map<String, Object> mappingJson,Map<String, Object> pageData) {
        Map<String, String> columnNamesMap = new LinkedHashMap<>();
        Map<String, Object> componentMap = new HashMap<>();

        Iterator<Row> rowIterator = sheet.rowIterator();
        int rowIndex = 0;

        while(rowIterator.hasNext()) {
            Row row = rowIterator.next();
            if(rowIndex == 0) {
                Iterator<Cell> cellItr = row.cellIterator();
                while(cellItr.hasNext()) {
                    Cell cell = cellItr.next();
                    CellType cellType = cell.getCellType();
                    if(cellType != CellType.BLANK) {
                        String colName = cell.getStringCellValue();
                        String updateColumnName=CaseUtils.toCamelCase(colName,false,' ');
                        columnNamesMap.put(updateColumnName, colName);
                    }
                }
                rowIndex ++;
                continue;
            }

            String[] colNamesList = new String[columnNamesMap.keySet().size()];
            columnNamesMap.keySet().toArray(colNamesList);

            List<Map<String, String>> mappingsList = new ArrayList<>();

            Map<String, String> mapping = new HashMap<>();

            for(int columnNumber=0; columnNumber<row.getLastCellNum(); columnNumber++) {
                if(columnNumber == 0){
                    mapping.put(colNamesList[columnNumber], "componentName");
                    continue;
                }
                Cell cell = row.getCell(columnNumber, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                String colValue = cell.getStringCellValue();

                mapping.put(colNamesList[columnNumber], colValue);
            }

            // to get component name
            String componentName= row.getCell(0).getStringCellValue();

            // checking if components mappings contains with the current parent name
            // if yes then will get those components mappings list and add the current mapping to the list
            if(pageData.containsKey(componentName)) {
                Map<String, Object> component = (Map<String, Object>)pageData.get(componentName);
                List<Map<String, String>> mappings = (List<Map<String, String>>) component.get("mappings");
                mappings.add(mapping);
            }
            else {
                mappingsList.add(mapping);
                componentMap.put("mappings", mappingsList);
                pageData.put(componentName, componentMap);
            }
            rowIndex ++;
        }
    }

    /**********************************************Page Data Methods end*******************************************************/

    /*----------------------------------------------forms data---------------------------------*/
    public void formsMappingFileToJson(XSSFSheet sheet, Map<String, Object> mappingJson, Map<String, Object> formsData) {
        Map<String, String> columnNamesMap = new LinkedHashMap<>();
        Map<String, Object> componentMap = new HashMap<>();

        Iterator<Row> rowIterator = sheet.rowIterator();
        int rowIndex = 0;

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            if (rowIndex == 0) {
                Iterator<Cell> cellItr = row.cellIterator();
                while (cellItr.hasNext()) {
                    Cell cell = cellItr.next();
                    CellType cellType = cell.getCellType();
                    if (cellType != CellType.BLANK) {
                        String colName = cell.getStringCellValue();
                        String updateColumnName = CaseUtils.toCamelCase(colName, false, ' ');
                        columnNamesMap.put(updateColumnName, colName);
                    }
                }
                rowIndex++;
                continue;
            }

            String[] colNamesList = new String[columnNamesMap.keySet().size()];
            columnNamesMap.keySet().toArray(colNamesList);

            List<Map<String, String>> mappingsList = new ArrayList<>();

            Map<String, String> mapping = new HashMap<>();

            for (int columnNumber = 0; columnNumber < row.getLastCellNum(); columnNumber++) {
                Cell cell = row.getCell(columnNumber, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                String colValue = cell.getStringCellValue();

                mapping.put(colNamesList[columnNumber], colValue);
            }

            mappingsList.add(mapping);
            componentMap.put("mappings", mappingsList);
            formsData.put("aemform", componentMap);

            rowIndex++;
        }
    }

}

/*            if(components.containsKey(parentComponentName)) {
                Map<String, Object> component = objectMapper.convertValue(components.get(parentComponentName), new TypeReference<Map<String, Object>>() {});
                List<Map<String, String>> mappings = objectMapper.convertValue(component.get("mappings"), new TypeReference<List<Map<String, String>>>() {});
                mappings.add(mapping);
                componentMap.put("mappings", mappings);
            }
            else {
                mappingsList.add(mapping);
                componentMap.put("mappings", mappingsList);
            }
            components.put(parentComponentName, componentMap);*/