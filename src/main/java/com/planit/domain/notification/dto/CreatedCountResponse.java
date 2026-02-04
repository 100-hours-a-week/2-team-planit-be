package com.planit.domain.notification.dto;

import lombok.Getter;

@Getter
public class CreatedCountResponse {
    private final long created;

    public CreatedCountResponse(long created) {
        this.created = created;
    }
}
