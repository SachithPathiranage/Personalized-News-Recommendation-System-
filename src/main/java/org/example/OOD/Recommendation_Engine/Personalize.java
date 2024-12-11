package org.example.OOD.Recommendation_Engine;

import javafx.scene.control.Alert;
import org.example.OOD.Database_Handler.DatabaseHandler;
import org.example.OOD.Models.Article;
import org.example.OOD.Models.UserPreferences;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static org.example.OOD.Configurations.Alerts.showAlert;

public class Personalize {

    // 1. Method to get keywords from user preferences
    public List<String> extractKeywordsFromUserPreferences(String userId) throws SQLException {
        List<Article> userPreferredArticles;
        List<Article> userLikedArticles = fetchUserPreferredArticles(userId, "liked");
        List<Article> userReadArticles = fetchUserPreferredArticles(userId, "read");
        userPreferredArticles = new ArrayList<>(userLikedArticles);
        userPreferredArticles.addAll(userReadArticles);

        List<String> keywords = new ArrayList<>();

        for (Article article : userPreferredArticles) {
            List<String> articleKeywords = extractKeywords(article.getText());

            // Add article category as a keyword
            keywords.add(article.getCategory());  // Include category as a keyword

            keywords.addAll(articleKeywords);
            //System.out.println(keywords);
        }

        return keywords.stream()
                .distinct()  // Remove duplicates
                .collect(Collectors.toList());
    }

    // Helper method to fetch user's preferred articles
    public List<Article> fetchUserPreferredArticles(String userId, String preferenceType) throws SQLException {
        List<Integer> preferredArticleIds = DatabaseHandler.getInstance().fetchUserPreferences(userId, preferenceType);
        List<Article> preferredArticles = new ArrayList<>();

        for (int articleId : preferredArticleIds) {
            Article article = DatabaseHandler.getInstance().fetchArticleById(articleId);
            if (article != null) {
                preferredArticles.add(article);
            }
        }

        return preferredArticles;
    }

    // 2. Method to extract keywords from an article using KeywordExtractor
    private List<String> extractKeywords(String articleText) throws SQLException {
        // Initialize the KeywordExtractor
        KeywordExtractor extractor = new KeywordExtractor();

        // Fetch the corpus from your database
        List<Article> allArticles = DatabaseHandler.fetchNewsFromDatabase();  // Ensure your DatabaseHandler instance is initialized
        List<String> corpus = allArticles.stream()
                .map(Article::getText)
                .filter(Objects::nonNull)  // Avoid null values in the corpus
                .collect(Collectors.toList());

        // Extract keywords from the article using the TF-IDF method from KeywordExtractor
        List<String> keywords = extractor.extractKeywordsBlended(articleText, corpus);

        // Remove stopwords from the extracted keywords
        Set<String> stopwords = getStopwords();
        keywords = keywords.stream()
                .filter(keyword -> !stopwords.contains(keyword.toLowerCase()))
                .collect(Collectors.toList());

        return keywords;
    }


    // 3. Method to compute the cosine similarity between user keywords and an article
    public double computeCosineSimilarity(List<String> userKeywords, String articleText) throws SQLException {
        List<String> articleKeywords = extractKeywords(articleText);

        Set<String> allKeywords = new HashSet<>(userKeywords);
        allKeywords.addAll(articleKeywords);

        // Creating vectors based on keyword frequencies
        Map<String, Integer> userKeywordFreq = getKeywordFrequency(userKeywords, allKeywords);
        Map<String, Integer> articleKeywordFreq = getKeywordFrequency(articleKeywords, allKeywords);

        return cosineSimilarity(userKeywordFreq, articleKeywordFreq);
    }

    // Helper method to compute the frequency of each keyword in a list
    private Map<String, Integer> getKeywordFrequency(List<String> keywords, Set<String> allKeywords) {
        Map<String, Integer> freqMap = new HashMap<>();
        for (String keyword : allKeywords) {
            freqMap.put(keyword, Collections.frequency(keywords, keyword));
        }
        return freqMap;
    }

