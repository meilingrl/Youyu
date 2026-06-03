package com.youyu.backend.service.search;

import java.util.List;

public record ProductSearchResult(List<Long> productIds, long total) {
}
