# JetBrains Marketplace Go-Live Checklist

## Metadata and Listing

- [ ] Confirm final plugin name and id are stable.
- [x] Finalize short/long description (user value, key features, offline behavior).
- [ ] Add final screenshots/gif of main flow (select -> filter -> export).
- [ ] Add icon assets (light/dark) and validate rendering.
- [x] Add tags/categories relevant to code tooling and productivity.
- [ ] Ensure changelog is up to date for the release version.

## Build and Verification

- [x] Run `:app:verifyPluginConfiguration`.
- [x] Run `:app:buildPlugin`.
- [ ] Run final smoke test in sandbox IDE.
- [ ] Validate behavior on a large repository and on a small sample project.

## Quality and UX

- [ ] Validate cancel behavior during scan/export.
- [ ] Validate output naming collisions in flat mode.
- [ ] Validate `.gitignore` behavior with typical wildcard patterns.
- [ ] Validate non-ASCII path handling (Windows/macOS/Linux).

## Legal and Support

- [x] Add `LICENSE`.
- [x] Add `NOTICE`.
- [x] Add privacy statement to listing/README.
- [x] Add support and donation links.
- [x] Add issue tracker link.

## Release Execution

- [ ] Bump version if needed.
- [ ] Rebuild plugin artifact.
- [ ] Upload package to Marketplace.
- [ ] Fill listing content and publish.
- [ ] Monitor first crash/error feedback and rating comments.
