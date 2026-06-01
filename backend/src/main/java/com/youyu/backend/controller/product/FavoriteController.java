package com.youyu.backend.controller.product;

import com.youyu.backend.common.api.ApiResponse;
import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.auth.AuthContextHolder;
import com.youyu.backend.common.auth.LoginRequired;
import com.youyu.backend.common.auth.UserRole;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.common.support.RequestContext;
import com.youyu.backend.service.product.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/favorites")
@LoginRequired(roles = {UserRole.USER})
public class FavoriteController {

    private final ProductService productService;

    public FavoriteController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(HttpServletRequest request) {
        return ApiResponse.success(productService.listFavorites(currentUserId()), traceId(request));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> add(@RequestBody Map<String, Object> payload,
                                                HttpServletRequest request) {
        Long productId = toLong(payload.get("productId"));
        if (productId == null) {
            throw new BusinessException(com.youyu.backend.common.api.ResultCode.BAD_REQUEST, "productId is required");
        }
        return ApiResponse.success(productService.addFavorite(currentUserId(), productId), traceId(request));
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Map<String, Object>> remove(@PathVariable Long productId,
                                                   HttpServletRequest request) {
        return ApiResponse.success(productService.removeFavorite(currentUserId(), productId), traceId(request));
    }

    private Long currentUserId() {
        return AuthContextHolder.get() == null ? null : AuthContextHolder.get().getUserId();
    }

    private String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE);
    }

    private Long toLong(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "productId must be a valid integer");
        }
    }
}
