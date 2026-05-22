package com.youyu.backend.mapper.user.impl;

import com.youyu.backend.common.support.JdbcGeneratedKey;
import com.youyu.backend.mapper.user.StudentVerificationMapper;
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
import org.springframework.stereotype.Component;

@Primary
@Component
public class JdbcStudentVerificationMapper implements StudentVerificationMapper {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JdbcTemplate jdbcTemplate;

    public JdbcStudentVerificationMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Map<String, Object>> findAll() {
        return jdbcTemplate.queryForList(baseSql() + " ORDER BY sv.submitted_at DESC, sv.id DESC").stream()
                .map(this::normalize)
                .toList();
    }

    @Override
    public List<Map<String, Object>> findVerificationsPaged(String keyword, String status, int offset, int limit) {
        StringBuilder sql = new StringBuilder(baseSql()).append(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendVerificationFilters(sql, args, keyword, status);
        sql.append(" ORDER BY sv.submitted_at DESC, sv.id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbcTemplate.queryForList(sql.toString(), args.toArray()).stream()
                .map(this::normalize)
                .toList();
    }

    @Override
    public long countVerifications(String keyword, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM student_verifications sv JOIN users u ON u.id = sv.user_id WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendVerificationFilters(sql, args, keyword, status);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0L : count;
    }

    @Override
    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM student_verifications", Long.class);
        return count == null ? 0L : count;
    }

    private void appendVerificationFilters(StringBuilder sql, List<Object> args, String keyword, String status) {
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (LOWER(COALESCE(sv.student_no, '')) LIKE ? OR LOWER(COALESCE(sv.real_name, '')) LIKE ? OR LOWER(COALESCE(sv.campus_email, '')) LIKE ?)");
            String like = "%" + keyword.trim().toLowerCase(java.util.Locale.ROOT) + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND sv.verification_status = ?");
            args.add(status.trim());
        }
    }

    @Override
    public Optional<Map<String, Object>> findById(Long id) {
        List<Map<String, Object>> items = jdbcTemplate.queryForList(baseSql() + " WHERE sv.id = ?", id).stream()
                .map(this::normalize)
                .toList();
        return items.stream().findFirst();
    }

    @Override
    public Optional<Map<String, Object>> findLatestByUserId(Long userId) {
        List<Map<String, Object>> items = jdbcTemplate.queryForList(
                        baseSql() + " WHERE sv.user_id = ? ORDER BY sv.submitted_at DESC, sv.id DESC LIMIT 1",
                        userId
                ).stream()
                .map(this::normalize)
                .toList();
        return items.stream().findFirst();
    }

    @Override
    public boolean existsApprovedStudentNoForOtherUser(String studentNo, Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM student_verifications
                        WHERE student_no = ? AND verification_status = 'approved' AND user_id <> ?
                        """,
                Integer.class,
                studentNo, userId
        );
        return count != null && count > 0;
    }

    @Override
    public Long insert(Long userId,
                       String studentNo,
                       String realName,
                       String college,
                       String major,
                       String grade,
                       String campusEmail,
                       String verificationMethod) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO student_verifications
                            (user_id, student_no, real_name, college, major, grade, campus_email, verification_method,
                             verification_status, submitted_at, reject_reason, review_note, risk_flag, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'pending_review', CURRENT_TIMESTAMP, '', '', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                            """,
                    new String[]{"id"}
            );
            statement.setLong(1, userId);
            statement.setString(2, studentNo);
            statement.setString(3, realName);
            statement.setString(4, blankToNull(college));
            statement.setString(5, blankToNull(major));
            statement.setString(6, blankToNull(grade));
            statement.setString(7, blankToNull(campusEmail));
            statement.setString(8, isBlank(verificationMethod) ? "manual_review" : verificationMethod);
            return statement;
        }, keyHolder);
        return JdbcGeneratedKey.requiredLong(keyHolder, "student verification id");
    }

    @Override
    public void review(Long verificationId,
                       String verificationStatus,
                       String rejectReason,
                       String reviewNote,
                       Long reviewerId) {
        jdbcTemplate.update(
                """
                        UPDATE student_verifications
                        SET verification_status = ?, reject_reason = ?, review_note = ?, reviewer_id = ?,
                            reviewed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                verificationStatus,
                rejectReason == null ? "" : rejectReason,
                reviewNote == null ? "" : reviewNote,
                reviewerId,
                verificationId
        );
    }

    private String baseSql() {
        return """
                SELECT sv.id, sv.user_id, sv.student_no, sv.real_name, sv.college, sv.major, sv.grade,
                       sv.campus_email, sv.verification_method, sv.verification_status, sv.submitted_at,
                       sv.reviewed_at, sv.reviewer_id, sv.reject_reason, sv.review_note, sv.risk_flag,
                       u.nickname AS user_nickname, reviewer.nickname AS reviewer_nickname
                FROM student_verifications sv
                JOIN users u ON u.id = sv.user_id
                LEFT JOIN users reviewer ON reviewer.id = sv.reviewer_id
                """;
    }

    private Map<String, Object> normalize(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", row.get("id"));
        result.put("verificationId", row.get("id"));
        result.put("userId", row.get("user_id"));
        result.put("studentNo", row.get("student_no"));
        result.put("realName", row.get("real_name"));
        result.put("college", defaultString(row.get("college")));
        result.put("collegeName", defaultString(row.get("college")));
        result.put("major", defaultString(row.get("major")));
        result.put("majorName", defaultString(row.get("major")));
        result.put("grade", defaultString(row.get("grade")));
        result.put("gradeName", defaultString(row.get("grade")));
        result.put("campusEmail", defaultString(row.get("campus_email")));
        result.put("verificationMethod", row.get("verification_method"));
        result.put("verificationStatus", row.get("verification_status"));
        result.put("riskFlag", toBoolean(row.get("risk_flag")));
        result.put("submittedAt", format(row.get("submitted_at")));
        result.put("reviewedAt", format(row.get("reviewed_at")));
        result.put("reviewerId", row.get("reviewer_id"));
        result.put("reviewedBy", reviewerLabel(row));
        result.put("rejectReason", defaultString(row.get("reject_reason")));
        result.put("reviewNote", defaultString(row.get("review_note")));
        result.put("userNickname", defaultString(row.get("user_nickname")));
        return result;
    }

    private String reviewerLabel(Map<String, Object> row) {
        Object reviewerId = row.get("reviewer_id");
        if (reviewerId == null) {
            return "";
        }
        String nickname = defaultString(row.get("reviewer_nickname"));
        return nickname.isBlank() ? "管理员#" + reviewerId : nickname;
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

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private String defaultString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
