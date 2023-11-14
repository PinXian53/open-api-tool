package com.example.openapitool.service;

import com.example.openapitool.model.open_api.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@SuppressWarnings("unchecked")
@Service
public class OpenApiSpecService {

    private static final Configuration jsonPathConfig = Configuration.defaultConfiguration()
        .jsonProvider(new JacksonJsonProvider())
        .mappingProvider(new JacksonMappingProvider())
        .addOptions(Option.SUPPRESS_EXCEPTIONS);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public OpenApiDoc getOpenApiDoc(String jsonContent) {
        var context = JsonPath.parse(jsonContent, jsonPathConfig);
        var openApiVersion = (String) context.read("$.openapi");
        var title = (String) context.read("$.info.title");
        var description = (String) context.read("$.info.description");
        var groups = parseToGroupList(context);

        return OpenApiDoc.builder()
            .openApiVersion(openApiVersion)
            .title(title)
            .description(description)
            .groups(groups)
            .build();
    }

    private List<Group> parseToGroupList(DocumentContext context) {
        var paths = (LinkedHashMap<String, LinkedHashMap<String, ?>>) context.read("$.paths");
        var apiMap = new LinkedHashMap<String, List<Api>>();
        paths.forEach((path, pathInfo) ->
            pathInfo.forEach((httpMethod, apiObject) -> {
                if ("parameters".equals(httpMethod)) {
                    return;
                }
                var apiInfo = (LinkedHashMap<String, ?>) apiObject;
                var summary = (String) apiInfo.get("summary");
                var deprecated = (Boolean) apiInfo.get("deprecated");
                var requestBody = (LinkedHashMap<String, ?>) apiInfo.get("requestBody");
                var requestParameters = (List<LinkedHashMap<?, ?>>) apiInfo.get("parameters");
                var responses = (LinkedHashMap<String, LinkedHashMap<?, ?>>) apiInfo.get("responses");
                var tags = (List<String>) apiInfo.get("tags");

                var api = new Api();
                api.setHttpMethod(httpMethod);
                api.setPath(path);
                api.setSummary(summary);
                api.setDeprecated(deprecated);
                setRequestParameters(api, requestParameters, context);
                setRequestPayloads(api, requestBody, context);
                setResponsePayloads(api, responses, context);

                tags.forEach(tag -> {
                    if (!apiMap.containsKey(tag)) {
                        apiMap.put(tag, new ArrayList<>());
                    }
                    apiMap.get(tag).add(api);
                });
            })
        );

        var groups = new ArrayList<Group>();
        apiMap.forEach((tag, apis) ->
            groups.add(new Group(tag, apis))
        );
        return groups;
    }

    private void setRequestPayloads(Api api, LinkedHashMap<String, ?> requestBody, DocumentContext context) {
        if (requestBody != null) {
            var requestPayloads = new ArrayList<RequestPayload>();
            var content = (LinkedHashMap<String, ?>) requestBody.get("content");
            content.forEach((mediaType, schemaObject) -> {
                var schema = (LinkedHashMap<String, ?>) schemaObject;
                var ref = (String) ((LinkedHashMap<String, ?>) schema.get("schema")).get("$ref");
                var examples = (LinkedHashMap<String, ?>) schema.get("examples");
                var request = new RequestPayload();
                request.setMediaType(mediaType);
                request.setRef(ref);
                request.setParameters(getPayloadParameter(ref, null, null, context));
                setRequestPayloadExample(request, examples);
                requestPayloads.add(request);
            });
            api.setRequestPayloads(requestPayloads);
        }
    }

