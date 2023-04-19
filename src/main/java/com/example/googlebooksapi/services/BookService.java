package com.example.googlebooksapi.services;

import com.example.googlebooksapi.dtos.GoogleBooksAPIResponse;
import com.example.googlebooksapi.dtos.BookResponse;
import com.example.googlebooksapi.dtos.OpenAIResponse;
import com.example.googlebooksapi.dtos.RecommendationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BookService {

    @Value("${app.api-key}")
    private String API_KEY;

    public ArrayList<BookResponse> booksBySearch(String search) {
        return callGoogleBooksApi(search).block().getItems();
    }

    public OpenAIResponse getRecommendation(RecommendationRequest request) {
        return getRecommendationFromOpenAI(request, 5);
    }


    private Mono<GoogleBooksAPIResponse> callGoogleBooksApi(String query) {
        Mono<GoogleBooksAPIResponse> response = WebClient.create()
                .get()
                .uri("https://www.googleapis.com/books/v1/volumes?q=" + query)
                .retrieve()
                .bodyToMono(GoogleBooksAPIResponse.class);
        return response;
    }

    private OpenAIResponse getRecommendationFromOpenAI(RecommendationRequest request, int recommendationAmount) {
        String titles = request.getBooks().stream().map(book -> "(" + book.getTitle() + ")").collect(Collectors.joining(", "));
        String authors = request.getBooks().stream().map(book -> book.getAuthors().stream().collect(Collectors.joining(", "))).collect(Collectors.joining(", "));
        String prompt = String.format("Give me %d recommended book titles based on the following book titles: %s and the following authors: %s", recommendationAmount, titles, authors);
        Map<String, Object> body = new HashMap<>();
        body.put("model", "text-davinci-003");
        body.put("prompt", prompt);
        body.put("temperature", 1);
        body.put("max_tokens", 1000);
        body.put("top_p", 1);
        body.put("frequency_penalty", 0.2);
        body.put("presence_penalty", 0);

        ObjectMapper mapper = new ObjectMapper();
        String json = "";
        try {
            json = mapper.writeValueAsString(body);
        } catch (Exception e) {
            e.printStackTrace();
        }

        OpenAIResponse response = WebClient.create()
                .post()
                .uri("https://api.openai.com/v1/completions")
                .header("Authorization", "Bearer " + API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(json))
                .retrieve()
                .bodyToMono(OpenAIResponse.class)
                .block();

        return response;
    }
}
