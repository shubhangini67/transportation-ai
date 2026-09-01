package com.tms.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DispatchPlanResponse {
    private String engine;
    private String routeLabel;
    private List<DispatchRecommendation> recommendations = new ArrayList<>();

    @Data
    public static class DispatchRecommendation {
        private int rank;
        private int score;
        private String vehicleId;
        private String vehicleNumber;
        private String vehicleType;
        private String driverId;
        private String driverName;
        private String reason;
    }
}
