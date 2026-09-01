package com.tms.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiChatTurn {
    private String role;
    @Size(max = 2000)
    private String content;
}
