# Pre-Sale Technical Audit Report

Project: Code Exporter for AI  
Date: 2026-03-23  
Target: JetBrains Marketplace release readiness

## Executive Status

- Current status: **Ready for first public release after content polish**.
- Build status: `:app:buildPlugin` and `:app:verifyPluginConfiguration` pass.
- High-priority publication blockers found during audit: **resolved**.

## What Was Audited

- Marketplace metadata and packaging.
- Core service logic (`ProjectExportService`) for correctness and scalability.
- UI flow and background task behavior (`MyToolWindowFactory`).
- Compatibility and Gradle/plugin configuration.
- Basic legal and distribution readiness.

## Fixed During Audit

1. Metadata and publication blockers:
   - Replaced placeholder vendor values in plugin metadata.
   - Updated plugin id/name to production-like values.
   - Added plugin icons (`pluginIcon.svg`, `pluginIcon_dark.svg`).
   - Added `CHANGELOG.md`, `LICENSE`, and `NOTICE`.

2. Compatibility and build health:
   - Updated IntelliJ Platform Gradle plugin to `2.13.1`.
   - Updated Java target/source to `21` for IntelliJ 2024.3 compatibility.
   - Added `.intellijPlatform` to root `.gitignore`.

3. Code risk fixes:
   - Improved `.gitignore` matching to use **relative paths**, not file names only.
   - Replaced full-file byte reads during export with streamed IO to reduce memory spikes.
   - Fixed refresh behavior to rebuild and rebind tree model after updates.
   - Added null guard for select-all action.
   - Removed duplicate Kotlin `ExportAction` source file.

## Risk Register (Current)

### P0 (Must fix before release)

- None open after applied fixes.

### P1 (Should fix in next iteration)

- `plugin.xml` still includes explicit `projectService` registration for a light service.
  - Not critical, but should keep one source of truth.
- Some UI and comments are mixed-language (RU/EN), while Marketplace text is EN.
  - Prefer consistent English for global audience.
- No automated test suite yet (unit/integration/UI smoke tests).

### P2 (Can defer)

- Token/line estimation currently uses full string reads in UI stats update; for very large files this can be optimized with streaming counters.
- Consider adding plugin verification in CI pipeline (GitHub Actions or similar).

## Compatibility Results

- Verified against IntelliJ IDEA Community 2024.3 platform dependency.
- Build and verify tasks completed successfully.
- No linter issues found in changed files.

## Recommended Next Technical Actions

1. Add automated smoke tests for:
   - flat export,
   - structure-preserving export,
   - cancel behavior,
   - `.gitignore` on/off behavior,
   - non-UTF8 and large-file handling.
2. Add CI workflow that runs:
   - `:app:verifyPluginConfiguration`
   - `:app:buildPlugin`
3. Add user-facing privacy statement in README and Marketplace page:
   - plugin works offline and does not transmit code externally.

## Backlog (Prioritized)

- **P1**: Add automated tests and CI verification pipeline.
- **P1**: Standardize all user-facing text to one language (English).
- **P2**: Optimize file stats calculation for very large selected files.
