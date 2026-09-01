package com.tms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardTrendResponse {
    private List<TrendPoint> tripTrend;
    private List<TrendPoint> bookingTrend;
    private Map<String, Long> tripsByStatus;
    private Map<String, Long> vehiclesByStatus;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String label;
        private long count;
    }
}

