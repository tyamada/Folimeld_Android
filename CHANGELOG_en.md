# Changelog

## [1.0.0] - 2026-09-09

### Added
- Initial release of Folimeld for Android, ported from the macOS version.
- Visual PDF page manipulation (reorder, rotate, insert, and delete).
- Thumbnail grid view with adjustable sizes (Small, Medium, Large).
- PDF metadata editing (Title, Author, etc.).
- Enhanced PDF details support: edit and persist page layout and reading direction.
- Support for viewing and creating password-protected PDFs.
- Desktop/Chrome OS optimization: full keyboard shortcut support.
- Localization support (Japanese, English) with persistent settings via DataStore.
- Adaptive app icons and supporter-only maid icon.

### Changed
- Refactored package name to `com.tyamada.folimeld` for Play Store compliance.
- Improved `Properties` screen UI with dropdown menus for layout and language selection.

### Fixed
- Resolved a startup crash caused by early access to the settings repository before Hilt injection.
- Fixed an issue where certain PDF page layouts were not correctly identified due to case sensitivity.

### Testing
- Added comprehensive Instrumented tests covering 16 PDF variants (L2R/R2L, various layouts, and cover page settings).
