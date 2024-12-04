from newsapi import NewsApiClient

def news_extractor():
    newsapi = NewsApiClient(api_key='0fc693d74fc84c36aae60970c9c88b68') 
    #use your API key here

    tech_articles = newsapi.get_everything(q='tech', language='en', \
    page_size=100)

    tech = pd.DataFrame(tech_articles['articles'])
    #adding the tech category
    tech['category'] = 'Tech'


    entertainment_articles = newsapi.get_everything(q='entertainment',\
    language='en', page_size=100)
    business_articles = newsapi.get_everything(q='business',\
    language='en', page_size=100)
    sports_articles = newsapi.get_everything(q='sports',\
    language='en', page_size=100)
    politics_articles = newsapi.get_everything(q='politics',\
    language='en', page_size=100)
    travel_articles = newsapi.get_everything(q='travel',\
    language='en', page_size=100)
    food_articles = newsapi.get_everything(q='food',\
    language='en', page_size=100)
    health_articles = newsapi.get_everything(q='health',\
    language='en', page_size=100)

    entertainment = pd.DataFrame(entertainment_articles['articles'])
    entertainment['category'] = 'Entertainment'
    business = pd.DataFrame(business_articles['articles'])
    business['category'] = 'Business'
    sports = pd.DataFrame(sports_articles['articles'])
    sports['category'] = 'Sports'
    politics = pd.DataFrame(politics_articles['articles'])
    politics['category'] = 'Politics'
    travel = pd.DataFrame(travel_articles['articles'])
    travel['category'] = 'Travel'
    food = pd.DataFrame(food_articles['articles'])
    food['category'] = 'Food'
    health = pd.DataFrame(health_articles['articles'])
    health['category'] = 'Health'

    categories = [tech, entertainment, business, sports, politics, \
    travel, food, health]
    df = pd.concat(categories)
    df.info()

    df.to_csv('articles2.csv',index=False)


import re
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.metrics import accuracy_score
from nltk.tokenize import word_tokenize
from nltk.corpus import stopwords
import nltk
nltk.download('punkt')       # For sentence and word tokenization
nltk.download('punkt_tab')   # Additional punkt resource for language support
nltk.download('stopwords')   # For removing stopwords
import joblib

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


def main():
    # Load data
    try:
        article_details = pd.read_csv('Articles_updated.csv',encoding='windows-1252')
    except FileNotFoundError:
        print("Error: The file 'articles.csv' does not exist.")
        return

    if 'title' not in article_details.columns or 'category' not in article_details.columns:
        print("Error: The required columns 'title' and 'category' are missing in the CSV file.")
        return

    # Check if 'description' column exists, if not, use 'title' only
    if 'description' not in article_details.columns:
        print("Warning: 'description' column is missing. Using only titles for prediction.")
        article_details['description'] = ''

    # Combine title and description columns
    article_details['combined_text'] = article_details['title'] + " " + article_details['description']

    # Clean the combined text
    article_details['cleaned_text'] = article_details['combined_text'].apply(cleaned_desc_column)

    # Split data into train and test sets
    X = article_details['cleaned_text']
    y = article_details['category']

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.30, random_state=90)
    print(f"Training set size: {X_train.shape[0]}")
    print(f"Test set size: {X_test.shape[0]}")

    # Create pipeline
    lr = Pipeline([
        ('tfidf', TfidfVectorizer(max_features=5000)),  # Limit features to improve performance
        ('clf', LogisticRegression(max_iter=1000)),
    ])

    # Train the model
    lr.fit(X_train, y_train)

    # Test the model
    y_pred = lr.predict(X_test)
    print(f"Accuracy: {accuracy_score(y_test, y_pred):.2f}")

    # Example predictions
    news = [
        "Biden to Sign Executive Order That Aims to Make Child Care Cheaper",
        "Google Stock Loses $57 Billion Amid Microsoft's AI Lead—And Reports It Could Be Replaced By Bing On Some Smartphones",
        "Poland suspends food imports from Ukraine to assist its farmers",
        "Can AI Solve The Air Traffic Control Problem? Let's Find Out",
        "Woman From Odisha Runs 42.5 KM In UK Marathon Wearing A Saree",
        "Hillary Clinton: Trump cannot win the election - but Biden will",
        "Jennifer Aniston and Adam Sandler starrer movie 'Murder Mystery 2' got released on March 24, this year"
    ]
    # Example descriptions (you should replace these with real descriptions if available)
    news_descriptions = [
        "The U.S. government is working on making childcare more affordable.",
        "Google has suffered major financial losses as Microsoft leads the AI race.",
        "Poland is taking action to support its farmers by suspending food imports.",
        "A deep dive into the challenges AI faces in air traffic control systems.",
        "A woman completed a marathon in the UK while wearing traditional clothing.",
        "Hillary Clinton speaks out on the presidential election, making bold predictions.",
        "The comedy movie 'Murder Mystery 2' starring Jennifer Aniston and Adam Sandler."
    ]
    # Combine titles and descriptions for prediction
    combined_news = [f"{title} {desc}" for title, desc in zip(news, news_descriptions)]

    # Clean and predict
    news_cleaned = [cleaned_desc_column(n) for n in combined_news]
    predicted = lr.predict(news_cleaned)

    # Display results
    for doc, category in zip(combined_news, predicted):
        print(f"News: {doc}\nPredicted Category: {category}\n")

    # Save the vectorizer
    joblib.dump(lr.named_steps['tfidf'], 'tfidf_vectorizer.pkl')

    # Save the model
    joblib.dump(lr, 'logistic_model.pkl')


if __name__ == "__main__":
    main()