    private void setRequestParameters(Api api, List<LinkedHashMap<?, ?>> requestParameters, DocumentContext context) {
        if (requestParameters != null) {
            var requestHeaderList = new ArrayList<Parameter>();
            var requestPathList = new ArrayList<Parameter>();
            var requestQueryList = new ArrayList<Parameter>();
            requestParameters.forEach(requestParameter -> {
                var name = (String) requestParameter.get("name");
                var in = (String) requestParameter.get("in");
                var description = (String) requestParameter.get("description");
                var required = (Boolean) requestParameter.get("required");
                var schema = (LinkedHashMap<?, ?>) requestParameter.get("schema");
                var type = (String) schema.get("type");
                var enumValues = (List<Object>) schema.get("enum");
                var ref = (String) schema.get("$ref");

                var refEnum = getRefEnum(ref, context);
                // 判斷是不是 ENUM
                if (refEnum != null) {
                    type = refEnum.getType();
                    enumValues = refEnum.getEnumValues();
                }

                if ("array".equals(type)) {
                    var items = (LinkedHashMap<?, ?>) schema.get("items");
                    var itemsType = (String)items.get("type");
                    var itemsRef = (String) items.get("$ref");
                    var itemsRefEnum = getRefEnum(itemsRef, context);
                    // 判斷是不是 ENUM
                    if (itemsRefEnum != null) {
                        itemsType = itemsRefEnum.getType();
                        enumValues = itemsRefEnum.getEnumValues();
                    }
                    type = "array[%s]".formatted(itemsType);
                }

                var format = (String) schema.get("format");
                if (format != null) {
                    type = "%s($%s)".formatted(type, format);
                }

                var parameter = new Parameter();
                parameter.setName(name);
                parameter.setType(type);
                parameter.setDescription(description);
                parameter.setRequired(required);
                parameter.setEnumValues(enumValues);
                parameter.setMinLength(null);
                parameter.setMaxLength(null);

                switch (in) {
                    case "query":
                        requestQueryList.add(parameter);
                        break;
                    case "path":
                        requestPathList.add(parameter);
                        break;
                    case "header":
                        requestHeaderList.add(parameter);
                        break;
                    default:
                        log.warn("Find unsupported parameter type: {}", in);
                        break;
                }
            });

            api.setRequestsHeaderParameters(requestHeaderList);
            api.setRequestsPathParameters(requestPathList);
            api.setRequestsQueryParameters(requestQueryList);
        }
    }

    private void setResponsePayloads(
        Api api,
        LinkedHashMap<String, LinkedHashMap<?, ?>> responses,
        DocumentContext context) {
        if (responses != null) {
            var responseList = new ArrayList<ResponsePayload>();
            responses.forEach((status, responseInfo) -> {
                var description = (String) responseInfo.get("description");
                var content = ((LinkedHashMap<String, LinkedHashMap<?, ?>>) responseInfo.get("content"));
                if (content != null) {
                    content.forEach((mediaType, schema) -> {
                        var ref = (String) ((LinkedHashMap<String, ?>) schema.get("schema")).get("$ref");
                        var response = new ResponsePayload();
                        response.setDescription(description);
                        response.setMediaType(mediaType);
                        response.setHttpCode(status);
                        response.setRef(ref);
                        response.setParameters(getPayloadParameter(ref, null, null, context));
                        responseList.add(response);
                    });
                } else {
                    var response = new ResponsePayload();
                    response.setHttpCode(status);
                    response.setDescription(description);
                    responseList.add(response);
                }
            });
            api.setResponsePayloads(responseList);
        }
    }

