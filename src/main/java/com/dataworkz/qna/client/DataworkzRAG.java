package com.dataworkz.qna.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

public class DataworkzRAG {
    public static final String LIST_SYSTEMS = "list.systems";
    public static final String GET_SYSTEM = "get.systems";
    public static final String ASK_QUESTION = "ask.question";
    public static final String GET_QUESTION = "get.question";
    public static final String LIST_LLMS = "list.llms";
    public static final String LIST_QUESTIONS = "list.questions";
    private static final String LIST_QNA_SYSTEMS_API = "/api/qna/v1/systems";
    private static final String GET_SYSTEM_API = "/api/qna/v1/systems/{systemId}";
    private static final String LIST_LLMS_API = "/api/qna/v1/systems/{systemId}/llm-providers";
    private static final String ASK_QUESTION_API = "/api/qna/v1/systems/{systemId}/answer?questionText={questionText}&llmProviderId={llmProviderId}";
    private static final String LIST_QUESTIONS_API = "/api/qna/v1/systems/{systemId}/questionshistory";
    private static final String GET_QUESTION_API = "/api/qna/v1/systems/{systemId}/questions/{questionId}";
    private final String dwHost;
    private final String apiKey;

    public DataworkzRAG(String dwHost, String apiKey) {
        if (dwHost == null || apiKey == null) {
            throw new IllegalArgumentException("service and key must be provided");
        }
        this.dwHost = dwHost.startsWith("https://") ? dwHost : "https://" + dwHost;
        this.apiKey = apiKey;
    }

    public RAGResponse listQnASystems() throws IOException, InterruptedException, URISyntaxException {
        HttpRequest request = getHttpRequest(LIST_QNA_SYSTEMS_API, Map.of());
        HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return new RAGResponse(LIST_SYSTEMS, response, null);
        } else {
            Gson gson = new Gson();
            return new RAGResponse(LIST_SYSTEMS, response, gson.fromJson(response.body(), new TypeToken<Map<String, String>>() {}.getType()));
        }
    }

    protected HttpRequest getHttpRequest(String apiTemplate, Map<String, String> paramMap) throws URISyntaxException {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .header("Authorization", "SSWS " + apiKey)
                .uri(new URI(dwHost + populateAPI(apiTemplate, paramMap)))
                .build();
        return request;
    }

    private String populateAPI(String apiTemplate, Map<String, String> params) {
        String ret = apiTemplate;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            ret = ret.replaceAll("\\{" + entry.getKey() + "}", URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return ret;
    }

    public RAGResponse listLLMs(String qnaSystemId) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = getHttpRequest(LIST_LLMS_API, Map.of("systemId", qnaSystemId));
        HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return new RAGResponse(LIST_LLMS, response, null);
        } else {
            Gson gson = new Gson();
            return new RAGResponse(LIST_LLMS, response, gson.fromJson(response.body(), new TypeToken<Map<String, String>>() {}.getType()));
        }
    }

    public RAGResponse getSystem(String qnaSystemId) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = getHttpRequest(GET_SYSTEM_API, Map.of("systemId", qnaSystemId));
        HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return new RAGResponse(GET_SYSTEM, response, null);
        } else {
            Gson gson = new Gson();
            return new RAGResponse(GET_SYSTEM, response, gson.fromJson(response.body(), new TypeToken<Map<String, Object>>() {}.getType()));
        }
    }

    public RAGResponse listQuestions(String qnaSystemId) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = getHttpRequest(LIST_QUESTIONS_API, Map.of("systemId", qnaSystemId));
        HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return new RAGResponse(LIST_QUESTIONS, response, null);
        } else {
            Gson gson = new Gson();
            return new RAGResponse(LIST_QUESTIONS, response, gson.fromJson(response.body(), new TypeToken<Map<String, Map<String, String>>>() {}.getType()));
        }
    }

    public RAGResponse getQuestion(String qnaSystemId, String questionId) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = getHttpRequest(GET_QUESTION_API, Map.of("systemId", qnaSystemId, "questionId", questionId));
        HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return new RAGResponse(GET_QUESTION, response, null);
        } else {
            Gson gson = new Gson();
            return new RAGResponse(GET_QUESTION, response, gson.fromJson(response.body(), new TypeToken<Map<String, String>>() {}.getType()));
        }
    }

    public RAGResponse askQuestion(String qnaSystemId, String llmProviderId, String questionText) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = getHttpRequest(ASK_QUESTION_API, Map.of("systemId", qnaSystemId,
                "llmProviderId", llmProviderId, "questionText", questionText));
        HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        Gson gson = new Gson();
        if (response.statusCode() != 200) {
            return new RAGResponse(ASK_QUESTION, response, null);
        } else {
            return new RAGResponse(ASK_QUESTION, response, gson.fromJson(response.body(), new TypeToken<Map<String, Object>>() {}.getType()));
        }
    }

    protected HttpClient getHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(60));
        configure(builder);
        return builder.build();
    }

    protected void configure(HttpClient.Builder builder) {
        // hook for subclasses to configure builder
    }
}
