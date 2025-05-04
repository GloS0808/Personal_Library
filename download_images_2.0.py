import os
import json
import requests

# Define folders
results_dir = r"C:\Users\semg6\PycharmProjects\Personal_Library\results"
img_dir = r"C:\Users\semg6\PycharmProjects\Personal_Library\img"

# Ensure the image directory exists
os.makedirs(img_dir, exist_ok=True)

# Track failed downloads
failed_downloads = []

# Loop over each .txt file
for filename in os.listdir(results_dir):
    if not filename.endswith(".txt"):
        continue

    file_path = os.path.join(results_dir, filename)
    try:
        with open(file_path, "r", encoding="utf-8") as file:
            data = json.load(file)

        # Create a safe filename based on the book title
        title = data.get("title", "untitled")
        title_safe = "".join(c for c in title if c.isalnum() or c in (" ", "_")).rstrip()
        image_name = f"{title_safe}.jpg"
        image_path = os.path.join(img_dir, image_name)

        # Skip download if image already exists
        if os.path.exists(image_path):
            print(f"⏭️ Skipping (already exists): {image_name}")
            continue

        # Extract image URL
        image_url = None
        if "imageLinks" in data:
            image_url = data["imageLinks"].get("thumbnail") or data["imageLinks"].get("smallThumbnail")

        if image_url:
            try:
                response = requests.get(image_url, timeout=10)
                response.raise_for_status()
                with open(image_path, "wb") as img_file:
                    img_file.write(response.content)
                print(f"✅ Downloaded: {image_name}")
            except Exception as e:
                print(f"❌ Failed to download image from {image_url}: {e}")
                failed_downloads.append((filename, image_url))
        else:
            print(f"⚠️ No image URL in: {filename}")
            failed_downloads.append((filename, None))

    except Exception as e:
        print(f"❌ Error processing {filename}: {e}")
        failed_downloads.append((filename, None))

# Summary
print("\n=== FAILED DOWNLOADS ===")
for entry in failed_downloads:
    print(entry)
