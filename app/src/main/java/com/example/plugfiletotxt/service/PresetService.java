package com.example.plugfiletotxt.service;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Service for managing export presets. Allows users to save and load filter configurations for
 * quick repeated exports.
 */
public class PresetService {

  private static final Logger LOG = Logger.getInstance(PresetService.class);

  private static final String PRESETS_PREFIX = "com.example.plugfiletotxt.preset.";
  private static final String PRESETS_LIST_KEY = "com.example.plugfiletotxt.presets.list";

  public static class ExportPreset {
    public String name;
    public int filterIndex; // 0=All, 1=Source, 2=Java/Kotlin, 3=XML
    public boolean respectGitignore;
    public boolean flatMode;
    public boolean openFolderAfter;
    public int hugeFileMode; // 0=Skip, 1=Partial, 2=Full
    public String outputFormat; // "text", "json", "markdown"
    public String changeType; // "all", "staged", "unstaged", "since_last"

    public ExportPreset() {}

    public ExportPreset(
        String name,
        int filterIndex,
        boolean respectGitignore,
        boolean flatMode,
        boolean openFolderAfter,
        int hugeFileMode,
        String outputFormat,
        String changeType) {
      this.name = name;
      this.filterIndex = filterIndex;
      this.respectGitignore = respectGitignore;
      this.flatMode = flatMode;
      this.openFolderAfter = openFolderAfter;
      this.hugeFileMode = hugeFileMode;
      this.outputFormat = outputFormat;
      this.changeType = changeType;
    }

    public String toJson() {
      return String.format(
          "{\"name\":\"%s\",\"filter\":%d,\"gitignore\":%b,\"flat\":%b,\"open\":%b,"
              + "\"huge\":%d,\"format\":\"%s\",\"changes\":\"%s\"}",
          escapedJson(name),
          filterIndex,
          respectGitignore,
          flatMode,
          openFolderAfter,
          hugeFileMode,
          outputFormat,
          changeType);
    }

    public static ExportPreset fromJson(String json) {
      // Simple JSON parser for preset
      ExportPreset preset = new ExportPreset();
      try {
        preset.name = extractJsonString(json, "name");
        preset.filterIndex = extractJsonInt(json, "filter");
        preset.respectGitignore = extractJsonBoolean(json, "gitignore");
        preset.flatMode = extractJsonBoolean(json, "flat");
        preset.openFolderAfter = extractJsonBoolean(json, "open");
        preset.hugeFileMode = extractJsonInt(json, "huge");
        preset.outputFormat = extractJsonString(json, "format");
        preset.changeType = extractJsonString(json, "changes");
      } catch (Exception e) {
        LOG.warn("Error parsing preset JSON", e);
      }
      return preset;
    }

    private String escapedJson(String str) {
      return str.replace("\"", "\\\"").replace("\n", "\\n");
    }
  }

  private final Project project;

  public PresetService(Project project) {
    this.project = project;
  }

  public static PresetService getInstance(@NotNull Project project) {
    return project.getService(PresetService.class);
  }

  /** Get all saved presets. */
  @NotNull
  public List<ExportPreset> getAllPresets() {
    List<ExportPreset> presets = new ArrayList<>();
    PropertiesComponent props = PropertiesComponent.getInstance();

    String listStr = props.getValue(PRESETS_LIST_KEY, "");
    if (listStr.isEmpty()) {
      // Return built-in presets
      return getBuiltInPresets();
    }

    String[] presetNames = listStr.split(",");
    for (String name : presetNames) {
      String key = PRESETS_PREFIX + name;
      String json = props.getValue(key);
      if (json != null) {
        presets.add(ExportPreset.fromJson(json));
      }
    }

    // Add built-in presets
    presets.addAll(getBuiltInPresets());

    return presets;
  }

  /** Get built-in presets. */
  @NotNull
  private List<ExportPreset> getBuiltInPresets() {
    List<ExportPreset> presets = new ArrayList<>();

    presets.add(
        new ExportPreset(
            "📝 Code Review", 1, true, false, false, 1, "text", "staged"));
    presets.add(
        new ExportPreset(
            "📚 Documentation", 1, true, true, false, 1, "markdown", "all"));
    presets.add(
        new ExportPreset(
            "🔍 All Source",
            1,
            true,
            false,
            false,
            1,
            "json",
            "all"));
    presets.add(
        new ExportPreset(
            "⚡ Quick Export",
            0,
            false,
            true,
            true,
            1,
            "text",
            "all"));
    presets.add(
        new ExportPreset(
            "🐛 Bug Analysis",
            1,
            true,
            false,
            false,
            2,
            "markdown",
            "unstaged"));
    presets.add(
        new ExportPreset(
            "🏗️ Architecture",
            2,
            true,
            false,
            false,
            1,
            "json",
            "all"));

    return presets;
  }

  /** Save a preset. */
  public void savePreset(@NotNull ExportPreset preset) {
    PropertiesComponent props = PropertiesComponent.getInstance();
    String json = preset.toJson();

    // Save preset
    String key = PRESETS_PREFIX + preset.name;
    props.setValue(key, json);

    // Update presets list
    String listStr = props.getValue(PRESETS_LIST_KEY, "");
    if (!listStr.contains(preset.name)) {
      if (listStr.isEmpty()) {
        listStr = preset.name;
      } else {
        listStr = listStr + "," + preset.name;
      }
      props.setValue(PRESETS_LIST_KEY, listStr);
    }

    LOG.info("Saved preset: " + preset.name);
  }

  /** Delete a preset. */
  public void deletePreset(@NotNull String presetName) {
    PropertiesComponent props = PropertiesComponent.getInstance();

    // Delete preset
    String key = PRESETS_PREFIX + presetName;
    props.unsetValue(key);

    // Update presets list
    String listStr = props.getValue(PRESETS_LIST_KEY, "");
    String[] presets = listStr.split(",");
    List<String> remaining = new ArrayList<>();

    for (String preset : presets) {
      if (!preset.equals(presetName)) {
        remaining.add(preset);
      }
    }

    props.setValue(PRESETS_LIST_KEY, String.join(",", remaining));
    LOG.info("Deleted preset: " + presetName);
  }

  /** Get preset by name. */
  @Nullable
  public ExportPreset getPreset(@NotNull String name) {
    for (ExportPreset preset : getAllPresets()) {
      if (preset.name.equals(name)) {
        return preset;
      }
    }
    return null;
  }

  /** Helper methods for JSON parsing. */
  private static String extractJsonString(String json, String key) {
    String pattern = "\"" + key + "\":\"([^\"]*)\"";
    java.util.regex.Matcher matcher =
        java.util.regex.Pattern.compile(pattern).matcher(json);
    return matcher.find() ? matcher.group(1) : "";
  }

  private static int extractJsonInt(String json, String key) {
    String pattern = "\"" + key + "\":(\\d+)";
    java.util.regex.Matcher matcher =
        java.util.regex.Pattern.compile(pattern).matcher(json);
    return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
  }

  private static boolean extractJsonBoolean(String json, String key) {
    String pattern = "\"" + key + "\":(true|false)";
    java.util.regex.Matcher matcher =
        java.util.regex.Pattern.compile(pattern).matcher(json);
    return matcher.find() && matcher.group(1).equals("true");
  }
}
