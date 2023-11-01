package com.example.openapitool.service;

import com.example.openapitool.constant.TemplateType;
import com.example.openapitool.model.open_api.*;
import com.example.openapitool.model.sheet.*;
import com.example.openapitool.util.JsonUtils;
import com.example.openapitool.util.YamlUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jxls.common.Context;
import org.jxls.util.JxlsHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
@Slf4j
@RequiredArgsConstructor
@Service
public class SheetService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenApiSpecService openApiSpecService;

    @Value("classpath:templates/api-spec-template.xlsx")
    Resource dafaultTemplateResource;

    @Value("classpath:templates/api-spec-template-hes.xlsx")
    Resource hesTemplateResource;

    @SneakyThrows
    public void convertOpenApiToSheet(TemplateType templateType, byte[] openApiBytes, OutputStream outputStream) {
        var content = readJsonContent(openApiBytes);
        var openApiDoc = openApiSpecService.getOpenApiDoc(content);
        var sheetParameters = convertToSheetParameter(openApiDoc);
        // 輸出到 excel
        try (var inputStream = getTemplateResource(templateType).getInputStream()) {
            var context = new Context();
            context.putVar("sheetParameters", sheetParameters);
            JxlsHelper.getInstance().processTemplate(inputStream, outputStream, context);
        }
    }

    @SneakyThrows
    private String readJsonContent(byte[] txtBytes) {
        var tmpFile = Paths.get(FileUtils.getTempDirectory().getPath(), UUID.randomUUID().toString()).toFile();
        FileUtils.writeByteArrayToFile(tmpFile, txtBytes);
        var content = FileUtils.readFileToString(tmpFile, StandardCharsets.UTF_8);
        if (!JsonUtils.isJson(content)) {
            content = YamlUtils.convertYamlToJson(content);
        }
        tmpFile.delete();
        return content;
    }

    private Resource getTemplateResource(TemplateType templateType) {
        if (templateType == null) {
            return dafaultTemplateResource;
        }
        return switch (templateType) {
            case DEFAULT -> dafaultTemplateResource;
            case HES -> hesTemplateResource;
        };
    }

    private List<SheetParameter> convertToSheetParameter(OpenApiDoc openApiDoc) {
        var sheetParameterList = new ArrayList<SheetParameter>();
        openApiDoc.getGroups().forEach(group ->
            group.getApis().forEach(api -> {
                var otherParameterList = getOtherParameters(api);
                var sheetParameter = new SheetParameter();
                sheetParameter.setPath(api.getPath());
                sheetParameter.setSummary(api.getSummary());
                sheetParameter.setHttpMethod(api.getHttpMethod());
                sheetParameter.setContentType("application/json");
                sheetParameter.setMemo(BooleanUtils.isTrue(api.getDeprecated()) ? "deprecated" : null);
                sheetParameter.setOtherParameters(otherParameterList);
                setRequestParametersAndExample(sheetParameter, api.getRequestPayloads());
                setResponseParametersAndExample(sheetParameter, api.getResponsePayloads());
                setShow(sheetParameter);
                sheetParameterList.add(sheetParameter);
            })
        );
        return sheetParameterList;
    }

    private void setRequestParametersAndExample(SheetParameter sheetParameter, List<RequestPayload> requestPayloads) {
        if (!CollectionUtils.isEmpty(requestPayloads)) {
            var requestPayload = requestPayloads.get(0);
            var parameter = requestPayload.getParameters();
            var requestParameter = toRequestParameter(parameter);
            sheetParameter.setRequestParameters(requestParameter);
            var payloadExamples = requestPayload.getPayloadExamples();
            if (!CollectionUtils.isEmpty(payloadExamples)) {
                var payloadExample = payloadExamples.get(0);
                sheetParameter.setRequestExample(payloadExample.getValue());
            } else {
                sheetParameter.setRequestExample(getDefaultExampleFromRequest(requestParameter));
            }
        }
    }

    private void setResponseParametersAndExample(
        SheetParameter sheetParameter,
        List<ResponsePayload> responsePayloads) {
        if (!CollectionUtils.isEmpty(responsePayloads)) {
            // 只顯示 200 的 response
            var successResponsePayloads = responsePayloads.stream()
                .filter(responsePayload -> "200".equals(responsePayload.getHttpCode()))
                .toList();
            if (!CollectionUtils.isEmpty(successResponsePayloads)) {
                var responsePayload = successResponsePayloads.get(0);
                var parameter = responsePayload.getParameters();
                var responseParameter = toResponseParameter(parameter);
                var payloadExample = responsePayload.getPayloadExample();
                sheetParameter.setResponseParameters(responseParameter);
                if (payloadExample != null) {
                    sheetParameter.setResponseExample(payloadExample);
                } else {
                    sheetParameter.setResponseExample(getDefaultExampleFromResponse(responseParameter));
                }
            }
        }
    }

    private ArrayList<OtherParameter> getOtherParameters(Api api) {
        var otherParameterList = new ArrayList<OtherParameter>();

        var requestsPathParameters = api.getRequestsPathParameters();
        if (!CollectionUtils.isEmpty(requestsPathParameters)) {
            requestsPathParameters.forEach(pathParameter ->
                otherParameterList.add(toOtherParameter("path", pathParameter))
            );
        }

        var requestsHeaderParameters = api.getRequestsHeaderParameters();
        if (!CollectionUtils.isEmpty(requestsPathParameters)) {
            requestsHeaderParameters.forEach(pathParameter ->
                otherParameterList.add(toOtherParameter("header", pathParameter))
            );
        }

        var requestsQueryParameters = api.getRequestsQueryParameters();
        if (!CollectionUtils.isEmpty(requestsPathParameters)) {
            requestsQueryParameters.forEach(pathParameter ->
                otherParameterList.add(toOtherParameter("query", pathParameter))
            );
        }
        return otherParameterList;
    }

    private OtherParameter toOtherParameter(String in, Parameter parameter) {
        var otherParameter = new OtherParameter();
        otherParameter.setIn(in);
        otherParameter.setName(parameter.getName());
        otherParameter.setType(parameter.getType());
        otherParameter.setMaxLength(parameter.getMaxLength());
        otherParameter.setRequired(toYN(parameter.getRequired()));
        otherParameter.setDescription(getDescription(parameter));
        return otherParameter;
    }

    private List<RequestParameter> toRequestParameter(List<Parameter> parameters) {
        if (CollectionUtils.isEmpty(parameters)) {
            return Collections.emptyList();
        }
        return parameters.stream().map(parameter -> {
            var requestParameter = new RequestParameter();
            requestParameter.setLevel(parameter.getLevel());
            requestParameter.setName(parameter.getName());
            requestParameter.setType(parameter.getType());
            requestParameter.setMaxLength(parameter.getMaxLength());
            requestParameter.setRequired(toYN(parameter.getRequired()));
            requestParameter.setDescription(getDescription(parameter));
            return requestParameter;
        }).toList();
    }

    private List<ResponseParameter> toResponseParameter(List<Parameter> parameters) {
        if (CollectionUtils.isEmpty(parameters)) {
            return Collections.emptyList();
        }
        return parameters.stream().map(parameter -> {
            var responseParameter = new ResponseParameter();
            responseParameter.setLevel(parameter.getLevel());
            responseParameter.setName(parameter.getName());
            responseParameter.setType(parameter.getType());
            responseParameter.setMaxLength(parameter.getMaxLength());
            responseParameter.setDescription(getDescription(parameter));
            return responseParameter;
        }).toList();
    }

    private String getDescription(Parameter parameter) {
        var description = parameter.getDescription();
        if (CollectionUtils.isEmpty(parameter.getEnumValues())) {
            return description;
        }
        var allowValue = parameter.getEnumValues().stream().map(Object::toString)
            .collect(Collectors.joining(","));
        if (StringUtils.isEmpty(description)) {
            return "allowableValues: %s".formatted(allowValue);
        }
        return "%s (allowableValues: %s)".formatted(description, allowValue);
    }

    private String getDefaultExampleFromRequest(List<RequestParameter> parameters) {
        return getDefaultExample(parameters.stream().map(PayloadParameter.class::cast).toList());
    }

    private String getDefaultExampleFromResponse(List<ResponseParameter> parameters) {
        return getDefaultExample(parameters.stream().map(PayloadParameter.class::cast).toList());
    }

    private void setShow(SheetParameter sheetParameter) {
        sheetParameter.setShowOtherParameters(!CollectionUtils.isEmpty(sheetParameter.getOtherParameters()));
        sheetParameter.setShowRequestParameters(!CollectionUtils.isEmpty(sheetParameter.getRequestParameters()));
        sheetParameter.setShowRequestExample(!StringUtils.isEmpty(sheetParameter.getRequestExample()));
        sheetParameter.setShowResponseParameters(!CollectionUtils.isEmpty(sheetParameter.getResponseParameters()));
        sheetParameter.setShowResponseExample(!StringUtils.isEmpty(sheetParameter.getResponseExample()));
    }

    private String getDefaultExample(List<PayloadParameter> parameters) {
        if (CollectionUtils.isEmpty(parameters)) {
            return null;
        }

        try {
            var defaultMap = new LinkedHashMap<String, Object>();
            var tmpLevelMap = new LinkedHashMap<Integer, LinkedHashMap<String, Object>>();
            tmpLevelMap.put(1, defaultMap);
            for (PayloadParameter parameter : parameters) {
                var type = parameter.getType();
                boolean isArray = isArray(type);
                if (isArray) {
                    type = getArrayType(type);
                }
                // 目前只列出有用到的類型，後續可依需求擴充
                var value = switch (type) {
                    case "string", "string($byte)", "string($binary)" -> "string";
                    case "string($date)" -> "2023-12-31";
                    case "string($date-time)" -> "2023-12-31T00:00:00.000Z";
                    case "number", "number($bigdecimal)" -> 1;
                    case "integer", "integer($int32)", "integer($int64)" -> 0;
                    case "boolean" -> true;
                    case "object" -> new LinkedHashMap<String, Object>();
                    default -> throw new IllegalStateException("Unexpected value: " + type);
                };
                var level = parameter.getLevel();
                if (type.equals("object")) {
                    tmpLevelMap.put(level + 1, (LinkedHashMap<String, Object>) value);
                }
                tmpLevelMap.get(level).put(parameter.getName(), isArray ? List.of(value) : value);
            }
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(defaultMap);
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return null;
        }
    }

    private boolean isArray(String type) {
        return type.startsWith("array");
    }

    private String getArrayType(String type) {
        return type.replace("array", "")
            .replace("[", "")
            .replace("]", "");
    }

    private String toYN(Boolean bool) {
        if (bool == null) {
            return null;
        }
        return bool ? "Y" : "N";
    }

}
