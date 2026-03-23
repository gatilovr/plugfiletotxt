# Changelog

All notable changes to this project are documented in this file.

## [1.0.1] - 2026-03-24

- Replaced deprecated `CheckboxTree` constructor usage for Marketplace compatibility.
- Improved scalability and memory behavior for large repositories:
  - huge-file handling modes (skip/partial/full),
  - streaming line/token counting,
  - parallelized file filtering during scan with bounded threads,
  - more reliable cancellation during scan and export.
- Added GitHub Actions CI workflow for plugin verification and build.
- Updated README with practical use cases and export output examples.

## [1.0.0] - 2026-03-23

- Initial public release.
- Export selected files to `.txt` from any chosen folder.
- Added file filters, optional `.gitignore` support, and project statistics.
- Added flat and folder-preserving export modes.
