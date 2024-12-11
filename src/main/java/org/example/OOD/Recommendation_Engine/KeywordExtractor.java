package org.example.OOD.Recommendation_Engine;

import java.util.*;
import java.util.stream.Collectors;

public class KeywordExtractor {

    // Example method using TF-IDF for keyword extraction
    public List<String> extractKeywordsUsingTFIDF(String text, List<String> corpus) {
        // Simple split based on whitespace and punctuation
        String[] words = text.split("\\s+");

        // Step 1: Calculate term frequency (TF)
        Map<String, Integer> termFrequency = new HashMap<>();
        for (String word : words) {
            word = word.toLowerCase().replaceAll("[^a-zA-Z]", "");
            if (!word.isEmpty()) {
                termFrequency.put(word, termFrequency.getOrDefault(word, 0) + 1);
            }
        }

        // Step 2: Calculate inverse document frequency (IDF)
        Map<String, Double> inverseDocumentFrequency = new HashMap<>();
        for (String word : termFrequency.keySet()) {
            int docCountWithWord = 0;
            for (String document : corpus) {
                if (document.toLowerCase().contains(word)) {
                    docCountWithWord++;
                }
            }
            inverseDocumentFrequency.put(word, Math.log((double) corpus.size() / (1 + docCountWithWord)));
        }

        // Step 3: Calculate TF-IDF for each word and sort by score
        Map<String, Double> tfidfScores = new HashMap<>();
        for (Map.Entry<String, Integer> entry : termFrequency.entrySet()) {
            String word = entry.getKey();
            int tf = entry.getValue();
            double idf = inverseDocumentFrequency.getOrDefault(word, 0.0);
            double tfidf = tf * idf;
            tfidfScores.put(word, tfidf);
        }

        // Sort words by TF-IDF score in descending order
        return tfidfScores.entrySet().stream()
                .sorted((entry1, entry2) -> Double.compare(entry2.getValue(), entry1.getValue()))
                .limit(5) // Get top 5 keywords
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // Example method using RAKE for keyword extraction
    public List<String> extractKeywordsUsingRAKE(String text) {
        // Simple split based on punctuation
        String[] words = text.split("[^a-zA-Z]+");
        List<String> filteredWords = Arrays.stream(words)
                .map(String::toLowerCase)
                .filter(word -> !word.isEmpty())
                .collect(Collectors.toList());

        // Step 1: Generate candidate keyword phrases
        List<String> candidateKeywords = new ArrayList<>();
        StringBuilder currentPhrase = new StringBuilder();
        for (String word : filteredWords) {
            // Simple rule: if it's a noun-like word, group it into a phrase
            if (isNoun(word)) {
                if (currentPhrase.length() > 0) {
                    currentPhrase.append(" ");
                }
                currentPhrase.append(word);
            } else {
                if (currentPhrase.length() > 0) {
                    candidateKeywords.add(currentPhrase.toString());
                    currentPhrase.setLength(0);  // Reset the phrase
                }
            }
        }
        if (currentPhrase.length() > 0) {
            candidateKeywords.add(currentPhrase.toString());
        }

        // Step 2: Rank candidate phrases based on frequency (simplified version)
        Map<String, Integer> phraseFrequency = new HashMap<>();
        for (String phrase : candidateKeywords) {
            phraseFrequency.put(phrase, phraseFrequency.getOrDefault(phrase, 0) + 1);
        }

        // Step 3: Sort phrases by frequency and return top 5
        return phraseFrequency.entrySet().stream()
                .sorted((entry1, entry2) -> Integer.compare(entry2.getValue(), entry1.getValue()))
                .limit(5) // Get top 5 keyword phrases
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public List<String> extractKeywordsBlended(String text, List<String> corpus) {
        // Extract keywords using TF-IDF
        List<String> tfidfKeywords = extractKeywordsUsingTFIDF(text, corpus);

        // Extract keywords using RAKE
        List<String> rakeKeywords = extractKeywordsUsingRAKE(text);

        // Combine all keywords
        Set<String> combinedKeywords = new LinkedHashSet<>();
        combinedKeywords.addAll(tfidfKeywords);
        combinedKeywords.addAll(rakeKeywords);

        // Split phrases into individual words and collect unique words
        Set<String> uniqueWords = combinedKeywords.stream()
                .flatMap(phrase -> Arrays.stream(phrase.split("\\s+"))) // Split each phrase by spaces
                .map(String::toLowerCase) // Normalize case
                .filter(word -> !word.isEmpty()) // Filter out empty words
                .collect(Collectors.toCollection(LinkedHashSet::new)); // Maintain insertion order and uniqueness

        // Convert to a list and return
        return new ArrayList<>(uniqueWords);
    }


    // Helper method to determine if a word is likely a noun
    private boolean isNoun(String word) {
        // A simple approach: consider words with 2 or more characters as potential nouns
        return word.length() > 2;
    }

    // Example usage
    public static void main(String[] args) {
//        String classPath = System.getProperty("java.class.path");
//        System.out.println("Classpath: " + classPath);

        KeywordExtractor extractor = new KeywordExtractor();

        // Example article text
        String articleText = "AI in healthcare is revolutionizing the way doctors diagnose and treat diseases. AI-based tools assist in analyzing medical images, predicting patient outcomes, and personalizing treatments.";

        // Example corpus (more documents would be included in a real scenario)
        List<String> corpus = Arrays.asList(
                "AI in healthcare and its applications in patient treatment.",
                "The role of machine learning in healthcare diagnostics.",
                "Healthcare and AI: How AI is transforming the medical industry."
        );

        // Extract keywords using TF-IDF
        List<String> tfidfKeywords = extractor.extractKeywordsUsingTFIDF(articleText, corpus);
        System.out.println("TF-IDF Keywords: " + tfidfKeywords);

        // Extract keywords using RAKE
        List<String> rakeKeywords = extractor.extractKeywordsUsingRAKE(articleText);
        System.out.println("RAKE Keywords: " + rakeKeywords);
    }
}
