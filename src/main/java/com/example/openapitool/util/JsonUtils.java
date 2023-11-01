package com.example.openapitool.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

import java.io.IOException;

@UtilityClass
public class JsonUtils {
    private static final ObjectMapper Obj = new ObjectMapper();

    static {
        Obj.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        Obj.registerModule(new JavaTimeModule());
    }

    public static boolean isJson(String content) {
        if (!StringUtils.hasLength(content)) {
            return false;
        }
        try {
            Obj.readTree(content);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
