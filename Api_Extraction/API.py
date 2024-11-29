from io import BytesIO

import requests
import mysql.connector
from datetime import datetime

# Configure API and database connection
API_KEY = '0fc693d74fc84c36aae60970c9c88b68'  # Replace with your News API key
API_URL = 'https://newsapi.org/v2/top-headlines'
COUNTRY = 'us'

DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '',
    'database': 'news_recommendation'
}

def fetch_news():
    """Fetch news from the API."""
    params = {
        'apiKey': API_KEY,
        'country': COUNTRY
    }
    response = requests.get(API_URL, params=params)
    if response.status_code == 200:
        return response.json().get('articles', [])
    else:
        print(f"Error: {response.status_code}, {response.text}")
        return []

def download_image(url):
    """Download image from URL and return binary data."""
    try:
        response = requests.get(url)
        if response.status_code == 200:
            return BytesIO(response.content).getvalue()
        else:
            print(f"Failed to download image: {response.status_code}")
            return None
    except Exception as e:
        print(f"Error downloading image: {e}")
        return None

def store_news_to_db(articles):
    """Store news articles to the MySQL database."""
    connection = mysql.connector.connect(**DB_CONFIG)
    cursor = connection.cursor()

    # Insert query
    insert_query = """
        INSERT INTO news (title, description, content, url, published_at, source_name, author, image)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
    """
    for article in articles:
        try:
            image_data = download_image(article.get('urlToImage'))
            cursor.execute(insert_query, (
                article.get('title'),
                article.get('description'),
                article.get('content'),
                article.get('url'),
                datetime.strptime(article.get('publishedAt'), '%Y-%m-%dT%H:%M:%SZ'),
                article.get('source', {}).get('name'),
                article.get('author'),
                image_data
            ))
        except Exception as e:
            print(f"Failed to insert article: {e}")

    connection.commit()
    cursor.close()
    connection.close()

if __name__ == '__main__':
    news_articles = fetch_news()
    if news_articles:
        store_news_to_db(news_articles)
        print("News articles have been successfully stored to the database.")
    else:
        print("No news articles fetched.")
