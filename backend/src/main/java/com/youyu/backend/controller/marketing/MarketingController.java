package com.youyu.backend.controller.marketing;

import com.youyu.backend.common.api.ApiResponse;
import com.youyu.backend.common.auth.AuthContextHolder;
import com.youyu.backend.common.auth.LoginRequired;
import com.youyu.backend.common.support.RequestContext;
import com.youyu.backend.service.marketing.MarketingService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/marketing")
public class MarketingController {

    private final MarketingService marketingService;

    public MarketingController(MarketingService marketingService) {
        this.marketingService = marketingService;
    }

    @GetMapping("/owner/coupons")
    @LoginRequired
    public ApiResponse<List<Map<String, Object>>> ownerCoupons(HttpServletRequest request) {
        return ApiResponse.success(marketingService.listOwnerCoupons(currentUserId()), traceId(request));
    }

    @PostMapping("/owner/coupons")
    @LoginRequired
    public ApiResponse<Map<String, Object>> createOwnerCoupon(@RequestBody Map<String, Object> command,
                                                              HttpServletRequest request) {
        return ApiResponse.success(marketingService.createOwnerCoupon(currentUserId(), command), traceId(request));
    }

    @PutMapping("/owner/coupons/{couponId}")
    @LoginRequired
    public ApiResponse<Map<String, Object>> updateOwnerCoupon(@PathVariable Long couponId,
                                                              @RequestBody Map<String, Object> command,
                                                              HttpServletRequest request) {
        return ApiResponse.success(marketingService.updateOwnerCoupon(currentUserId(), couponId, command), traceId(request));
    }

    @PutMapping("/owner/coupons/{couponId}/status")
    @LoginRequired
    public ApiResponse<Map<String, Object>> updateOwnerCouponStatus(@PathVariable Long couponId,
                                                                    @RequestBody Map<String, Object> command,
                                                                    HttpServletRequest request) {
        return ApiResponse.success(marketingService.updateOwnerCouponStatus(currentUserId(), couponId, command), traceId(request));
    }

    @GetMapping("/coupons/available")
    @LoginRequired
    public ApiResponse<List<Map<String, Object>>> availableCoupons(@RequestParam Long shopId,
                                                                   HttpServletRequest request) {
        return ApiResponse.success(marketingService.listAvailableCoupons(shopId), traceId(request));
    }

    @PostMapping("/coupons/{couponId}/claim")
    @LoginRequired
    public ApiResponse<Map<String, Object>> claimCoupon(@PathVariable Long couponId,
                                                       HttpServletRequest request) {
        return ApiResponse.success(marketingService.claimCoupon(currentUserId(), couponId), traceId(request));
    }

    @GetMapping("/my-coupons")
    @LoginRequired
    public ApiResponse<List<Map<String, Object>>> myCoupons(HttpServletRequest request) {
        return ApiResponse.success(marketingService.listMyCoupons(currentUserId()), traceId(request));
    }

    @GetMapping("/owner/activities")
    @LoginRequired
    public ApiResponse<List<Map<String, Object>>> ownerActivities(HttpServletRequest request) {
        return ApiResponse.success(marketingService.listOwnerActivities(currentUserId()), traceId(request));
    }

    @PostMapping("/owner/activities")
    @LoginRequired
    public ApiResponse<Map<String, Object>> createOwnerActivity(@RequestBody Map<String, Object> command,
                                                                HttpServletRequest request) {
        return ApiResponse.success(marketingService.createOwnerActivity(currentUserId(), command), traceId(request));
    }

    @PutMapping("/owner/activities/{activityId}")
    @LoginRequired
    public ApiResponse<Map<String, Object>> updateOwnerActivity(@PathVariable Long activityId,
                                                                @RequestBody Map<String, Object> command,
                                                                HttpServletRequest request) {
        return ApiResponse.success(marketingService.updateOwnerActivity(currentUserId(), activityId, command), traceId(request));
    }

    @PutMapping("/owner/activities/{activityId}/status")
    @LoginRequired
    public ApiResponse<Map<String, Object>> updateOwnerActivityStatus(@PathVariable Long activityId,
                                                                      @RequestBody Map<String, Object> command,
                                                                      HttpServletRequest request) {
        return ApiResponse.success(marketingService.updateOwnerActivityStatus(currentUserId(), activityId, command), traceId(request));
    }

    @GetMapping("/shops/{shopId}/activities")
    public ApiResponse<List<Map<String, Object>>> publicActivities(@PathVariable Long shopId,
                                                                   HttpServletRequest request) {
        return ApiResponse.success(marketingService.listPublicActivities(shopId), traceId(request));
    }

    private Long currentUserId() {
        return AuthContextHolder.get().getUserId();
    }

    private String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE);
    }
}
