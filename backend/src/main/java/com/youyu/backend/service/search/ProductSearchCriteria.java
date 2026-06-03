package com.youyu.backend.service.search;

public record ProductSearchCriteria(String keyword,
                                    Long categoryId,
                                    String productType,
                                    String sort,
                                    int page,
                                    int pageSize) {
}
