package com.youyu.backend.service.report.impl;

import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.mapper.report.ReportMapper;
import com.youyu.backend.mapper.user.UserMapper;
import com.youyu.backend.service.report.ReportService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportServiceImpl implements ReportService {

    private static final Set<String> SUPPORTED_TARGET_TYPES = Set.of(
            "product",
            "order",
            "shop",
            "user",
            "digital_product",
            "digital_order"
    );
    private static final int MAX_REASON_LENGTH = 64;
    private static final int MAX_CONTENT_LENGTH = 1000;
    private static final int MAX_TARGET_LABEL_LENGTH = 255;

    private final ReportMapper reportMapper;
    private final UserMapper userMapper;

    public ReportServiceImpl(ReportMapper reportMapper, UserMapper userMapper) {
        this.reportMapper = reportMapper;
        this.userMapper = userMapper;
    }

    @Override
    public Map<String, Object> moduleInfo() {
        return Map.of(
                "module", "report",
                "status", "ready",
                "next", "Pending report, credit and risk restriction workflow"
        );
    }

    @Override
    @Transactional
    public Map<String, Object> submitReport(Long reporterUserId, Map<String, Object> command) {
        if (reporterUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Login is required to submit a report");
        }
        if (command == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Report payload is required");
        }

        String targetType = requiredString(command.get("targetType"), "targetType");
        if (!SUPPORTED_TARGET_TYPES.contains(targetType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Unsupported report targetType");
        }
        Long targetId = positiveLong(command.get("targetId"), "targetId");
        String reason = requiredString(firstPresent(command, "reasonType", "reason"), "reason");
        if (reason.length() > MAX_REASON_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Report reason is too long");
        }
        String content = requiredString(command.get("content"), "content");
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Report content is too long");
        }
        String targetLabel = optionalString(command.get("targetLabel"), MAX_TARGET_LABEL_LENGTH);

        Map<String, Object> reporter = userMapper.findById(reporterUserId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Reporter user does not exist"));
        String reporterName = firstNonBlank(reporter.get("nickname"), reporter.get("username"), "User#" + reporterUserId);

        Long reportId = reportMapper.insert(
                reporterUserId,
                reporterName,
                targetType,
                targetId,
                targetLabel,
                reason,
                content
        );
        Map<String, Object> report = reportMapper.findById(reportId)
                .orElseThrow(() -> new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "Report was not created"));
        return linkedMap("report", report);
    }

    private Object firstPresent(Map<String, Object> source, String primaryKey, String fallbackKey) {
        Object primary = source.get(primaryKey);
        return primary == null ? source.get(fallbackKey) : primary;
    }

    private String requiredString(Object value, String fieldName) {
        String result = value == null ? "" : String.valueOf(value).trim();
        if (result.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Report " + fieldName + " is required");
        }
        return result;
    }

    private String optionalString(Object value, int maxLength) {
        if (value == null) {
            return "";
        }
        String result = String.valueOf(value).trim();
        if (result.length() > maxLength) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Report targetLabel is too long");
        }
        return result;
    }

    private Long positiveLong(Object value, String fieldName) {
        if (value == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Report " + fieldName + " is required");
        }
        try {
            long result = value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
            if (result <= 0) {
                throw new NumberFormatException("non-positive");
            }
            return result;
        } catch (NumberFormatException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Report " + fieldName + " must be a positive number");
        }
    }

    private String firstNonBlank(Object first, Object second, String fallback) {
        String firstValue = first == null ? "" : String.valueOf(first).trim();
        if (!firstValue.isBlank()) {
            return firstValue;
        }
        String secondValue = second == null ? "" : String.valueOf(second).trim();
        return secondValue.isBlank() ? fallback : secondValue;
    }

    private Map<String, Object> linkedMap(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }
}
