from flask import Flask, render_template, request, send_from_directory, redirect, url_for

import requests

import json

import os

import mysql.connector

from config import DB_CONFIG

from datetime import datetime

import shutil


app = Flask(__name__)

from prometheus_flask_exporter import PrometheusMetrics

# Initialize Prometheus metrics

metrics = PrometheusMetrics(app)

# add customer metrics

metrics.info('app_info', 'Application info', version='1.0.0', server=os.uname().nodename)

app.config['PROPAGATE_EXCEPTIONS'] = True


RESULTS_DIR = "results"


if not os.path.exists(RESULTS_DIR):

    os.makedirs(RESULTS_DIR)



def get_book_data():

    try:

        conn = mysql.connector.connect(**DB_CONFIG)

        cursor = conn.cursor(dictionary=True)


        # First get all books with their basic info

        cursor.execute("""

                       SELECT b.book_id,

                              b.title,

                              b.subtitle,

                              b.publisher,

                              b.published_date,

                              b.description,

                              b.page_count,

                              b.average_rating,

                              b.thumbnail,

                              c.category_name

                       FROM books b

                                LEFT JOIN categories c ON b.category_id = c.category_id

                       """)

        books = cursor.fetchall()


        # Now enrich each book with additional data

        for book in books:

            # Get authors

            cursor.execute("""

                           SELECT GROUP_CONCAT(a.name SEPARATOR ', ') AS authors

                           FROM book_authors ba

                                    JOIN authors a ON ba.author_id = a.author_id

                           WHERE ba.book_id = %s

                           """, (book['book_id'],))

            authors = cursor.fetchone()

            book['authors'] = authors['authors'] if authors and authors['authors'] else 'N/A'


            # Get user book info (owner, status, etc)

            cursor.execute("""

                           SELECT ub.*, u.name as owner_name

                           FROM user_books ub

                                    JOIN users u ON ub.user_id = u.user_id

                           WHERE ub.book_id = %s LIMIT 1

                           """, (book['book_id'],))

            user_book = cursor.fetchone()


            if user_book:

                book.update({

                    'status': user_book['status'],

                    'user_rating': user_book['user_rating'],

                    'current_page': user_book['current_page'],

                    'notes': user_book['notes'],

                    'started_date': user_book['started_date'],

                    'read_date': user_book['read_date'],

                    'user_id': user_book['user_id'],

                    'owner_name': user_book['owner_name']

                })

            else:

                # Set default values if no user_book entry exists

                book.update({

                    'status': None,

                    'user_rating': None,

                    'current_page': None,

                    'notes': None,

                    'started_date': None,

                    'read_date': None,

                    'user_id': None,

                    'owner_name': None

                })


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

        conn = mysql.connector.connect(**DB_CONFIG)

        cursor = conn.cursor(dictionary=True)


        # Debug: Check if we can connect to the database

        cursor.execute("SELECT COUNT(*) as count FROM books")

        book_count = cursor.fetchone()['count']

        print(f"DEBUG: Found {book_count} books in database")


        cursor.execute("SELECT * FROM users")

        users = cursor.fetchall()

        print(f"DEBUG: Found {len(users)} users")


        books = get_book_data()

        print(f"DEBUG: Retrieved {len(books)} books after processing")

        

        # Rest of your index route...

        lookup_successful = False

        isbn = None

        if request.method == 'POST':

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


                    insert_book_data(book_data)

                    books = get_book_data()

                    return render_template('index.html',

                                           lookup_successful=True,

                                           isbn=isbn,

                                           books=books,

                                           users=users)

                else:

                    error_message = 'Book not found with that ISBN.'

                    return render_template('index.html',

                                           error=error_message,

                                           isbn=isbn,

                                           books=books,

                                           users=users)


        return render_template('index.html',

                               lookup_successful=lookup_successful,

                               isbn=isbn,

                               books=books,

                               users=users)

    except Exception as e:

        print("ERROR:", str(e))

        raise

    finally:

        if conn:

            cursor.close()

            conn.close()



