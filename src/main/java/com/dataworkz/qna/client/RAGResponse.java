package com.dataworkz.qna.client;

import java.net.http.HttpResponse;
import java.util.Map;

public class RAGResponse {
    private final String responseType;
    private HttpResponse response;
    private Map<String, ?> payload;

    public RAGResponse(String responseType, HttpResponse response, Map<String, ?> payload) {
        this.responseType = responseType;
        this.response = response;
        this.payload = payload;
    }

    public Map<String, ?> getPayload() {
        return payload;
    }

    public boolean hasPayload() {
        return payload != null;
    }

    public HttpResponse getResponse() {
        return response;
    }

    @Override
    public String toString() {
        return payload == null ? response.toString() + "==>" + response.body() : payload.toString();
    }
}
