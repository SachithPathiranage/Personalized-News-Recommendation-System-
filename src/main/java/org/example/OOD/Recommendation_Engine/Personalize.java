package org.example.OOD.Recommendation_Engine;

import org.example.OOD.Database_Handler.DatabaseHandler;
import org.example.OOD.Models.Article;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

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
            System.out.println(articleKeywords);

            // Add article category as a keyword
            keywords.add(article.getCategory().toLowerCase());  // Include category as a keyword

            keywords.addAll(articleKeywords);
        }

        return keywords.stream()
                .distinct()  // Remove duplicates
                .collect(Collectors.toList());
    }

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
        List<String> keywords = extractor.extractKeywordsUsingTFIDF(articleText, corpus);

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
        List<String> userKeywords = extractKeywordsFromUserPreferences(userId);
        DatabaseHandler.getInstance();
        List<Article> allArticles = DatabaseHandler.fetchNewsFromDatabase();

        List<Article> recommendedArticles = new ArrayList<>();

        for (Article article : allArticles) {
            double similarityScore = computeCosineSimilarity(userKeywords, article.getText());
            if (similarityScore > 0.1) {  // A threshold for similarity (this can be adjusted)
                article.setSimilarityScore(similarityScore);
                recommendedArticles.add(article);
            }
        }

        recommendedArticles.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));

        return recommendedArticles.stream()
                .limit(topN)  // Limit to top N articles
                .collect(Collectors.toList());
    }

    // 5. Helper method to get a list of stopwords (optional)
    private Set<String> getStopwords() {
        // Example stopwords set
        return new HashSet<>(Arrays.asList("the", "and", "is", "in", "to", "of", "a", "on", "for", "with"));
    }
}