def insert_book_data(book_data):

    try:

        conn = mysql.connector.connect(**DB_CONFIG)

        cursor = conn.cursor()


        # Handle Category

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


        # Handle Book

        cursor.execute("SELECT book_id FROM books WHERE isbn_13 = %s OR isbn_10 = %s",

                       (book_data.get('industryIdentifiers', [{}, {}])[0].get('identifier'),

                        book_data.get('industryIdentifiers', [{}, {}])[1].get('identifier')))

        existing_book = cursor.fetchone()

        if not existing_book:

            cursor.execute(

                """

                INSERT INTO books (isbn_13, isbn_10, title, subtitle, publisher, published_date,

                                   description, page_count, average_rating, thumbnail, category_id)

                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)

                """,

                (

                    book_data.get('industryIdentifiers', [{}, {}])[0].get('identifier'),

                    book_data.get('industryIdentifiers', [{}, {}])[1].get('identifier'),

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


        # Handle Authors

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


                # Insert into book_authors

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



@app.route('/results/<path:filename>')

def download_file(filename):

    return send_from_directory(RESULTS_DIR, filename, as_attachment=False)



@app.route('/update_book', methods=['GET', 'POST'])

def update_book():

    try:

        conn = mysql.connector.connect(**DB_CONFIG)

        cursor = conn.cursor(dictionary=True)


        book_id = request.args.get('book_id') or request.form.get('book_id')

        user_id = request.form.get('owner')


        if request.method == 'POST':

            status = request.form.get('status')

            rating = request.form.get('rating') or None

            current_page = request.form.get('current_page') or None

            notes = request.form.get('notes')

            started_date = request.form.get('started_date') or None

            read_date = request.form.get('read_date') or None


            query = """

                    INSERT INTO user_books (user_id, book_id, status, user_rating, current_page, notes, started_date, \

                                            read_date)

                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s) ON DUPLICATE KEY \

                    UPDATE \

                        status = \

                    VALUES (status), user_rating = \

                    VALUES (user_rating), current_page = \

                    VALUES (current_page), notes = \

                    VALUES (notes), started_date = \

                    VALUES (started_date), read_date = \

                    VALUES (read_date) \

                    """

            cursor.execute(query, (user_id, book_id, status, rating, current_page, notes, started_date, read_date))

            conn.commit()

            return redirect(url_for('index'))


        # GET: Load current values

        cursor.execute("SELECT * FROM users")

        users = cursor.fetchall()


        cursor.execute("""

                       SELECT b.*, ub.*, c.category_name, u.name as owner_name

                       FROM books b

                                LEFT JOIN user_books ub ON ub.book_id = b.book_id

                                LEFT JOIN categories c ON c.category_id = b.category_id

                                LEFT JOIN users u ON ub.user_id = u.user_id

                       WHERE b.book_id = %s

                       """, (book_id,))

        book = cursor.fetchone()


        return render_template('update_book.html', book=book, users=users)

    except Exception as e:

        print(f"Error: {e}")

        return "An error occurred", 500

    finally:

        if 'cursor' in locals():

            cursor.close()

        if 'conn' in locals():

            conn.close()



@app.route('/server-info')

def server_info():

    """Return which server is handling the request"""

    return {

        'server': os.uname().nodename,

        'ip': request.host,

        'timestamp': datetime.now().isoformat()

    }



@app.route('/health')

def health_check():

    """Health check endpoint for load balancer and monitoring"""

    health_status = {

        'status': 'healthy',

        'timestamp': datetime.now().isoformat(),

        'server': os.uname().nodename,

        'checks': {}

    }

    

    overall_healthy = True

    

    # Check database connection

    try:

        conn = mysql.connector.connect(**DB_CONFIG)

        cursor = conn.cursor()

        cursor.execute("SELECT 1")

        cursor.fetchone()

        cursor.close()

        conn.close()

        

        health_status['checks']['database'] = {

            'status': 'healthy',

            'message': 'Database connection successful'

        }

    except Exception as e:

        overall_healthy = False

        health_status['checks']['database'] = {

            'status': 'unhealthy',

            'message': f'Database connection failed: {str(e)}'

        }

    

    # Check disk space

    try:

        disk = shutil.disk_usage('/')

        disk_percent = (disk.used / disk.total) * 100

        

        if disk_percent > 90:

            overall_healthy = False

            disk_status = 'critical'

        elif disk_percent > 80:

            disk_status = 'warning'

        else:

            disk_status = 'healthy'

        

        health_status['checks']['disk'] = {

            'status': disk_status,

            'total_gb': round(disk.total / (1024**3), 2),

            'used_gb': round(disk.used / (1024**3), 2),

            'free_gb': round(disk.free / (1024**3), 2),

            'percent_used': round(disk_percent, 2)

        }

    except Exception as e:

        health_status['checks']['disk'] = {

            'status': 'unknown',

            'message': f'Disk check failed: {str(e)}'

        }

    

    # Check results directory

    try:

        if os.path.exists(RESULTS_DIR) and os.access(RESULTS_DIR, os.W_OK):

            health_status['checks']['storage'] = {

                'status': 'healthy',

                'message': 'Results directory accessible'

            }

        else:

            overall_healthy = False

            health_status['checks']['storage'] = {

                'status': 'unhealthy',

                'message': 'Results directory not accessible'

            }

    except Exception as e:

        overall_healthy = False

        health_status['checks']['storage'] = {

            'status': 'unhealthy',

            'message': f'Storage check failed: {str(e)}'

        }

    

    # Set overall status

    health_status['status'] = 'healthy' if overall_healthy else 'unhealthy'

    

    # Return appropriate HTTP status code

    status_code = 200 if overall_healthy else 503

    

    return health_status, status_code



@app.route('/status')

def status_page():

    """Detailed system status dashboard"""

    status = {

        'server': os.uname().nodename,

        'timestamp': datetime.now().isoformat(),

        'uptime': None,

        'services': {},

        'system': {}

    }

    

    # Check database with additional info

    try:

        conn = mysql.connector.connect(**DB_CONFIG)

        cursor = conn.cursor(dictionary=True)

        

        # Get book count

        cursor.execute("SELECT COUNT(*) as count FROM books")

        book_count = cursor.fetchone()['count']

        

        # Get user count

        cursor.execute("SELECT COUNT(*) as count FROM users")

        user_count = cursor.fetchone()['count']

        

        cursor.close()

        conn.close()

        

        status['services']['database'] = {

            'status': 'healthy',

            'books': book_count,

            'users': user_count,

            'host': DB_CONFIG.get('host', 'localhost')

        }

    except Exception as e:

        status['services']['database'] = {

            'status': 'unhealthy',

            'error': str(e)

        }

    

    # System information

    try:

        disk = shutil.disk_usage('/')

        status['system']['disk'] = {

            'total_gb': round(disk.total / (1024**3), 2),

            'used_gb': round(disk.used / (1024**3), 2),

            'free_gb': round(disk.free / (1024**3), 2),

            'percent_used': round((disk.used / disk.total) * 100, 2)

        }

    except Exception as e:

        status['system']['disk'] = {'error': str(e)}

    

    # Python version

    import sys

    status['system']['python_version'] = sys.version

    

    # Application info

    status['application'] = {

        'name': 'Personal Library',

        'version': '1.0.0',

        'flask_version': Flask.__version__

    }

    

    return status, 200



if __name__ == '__main__':

    app.run(host='0.0.0.0', port=5000, debug=True)
