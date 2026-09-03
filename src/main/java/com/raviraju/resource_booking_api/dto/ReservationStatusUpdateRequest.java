package com.raviraju.resource_booking_api.dto;

import com.raviraju.resource_booking_api.entity.ReservationStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private ReservationStatus status;
}
