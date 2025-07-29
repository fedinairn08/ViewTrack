package com.viewTrack.dto;

import lombok.Data;

@Data
public class EmptyApiResponse {
    private boolean error = false;
    private boolean isEmpty = false;
    private boolean result = true;
}
