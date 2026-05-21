package com.campusmarket.backend.mapper.user;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface StudentVerificationMapper {

    List<Map<String, Object>> findAll();

    List<Map<String, Object>> findVerificationsPaged(String keyword, String status, int offset, int limit);

    long countVerifications(String keyword, String status);

    long countAll();

    Optional<Map<String, Object>> findById(Long id);

    Optional<Map<String, Object>> findLatestByUserId(Long userId);

    boolean existsApprovedStudentNoForOtherUser(String studentNo, Long userId);

    Long insert(Long userId,
                String studentNo,
                String realName,
                String college,
                String major,
                String grade,
                String campusEmail,
                String verificationMethod);

    void review(Long verificationId,
                String verificationStatus,
                String rejectReason,
                String reviewNote,
                Long reviewerId);
}
