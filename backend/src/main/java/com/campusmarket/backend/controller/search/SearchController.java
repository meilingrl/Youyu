package com.campusmarket.backend.controller.search;

import com.campusmarket.backend.common.api.ApiResponse;
import com.campusmarket.backend.common.support.RequestContext;
import com.campusmarket.backend.service.search.SearchService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/hot")
    public ApiResponse<List<Map<String, Object>>> hot(HttpServletRequest request) {
        return ApiResponse.success(
                searchService.listHotKeywords(),
                (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE));
    }

    @GetMapping("/suggest")
    public ApiResponse<List<Map<String, Object>>> suggest(@RequestParam(name = "q", required = false) String query,
                                                          @RequestParam(defaultValue = "8") int limit,
                                                          HttpServletRequest request) {
        return ApiResponse.success(
                searchService.suggestKeywords(query, limit),
                (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE));
    }
}
