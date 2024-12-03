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
        // Create a JSON array to hold the article objects
        JSONArray articlesArray = new JSONArray();

        // Loop through the articles and create a JSON object for each one
        for (String article : articles) {
            JSONObject articleObject = new JSONObject();
            articleObject.put("title", article);  // Assuming the article string is both title and description
            articleObject.put("description", article);  // You can change this if you have separate title and description
            articlesArray.put(articleObject);
        }

        // Create the request body with the articles array
        JSONObject requestBody = new JSONObject();
        requestBody.put("articles", articlesArray);

        // Set up the HTTP client and request
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:5000/categorize"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        // Send the request and get the response
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Check if the request was successful
        if (response.statusCode() == 200) {
            // Debugging: Print the response body
            System.out.println("Response Body: " + response.body());

            // Parse the response
            JSONObject responseBody = new JSONObject(response.body());
            JSONArray categoriesArray = responseBody.getJSONArray("categories");

            // Convert JSONArray to String[]
            String[] categories = new String[categoriesArray.length()];
            for (int i = 0; i < categoriesArray.length(); i++) {
                JSONObject categoryObject = categoriesArray.getJSONObject(i);

                // Debugging: Print each category object to inspect its structure
                System.out.println("Category Object: " + categoryObject.toString());

                // Accessing the category key - If the structure is different, you may need to adjust the key name
                categories[i] = categoryObject.getString("predicted_category");  // Ensure the key matches exactly
            }

            return categories;
        } else {
            throw new RuntimeException("Failed to categorize articles: " + response.body());
        }
    }
}
