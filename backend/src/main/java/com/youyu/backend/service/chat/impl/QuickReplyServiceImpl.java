package com.youyu.backend.service.chat.impl;

import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.mapper.chat.QuickReplyMapper;
import com.youyu.backend.service.chat.QuickReplyService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuickReplyServiceImpl implements QuickReplyService {

    private static final int MAX_CONTENT_LENGTH = 500;

    private final QuickReplyMapper quickReplyMapper;

    public QuickReplyServiceImpl(QuickReplyMapper quickReplyMapper) {
        this.quickReplyMapper = quickReplyMapper;
    }

    @Override
    public List<Map<String, Object>> listQuickReplies(Long userId) {
        requireUser(userId);
        return quickReplyMapper.findByUserId(userId);
    }

    @Override
    @Transactional
    public Map<String, Object> createQuickReply(Long userId, String content, Integer sortOrder) {
        requireUser(userId);
        Map<String, Object> quickReply = new LinkedHashMap<>();
        quickReply.put("userId", userId);
        quickReply.put("content", normalizeContent(content));
        quickReply.put("sortOrder", normalizeSortOrder(sortOrder));
        Long id = quickReplyMapper.insert(quickReply);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        return result;
    }

    @Override
    @Transactional
    public void updateQuickReply(Long userId, Long id, String content, Integer sortOrder) {
        requireUser(userId);
        Map<String, Object> existing = requireExisting(id);
        ensureOwner(existing, userId);
        quickReplyMapper.update(id, userId, normalizeContent(content), normalizeSortOrder(sortOrder));
    }

    @Override
    @Transactional
    public void deleteQuickReply(Long userId, Long id) {
        requireUser(userId);
        Map<String, Object> existing = requireExisting(id);
        ensureOwner(existing, userId);
        quickReplyMapper.delete(id, userId);
    }

    private void requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Please log in first");
        }
    }

    private Map<String, Object> requireExisting(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Quick reply id is required");
        }
        return quickReplyMapper.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Quick reply not found"));
    }

    private void ensureOwner(Map<String, Object> quickReply, Long userId) {
        if (!Objects.equals(quickReply.get("userId"), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Cannot operate another user's quick reply");
        }
    }

    private String normalizeContent(String content) {
        String text = content == null ? "" : content.trim();
        if (text.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "content is required");
        }
        if (text.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "content cannot exceed " + MAX_CONTENT_LENGTH + " characters");
        }
        return text;
    }

    private int normalizeSortOrder(Integer sortOrder) {
        return sortOrder == null ? 0 : sortOrder;
    }
}
