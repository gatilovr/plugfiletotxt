package com.example.plugfiletotxt.export;

import com.intellij.openapi.diagnostic.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

/**
 * JSON exporter with code metadata. Exports selected files as a JSON document with extracted code
 * structure information (classes, functions, complexity metrics).
 */
public class JsonExporter {

  private static final Logger LOG = Logger.getInstance(JsonExporter.class);

  private static final Pattern CLASS_PATTERN = Pattern.compile("^\\s*(?:public\\s+)?class\\s+(\\w+)");
  private static final Pattern FUNCTION_PATTERN =
      Pattern.compile("(?:public|private|protected)?\\s+\\w+\\s+(\\w+)\\s*\\(");
  private static final Pattern IMPORT_PATTERN = Pattern.compile("^\\s*import\\s+([^;]+)");

  public void exportToFile(
      @NotNull List<File> files,
      @NotNull File outputFile,
      @NotNull String projectName,
      long exportTimestamp)
      throws IOException {

    StringBuilder json = new StringBuilder();
    json.append("{\n");
    json.append(String.format("  \"project\": \"%s\",\n", escapeJson(projectName)));
    json.append(String.format("  \"exportDate\": \"%s\",\n", formatDate(exportTimestamp)));
    json.append(String.format("  \"fileCount\": %d,\n", files.size()));
    json.append("  \"files\": [\n");

    for (int i = 0; i < files.size(); i++) {
      File file = files.get(i);
      json.append("    {\n");
      json.append(String.format("      \"path\": \"%s\",\n", escapeJson(file.getName())));
      json.append(
          String.format("      \"type\": \"%s\",\n", escapeJson(getFileType(file))));
      json.append(String.format("      \"size\": %d,\n", file.length()));

      try {
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        long lines = content.split("\n", -1).length;
        json.append(String.format("      \"lines\": %d,\n", lines));

        // Extract metadata
        List<String> classes = extractClasses(content);
        List<String> functions = extractFunctions(content);
        List<String> imports = extractImports(content);

        if (!classes.isEmpty()) {
          json.append("      \"classes\": [");
          for (int j = 0; j < classes.size(); j++) {
            json.append(String.format("\"%s\"", escapeJson(classes.get(j))));
            if (j < classes.size() - 1) json.append(", ");
          }
          json.append("],\n");
        }

        if (!functions.isEmpty()) {
          json.append("      \"functions\": [");
          for (int j = 0; j < Math.min(10, functions.size()); j++) {
            json.append(String.format("\"%s\"", escapeJson(functions.get(j))));
            if (j < Math.min(10, functions.size()) - 1) json.append(", ");
          }
          if (functions.size() > 10) {
            json.append(String.format(", \"... +%d more\"", functions.size() - 10));
          }
          json.append("],\n");
        }

        if (!imports.isEmpty()) {
          json.append("      \"imports\": [");
          for (int j = 0; j < Math.min(5, imports.size()); j++) {
            json.append(String.format("\"%s\"", escapeJson(imports.get(j))));
            if (j < Math.min(5, imports.size()) - 1) json.append(", ");
          }
          if (imports.size() > 5) {
            json.append(String.format(", \"... +%d more\"", imports.size() - 5));
          }
          json.append("]\n");
        } else {
          json.setLength(json.length() - 2); // remove last comma
          json.append("\n");
        }
      } catch (IOException e) {
        LOG.warn("Error reading file: " + file, e);
        json.setLength(json.length() - 2); // remove last comma
        json.append("\n");
      }

      json.append("    }");
      if (i < files.size() - 1) json.append(",");
      json.append("\n");
    }

    json.append("  ]\n");
    json.append("}\n");

    Files.writeString(outputFile.toPath(), json.toString(), StandardCharsets.UTF_8);
  }

  @NotNull
  private String getFileType(@NotNull File file) {
    String name = file.getName();
    int lastDot = name.lastIndexOf('.');
    return lastDot > 0 ? name.substring(lastDot + 1) : "unknown";
  }

  @NotNull
  private List<String> extractClasses(@NotNull String content) {
    List<String> classes = new ArrayList<>();
    Matcher matcher = CLASS_PATTERN.matcher(content);
    while (matcher.find()) {
      classes.add(matcher.group(1));
    }
    return classes;
  }

  @NotNull
  private List<String> extractFunctions(@NotNull String content) {
    List<String> functions = new ArrayList<>();
    Matcher matcher = FUNCTION_PATTERN.matcher(content);
    while (matcher.find()) {
      functions.add(matcher.group(1));
    }
    return functions;
  }

  @NotNull
  private List<String> extractImports(@NotNull String content) {
    List<String> imports = new ArrayList<>();
    Matcher matcher = IMPORT_PATTERN.matcher(content);
    while (matcher.find()) {
      imports.add(matcher.group(1));
    }
    return imports;
  }

  @NotNull
  private String formatDate(long timestamp) {
    return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        .format(new java.util.Date(timestamp));
  }

  @NotNull
  private String escapeJson(@NotNull String str) {
    return str.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }
}
