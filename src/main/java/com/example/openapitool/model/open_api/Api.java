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
public class Api {
    String httpMethod;
    String path;
    String summary;
    Boolean deprecated;
    List<Parameter> requestsHeaderParameters;
    List<Parameter> requestsPathParameters;
    List<Parameter> requestsQueryParameters;
    List<RequestPayload> requestPayloads;
    List<ResponsePayload> responsePayloads;
}
