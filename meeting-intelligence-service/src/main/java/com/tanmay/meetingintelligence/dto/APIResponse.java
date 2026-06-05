package com.tanmay.meetingintelligence.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiResponse<T> {

    private String traceId;

    private boolean success;

    private T data;

    private ErrorResponse error;
}