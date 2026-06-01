package com.youyu.backend.mapper.support.impl;

import com.youyu.backend.common.support.JdbcGeneratedKey;
import com.youyu.backend.mapper.support.SupportTicketMapper;
import java.sql.PreparedStatement;
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
public class JdbcSupportTicketMapper implements SupportTicketMapper {

    private final JdbcTemplate jdbcTemplate;

    public JdbcSupportTicketMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Map<String, Object>> findTicketById(Long ticketId) {
        return jdbcTemplate.queryForList("""
                        SELECT st.*, requester.username AS requester_username, requester.nickname AS requester_nickname,
                               assignee.username AS assignee_username, assignee.nickname AS assignee_nickname
                        FROM support_tickets st
                        LEFT JOIN users requester ON requester.id = st.requester_user_id
                        LEFT JOIN users assignee ON assignee.id = st.assigned_admin_user_id
                        WHERE st.id = ?
                        """, ticketId).stream()
                .findFirst()
                .map(this::normalizeTicket);
    }

    @Override
    public Long insertTicket(Map<String, Object> command) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO support_tickets (
                        ticket_no, requester_user_id, category, subject, content, status, priority,
                        related_type, related_id, last_replied_by, last_replied_at, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, 'open', ?, ?, ?, 'user', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, new String[]{"id"});
            ps.setString(1, String.valueOf(command.get("ticketNo")));
            ps.setLong(2, toLong(command.get("requesterUserId")));
            ps.setString(3, String.valueOf(command.get("category")));
            ps.setString(4, String.valueOf(command.get("subject")));
            ps.setString(5, String.valueOf(command.get("content")));
            ps.setString(6, String.valueOf(command.get("priority")));
            ps.setString(7, nullableString(command.get("relatedType")));
            Long relatedId = nullableLong(command.get("relatedId"));
            if (relatedId == null) {
                ps.setObject(8, null);
            } else {
                ps.setLong(8, relatedId);
            }
            return ps;
        }, keyHolder);
        return JdbcGeneratedKey.requiredLong(keyHolder, "support_ticket_id");
    }

    @Override
    public Long insertMessage(Long ticketId, Long senderUserId, String senderRole, String messageType, String content) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO support_ticket_messages (
                        ticket_id, sender_user_id, sender_role, message_type, content, created_at
                    ) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    """, new String[]{"id"});
            ps.setLong(1, ticketId);
            if (senderUserId == null) {
                ps.setObject(2, null);
            } else {
                ps.setLong(2, senderUserId);
            }
            ps.setString(3, senderRole);
            ps.setString(4, messageType);
            ps.setString(5, content);
            return ps;
        }, keyHolder);
        return JdbcGeneratedKey.requiredLong(keyHolder, "support_ticket_message_id");
    }

    @Override
    public List<Map<String, Object>> findUserTickets(Long requesterUserId, String status, int offset, int limit) {
        QueryParts query = buildUserFilter("""
                SELECT st.*, requester.username AS requester_username, requester.nickname AS requester_nickname,
                       assignee.username AS assignee_username, assignee.nickname AS assignee_nickname
                FROM support_tickets st
                LEFT JOIN users requester ON requester.id = st.requester_user_id
                LEFT JOIN users assignee ON assignee.id = st.assigned_admin_user_id
                WHERE st.requester_user_id = ?
                """, requesterUserId, status);
        query.sql().append(" ORDER BY st.updated_at DESC, st.id DESC LIMIT ? OFFSET ?");
        query.args().add(limit);
        query.args().add(offset);
        return jdbcTemplate.queryForList(query.sql().toString(), query.args().toArray()).stream()
                .map(this::normalizeTicket)
                .toList();
    }

    @Override
    public long countUserTickets(Long requesterUserId, String status) {
        QueryParts query = buildUserFilter("""
                SELECT COUNT(*)
                FROM support_tickets st
                WHERE st.requester_user_id = ?
                """, requesterUserId, status);
        Long count = jdbcTemplate.queryForObject(query.sql().toString(), Long.class, query.args().toArray());
        return count == null ? 0L : count;
    }

    @Override
    public List<Map<String, Object>> findAdminTickets(String status,
                                                      String category,
                                                      Long assignedAdminUserId,
                                                      String keyword,
                                                      int offset,
                                                      int limit) {
        QueryParts query = buildAdminFilter("""
                SELECT st.*, requester.username AS requester_username, requester.nickname AS requester_nickname,
                       assignee.username AS assignee_username, assignee.nickname AS assignee_nickname
                FROM support_tickets st
                LEFT JOIN users requester ON requester.id = st.requester_user_id
                LEFT JOIN users assignee ON assignee.id = st.assigned_admin_user_id
                WHERE 1=1
                """, status, category, assignedAdminUserId, keyword);
        query.sql().append(" ORDER BY st.updated_at DESC, st.id DESC LIMIT ? OFFSET ?");
        query.args().add(limit);
        query.args().add(offset);
        return jdbcTemplate.queryForList(query.sql().toString(), query.args().toArray()).stream()
                .map(this::normalizeTicket)
                .toList();
    }

    @Override
    public long countAdminTickets(String status, String category, Long assignedAdminUserId, String keyword) {
        QueryParts query = buildAdminFilter("""
                SELECT COUNT(*)
                FROM support_tickets st
                LEFT JOIN users requester ON requester.id = st.requester_user_id
                WHERE 1=1
                """, status, category, assignedAdminUserId, keyword);
        Long count = jdbcTemplate.queryForObject(query.sql().toString(), Long.class, query.args().toArray());
        return count == null ? 0L : count;
    }

    @Override
    public List<Map<String, Object>> findMessages(Long ticketId, boolean includeInternalNotes) {
        String sql = """
                SELECT m.*, sender.username AS sender_username, sender.nickname AS sender_nickname
                FROM support_ticket_messages m
                LEFT JOIN users sender ON sender.id = m.sender_user_id
                WHERE m.ticket_id = ?
                """;
        List<Object> args = new ArrayList<>();
        args.add(ticketId);
        if (!includeInternalNotes) {
            sql += " AND m.message_type <> ?";
            args.add("internal_note");
        }
        sql += " ORDER BY m.created_at ASC, m.id ASC";
        return jdbcTemplate.queryForList(sql, args.toArray()).stream()
                .map(this::normalizeMessage)
                .toList();
    }

    @Override
    public int updateTicketAfterMessage(Long ticketId, String lastRepliedBy) {
        return jdbcTemplate.update("""
                UPDATE support_tickets
                SET last_replied_by = ?, last_replied_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, lastRepliedBy, ticketId);
    }

    @Override
    public int updateStatus(Long ticketId, String status, Long assignedAdminUserId, boolean assignAdmin) {
        StringBuilder sql = new StringBuilder("""
                UPDATE support_tickets
                SET status = ?,
                    resolved_at = CASE WHEN ? = 'resolved' THEN CURRENT_TIMESTAMP ELSE resolved_at END,
                    closed_at = CASE WHEN ? = 'closed' THEN CURRENT_TIMESTAMP ELSE closed_at END,
                    updated_at = CURRENT_TIMESTAMP
                """);
        List<Object> args = new ArrayList<>();
        args.add(status);
        args.add(status);
        args.add(status);
        if (assignAdmin) {
            sql.append(", assigned_admin_user_id = ?");
            args.add(assignedAdminUserId);
        }
        sql.append(" WHERE id = ?");
        args.add(ticketId);
        return jdbcTemplate.update(sql.toString(), args.toArray());
    }

    private QueryParts buildUserFilter(String baseSql, Long requesterUserId, String status) {
        StringBuilder sql = new StringBuilder(baseSql);
        List<Object> args = new ArrayList<>();
        args.add(requesterUserId);
        if (status != null && !status.isBlank()) {
            sql.append(" AND st.status = ?");
            args.add(status.trim());
        }
        return new QueryParts(sql, args);
    }

    private QueryParts buildAdminFilter(String baseSql,
                                        String status,
                                        String category,
                                        Long assignedAdminUserId,
                                        String keyword) {
        StringBuilder sql = new StringBuilder(baseSql);
        List<Object> args = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            sql.append(" AND st.status = ?");
            args.add(status.trim());
        }
        if (category != null && !category.isBlank()) {
            sql.append(" AND st.category = ?");
            args.add(category.trim());
        }
        if (assignedAdminUserId != null) {
            sql.append(" AND st.assigned_admin_user_id = ?");
            args.add(assignedAdminUserId);
        }
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.trim().toLowerCase(java.util.Locale.ROOT) + "%";
            sql.append("""
                     AND (
                        LOWER(st.ticket_no) LIKE ?
                        OR LOWER(st.subject) LIKE ?
                        OR LOWER(st.content) LIKE ?
                        OR LOWER(COALESCE(CAST(st.related_id AS VARCHAR), '')) LIKE ?
                        OR LOWER(COALESCE(requester.username, '')) LIKE ?
                        OR LOWER(COALESCE(requester.nickname, '')) LIKE ?
                     )
                    """);
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        return new QueryParts(sql, args);
    }

    private Map<String, Object> normalizeTicket(Map<String, Object> row) {
        return linkedMap(
                "id", row.get("id"),
                "ticketNo", defaultString(row.get("ticket_no")),
                "requesterUserId", toLong(row.get("requester_user_id")),
                "requesterName", displayName(row.get("requester_nickname"), row.get("requester_username")),
                "category", defaultString(row.get("category")),
                "subject", defaultString(row.get("subject")),
                "content", defaultString(row.get("content")),
                "status", defaultString(row.get("status")),
                "priority", defaultString(row.get("priority")),
                "relatedType", nullableString(row.get("related_type")),
                "relatedId", nullableLong(row.get("related_id")),
                "assignedAdminUserId", nullableLong(row.get("assigned_admin_user_id")),
                "assignedAdminName", displayName(row.get("assignee_nickname"), row.get("assignee_username")),
                "lastRepliedBy", nullableString(row.get("last_replied_by")),
                "lastRepliedAt", row.get("last_replied_at"),
                "resolvedAt", row.get("resolved_at"),
                "closedAt", row.get("closed_at"),
                "createdAt", row.get("created_at"),
                "updatedAt", row.get("updated_at"),
                "requester", linkedMap(
                        "id", row.get("requester_user_id"),
                        "username", defaultString(row.get("requester_username")),
                        "nickname", defaultString(row.get("requester_nickname"))
                ),
                "assignee", linkedMap(
                        "id", nullableLong(row.get("assigned_admin_user_id")),
                        "username", defaultString(row.get("assignee_username")),
                        "nickname", defaultString(row.get("assignee_nickname"))
                )
        );
    }

    private Map<String, Object> normalizeMessage(Map<String, Object> row) {
        return linkedMap(
                "id", row.get("id"),
                "ticketId", row.get("ticket_id"),
                "senderUserId", nullableLong(row.get("sender_user_id")),
                "senderRole", defaultString(row.get("sender_role")),
                "messageType", defaultString(row.get("message_type")),
                "content", defaultString(row.get("content")),
                "createdAt", row.get("created_at"),
                "sender", linkedMap(
                        "id", nullableLong(row.get("sender_user_id")),
                        "username", defaultString(row.get("sender_username")),
                        "nickname", defaultString(row.get("sender_nickname"))
                )
        );
    }

    private Map<String, Object> linkedMap(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Long nullableLong(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return toLong(value);
    }

    private String nullableString(Object value) {
        if (value == null) {
            return null;
        }
        String string = String.valueOf(value);
        return string.isBlank() ? null : string;
    }

    private String defaultString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String displayName(Object nickname, Object username) {
        String display = defaultString(nickname);
        if (!display.isBlank()) {
            return display;
        }
        return defaultString(username);
    }

    private record QueryParts(StringBuilder sql, List<Object> args) {
    }
}
