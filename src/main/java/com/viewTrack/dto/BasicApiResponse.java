package com.viewTrack.dto;

import lombok.Data;

@Data
public class BasicApiResponse<T> {
    private boolean error;
    private T result;

    public BasicApiResponse(boolean error, T result) {
        this.error = error;
        this.result = result;
    }

    public BasicApiResponse(T result) {
        this.error = false;
        this.result = result;
    }
}
