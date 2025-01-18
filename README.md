# Image Palette Generator

This simple Android application allows you to generate a random color palette from an image.  Upload an image from your gallery or take a photo using your camera, and the app will extract five random colors to create a unique palette.

## Features

* **Image Upload:** Select an image from your device's gallery.
* **Camera Integration:** Capture a photo directly within the app.
* **Random Palette Generation:** Extracts five random colors from the chosen image.
* **Palette History:** The last five palettes generated from an image are saved and can be loaded back into the app.
* **Palette Favorites:** Easily favorite palettes and reload them on demand.
* **Color Code Display:** Presents the generated palette with hex color codes.
* **Copy to Clipboard:** Copy individual color codes to your clipboard with a single tap.
* **Palette Regeneration:** Generate a new palette from the same image with a click of a button.

## Usage

1. **Choose an Image:** Select "Upload Image" to browse your gallery or "Open Camera" to take a new photo.
2. **Generate Palette:** After selecting an image, the app will automatically generate a five-color palette.
3. **Copy Color Codes:** Tap on any color swatch to copy its hex code to the clipboard.  A toast message will confirm the copy action.
4. **Regenerate Palette:** If you're not satisfied with the current palette, press the "Generate New Palette" button to create a new one.

## Dependencies

The project utilizes several libraries, including:

* **Jetpack Compose:** For building the UI.
* **CameraX:** For camera functionality.
* **Coil:** For image loading and display.
* **AndroidX Activity:** For activity result handling.

## Future Enhancement Ideas

* **Customizable Palette Size:** Allow users to specify the number of colors in the generated palette.
* **Palette Saving:** Enable users to save and load their generated palettes.
* **Improved Color Picking Algorithm:** Explore more sophisticated algorithms for extracting representative colors from an image.
