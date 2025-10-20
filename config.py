import os

# Database configuration with environment variable 
# Uses environment variables when available, falls back to defaults for local development
DB_CONFIG = {
    'user': os.getenv('DB_USER', 'librarian'),
    'password': os.getenv('DB_PASSWORD', 'UPDATEWITHPASSWORD'),
    'host': os.getenv('DB_HOST', 'localhost'),  # Will be 'mysql' in Docker
    'database': os.getenv('DB_NAME', 'personal_library')
}

import time

def get_db_config_with_retry():
    """Returns DB config with retry logic for Docker container startup"""
    return DB_CONFIG