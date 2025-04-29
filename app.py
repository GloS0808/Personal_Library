from flask import Flask, render_template, request, send_from_directory
import requests
import json
import os
import mysql.connector
from config import DB_CONFIG

app = Flask(__name__)
app.config['PROPAGATE_EXCEPTIONS'] = True  # This will show more detailed errors
RESULTS_DIR = "results"

# Create the results directory if it doesn't exist
if not os.path.exists(RESULTS_DIR):
    os.makedirs(RESULTS_DIR)

def insert_book_data(book_data):
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor()

        # --- Handle Category ---
        category_id = None
        if 'categories' in book_data and book_data['categories']:
            category_name = book_data['categories'][0]
            cursor.execute("SELECT category_id FROM categories WHERE category_name = %s", (category_name,))
            result = cursor.fetchone()
            if result:
                category_id = result[0]
            else:
                cursor.execute("INSERT INTO categories (category_name) VALUES (%s)", (category_name,))
                category_id = cursor.lastrowid
            conn.commit()

        # --- Handle Book ---
        cursor.execute("SELECT book_id FROM books WHERE isbn_13 = %s OR isbn_10 = %s",
                       (book_data.get('industryIdentifiers',[{},{}])[0].get('identifier'), book_data.get('industryIdentifiers',[{},{}])[1].get('identifier')))
        existing_book = cursor.fetchone()
        if not existing_book:
            cursor.execute(
                """
                INSERT INTO books (isbn_13, isbn_10, title, subtitle, publisher, published_date,
                                description, page_count, average_rating, thumbnail, category_id)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """,
                (
                    book_data.get('industryIdentifiers',[{},{}])[0].get('identifier'),
                    book_data.get('industryIdentifiers',[{},{}])[1].get('identifier'),
                    book_data.get('title'),
                    book_data.get('subtitle'),
                    book_data.get('publisher'),
                    book_data.get('publishedDate'),
                    book_data.get('description'),
                    book_data.get('pageCount'),
                    book_data.get('averageRating'),
                    book_data.get('imageLinks', {}).get('thumbnail'),
                    category_id
                )
            )
            book_id = cursor.lastrowid
            conn.commit()
        else:
            book_id = existing_book[0]

        # --- Handle Authors ---
        if 'authors' in book_data and book_data['authors']:
            for author_name in book_data['authors']:
                cursor.execute("SELECT author_id FROM authors WHERE name = %s", (author_name,))
                author = cursor.fetchone()
                if author:
                    author_id = author[0]
                else:
                    cursor.execute("INSERT INTO authors (name) VALUES (%s)", (author_name,))
                    author_id = cursor.lastrowid
                    conn.commit()

                # Insert into book_authors (many-to-many relationship)
                cursor.execute(
                    "INSERT INTO book_authors (book_id, author_id) VALUES (%s, %s)",
                    (book_id, author_id)
                )
                conn.commit()

        print("Book data inserted successfully!")

    except mysql.connector.Error as err:
        print(f"Error inserting data: {err}")
    finally:
        if conn:
            cursor.close()
            conn.close()

def get_book_data():
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor()

        cursor.execute("""
            SELECT
                b.title, b.subtitle, b.publisher, b.published_date,
                b.description, b.page_count, b.average_rating, b.thumbnail,
                c.category_name,
                GROUP_CONCAT(a.name) AS authors
            FROM books b
            LEFT JOIN categories c ON b.category_id = c.category_id
            LEFT JOIN book_authors ba ON b.book_id = ba.book_id
            LEFT JOIN authors a ON ba.author_id = a.author_id
            GROUP BY b.book_id
        """)
        books = cursor.fetchall()
        return books
    except mysql.connector.Error as err:
        print(f"Error fetching book data: {err}")
        return []
    finally:
        if conn:
            cursor.close()
            conn.close()


@app.route('/', methods=['GET', 'POST'])
def index():
    try:
        lookup_successful = False
        isbn = None
        books = get_book_data()

        if request.method == 'POST':
            isbn = request.form['isbn'].strip()
            if isbn:
                api_url = f"https://www.googleapis.com/books/v1/volumes?q=isbn:{isbn}"
                response = requests.get(api_url)
                response.raise_for_status()
                data = response.json()
                print("API Response:", data)  # Debug print

                if 'items' in data and data['items']:
                    book_data = data['items'][0]['volumeInfo']
                    print("Book Data:", book_data)  # Debug print

                    filepath = os.path.join(RESULTS_DIR, f"{isbn}.txt")
                    with open(filepath, 'w') as f:
                        json.dump(book_data, f, indent=4)

                    insert_book_data(book_data)
                    books = get_book_data()

                    return render_template('index.html',
                                        lookup_successful=True,
                                        isbn=isbn,
                                        books=books)
                else:
                    error_message = 'Book not found with that ISBN.'
                    return render_template('index.html',
                                        error=error_message,
                                        isbn=isbn,
                                        books=books)

        return render_template('index.html',
                            lookup_successful=lookup_successful,
                            isbn=isbn,
                            books=books)
    except Exception as e:
        print("ERROR:", str(e))  # This will show in your console
        raise  # Re-raise the exception to see it in the browser

@app.route('/results/<path:filename>')
def download_file(filename):
    return send_from_directory(RESULTS_DIR, filename, as_attachment=False)

if __name__ == '__main__':
    app.run(debug=True)