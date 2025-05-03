-- database user stuffs
DROP USER IF EXISTS 'librarian_user'@'localhost';
CREATE USER 'librarian_user'@'localhost' IDENTIFIED WITH mysql_native_password BY 'readingtillwedie';

-- Drop and create database
DROP DATABASE IF EXISTS personal_library;
CREATE DATABASE personal_library;
GRANT ALL PRIVILEGES ON personal_library.* TO 'librarian_user'@'localhost';

-- Use the database
USE personal_library;

-- Drop existing tables if they exist
DROP TABLE IF EXISTS user_books;
DROP TABLE IF EXISTS books;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS authors;
DROP TABLE IF EXISTS book_authors;
DROP TABLE IF EXISTS users;

-- Create users table
CREATE TABLE users (
  user_id INT NOT NULL AUTO_INCREMENT,
  name VARCHAR(70) NOT NULL,
  PRIMARY KEY (user_id)
);

-- Insert initial users
INSERT INTO users (name) VALUES ('Melissa'), ('Seth'), ('Dillon');

-- Create categories table
CREATE TABLE categories (
  category_id INT NOT NULL AUTO_INCREMENT,
  category_name VARCHAR(70) NOT NULL,
  PRIMARY KEY (category_id)
);

-- Create authors table (new addition)
CREATE TABLE authors (
  author_id INT NOT NULL AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL UNIQUE,
  PRIMARY KEY (author_id)
);

-- Create books table
CREATE TABLE books (
  book_id INT NOT NULL AUTO_INCREMENT,
  isbn_13 VARCHAR(26),
  isbn_10 VARCHAR(20),
  title VARCHAR(255) NOT NULL,
  subtitle VARCHAR(255),
  publisher VARCHAR(100),
  published_date VARCHAR(20),
  description TEXT,
  page_count INT,
  average_rating FLOAT,
  thumbnail TEXT,
  category_id INT,
  PRIMARY KEY (book_id),
  FOREIGN KEY (category_id) REFERENCES categories(category_id)
);

-- Create book_authors table (many-to-many relationship between books and authors)
CREATE TABLE book_authors (
  book_id INT NOT NULL,
  author_id INT NOT NULL,
  PRIMARY KEY (book_id, author_id),
  FOREIGN KEY (book_id) REFERENCES books(book_id),
  FOREIGN KEY (author_id) REFERENCES authors(author_id)
);

-- Create user_books table (many-to-many relationship between users and books)
CREATE TABLE user_books (
  user_book_id INT NOT NULL AUTO_INCREMENT,
  user_id INT NOT NULL,
  book_id INT NOT NULL,
  status ENUM('owned', 'reading', 'read', 'wishlist') DEFAULT 'owned',
  added_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  read_date DATE NULL, -- When the book was finished
  user_rating TINYINT NULL, -- User's personal rating (1-5 stars)
  current_page INT NULL, -- For tracking reading progress
  notes TEXT NULL, -- Personal notes about the book
  started_date DATE,
  PRIMARY KEY (user_book_id),
  FOREIGN KEY (user_id) REFERENCES users(user_id),
  FOREIGN KEY (book_id) REFERENCES books(book_id),
  UNIQUE KEY (user_id, book_id) -- Prevent duplicate entries
);
);