package com.example.openapitool.model.sheet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class SheetParameter {
    String path;
    String summary;
    String httpMethod;
    String contentType;
    String memo;
    List<OtherParameter> otherParameters;
    List<RequestParameter> requestParameters;
    String requestExample;
    List<ResponseParameter> responseParameters;
    String responseExample;
    // 用於判斷區塊是否顯示
    Boolean showOtherParameters;
    Boolean showRequestParameters;
    Boolean showRequestExample;
    Boolean showResponseParameters;
    Boolean showResponseExample;
}
