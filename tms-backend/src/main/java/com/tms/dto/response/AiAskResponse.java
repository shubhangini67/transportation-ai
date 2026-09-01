package com.tms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class AiAskResponse {
    private String answer;
    private String botName;
    private boolean live;
    private String intent;
    private int confidence;
    private boolean usedLlm;
    @Builder.Default
    private List<String> facts = new ArrayList<>();
    @Builder.Default
    private List<Link> links = new ArrayList<>();
    @Builder.Default
    private List<String> suggestions = new ArrayList<>();

    @Data
    @Builder
    public static class Link {
        private String path;
        private String label;
    }
}
