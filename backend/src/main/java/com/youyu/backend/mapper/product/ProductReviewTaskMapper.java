package com.youyu.backend.mapper.product;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ProductReviewTaskMapper {

    List<Map<String, Object>> findAll();

    List<Map<String, Object>> findReviewTasksPaged(String keyword, String status, int offset, int limit);

    long countReviewTasks(String keyword, String status);

    long countAll();

    Optional<Map<String, Object>> findById(Long id);

    Long insertPending(Long productId);

    void updateReviewResult(Long id, String reviewStatus, Long reviewerId, String rejectReason, String reviewNote);
}
