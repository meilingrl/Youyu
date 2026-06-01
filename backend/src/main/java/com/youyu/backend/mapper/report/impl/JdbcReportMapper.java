package com.youyu.backend.mapper.report.impl;

import com.youyu.backend.mapper.report.ReportMapper;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

@Primary
@Component
public class JdbcReportMapper implements ReportMapper {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JdbcTemplate jdbcTemplate;

    public JdbcReportMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Map<String, Object>> findAll() {
        return jdbcTemplate.queryForList("SELECT * FROM reports ORDER BY submitted_at DESC").stream()
                .map(this::normalizeReport)
                .toList();
    }

    @Override
    public List<Map<String, Object>> findReportsPaged(String keyword, String status, String targetType, int offset, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM reports WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendReportFilters(sql, args, keyword, status, targetType);
        sql.append(" ORDER BY submitted_at DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbcTemplate.queryForList(sql.toString(), args.toArray()).stream()
                .map(this::normalizeReport)
                .toList();
    }

    @Override
    public long countReports(String keyword, String status, String targetType) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM reports WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendReportFilters(sql, args, keyword, status, targetType);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0L : count;
    }

    @Override
    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reports", Long.class);
        return count == null ? 0L : count;
    }

    private void appendReportFilters(StringBuilder sql, List<Object> args, String keyword, String status, String targetType) {
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (LOWER(COALESCE(target_label, '')) LIKE ? OR LOWER(COALESCE(reporter_name, '')) LIKE ? OR LOWER(COALESCE(content, '')) LIKE ?)");
            String like = "%" + keyword.trim().toLowerCase(java.util.Locale.ROOT) + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(status.trim());
        }
        if (targetType != null && !targetType.isBlank()) {
            sql.append(" AND target_type = ?");
            args.add(targetType.trim());
        }
    }

    @Override
    public Optional<Map<String, Object>> findById(Long id) {
        List<Map<String, Object>> reports = jdbcTemplate.queryForList("SELECT * FROM reports WHERE id = ?", id).stream()
                .map(this::normalizeReport)
                .toList();
        return reports.stream().findFirst();
    }

    @Override
    public List<Map<String, Object>> findByTarget(String targetType, Long targetId) {
        return jdbcTemplate.queryForList(
                        "SELECT * FROM reports WHERE target_type = ? AND target_id = ? ORDER BY submitted_at DESC, id DESC",
                        targetType,
                        targetId
                ).stream()
                .map(this::normalizeReport)
                .toList();
    }

    @Override
    public Long insert(Long reporterUserId,
                       String reporterName,
                       String targetType,
                       Long targetId,
                       String targetLabel,
                       String reasonType,
                       String content) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                            INSERT INTO reports (
                                reporter_user_id, reporter_name, target_type, target_id,
                                target_label, reason_type, content, status
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, 'pending')
                            """,
                    new String[]{"id"}
            );
            ps.setLong(1, reporterUserId);
            ps.setString(2, reporterName);
            ps.setString(3, targetType);
            ps.setLong(4, targetId);
            ps.setString(5, targetLabel);
            ps.setString(6, reasonType);
            ps.setString(7, content);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    @Override
    public void updateStatus(Long reportId, String status, String processedBy, String resolution) {
        jdbcTemplate.update(
                """
                        UPDATE reports
                        SET status = ?, processed_by = ?, resolution = ?,
                            processed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                status, processedBy, resolution, reportId
        );
    }

    private Map<String, Object> normalizeReport(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", row.get("id"));
        result.put("reporterUserId", toLong(row.get("reporter_user_id")));
        result.put("reporterName", defaultString(row.get("reporter_name")));
        result.put("targetType", defaultString(row.get("target_type")));
        result.put("targetId", toLong(row.get("target_id")));
        result.put("targetLabel", defaultString(row.get("target_label")));
        result.put("reasonType", defaultString(row.get("reason_type")));
        result.put("content", defaultString(row.get("content")));
        result.put("status", defaultString(row.get("status")));
        result.put("submittedAt", format(row.get("submitted_at")));
        result.put("processedAt", format(row.get("processed_at")));
        result.put("processedBy", defaultString(row.get("processed_by")));
        result.put("resolution", defaultString(row.get("resolution")));
        return result;
    }

    private String format(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().format(DATETIME_FORMATTER);
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.format(DATETIME_FORMATTER);
        }
        return String.valueOf(value);
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private String defaultString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
