# Code Exporter for AI

A universal plugin for exporting code files from any project directory to text format for AI analysis.

## Features

- **Universal Directory Selection**: Works with any folder on your disk, not just Android Studio projects
- **Manual Project Selection**: Choose your project directory using the browse button
- **Flexible Output**: Select where to save exported files
- **Smart Filtering**: Filter files by type (All Files, Source Files, Java/Kotlin, XML)
- **.gitignore Support**: Optionally respect .gitignore rules when scanning files
- **Visual File Tree**: Browse and select files using an intuitive tree view
- **Batch Operations**: Select All, Clear All, Expand All, Collapse All
- **Live Statistics**: See file count, size, lines, and estimated tokens
- **Customizable Export**: Export with or without folder structure
- **Post-Export Actions**: Automatically open the output folder

## Usage

1. **Select Project Directory**
   - Click the "Browse..." button next to "Project Folder"
   - Navigate to and select the root directory of your project
   - The file tree will automatically populate with the directory structure

2. **Select Output Directory**
   - Click the "Browse..." button next to "Output Folder"
   - Choose where you want to save the exported files

3. **Filter Files**
   - Use the dropdown to filter files by type:
     - All Files: Include everything
     - Source Files: .java, .kt, .xml files only
     - Java/Kotlin Only: .java and .kt files
     - XML Only: .xml files only
   - Check "Respect .gitignore" to exclude files listed in .gitignore

4. **Select Files**
   - Check individual files or directories in the tree
   - Use "Select All" to select everything
   - Use "Clear All" to deselect everything
   - Use "Expand All" and "Collapse All" to navigate the tree
   - Click "Refresh" to reload the directory structure

5. **Export Files**
   - Click "Export" to start the export process
   - Check "Without folders" to export all files to a single directory
   - Check "Open folder after export" to automatically open the output directory
   - Choose a huge-file policy: skip, partial (first 1 MB), or full export
   - View progress and results in the dialog

## Build Requirements

- **Java Development Kit (JDK)**: JDK 21 or later is required for building the plugin.
- **IntelliJ Platform Gradle Plugin**: Version 2.14.0 or later.
- **Target IntelliJ IDEA**: Community Edition 2024.3 (sinceBuild = "243").

## Building and Testing

### Build Commands

```bash
# Build plugin
./gradlew :app:buildPlugin

# Run unit tests (skip instrumentation due to JDK path issues)
./gradlew :app:test --no-daemon -x instrumentCode -x instrumentTestCode

# Check code formatting
./gradlew :app:spotlessCheck

# Apply formatting fixes
./gradlew :app:spotlessApply

# Verify plugin configuration
./gradlew :app:verifyPluginConfiguration
```

### CI/CD

The project includes GitHub Actions CI workflow (`.github/workflows/ci.yml`) that runs on every push/pull request:

1. **Setup**: Java 21, Gradle
2. **Tests**: Runs unit tests with instrumentation disabled
3. **Formatting**: Checks code formatting with Spotless
4. **Verification**: Verifies plugin configuration
5. **Build**: Builds the plugin artifact

### Known Issues

- **Instrumentation tasks (`instrumentCode`, `instrumentTestCode`)**: Disabled in CI due to JDK path issues (`/usr/local/sdkman/candidates/java/21.0.10-ms/Packages does not exist`). This is a known issue with the IntelliJ Platform Gradle Plugin and certain JDK installations. The plugin still builds and works correctly without instrumentation.

## Technical Architecture

The plugin has been refactored to work independently of Android Studio's Project object, making it universally compatible with any folder structure.

### Key Components

- **FileSelectorDialog**: Main UI component for directory selection and file browsing
- **ProjectExportService**: Core service handling file scanning, filtering, and export operations
- **MyToolWindowFactory**: Integration point with IntelliJ Platform UI

### File Scanning

The plugin uses standard Java File API to scan directories recursively, applying filters based on file extensions and .gitignore rules. Maximum tree depth is limited to prevent performance issues with deeply nested structures.

### Export Process

Files are exported as .txt files with their original content. When "Without folders" is selected, files with duplicate names are numbered sequentially (e.g., Main.java.txt, Main_1.java.txt).

## Benefits

- **IDE Independence**: No longer tied to Android Studio - works with any project
- **Universal Compatibility**: Analyze projects from any source control system or development environment
- **Simplified Workflow**: Direct folder selection eliminates project import requirements
- **Enhanced Flexibility**: Choose exactly which files to export from any directory

Perfect for preparing codebases for AI analysis, code reviews, documentation generation, or sharing with team members.

## Use Cases and Example Output

### Use cases

- **AI code review context**: Export only `src`, `build.gradle`, and config files.
- **Bugfix handoff**: Export files touched by a feature/fix plus related configs.
- **Architecture explanation**: Export key modules and README/docs into one bundle.

### Example output (structured mode)

```text
output/
  app/src/main/java/com/example/plugfiletotxt/service/ProjectExportService.java.txt
  app/src/main/java/com/example/plugfiletotxt/window/MyToolWindowFactory.java.txt
  README.md.txt
  build.gradle.kts.txt
```

### Example output (flat mode)

```text
output/
  ProjectExportService.java.txt
  MyToolWindowFactory.java.txt
  README.md.txt
  build.gradle.kts.txt
```

## Privacy

Code Exporter for AI works locally on your machine.
By default, the plugin does not send source code to external services.
You choose source and output folders manually, and exported files are stored locally.

## Issue Tracker

- Report bugs and request features: https://github.com/gatilovr/plugfiletotxt/issues

## Support and Donations

- Support / links hub: https://dalink.to/gatilovrv
- Donate on Boosty: https://boosty.to/gatilovrv/donate

## License

This project is licensed under Apache-2.0. See `LICENSE`.