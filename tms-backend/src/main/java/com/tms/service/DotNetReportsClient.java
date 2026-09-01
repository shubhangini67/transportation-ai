package com.tms.service;

import com.tms.dto.request.DispatchSnapshotRequest;
import com.tms.dto.request.FleetSnapshotRequest;
import com.tms.dto.response.DispatchPlanResponse;
import com.tms.dto.response.FleetInsightsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Calls the ASP.NET Core reporting microservice.
 * The React UI never talks to .NET directly — Spring Boot is the gateway.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DotNetReportsClient {

    private final RestTemplate restTemplate;

    @Value("${app.dotnet.reports-url:http://localhost:5080}")
    private String reportsUrl;

    @Value("${app.dotnet.api-key:tms-internal-dev-key}")
    private String apiKey;

    @Value("${app.dotnet.enabled:true}")
    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public DispatchPlanResponse recommendDispatch(DispatchSnapshotRequest snapshot) {
        String url = reportsUrl.replaceAll("/$", "") + "/api/v1/dispatch/recommend";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Api-Key", apiKey);
        HttpEntity<DispatchSnapshotRequest> entity = new HttpEntity<>(snapshot, headers);
        try {
            ResponseEntity<DispatchPlanResponse> response =
                    restTemplate.exchange(url, HttpMethod.POST, entity, DispatchPlanResponse.class);
            return response.getBody();
        } catch (RestClientException ex) {
            log.warn("ASP.NET dispatch service unavailable at {}: {}", url, ex.getMessage());
            throw ex;
        }
    }

    public FleetInsightsResponse analyzeFleet(FleetSnapshotRequest snapshot) {
        String url = reportsUrl.replaceAll("/$", "") + "/api/v1/insights/fleet";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Api-Key", apiKey);
        HttpEntity<FleetSnapshotRequest> entity = new HttpEntity<>(snapshot, headers);
        try {
            ResponseEntity<FleetInsightsResponse> response =
                    restTemplate.exchange(url, HttpMethod.POST, entity, FleetInsightsResponse.class);
            return response.getBody();
        } catch (RestClientException ex) {
            log.warn("ASP.NET reports service unavailable at {}: {}", url, ex.getMessage());
            throw ex;
        }
    }
}
