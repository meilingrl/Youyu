package com.campusmarket.backend.mapper.search;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SearchGovernanceMapper {

    List<Map<String, Object>> findAll();

    List<Map<String, Object>> findAllActive();

    Optional<Map<String, Object>> findById(Long id);

    Long insert(Map<String, Object> rule);

    void update(Long id, Map<String, Object> rule);

    void delete(Long id);

    List<Map<String, Object>> findActiveByType(String ruleType);
}
