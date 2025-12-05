package com.quip.coa.service;

import com.quip.coa.utilities.Constants;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CaseUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class ConvertMappingFileToJsonV2 {

    private static final Logger log = LoggerFactory.getLogger(ConvertMappingFileToJsonV2.class);

    public static final int UNIQUE_ID_COLUMN_NUMBER = 5;

    public Map<String, Object> convertMappingFileToJson(InputStream inputFile, String domain) throws IOException {

        Map<String, Object> mappingJson = new LinkedHashMap<>();
        Map<String, Object> components = new HashMap<>();

        try (XSSFWorkbook workbook = new XSSFWorkbook(inputFile)) {

            XSSFSheet sheet = workbook.getSheet("QUIPMasterMapping");
            log.info("Loaded Excel sheet: QUIPMasterMapping");

            transformComponentMappingFileToJson(sheet, mappingJson, components);
        }

        mappingJson.put(Constants.FIELD_COMPONENTS_DATA, components);
        mappingJson.put("domain", domain);

        log.info("Finished processing mapping file for domain: {}", domain);

        return mappingJson;
    }

    /**********************************************
     * READ HEADER MAPS
     **********************************************/
    public void getComponentColumnMaps(Iterator<Cell> cellItr,
                                       Map<String, String> columnNamesMap,
                                       Map<String, Object> mappingJson) {

        while (cellItr.hasNext()) {
            Cell cell = cellItr.next();

            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String colName = cell.getStringCellValue();

                if (StringUtils.isNotEmpty(colName)) {
                    String camel = CaseUtils.toCamelCase(colName, false, ' ');
                    columnNamesMap.put(camel, colName);

                    log.info("[HEADER] Mapped header: '{}' (camel-case '{}')", colName, camel);
                }
            }
        }

        mappingJson.put("columnsMap", columnNamesMap);
    }

    /**********************************************
     * HANDLE CHILD COMPONENTS
     **********************************************/
    public void processComponentChildComponents(
            Row row,
            Map<String, Object> components,
            Map<String, String> mapping,
            Map<String, Object> componentMap,
            List<Map<String, String>> mappingsList,
            List<String> childList) {

        String uniqueIdColumn = row
                .getCell(UNIQUE_ID_COLUMN_NUMBER, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)
                .getStringCellValue();

        if (!uniqueIdColumn.contains(".")) return;

        String[] parts = uniqueIdColumn.split("\\.");
        String componentName = parts[parts.length - 2].trim();
        String parentName = (parts.length >= 3)
                ? parts[parts.length - 3].trim()
                : row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();

        log.info("[CHILD] Processing component='{}' parent='{}'", componentName, parentName);

        /***** COMPONENT LOGIC *****/
        Map<String, Object> component = (Map<String, Object>) components.get(componentName);

        if (component != null) {
            ((List<Map<String, String>>) component.get("mappings")).add(mapping);
            log.info("[CHILD] Added mapping to existing component '{}'", componentName);
        } else {
            mappingsList.add(mapping);
            componentMap.put("mappings", mappingsList);
            componentMap.put("child", childList);
            components.put(componentName, componentMap);

            log.info("[CHILD] Created component '{}'", componentName);
        }

        /***** PARENT LOGIC *****/
        if (StringUtils.isNotEmpty(parentName)) {
            Map<String, Object> parentComponent = (Map<String, Object>) components.get(parentName);

            if (parentComponent == null) {
                parentComponent = new HashMap<>();
                parentComponent.put("mappings", new ArrayList<>());
                parentComponent.put("child", new ArrayList<>());
                components.put(parentName, parentComponent);

                log.warn("[CHILD] Created missing parent '{}'", parentName);
            }

            List<String> child = (List<String>) parentComponent.get("child");

            if (!child.contains(componentName)) {
                child.add(componentName);
                log.info("[CHILD] Linked '{}' -> child '{}'", parentName, componentName);
            }
        }
    }

    /**********************************************
     * MAIN PARSER — FULLY SAFE + LOGGED
     **********************************************/
    public void transformComponentMappingFileToJson(
            XSSFSheet sheet,
            Map<String, Object> mappingJson,
            Map<String, Object> components) throws IOException {

        Map<String, String> columnNamesMap = new LinkedHashMap<>();

        Iterator<Row> rowIterator = sheet.rowIterator();
        int rowIndex = 0;

        List<String> headerColumns = new ArrayList<>();

        while (rowIterator.hasNext()) {

            Row row = rowIterator.next();

            /**********************************
             * HEADER ROW
             **********************************/
            if (rowIndex == 0) {

                int lastCol = row.getLastCellNum();
                log.info("[HEADER] Total columns in header: {}", lastCol);

                for (int i = 0; i < lastCol; i++) {

                    Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    String text = cell.getStringCellValue();

                    if (StringUtils.isNotEmpty(text)) {
                        String camel = CaseUtils.toCamelCase(text, false, ' ');
                        headerColumns.add(camel);
                        columnNamesMap.put(camel, text);

                        log.info("[HEADER] Column {} => '{}' (camel '{}')", i, text, camel);
                    } else {
                        headerColumns.add("");
                        log.warn("[HEADER] Column {} has EMPTY header", i);
                    }
                }

                mappingJson.put("columnsMap", columnNamesMap);
                rowIndex++;
                continue;
            }

            /**********************************
             * DATA ROWS
             **********************************/

            int lastCol = row.getLastCellNum();
            log.info("\n[ROW] Processing row {} with {} columns", rowIndex, lastCol);

            Map<String, String> mapping = new HashMap<>();
            boolean rowContainsDot = false;

            for (int col = 0; col < lastCol; col++) {

                if (col >= headerColumns.size()) {
                    log.warn("[ROW] Extra column at index {} (no header) — skipping", col);
                    continue;
                }

                String header = headerColumns.get(col);

                if (StringUtils.isEmpty(header)) {
                    log.warn("[ROW] Skipping column {} (empty header)", col);
                    continue;
                }

                Cell cell = row.getCell(col, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                String value = cell.getStringCellValue();

                if (StringUtils.isNotEmpty(value) && value.contains(".")) {
                    String[] parts = value.split("\\.");
                    value = parts[parts.length - 1];
                    rowContainsDot = true;
                }

                mapping.put(header, value);

                log.info("[MAP] Col {} header '{}' = '{}'", col, header, value);
            }

            Map<String, Object> componentMap = new HashMap<>();
            List<Map<String, String>> mappingsList = new ArrayList<>();
            List<String> childList = new ArrayList<>();

            if (rowContainsDot) {
                processComponentChildComponents(row, components, mapping, componentMap, mappingsList, childList);
            } else {
                String parentName = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();

                log.info("[PARENT] Processing top-level component '{}'", parentName);

                Map<String, Object> parent = (Map<String, Object>) components.get(parentName);

                if (parent != null) {
                    ((List<Map<String, String>>) parent.get("mappings")).add(mapping);
                    log.info("[PARENT] Added mapping to existing parent '{}'", parentName);
                } else {
                    mappingsList.add(mapping);
                    componentMap.put("mappings", mappingsList);
                    componentMap.put("child", childList);
                    components.put(parentName, componentMap);

                    log.info("[PARENT] Created parent '{}'", parentName);
                }
            }

            rowIndex++;
        }
    }
}
