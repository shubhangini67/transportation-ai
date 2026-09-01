package com.tms.dto.request;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DispatchSnapshotRequest {
    private String origin;
    private String destination;
    private double distanceKm;
    private int estimatedMinutes;
    private int requiredCapacity;
    private List<DispatchVehicle> vehicles = new ArrayList<>();
    private List<DispatchDriver> drivers = new ArrayList<>();

    @Data
    public static class DispatchVehicle {
        private String id;
        private String number;
        private String type;
        private int capacity;
        private String status;
        private String currentLocation;
        private boolean busyOnTrip;
    }

    @Data
    public static class DispatchDriver {
        private String id;
        private String name;
        private String status;
        private boolean busyOnTrip;
    }
}
