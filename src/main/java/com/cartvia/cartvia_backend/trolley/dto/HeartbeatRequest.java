package com.cartvia.cartvia_backend.trolley.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HeartbeatRequest {

    @Min(0)
    @Max(100)
    private Integer batteryPct;

    private Integer rssi;
}
