package com.raviraju.resource_booking_api.util;

import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.raviraju.resource_booking_api.exception.BadRequestException;

public final class PageableValidator {

    private PageableValidator() {
        // Utility class
    }

    public static void validate(Pageable pageable, Set<String> allowedSortProperties) {
        if (pageable.getPageNumber() < 0) {
            throw new BadRequestException("Page index must not be less than zero.");
        }
        if (pageable.getPageSize() < 1 || pageable.getPageSize() > 100) {
            throw new BadRequestException("Page size must be between 1 and 100.");
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
