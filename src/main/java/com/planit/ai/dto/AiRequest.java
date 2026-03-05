package com.planit.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiRequest {
    private String tripId;
    private String content;
    private String userJWT;
}
