# Folimeld for Android

[日本語](README.md) | [English](README_en.md)

Folimeld is an Android application for visually rearranging, rotating, inserting, and deleting PDF pages. Ported from the desktop version and optimized for mobile, tablet, and Chrome OS environments, it processes files locally without sending them to external services.

## Features

- **Visual Editing**: Rearrange PDF pages using thumbnail previews.
- **Batch Operations**: Select, move, rotate, or delete multiple pages at once.
- **Page Insertion**: Insert another PDF or a blank page of the same size.
- **Advanced Settings**: Edit the PDF version, page layout, and binding direction.
- **Security**: Supports setting and removing opening passwords.
- **Device Optimization**: Optimized for smartphones, tablets, and Chromebooks (with keyboard shortcut support).
- **Privacy**: All processing is done locally on your device.

## Usage

1. Launch the app and select a PDF using the **Open** icon in the top right.
2. Tap pages to select them (supports multi-selection).
3. Use the bottom toolbar to move, rotate, insert, or delete pages.
4. Export the PDF using the **Save** icon in the top right.

### Keyboard Shortcuts (Chromebook / External Keyboard)

- `Ctrl + O`: Open
- `Ctrl + S`: Save
- `Delete` / `Backspace`: Delete
- `Ctrl + I`: Insert PDF
- `Ctrl + Shift + I`: Insert blank page
- `Alt + Up/Down`: Move pages up or down
- `Ctrl + Left/Right`: Rotate pages left or right

## Technical Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **DI**: Hilt
- **PDF Engine**: PdfBox-Android

## License

Folimeld is released under the [GNU Affero General Public License v3.0](LICENSE).

Third-party libraries (PdfBox-Android, Hilt, Jetpack Compose, etc.) are covered by their respective licenses (mostly Apache License 2.0).
Full license details are available in the app under **Version Info → Licenses**.
