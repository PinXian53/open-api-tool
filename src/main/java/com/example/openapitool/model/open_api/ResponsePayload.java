package com.example.openapitool.model.open_api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ResponsePayload {
    String httpCode;
    String ref;
    String mediaType;
    String description;
    List<Parameter> parameters;
    String payloadExample;
}
