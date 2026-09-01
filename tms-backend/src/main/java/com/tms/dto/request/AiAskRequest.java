package com.tms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiAskRequest {
    @NotBlank
    @Size(max = 500)
    private String message;

    @Size(max = 120)
    private String pagePath;

    @Valid
    @Size(max = 8)
    private List<AiChatTurn> history = new ArrayList<>();
}
