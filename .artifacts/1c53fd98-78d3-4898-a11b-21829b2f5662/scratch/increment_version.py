import re
import os

def increment_version(file_path):
    if not os.path.exists(file_path):
        print(f"File not found: {file_path}")
        return

    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Increment versionCode = 1016
    def vc_replace(match):
        old_vc = int(match.group(1))
        new_vc = old_vc + 1
        print(f"Incrementing versionCode: {old_vc} -> {new_vc}")
        return f"versionCode = {new_vc}"

    new_content = re.sub(r'versionCode\s*=\s*(\d+)', vc_replace, content)

    # Increment versionName = "1.016 Personal Library 2026"
    # Matches version numbers like 1.016, 1.2, etc.
    def vn_replace(match):
        full_match = match.group(0)
        version_str = match.group(1)
        suffix = match.group(2)
        
        # Split version into components
        parts = version_str.split('.')
        if not parts:
            return full_match
            
        # Increment the last component
        try:
            last_part = parts[-1]
            length = len(last_part)
            new_val = int(last_part) + 1
            parts[-1] = str(new_val).zfill(length)
            new_version_str = ".".join(parts)
            print(f"Incrementing versionName: {version_str}{suffix} -> {new_version_str}{suffix}")
            return f'versionName = "{new_version_str}{suffix}"'
        except ValueError:
            return full_match

    new_content = re.sub(r'versionName\s*=\s*"([\d.]+)(.*?)"', vn_replace, new_content)

    if new_content != content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Successfully updated {file_path}")
    else:
        print(f"No version changes made to {file_path}")

if __name__ == "__main__":
    # Path relative to project root
    target_file = "app/build.gradle.kts"
    increment_version(target_file)