    @SneakyThrows
    private void setRequestPayloadExample(RequestPayload requestPayload, LinkedHashMap<String, ?> examples) {
        if (examples == null) {
            return;
        }
        var payloadExamples = new ArrayList<PayloadExample>();
        for (Map.Entry<String, ?> entry : examples.entrySet()) {
            var exampleName = entry.getKey();
            var exampleValue = entry.getValue();
            var exampleMap = ((LinkedHashMap<String, ?>) exampleValue).get("value");
            var jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exampleMap);
            payloadExamples.add(new PayloadExample(exampleName, jsonString));
        }
        requestPayload.setPayloadExamples(payloadExamples);
    }

    private List<Parameter> getPayloadParameter(
        String ref,
        String parentRef,
        String parentSeq,
        DocumentContext context) {
        if (ref == null || ref.equals(parentRef)) {
            // ref 等於 parentRef 就跳過，避免無限迴圈造成 StackOverflowError
            return Collections.emptyList();
        }

        var jsonPath = refToJsonPath(ref);
        var components = (LinkedHashMap<String, ?>) context.read(jsonPath);

        var requiredFieldName = (List<String>) components.get("required");
        var parameterList = new ArrayList<Parameter>();

        var properties = (LinkedHashMap<String, LinkedHashMap<String, ?>>) components.get("properties");
        if (properties == null) {
            return Collections.emptyList();
        }

        int index = 1;

        for (Map.Entry<String, LinkedHashMap<String, ?>> entry : properties.entrySet()) {
            String fieldName = entry.getKey();
            LinkedHashMap<String, ?> property = entry.getValue();
            var subRef = (String) property.get("$ref");
            var description = (String) property.get("description");
            var type = (String) property.get("type");
            var required = isRequired(requiredFieldName, fieldName);
            var example = property.get("example");
            var enumValue = (List<Object>) property.get("enum");
            var refEnum = getRefEnum(subRef, context);
            // 判斷是不是 ENUM
            if (refEnum != null) {
                type = refEnum.getType();
                enumValue = refEnum.getEnumValues();
                subRef = null;
            }

            if (subRef != null) {
                type = "object";
            }

            if ("array".equals(type)) {
                var items = (LinkedHashMap<?, ?>) property.get("items");
                var itemsType = (String) items.get("type");
                var itemsRef = (String) items.get("$ref");
                if (itemsRef == null) {
                    type = "array[%s]".formatted(itemsType);
                } else {
                    var subRefEnum = getRefEnum(itemsRef, context);
                    if (subRefEnum != null) {
                        type = "array[%s]".formatted(subRefEnum.getType());
                        enumValue = subRefEnum.getEnumValues();
                    }else {
                        type = "array[object]";
                        subRef = itemsRef;
                    }
                }
            }

            var seq = getSeq(parentSeq, index);

            if (subRef == null) {
                var format = (String) property.get("format");

                if (format != null) {
                    type = "%s($%s)".formatted(type, format);
                }
                var parameter = new Parameter();
                parameter.setSequence(seq);
                parameter.setName(fieldName);
                parameter.setType(type);
                parameter.setDescription(description);
                parameter.setRequired(required);
                parameter.setEnumValues(enumValue);
                parameter.setExample(example);
                parameterList.add(parameter);
            } else {
                var parameter = new Parameter();
                parameter.setSequence(seq);
                parameter.setName(fieldName);
                parameter.setType(type);
                parameter.setDescription(description);
                parameter.setRequired(required);
                parameter.setExample(example);
                parameterList.add(parameter);
                parameterList.addAll(getPayloadParameter(subRef, ref, seq, context));
            }
            index++;
        }
        return parameterList;
    }

    private String getSeq(String parentSeq, int index) {
        if (parentSeq == null) {
            return String.valueOf(index);
        }
        return "%s.%s".formatted(parentSeq, index);
    }

    private RefEnum getRefEnum(String ref, DocumentContext context) {
        if (ref == null) {
            return null;
        }
        var jsonPath = refToJsonPath(ref);
        var components = (LinkedHashMap<String, ?>) context.read(jsonPath);
        var enumValues = (List<Object>) components.get("enum");
        if (enumValues == null) {
            return null;
        }
        var type = (String) components.get("type");
        return RefEnum.builder()
            .type(type)
            .enumValues(enumValues)
            .build();
    }

    private String refToJsonPath(String ref) {
        return ref.replace("#", "$").replace("/", ".");
    }

    private boolean isRequired(List<String> requiredFieldName, String fieldName) {
        if (requiredFieldName == null) {
            return false;
        }
        return requiredFieldName.contains(fieldName);
    }
}
