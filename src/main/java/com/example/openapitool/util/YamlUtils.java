package com.example.openapitool.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class YamlUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @SneakyThrows
    public static String convertYamlToJson(String yaml) {
        var yamlReader = new ObjectMapper(new YAMLFactory());
        var obj = yamlReader.readValue(yaml, Object.class);
        return OBJECT_MAPPER.writeValueAsString(obj);
    }

}