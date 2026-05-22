package com.youyu.backend.service.shop;

import java.util.Map;

public interface ShopService {

    Map<String, Object> moduleInfo();

    Map<String, Object> insightSnapshot(Long shopId);

    Map<String, Object> getShopDetail(Long shopId);

    Map<String, Object> getMyShop(Long ownerUserId);

    Map<String, Object> applyShop(Long ownerUserId, Map<String, Object> command);
}
