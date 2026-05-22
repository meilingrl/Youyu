package com.youyu.backend.service.search.impl;

import static com.youyu.backend.mapper.common.MapperTypeConverters.toInt;

import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.mapper.search.SearchGovernanceMapper;
import com.youyu.backend.mapper.search.SearchLogMapper;
import com.youyu.backend.service.search.SearchService;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchServiceImpl.class);

    // 热搜统计窗口：7 天内的搜索数据参与排名，超过 7 天的数据不再纳入
    private static final int HOT_SEARCH_DAYS = 7;
    private static final int HOT_SEARCH_LIMIT = 10;
    private static final int SUGGESTION_LIMIT = 8;
    private static final int MAX_PAGE_SIZE = 50;
    // 每日衰减系数：第 N 天前的搜索权重 = 0.85^N，7 天前约为原始的 32%
    private static final double DAILY_DECAY = 0.85d;

    private static final Set<String> VALID_RULE_TYPES =
            Set.of("SENSITIVE_WORD", "STOP_WORD", "HIDE_KEYWORD", "PIN_KEYWORD");

    private final SearchLogMapper searchLogMapper;
    private final SearchGovernanceMapper searchGovernanceMapper;

    public SearchServiceImpl(SearchLogMapper searchLogMapper,
                           SearchGovernanceMapper searchGovernanceMapper) {
        this.searchLogMapper = searchLogMapper;
        this.searchGovernanceMapper = searchGovernanceMapper;
    }

    @Override
    public void recordKeywordSearch(String keyword, Long userId, int resultCount) {
        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword == null) {
            return;
        }
        try {
            searchLogMapper.insert(cleanKeyword(keyword), normalizedKeyword, userId, Math.max(resultCount, 0));
        } catch (Exception e) {
            log.warn("Failed to record keyword search for keyword={}", normalizedKeyword, e);
        }
    }

    @Override
    public List<Map<String, Object>> listHotKeywords() {
        GovernanceSnapshot governance = loadGovernanceSnapshot();
        List<Map<String, Object>> ranked = buildRankedKeywords(
                searchLogMapper.findRecentDailyAggregates(HOT_SEARCH_DAYS),
                topKeywordMap(searchLogMapper.findTopKeywordsForRecentWindow(HOT_SEARCH_DAYS)));
        return applyGovernance(ranked, governance, HOT_SEARCH_LIMIT, true, null);
    }

    @Override
    public List<Map<String, Object>> suggestKeywords(String query, int limit) {
        String normalizedQuery = normalizeKeyword(query);
        if (normalizedQuery == null) {
            return List.of();
        }

        int normalizedLimit = Math.min(SUGGESTION_LIMIT, Math.max(1, limit));
        GovernanceSnapshot governance = loadGovernanceSnapshot();
        List<Map<String, Object>> ranked = buildRankedKeywords(
                searchLogMapper.findRecentDailyAggregatesByPrefix(normalizedQuery, HOT_SEARCH_DAYS),
                topKeywordMap(searchLogMapper.findTopKeywordsForRecentWindowByPrefix(normalizedQuery, HOT_SEARCH_DAYS)));
        return applyGovernance(ranked, governance, normalizedLimit, false, normalizedQuery);
    }

    // 治理管线执行顺序：
    // 1) 过滤：移除被隐藏的关键词和包含敏感词的关键词
    // 2) 置顶处理：将置顶关键词移至首位；若置顶词不在结果中且允许合成，创建合成条目
    // 3) 截断：取前 limit 条返回
    private List<Map<String, Object>> applyGovernance(List<Map<String, Object>> ranked,
                                                      GovernanceSnapshot governance,
                                                      int limit,
                                                      boolean allowSyntheticPins,
                                                      String normalizedPrefix) {
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> item : ranked) {
            String nk = String.valueOf(item.get("normalizedKeyword")).toLowerCase(Locale.ROOT);
            if (governance.hiddenKeywords.contains(nk) || isBlocked(nk, governance.blockedKeywords)) {
                continue;
            }
            filtered.add(item);
        }

        for (Map<String, Object> pinRule : governance.pinRules) {
            String pinKeyword = String.valueOf(pinRule.get("keyword")).toLowerCase(Locale.ROOT);
            if (normalizedPrefix != null && !pinKeyword.startsWith(normalizedPrefix.toLowerCase(Locale.ROOT))) {
                continue;
            }
            boolean alreadyPresent = false;
            for (int i = 0; i < filtered.size(); i++) {
                if (pinKeyword.equals(String.valueOf(filtered.get(i).get("normalizedKeyword")).toLowerCase(Locale.ROOT))) {
                    Map<String, Object> item = filtered.remove(i);
                    item.put("pinned", true);
                    filtered.add(0, item);
                    alreadyPresent = true;
                    break;
                }
            }
            if (!alreadyPresent && allowSyntheticPins) {
                Map<String, Object> pinned = new LinkedHashMap<>();
                pinned.put("keyword", pinRule.get("displayLabel") != null
                        && !String.valueOf(pinRule.get("displayLabel")).isBlank()
                        ? pinRule.get("displayLabel") : pinKeyword);
                pinned.put("normalizedKeyword", pinKeyword);
                pinned.put("searchCount", 0);
                pinned.put("resultCountSum", 0);
                pinned.put("score", 0d);
                pinned.put("pinned", true);
                filtered.add(0, pinned);
            }
        }

        return filtered.stream().limit(limit).toList();
    }

    private List<Map<String, Object>> buildRankedKeywords(List<Map<String, Object>> dailyAggregates,
                                                          Map<String, String> topKeywords) {
        LocalDate today = LocalDate.now();
        Map<String, Map<String, Object>> aggregates = new LinkedHashMap<>();

        for (Map<String, Object> row : dailyAggregates) {
            String normalizedKeyword = string(row.get("normalizedKeyword")).toLowerCase(Locale.ROOT);
            if (normalizedKeyword.isBlank()) {
                continue;
            }

            LocalDate searchDate = toLocalDate(row.get("searchDate"));
            long dayDiff = Math.max(0, ChronoUnit.DAYS.between(searchDate, today));
            double weight = Math.pow(DAILY_DECAY, dayDiff);
            int searchCount = toInt(row.get("searchCount"));
            int resultCountSum = toInt(row.get("resultCountSum"));

            Map<String, Object> item = aggregates.computeIfAbsent(normalizedKeyword, key -> {
                Map<String, Object> created = new LinkedHashMap<>();
                created.put("keyword", topKeywords.getOrDefault(key, key));
                created.put("normalizedKeyword", key);
                created.put("searchCount", 0);
                created.put("resultCountSum", 0);
                created.put("score", 0d);
                return created;
            });
            item.put("searchCount", toInt(item.get("searchCount")) + searchCount);
            item.put("resultCountSum", toInt(item.get("resultCountSum")) + resultCountSum);
            item.put("score", toDouble(item.get("score")) + weight * searchCount);
        }

        return aggregates.values().stream()
                .sorted((left, right) -> {
                    int scoreCompare = Double.compare(toDouble(right.get("score")), toDouble(left.get("score")));
                    if (scoreCompare != 0) {
                        return scoreCompare;
                    }
                    int countCompare = Integer.compare(toInt(right.get("searchCount")), toInt(left.get("searchCount")));
                    if (countCompare != 0) {
                        return countCompare;
                    }
                    return String.valueOf(left.get("normalizedKeyword")).compareTo(String.valueOf(right.get("normalizedKeyword")));
                })
                .map(item -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("keyword", item.get("keyword"));
                    result.put("normalizedKeyword", item.get("normalizedKeyword"));
                    result.put("searchCount", item.get("searchCount"));
                    result.put("resultCountSum", item.get("resultCountSum"));
                    result.put("score", Math.round(toDouble(item.get("score")) * 1000d) / 1000d);
                    result.put("pinned", false);
                    return result;
                })
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private Map<String, String> topKeywordMap(List<Map<String, Object>> rows) {
        Map<String, String> keywords = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String normalizedKeyword = string(row.get("normalizedKeyword")).toLowerCase(Locale.ROOT);
            if (!normalizedKeyword.isBlank()) {
                keywords.put(normalizedKeyword, string(row.get("keyword")));
            }
        }
        return keywords;
    }

    private GovernanceSnapshot loadGovernanceSnapshot() {
        List<Map<String, Object>> activeRules = searchGovernanceMapper.findAllActive();
        Set<String> hiddenKeywords = new HashSet<>();
        Set<String> blockedKeywords = new HashSet<>();
        List<Map<String, Object>> pinRules = new ArrayList<>();

        for (Map<String, Object> rule : activeRules) {
            String ruleType = string(rule.get("ruleType")).toUpperCase(Locale.ROOT);
            String keyword = string(rule.get("keyword")).toLowerCase(Locale.ROOT);
            if (keyword.isBlank()) {
                continue;
            }
            switch (ruleType) {
                case "HIDE_KEYWORD" -> hiddenKeywords.add(keyword);
                case "SENSITIVE_WORD", "STOP_WORD" -> blockedKeywords.add(keyword);
                case "PIN_KEYWORD" -> pinRules.add(rule);
                default -> {
                }
            }
        }

        return new GovernanceSnapshot(hiddenKeywords, blockedKeywords, pinRules);
    }

    // 敏感词/停用词使用子串匹配（contains），而非精确匹配：
    // 搜索 "代考微积分" 时 "代考" 作为子串被屏蔽
    private boolean isBlocked(String normalizedKeyword, Set<String> blockedKeywords) {
        if (blockedKeywords.contains(normalizedKeyword)) {
            return true;
        }
        for (String blocked : blockedKeywords) {
            if (normalizedKeyword.contains(blocked)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Map<String, Object>> listGovernanceRules() {
        return searchGovernanceMapper.findAll();
    }

    @Override
    public Map<String, Object> createGovernanceRule(Map<String, Object> command) {
        String ruleType = String.valueOf(command.get("ruleType")).trim().toUpperCase(Locale.ROOT);
        if (!VALID_RULE_TYPES.contains(ruleType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "无效的规则类型: " + ruleType);
        }
        String keyword = normalizeKeyword(String.valueOf(command.get("keyword")));
        if (keyword == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "关键词不能为空或仅含标点符号");
        }
        String displayLabel = command.containsKey("displayLabel")
                ? String.valueOf(command.get("displayLabel")).trim() : "";

        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("ruleType", ruleType);
        rule.put("keyword", keyword);
        rule.put("displayLabel", displayLabel.isBlank() ? null : displayLabel);
        Long id = searchGovernanceMapper.insert(rule);
        return searchGovernanceMapper.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "创建规则后查询失败"));
    }

    @Override
    public Map<String, Object> updateGovernanceRule(Long id, Map<String, Object> command) {
        Map<String, Object> existing = searchGovernanceMapper.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "治理规则不存在"));

        Map<String, Object> updates = new LinkedHashMap<>();
        if (command.containsKey("keyword")) {
            String keyword = normalizeKeyword(String.valueOf(command.get("keyword")));
            if (keyword == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "关键词不能为空或仅含标点符号");
            }
            updates.put("keyword", keyword);
        }
        if (command.containsKey("displayLabel")) {
            String label = String.valueOf(command.get("displayLabel")).trim();
            updates.put("displayLabel", label.isBlank() ? null : label);
        }
        if (command.containsKey("isActive")) {
            Object isActive = command.get("isActive");
            updates.put("isActive", isActive);
        }

        if (!updates.isEmpty()) {
            searchGovernanceMapper.update(id, updates);
        }
        return searchGovernanceMapper.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "更新规则后查询失败"));
    }

    @Override
    public void deleteGovernanceRule(Long id) {
        searchGovernanceMapper.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "治理规则不存在"));
        searchGovernanceMapper.delete(id);
    }

    @Override
    public Map<String, Object> listSearchLogs(int page, int pageSize) {
        page = Math.max(1, page);
        pageSize = Math.min(MAX_PAGE_SIZE, Math.max(1, pageSize));
        int offset = (page - 1) * pageSize;

        List<Map<String, Object>> items = searchLogMapper.findPage(offset, pageSize);
        long total = searchLogMapper.countAll();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return result;
    }

    private String cleanKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim().replaceAll("\\s+", " ");
    }

    private String normalizeKeyword(String keyword) {
        String cleaned = cleanKeyword(keyword);
        if (cleaned.isBlank()) {
            return null;
        }
        boolean meaningful = cleaned.codePoints().anyMatch(Character::isLetterOrDigit);
        if (!meaningful) {
            return null;
        }
        String normalized = cleaned.toLowerCase(java.util.Locale.ROOT);
        return normalized.length() > 255 ? normalized.substring(0, 255) : normalized;
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }

    private double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record GovernanceSnapshot(Set<String> hiddenKeywords,
                                      Set<String> blockedKeywords,
                                      List<Map<String, Object>> pinRules) {
    }
}
