package com.example.plugfiletotxt.model;

import com.example.plugfiletotxt.service.ProjectExportService;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Export configuration model. Encapsulates all export settings and run them together.
 */
public class ExportConfig {

  public enum OutputFormat {
    TEXT("Plain Text", "txt"),
    JSON("JSON with Metadata", "json"),
    MARKDOWN("Markdown Formatted", "md");

    public final String displayName;
    public final String extension;

    OutputFormat(String displayName, String extension) {
      this.displayName = displayName;
      this.extension = extension;
    }
  }

  public enum ChangeType {
    ALL("All Files"),
    STAGED("Staged Changes"),
    UNSTAGED("Unstaged Changes"),
    SINCE_LAST_COMMIT("Since Last Commit");

    public final String displayName;

    ChangeType(String displayName) {
      this.displayName = displayName;
    }
  }

  // Filtering options
  public int filterIndex = 0; // 0=All, 1=Source, 2=Java/Kotlin, 3=XML
  public boolean respectGitignore = true;
  public ChangeType changeType = ChangeType.ALL;

  // Export options
  public boolean flatMode = false;
  public boolean openFolderAfter = true;
  public OutputFormat outputFormat = OutputFormat.TEXT;
  public ProjectExportService.HugeFileMode hugeFileMode =
      ProjectExportService.HugeFileMode.PARTIAL;

  // Selected files
  public List<File> selectedFiles = new ArrayList<>();

  // Paths
  public File projectFolder;
  public File outputFolder;

  // Metadata
  public String exportName;
  public boolean includeMetadata = true;

  public ExportConfig() {}

  public ExportConfig(
      File projectFolder,
      File outputFolder,
      int filterIndex,
      boolean respectGitignore,
      boolean flatMode,
      OutputFormat format) {
    this.projectFolder = projectFolder;
    this.outputFolder = outputFolder;
    this.filterIndex = filterIndex;
    this.respectGitignore = respectGitignore;
    this.flatMode = flatMode;
    this.outputFormat = format;
  }

  @NotNull
  public String getOutputFileName() {
    String base = exportName != null ? exportName : "export";
    String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
    return String.format("%s_%s.%s", base, timestamp, outputFormat.extension);
  }

  @NotNull
  @Override
  public String toString() {
    return String.format(
        "ExportConfig{files=%d, format=%s, changes=%s, flat=%b}",
        selectedFiles.size(), outputFormat, changeType, flatMode);
  }
}
