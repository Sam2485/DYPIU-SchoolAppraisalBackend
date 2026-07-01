package com.director_appraisal.director_appraisal.service;

import com.director_appraisal.director_appraisal.model.Submission;
import com.director_appraisal.director_appraisal.repository.SnapshotRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.Column;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TableDataPromotionService {

    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;
    private final SnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    private Map<String, TableConfig> tableConfigs = Map.of();

    @PostConstruct
    void initializeTableConfigs() {
        Map<String, TableConfig> configs = new LinkedHashMap<>();

        entityManager.getMetamodel().getEntities().forEach(entityType -> {
            Class<?> javaType = entityType.getJavaType();
            Package javaPackage = javaType.getPackage();
            if (javaPackage == null) {
                return;
            }

            String packageName = javaPackage.getName();
            if (!packageName.endsWith(".model.academic") && !packageName.endsWith(".model.administrative")) {
                return;
            }

            Table table = javaType.getAnnotation(Table.class);
            if (table == null || table.name().isBlank()) {
                return;
            }

            List<ColumnConfig> columns = new ArrayList<>();
            for (Field field : javaType.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isAnnotationPresent(Id.class)) {
                    continue;
                }
                if ("id".equals(field.getName()) || "submissionId".equals(field.getName())) {
                    continue;
                }

                Column column = field.getAnnotation(Column.class);
                String columnName = column != null && !column.name().isBlank()
                        ? column.name()
                        : camelToSnake(field.getName());
                columns.add(new ColumnConfig(columnName, buildCandidates(field.getName(), columnName)));
            }

            configs.put(table.name(), new TableConfig(table.name(), columns));
        });

        tableConfigs = Map.copyOf(configs);
    }

    @Transactional
    public void syncNormalizedTablesAndClearSnapshots(Submission submission) {
        syncNormalizedTables(submission);
        snapshotRepository.deleteBySubmissionId(submission.getId());
    }

    private void syncNormalizedTables(Submission submission) {
        String tablesData = submission.getTablesData();
        if (tablesData == null || tablesData.isBlank() || "{}".equals(tablesData.trim())) {
            return;
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(tablesData);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot sync submission tables because tables data is invalid JSON", e);
        }

        if (!root.isObject()) {
            throw new IllegalStateException("Cannot sync submission tables because tables data must be a JSON object");
        }

        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String tableName = toTableName(entry.getKey());
            TableConfig tableConfig = tableConfigs.get(tableName);
            JsonNode rowsNode = entry.getValue();

            if (tableConfig == null || !rowsNode.isArray()) {
                continue;
            }

            replaceTableRows(submission.getId(), tableConfig, rowsNode);
        }
    }

    private void replaceTableRows(Long submissionId, TableConfig tableConfig, JsonNode rowsNode) {
        jdbcTemplate.update("DELETE FROM public." + tableConfig.tableName() + " WHERE submission_id = ?", submissionId);

        for (JsonNode rowNode : rowsNode) {
            if (!rowNode.isObject()) {
                continue;
            }

            Map<String, JsonNode> rowValues = normalizeRow(rowNode);
            List<String> columns = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            boolean hasValue = false;

            columns.add("submission_id");
            values.add(submissionId);

            for (ColumnConfig column : tableConfig.columns()) {
                String value = valueForColumn(rowValues, column);
                columns.add(column.columnName());
                values.add(value);
                if (value != null && !value.isBlank()) {
                    hasValue = true;
                }
            }

            if (!hasValue) {
                continue;
            }

            String placeholders = String.join(", ", columns.stream().map(column -> "?").toList());
            String sql = "INSERT INTO public." + tableConfig.tableName()
                    + " (" + String.join(", ", columns) + ") VALUES (" + placeholders + ")";
            jdbcTemplate.update(sql, values.toArray());
        }
    }

    private Map<String, JsonNode> normalizeRow(JsonNode rowNode) {
        Map<String, JsonNode> rowValues = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = rowNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            rowValues.putIfAbsent(normalize(entry.getKey()), entry.getValue());
        }
        return rowValues;
    }

    private String valueForColumn(Map<String, JsonNode> rowValues, ColumnConfig column) {
        for (String candidate : column.candidates()) {
            JsonNode value = rowValues.get(candidate);
            if (value != null && !value.isNull()) {
                return stringify(value);
            }
        }
        return null;
    }

    private String stringify(JsonNode value) {
        if (value.isTextual()) {
            return value.asText();
        }
        if (value.isNumber() || value.isBoolean()) {
            return value.asText();
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return value.asText();
        }
    }

    private static Set<String> buildCandidates(String fieldName, String columnName) {
        Set<String> candidates = new HashSet<>();
        candidates.add(normalize(fieldName));
        candidates.add(normalize(columnName));
        candidates.add(normalize(toDisplayLabel(fieldName)));

        if (columnName.endsWith("_name")) {
            candidates.add(normalize(columnName.substring(0, columnName.length() - "_name".length())));
        }
        if (fieldName.endsWith("Name") && fieldName.length() > "Name".length()) {
            candidates.add(normalize(fieldName.substring(0, fieldName.length() - "Name".length())));
        }

        if ("students_admitted".equals(columnName)) {
            candidates.add(normalize("No. of Students Admitted"));
        }
        if ("attachment".equals(columnName)) {
            candidates.add(normalize("Attachment (Attach List of the Students)"));
        }

        return candidates;
    }

    private static String toTableName(String key) {
        return key.trim()
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "")
                .toLowerCase();
    }

    private static String camelToSnake(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }

    private static String toDisplayLabel(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1 $2");
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private record TableConfig(String tableName, List<ColumnConfig> columns) {
    }

    private record ColumnConfig(String columnName, Set<String> candidates) {
    }
}
