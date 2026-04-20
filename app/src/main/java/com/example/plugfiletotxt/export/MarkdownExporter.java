package com.example.plugfiletotxt.export;

import com.intellij.openapi.diagnostic.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jetbrains.annotations.NotNull;

/**
 * Markdown exporter with code blocks and organization by file type. Exports selected files as a
 * readable Markdown document with code blocks, metadata, and organized structure.
 */
public class MarkdownExporter {

  private static final Logger LOG = Logger.getInstance(MarkdownExporter.class);

  public void exportToFile(
      @NotNull List<File> files, @NotNull File outputFile, @NotNull String projectName)
      throws IOException {

    StringBuilder md = new StringBuilder();

    // Header
    md.append(String.format("# Code Export: %s\n\n", projectName));
    md.append(String.format("**Export Date:** %s\n\n", getCurrentDate()));
    md.append(String.format("**Total Files:** %d\n\n", files.size()));

    // Table of contents
    md.append("## 📋 Table of Contents\n\n");

    // Group files by type
    Map<String, List<File>> filesByType = groupByType(files);
    for (String type : filesByType.keySet()) {
      List<File> typeFiles = filesByType.get(type);
      md.append(String.format("- **%s** (%d files)\n", formatType(type), typeFiles.size()));
    }
    md.append("\n");

    // Export files grouped by type
    for (String type : filesByType.keySet()) {
      List<File> typeFiles = filesByType.get(type);
      md.append(String.format("## 📂 %s Files (%d)\n\n", formatType(type), typeFiles.size()));

      for (File file : typeFiles) {
        md.append(String.format("### %s\n\n", file.getName()));
        md.append(String.format("**Size:** %s | **Path:** `%s`\n\n", formatSize(file.length()), 
            file.getPath()));

        try {
          String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
          long lines = content.split("\n", -1).length;
          md.append(String.format("**Lines:** %d\n\n", lines));

          // Add code block
          String lang = getLanguageForHighlight(type);
          md.append(String.format("```%s\n", lang));

          // Truncate very long files
          if (content.length() > 50_000) {
            md.append(content, 0, 50_000);
            md.append("\n\n... *(file truncated, view full in editor)* ...\n");
          } else {
            md.append(content);
          }

          md.append("\n```\n\n");
          md.append("---\n\n");
        } catch (IOException e) {
          LOG.warn("Error reading file: " + file, e);
          md.append("*[Error reading file]*\n\n");
        }
      }
    }

    // Footer
    md.append("## 📊 Summary\n\n");
    long totalSize = files.stream().mapToLong(File::length).sum();
    md.append(
        String.format("- **Total Files:** %d\n", files.size()));
    md.append(String.format("- **Total Size:** %s\n", formatSize(totalSize)));
    md.append(String.format("- **Generated:** %s\n", getCurrentDateTime()));

    Files.writeString(outputFile.toPath(), md.toString(), StandardCharsets.UTF_8);
  }

  @NotNull
  private Map<String, List<File>> groupByType(@NotNull List<File> files) {
    Map<String, List<File>> grouped = new TreeMap<>();

    for (File file : files) {
      String type = getFileType(file);
      grouped.computeIfAbsent(type, k -> new java.util.ArrayList<>()).add(file);
    }

    return grouped;
  }

  @NotNull
  private String getFileType(@NotNull File file) {
    String name = file.getName();
    int lastDot = name.lastIndexOf('.');
    return lastDot > 0 ? name.substring(lastDot + 1).toLowerCase() : "unknown";
  }

  @NotNull
  private String formatType(@NotNull String type) {
    return switch (type) {
      case "java" -> "☕ Java";
      case "kt" -> "🎯 Kotlin";
      case "xml" -> "📄 XML";
      case "json" -> "📋 JSON";
      case "gradle" -> "🔨 Gradle";
      case "properties" -> "⚙️ Properties";
      case "md" -> "📝 Markdown";
      case "yml", "yaml" -> "⚡ YAML";
      case "sql" -> "💾 SQL";
      case "html" -> "🌐 HTML";
      case "css" -> "🎨 CSS";
      case "js" -> "📜 JavaScript";
      case "py" -> "🐍 Python";
      case "sh", "bash" -> "💻 Shell";
      default -> "📄 ".concat(type).toUpperCase();
    };
  }

  @NotNull
  private String getLanguageForHighlight(@NotNull String type) {
    return switch (type) {
      case "java" -> "java";
      case "kt" -> "kotlin";
      case "xml" -> "xml";
      case "json" -> "json";
      case "gradle" -> "gradle";
      case "properties" -> "properties";
      case "md" -> "markdown";
      case "yml", "yaml" -> "yaml";
      case "sql" -> "sql";
      case "html" -> "html";
      case "css" -> "css";
      case "js" -> "javascript";
      case "py" -> "python";
      case "sh", "bash" -> "bash";
      default -> "text";
    };
  }

  @NotNull
  private String formatSize(long bytes) {
    if (bytes >= 1_000_000) {
      return String.format("%.2f MB", bytes / 1_000_000.0);
    } else if (bytes >= 1_000) {
      return String.format("%.2f KB", bytes / 1_000.0);
    } else {
      return bytes + " B";
    }
  }

  @NotNull
  private String getCurrentDate() {
    return new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
  }

  @NotNull
  private String getCurrentDateTime() {
    return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
  }
}
