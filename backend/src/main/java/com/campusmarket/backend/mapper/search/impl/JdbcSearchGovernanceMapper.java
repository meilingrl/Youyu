package com.campusmarket.backend.mapper.search.impl;

import com.campusmarket.backend.common.support.JdbcGeneratedKey;
import com.campusmarket.backend.mapper.search.SearchGovernanceMapper;
import java.sql.PreparedStatement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

@Primary
@Component
public class JdbcSearchGovernanceMapper implements SearchGovernanceMapper {

    private final JdbcTemplate jdbcTemplate;

    public JdbcSearchGovernanceMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Map<String, Object>> findAll() {
        return jdbcTemplate.queryForList(
                "SELECT id, rule_type, keyword, display_label, is_active, created_at, updated_at "
                + "FROM search_governance_rules ORDER BY rule_type, keyword")
                .stream().map(this::toApiMap).toList();
    }

    @Override
    public List<Map<String, Object>> findAllActive() {
        return jdbcTemplate.queryForList(
                "SELECT id, rule_type, keyword, display_label, is_active, created_at, updated_at "
                + "FROM search_governance_rules WHERE is_active = 1 ORDER BY id ASC")
                .stream().map(this::toApiMap).toList();
    }

    @Override
    public Optional<Map<String, Object>> findById(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, rule_type, keyword, display_label, is_active, created_at, updated_at "
                + "FROM search_governance_rules WHERE id = ?", id);
        return rows.stream().findFirst().map(this::toApiMap);
    }

    @Override
    public Long insert(Map<String, Object> rule) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO search_governance_rules (rule_type, keyword, display_label) VALUES (?, ?, ?)",
                    new String[]{"id"});
            ps.setString(1, string(rule.get("ruleType")));
            ps.setString(2, string(rule.get("keyword")));
            ps.setString(3, string(rule.get("displayLabel")));
            return ps;
        }, keyHolder);
        return JdbcGeneratedKey.requiredLong(keyHolder, "search governance rule id");
    }

    @Override
    public void update(Long id, Map<String, Object> rule) {
        jdbcTemplate.update("""
                UPDATE search_governance_rules
                SET keyword = ?, display_label = ?, is_active = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                string(rule.getOrDefault("keyword", rule.get("KEYWORD"))),
                string(rule.getOrDefault("displayLabel", rule.get("DISPLAY_LABEL"))),
                bool(rule.getOrDefault("isActive", rule.get("IS_ACTIVE"))),
                id);
    }

    @Override
    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM search_governance_rules WHERE id = ?", id);
    }

    @Override
    public List<Map<String, Object>> findActiveByType(String ruleType) {
        return jdbcTemplate.queryForList(
                "SELECT id, rule_type, keyword, display_label, is_active, created_at, updated_at "
                + "FROM search_governance_rules WHERE rule_type = ? AND is_active = 1", ruleType)
                .stream().map(this::toApiMap).toList();
    }

    private Map<String, Object> toApiMap(Map<String, Object> row) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", first(row, "ID"));
        map.put("ruleType", first(row, "RULE_TYPE"));
        map.put("keyword", first(row, "KEYWORD"));
        map.put("displayLabel", first(row, "DISPLAY_LABEL"));
        map.put("isActive", boolOrNull(first(row, "IS_ACTIVE")));
        map.put("createdAt", first(row, "CREATED_AT"));
        map.put("updatedAt", first(row, "UPDATED_AT"));
        return map;
    }

    private Object first(Map<String, Object> row, String upperKey) {
        return row.get(upperKey);
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean bool(Object value) {
        if (value == null) return true;
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.intValue() != 0;
        return "1".equals(String.valueOf(value)) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private Object boolOrNull(Object value) {
        if (value == null) return true;
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.intValue() != 0;
        return "1".equals(String.valueOf(value)) || "true".equalsIgnoreCase(String.valueOf(value));
    }
}