    // Helper method to calculate cosine similarity between two frequency maps
    private double cosineSimilarity(Map<String, Integer> vector1, Map<String, Integer> vector2) {
        Set<String> allKeys = new HashSet<>(vector1.keySet());
        allKeys.addAll(vector2.keySet());

        double dotProduct = 0;
        double magnitude1 = 0;
        double magnitude2 = 0;

        for (String key : allKeys) {
            int freq1 = vector1.getOrDefault(key, 0);
            int freq2 = vector2.getOrDefault(key, 0);
            dotProduct += freq1 * freq2;
            magnitude1 += freq1 * freq1;
            magnitude2 += freq2 * freq2;
        }

        return dotProduct / (Math.sqrt(magnitude1) * Math.sqrt(magnitude2));
    }

    // 4. Method to recommend articles based on the keywords extracted from user preferences
    public List<Article> recommendArticles(String userId, int topN) throws SQLException {
        // Extract keywords from user preferences
        List<String> userKeywords = extractKeywordsFromUserPreferences(userId);

        // Fetch all articles from the database
        DatabaseHandler.getInstance();
        List<Article> allArticles = DatabaseHandler.fetchNewsFromDatabase();

        // Fetch user's preferred categories (based on liked or read articles)
        List<String> preferredCategories = fetchPreferredCategories(userId);
        //System.out.println(preferredCategories);

        // List to store recommended articles
        List<Article> recommendedArticles = new ArrayList<>();

        for (Article article : allArticles) {
            // Calculate similarity score based on keywords
            double similarityScore = computeCosineSimilarity(userKeywords, article.getText());


            // Boost score if the article belongs to a preferred category
            if (preferredCategories.contains(article.getCategory())) {
                similarityScore *= 1.5; // Boost factor for preferred categories
                //System.out.println( article.getTitle() + " - " + article.getCategory()+ " - " + preferredCategories.contains(article.getCategory()));
                //System.out.println(similarityScore);
            }

            // Consider the article if it meets a threshold
            if (similarityScore > 0.1) { // Threshold for similarity (adjustable)
                article.setSimilarityScore(similarityScore);
                recommendedArticles.add(article);
            }
        }

        // Sort articles by similarity score in descending order
        recommendedArticles.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));

        // Return the top N recommended articles
        return recommendedArticles.stream()
                .limit(topN)
                .collect(Collectors.toList());
    }

    // Helper method to fetch user's preferred categories
    private List<String> fetchPreferredCategories(String userId) throws SQLException {
        // Fetch all user preferences using the provided method
        Map<String, List<Integer>> preferencesMap = DatabaseHandler.getInstance().fetchAllUserPreferences(userId);

        // Combine liked and read articles (disliked articles are not relevant for category preference)
        Set<Integer> preferredArticleIds = new HashSet<>();
        preferredArticleIds.addAll(preferencesMap.get("liked"));
        preferredArticleIds.addAll(preferencesMap.get("read"));

        // Use a Set to avoid duplicate categories
        Set<String> preferredCategories = new HashSet<>();

        // Iterate through each article ID and fetch its category
        for (int articleId : preferredArticleIds) {
            String category = DatabaseHandler.getInstance().fetchArticleCategoryById(articleId);
            if (category != null && !category.isEmpty()) {
                preferredCategories.add(category);
            }
        }

        // Debug: Print the preferred categories
        //System.out.println(preferredCategories);

        // Convert Set to List and return
        return new ArrayList<>(preferredCategories);
    }


    // 5. Helper method to get a list of stopwords (optional)
    private Set<String> getStopwords() {
        // Example stopwords set
        return new HashSet<>(Arrays.asList("the", "and", "is", "in", "to", "of", "a", "on", "for", "with"));
    }

    // 6. Method to fetch and handle recommendations
    public List<Article> fetchAndHandleRecommendations(String userId, int maxArticles) throws SQLException {
        // Fetch the top N recommended articles
        List<Article> recommendations = recommendArticles(userId, maxArticles);

        if (recommendations.isEmpty()) {
            System.out.println("No recommendations available.");
            showAlert(Alert.AlertType.ERROR, "Error", "No recommendations available.");
            throw new SQLException("No recommendations available.");
        }

        // Log the top recommendations
        System.out.println("Top recommendations:");
        for (Article article : recommendations) {
            System.out.println("- " + article.getTitle() + " (Similarity Score: " + article.getSimilarityScore() + ")");
        }

        return recommendations;
    }
}
