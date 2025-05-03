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

        # Check if book already exists before proceeding
        isbn_13 = book_data.get('industryIdentifiers', [{}, {}])[0].get('identifier')
        isbn_10 = book_data.get('industryIdentifiers', [{}, {}])[1].get('identifier')

        cursor.execute("SELECT book_id FROM books WHERE isbn_13 = %s OR isbn_10 = %s",
                       (isbn_13, isbn_10))
        existing_book = cursor.fetchone()

        if existing_book:
            print("Book already exists in the database")
            return False, "This book is already in your library"

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
                try:
                    cursor.execute(
                        "INSERT INTO book_authors (book_id, author_id) VALUES (%s, %s)",
                        (book_id, author_id)
                    )
                    conn.commit()
                except mysql.connector.Error as err:
                    if err.errno != 1062:  # Ignore duplicate author-book relationships
                        raise

        print("Book data inserted successfully!")
        return True, "Book added successfully"

    except mysql.connector.Error as err:
        if err.errno == 1062:  # Duplicate entry error code
            print("Duplicate entry prevented")
            return False, "This book is already in your library"
        else:
            print(f"Error inserting data: {err}")
            return False, f"Database error: {err}"
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


# Add to app.py

def get_users():
    """Get list of all users"""
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor(dictionary=True)
        cursor.execute("SELECT * FROM users ORDER BY name")
        return cursor.fetchall()
    except mysql.connector.Error as err:
        print(f"Error fetching users: {err}")
        return []
    finally:
        if conn:
            cursor.close()
            conn.close()


def get_user_books(user_id=None):
    """Get books with user-specific tracking info"""
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor(dictionary=True)

        query = """
                SELECT b.*, \
                       c.category_name, \
                       GROUP_CONCAT(a.name) AS authors, \
                       ub.status, \
                       ub.read_date, \
                       ub.user_rating, \
                       ub.current_page, \
                       ub.notes, \
                       ub.started_date, \
                       ub.added_on
                FROM books b
                         LEFT JOIN categories c ON b.category_id = c.category_id
                         LEFT JOIN book_authors ba ON b.book_id = ba.book_id
                         LEFT JOIN authors a ON ba.author_id = a.author_id
                         LEFT JOIN user_books ub ON b.book_id = ub.book_id AND ub.user_id = %s
                GROUP BY b.book_id \
                """
        cursor.execute(query, (user_id,))
        return cursor.fetchall()
    except mysql.connector.Error as err:
        print(f"Error fetching user books: {err}")
        return []
    finally:
        if conn:
            cursor.close()
            conn.close()


def update_user_book(user_id, book_id, status=None, current_page=None,
                     rating=None, notes=None, read_date=None, started_date=None):
    """Update or create user-book relationship"""
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor()

        # Check if relationship exists
        cursor.execute("""
                       SELECT user_book_id
                       FROM user_books
                       WHERE user_id = %s
                         AND book_id = %s
                       """, (user_id, book_id))
        exists = cursor.fetchone()

        if exists:
            # Update existing
            update_fields = []
            params = []

            if status:
                update_fields.append("status = %s")
                params.append(status)
            if current_page is not None:
                update_fields.append("current_page = %s")
                params.append(current_page)
            if rating is not None:
                update_fields.append("user_rating = %s")
                params.append(rating)
            if notes is not None:
                update_fields.append("notes = %s")
                params.append(notes)
            if read_date:
                update_fields.append("read_date = %s")
                params.append(read_date)
            if started_date:
                update_fields.append("started_date = %s")
                params.append(started_date)

            if update_fields:
                query = f"""
                    UPDATE user_books 
                    SET {', '.join(update_fields)}
                    WHERE user_id = %s AND book_id = %s
                """
                params.extend([user_id, book_id])
                cursor.execute(query, tuple(params))
        else:
            # Create new
            cursor.execute("""
                           INSERT INTO user_books
                           (user_id, book_id, status, current_page, user_rating,
                            notes, read_date, started_date)
                           VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                           """, (
                               user_id, book_id, status or 'owned',
                               current_page, rating, notes, read_date, started_date
                           ))

        conn.commit()
        return True
    except mysql.connector.Error as err:
        print(f"Error updating user book: {err}")
        return False
    finally:
        if conn:
            cursor.close()
            conn.close()


@app.route('/', methods=['GET', 'POST'])
def index():
    try:
        lookup_successful = False
        isbn = None
        message = None
        message_type = None
        users = get_users()

        # Get selected user from session or default to first user
        selected_user_id = request.args.get('user_id') or request.form.get('user_id')
        if not selected_user_id and users:
            selected_user_id = users[0]['user_id']

        if request.method == 'POST':
            # Handle ISBN lookup
            if 'isbn' in request.form:
                isbn = request.form['isbn'].strip()
                if isbn:
                    api_url = f"https://www.googleapis.com/books/v1/volumes?q=isbn:{isbn}"
                    response = requests.get(api_url)
                    response.raise_for_status()
                    data = response.json()

                    if 'items' in data and data['items']:
                        book_data = data['items'][0]['volumeInfo']
                        filepath = os.path.join(RESULTS_DIR, f"{isbn}.txt")
                        with open(filepath, 'w') as f:
                            json.dump(book_data, f, indent=4)

                        success, msg = insert_book_data(book_data)
                        if success:
                            lookup_successful = True
                            message_type = 'success'
                        else:
                            message_type = 'error'
                        message = msg
                    else:
                        message = 'Book not found with that ISBN.'
                        message_type = 'error'

            # Handle book status update
            elif 'update_book' in request.form:
                book_id = request.form['book_id']
                status = request.form.get('status')
                current_page = request.form.get('current_page')
                rating = request.form.get('rating')
                notes = request.form.get('notes')
                read_date = request.form.get('read_date')
                started_date = request.form.get('started_date')

                if current_page:
                    try:
                        current_page = int(current_page)
                    except ValueError:
                        current_page = None

                if rating:
                    try:
                        rating = int(rating)
                    except ValueError:
                        rating = None

                success = update_user_book(
                    selected_user_id, book_id, status, current_page,
                    rating, notes, read_date, started_date
                )

                if success:
                    message = "Book status updated successfully!"
                    message_type = 'success'
                else:
                    message = "Failed to update book status"
                    message_type = 'error'

        books = get_user_books(selected_user_id) if selected_user_id else []

        return render_template('index.html',
                               lookup_successful=lookup_successful,
                               isbn=isbn,
                               books=books,
                               users=users,
                               selected_user_id=selected_user_id,
                               message=message,
                               message_type=message_type)
    except Exception as e:
        print("ERROR:", str(e))
        return render_template('index.html',
                               books=[],
                               message=str(e),
                               message_type='error')

@app.route('/results/<path:filename>')
def download_file(filename):
    return send_from_directory(RESULTS_DIR, filename, as_attachment=False)

if __name__ == '__main__':
    app.run(debug=True)