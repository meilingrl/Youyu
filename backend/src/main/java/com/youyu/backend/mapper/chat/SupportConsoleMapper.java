package com.youyu.backend.mapper.chat;

import java.util.List;
import java.util.Map;

public interface SupportConsoleMapper {
    List<Map<String, Object>> findQueue(String filter, Long adminId, int offset, int limit);
    int countQueue(String filter, Long adminId);
    Map<String, Object> countByStatus(Long adminId);
}
