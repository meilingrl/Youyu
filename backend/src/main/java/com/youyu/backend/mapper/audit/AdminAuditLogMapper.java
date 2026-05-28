package com.youyu.backend.mapper.audit;

import java.util.List;
import java.util.Map;

public interface AdminAuditLogMapper {

    void insert(Long operatorUserId,
                String operatorRole,
                String action,
                String targetType,
                Long targetId,
                String summary);

    List<Map<String, Object>> findPaged(String action, String targetType, int offset, int limit);

    long count(String action, String targetType);
}
