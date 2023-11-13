package com.example.openapitool.util;

import com.example.openapitool.exception.InternalServerErrorException;
import okhttp3.*;

import java.io.IOException;

public class OkHttpUtils {

    private static final Headers defaultHeader = Headers.of("Content-Type", "application/json");

    private OkHttpUtils() {
    }

    public static String getRequest(String url) throws IOException {
        var request = new Request.Builder()
            .url(url)
            .headers(defaultHeader)
            .get()
            .build();
        return sendRequest(request);
    }

    private static String sendRequest(Request request) throws IOException {
        int statusCode;
        String responseBody = null;
        try (Response response = new OkHttpClient.Builder().build().newCall(request).execute()) {
            statusCode = response.code();
            if (response.body() != null) {
                responseBody = response.body().string();
            }
        }

        if (statusCode != 200 && statusCode != 204) {
            var msg = "Url: %s, Status Code: %s, Response Body: %s".formatted(request.url(), statusCode, responseBody);
            throw new InternalServerErrorException(msg);
        }
        return responseBody;
    }

}
