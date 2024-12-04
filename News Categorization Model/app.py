from flask import Flask, request, jsonify
import joblib
import numpy as np
import pandas as pd
import re
from nltk.tokenize import word_tokenize
from nltk.corpus import stopwords
import nltk

# Initialize Flask app
app = Flask(__name__)

# Download necessary NLTK resources (ensure they are available before running the app)
nltk.download('punkt')
nltk.download('stopwords')

# Load vectorizer and model once when the API starts
vectorizer = joblib.load('tfidf_vectorizer.pkl')  # Load the saved vectorizer
model = joblib.load('logistic_model.pkl')  # Load the saved logistic regression model

def cleaned_desc_column(text):
    # Ensure the input is a string and handle cases like NaN
    if not isinstance(text, str):
        text = str(text)  # Convert non-strings (e.g., float, NaN) to an empty string or 'unknown'

    # Remove commas, extra spaces, full stops, quotes, and other unwanted characters
    text = re.sub(r'[,\.\'\"\\]', '', text)
    text = re.sub(r'\s+', ' ', text)
    text = re.sub(r'[^a-zA-Z ]', ' ', text)  # Keep only alphabets and spaces

    # Tokenize and remove stop words
    text_tokens = word_tokenize(text)
    stop_words = set(stopwords.words('english'))
    filtered_text = [word for word in text_tokens if word not in stop_words]

    return " ".join(filtered_text)

@app.route('/categorize', methods=['POST'])
def categorize_articles():
    data = request.get_json()
    
    # Validate input: expects a list of articles with 'title' and 'description'
    articles = data.get('articles', [])
    
    if not isinstance(articles, list) or not all(isinstance(article, dict) and 'title' in article and 'description' in article for article in articles):
        return jsonify({"error": "Input must be a list of dictionaries with 'title' and 'description' fields."}), 400
    
    try:
        # Combine the 'title' and 'description' for each article
        combined_texts = [
            cleaned_desc_column(f"{article['title']} {article['description']}")
            for article in articles
        ]
        
        # Predict using the model pipeline
        predictions = model.predict(combined_texts).tolist()
        
        # Create response with articles and their predicted categories
        result = [
            {"title": article['title'], "description": article['description'], "predicted_category": category}
            for article, category in zip(articles, predictions)
        ]
        
        return jsonify({"categories": result})

    except Exception as e:
        return jsonify({"error": f"Error during model prediction: {str(e)}"}), 500


if __name__ == "__main__":
    app.run(debug=True)
