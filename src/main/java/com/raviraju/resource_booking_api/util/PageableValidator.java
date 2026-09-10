package com.raviraju.resource_booking_api.util;

import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.raviraju.resource_booking_api.exception.BadRequestException;

public final class PageableValidator {

    public static final int MIN_PAGE_SIZE = 1;
    public static final int MAX_PAGE_SIZE = 100;

    private PageableValidator() {
        // Utility class
    }

    public static void validate(Pageable pageable, Set<String> allowedSortProperties) {
        if (pageable.getPageNumber() < 0) {
            throw new BadRequestException("Page index must not be less than zero.");
        }
        if (pageable.getPageSize() < MIN_PAGE_SIZE || pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new BadRequestException("Page size must be between " + MIN_PAGE_SIZE + " and " + MAX_PAGE_SIZE + ".");
        }
        if (allowedSortProperties != null && !allowedSortProperties.isEmpty()) {
            for (Sort.Order order : pageable.getSort()) {
                if (!allowedSortProperties.contains(order.getProperty())) {
                    throw new BadRequestException("Invalid sort property: " + order.getProperty());
                }
            }
        }
    }
}
