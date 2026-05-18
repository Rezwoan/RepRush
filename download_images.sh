#!/usr/bin/env bash
# Run this script ONCE before building the app to bundle exercise images into assets.
# Prerequisites: git must be installed and available on PATH.
# Run from the project root: bash download_images.sh

set -e

TEMP_DIR="$(mktemp -d)"
REPO_DIR="$TEMP_DIR/free-exercise-db"
ASSET_DIR="app/src/main/assets/exercise_images"

echo "Cloning free-exercise-db (shallow clone)..."
git clone --depth=1 https://github.com/yuhonas/free-exercise-db.git "$REPO_DIR"

echo "Creating asset directory..."
mkdir -p "$ASSET_DIR"

echo "Copying exercise image folders..."
# Images live at exercises/<ExerciseName>/<number>.jpg in the repo.
# We copy each named subfolder to app/src/main/assets/exercise_images/<ExerciseName>/.
for dir in "$REPO_DIR/exercises"/*/; do
    folder_name="$(basename "$dir")"
    cp -r "$dir" "$ASSET_DIR/$folder_name"
done

echo "Cleaning up temporary files..."
rm -rf "$TEMP_DIR"

echo ""
echo "Done. Exercise images are now bundled at: $ASSET_DIR"
echo "Build the app with ./gradlew assembleDebug"
