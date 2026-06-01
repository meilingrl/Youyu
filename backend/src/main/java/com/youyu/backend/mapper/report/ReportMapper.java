package com.youyu.backend.mapper.report;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ReportMapper {

    List<Map<String, Object>> findAll();

    List<Map<String, Object>> findReportsPaged(String keyword, String status, String targetType, int offset, int limit);

    long countReports(String keyword, String status, String targetType);

    long countAll();

    Optional<Map<String, Object>> findById(Long id);

    List<Map<String, Object>> findByTarget(String targetType, Long targetId);

    Long insert(Long reporterUserId,
                String reporterName,
                String targetType,
                Long targetId,
                String targetLabel,
                String reasonType,
                String content);

    void updateStatus(Long reportId, String status, String processedBy, String resolution);
}
