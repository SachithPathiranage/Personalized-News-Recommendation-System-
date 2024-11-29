package org.example.OOD.Recommendation_Engine;

import org.json.JSONArray;
import org.json.JSONObject;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.List;

public class RecommendationEngine {

    public String[] categorizeArticles(List<String> articles) throws Exception {
        JSONObject requestBody = new JSONObject();
        requestBody.put("articles", articles);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:5000/categorize"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject responseBody = new JSONObject(response.body());
            JSONArray categoriesArray = responseBody.getJSONArray("categories");
            return categoriesArray.toList().toArray(new String[0]);
        } else {
            throw new RuntimeException("Failed to categorize articles: " + response.body());
        }
    }
}
