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
public class RequestPayload {
    String mediaType;
    String ref;
    List<Parameter> parameters;
    List<PayloadExample> payloadExamples;
}
