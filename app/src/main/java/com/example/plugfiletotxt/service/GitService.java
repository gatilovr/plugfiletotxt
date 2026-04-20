package com.example.plugfiletotxt.service;

import com.intellij.openapi.diagnostic.Logger;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Service for Git operations: detecting changes, diffs, and file status. Enables smart export of
 * only changed files for code review workflows.
 */
public class GitService {

  private static final Logger LOG = Logger.getInstance(GitService.class);

  public enum ChangeType {
    STAGED,
    UNSTAGED,
    ALL_CHANGES,
    SINCE_LAST_COMMIT
  }

  /**
   * Get files changed in the repository based on change type.
   *
   * @param rootFolder Project root folder containing .git
   * @param changeType Type of changes to detect
   * @return Set of changed files with their types
   */
  @NotNull
  public Map<File, ChangeType> getChangedFiles(
      @NotNull File rootFolder, @NotNull ChangeType changeType) {
    Map<File, ChangeType> changes = new HashMap<>();

    try {
      if (!isGitRepository(rootFolder)) {
        LOG.warn("Not a git repository: " + rootFolder);
        return changes;
      }

      Set<String> changedPaths = new HashSet<>();

      switch (changeType) {
        case STAGED -> changedPaths.addAll(getStagedFiles(rootFolder));
        case UNSTAGED -> changedPaths.addAll(getUnstagedFiles(rootFolder));
        case ALL_CHANGES -> {
          changedPaths.addAll(getStagedFiles(rootFolder));
          changedPaths.addAll(getUnstagedFiles(rootFolder));
        }
        case SINCE_LAST_COMMIT -> changedPaths.addAll(getLastCommitChanges(rootFolder));
      }

      // Convert paths to File objects
      for (String path : changedPaths) {
        File file = new File(rootFolder, path);
        if (file.exists() && file.isFile()) {
          changes.put(file, changeType);
        }
      }
    } catch (IOException e) {
      LOG.warn("Error getting changed files from git", e);
    }

    return changes;
  }

  /** Check if directory is a git repository. */
  public boolean isGitRepository(@NotNull File dir) {
    return new File(dir, ".git").exists();
  }

  /** Get staged (cached) files ready to commit. */
  @NotNull
  private Set<String> getStagedFiles(@NotNull File rootFolder) throws IOException {
    return runGitCommand(rootFolder, "git", "diff", "--cached", "--name-only");
  }

  /** Get unstaged (modified but not staged) files. */
  @NotNull
  private Set<String> getUnstagedFiles(@NotNull File rootFolder) throws IOException {
    return runGitCommand(rootFolder, "git", "diff", "--name-only");
  }

  /** Get files changed in current branch vs main. */
  @NotNull
  private Set<String> getLastCommitChanges(@NotNull File rootFolder) throws IOException {
    return runGitCommand(rootFolder, "git", "diff", "HEAD~1..HEAD", "--name-only");
  }

  /**
   * Get file diff (unified format) for AI analysis.
   *
   * @param file File to get diff for
   * @return Diff text or null if no changes
   */
  @Nullable
  public String getFileDiff(@NotNull File file) {
    try {
      File rootFolder = findGitRoot(file);
      if (rootFolder == null) {
        return null;
      }

      StringBuilder output = new StringBuilder();
      Process process =
          new ProcessBuilder("git", "diff", file.getAbsolutePath())
              .directory(rootFolder)
              .start();

      try (BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          output.append(line).append("\n");
        }
      }

      process.waitFor();
      return output.length() > 0 ? output.toString() : null;
    } catch (Exception e) {
      LOG.warn("Error getting file diff", e);
      return null;
    }
  }

  /** Get current branch name. */
  @Nullable
  public String getCurrentBranch(@NotNull File rootFolder) {
    try {
      File gitRoot = findGitRoot(rootFolder);
      if (gitRoot == null) return null;

      Set<String> result = runGitCommand(gitRoot, "git", "rev-parse", "--abbrev-ref", "HEAD");
      return result.isEmpty() ? null : result.iterator().next();
    } catch (IOException e) {
      LOG.warn("Error getting current branch", e);
      return null;
    }
  }

  /** Get latest commit hash. */
  @Nullable
  public String getLatestCommitHash(@NotNull File rootFolder) {
    try {
      File gitRoot = findGitRoot(rootFolder);
      if (gitRoot == null) return null;

      Set<String> result = runGitCommand(gitRoot, "git", "rev-parse", "HEAD");
      return result.isEmpty() ? null : result.iterator().next();
    } catch (IOException e) {
      LOG.warn("Error getting commit hash", e);
      return null;
    }
  }

  /** Run git command and return output lines as set. */
  @NotNull
  private Set<String> runGitCommand(@NotNull File workDir, @NotNull String... args)
      throws IOException {
    Set<String> result = new HashSet<>();

    try {
      ProcessBuilder pb = new ProcessBuilder(args);
      pb.directory(workDir);
      pb.redirectErrorStream(true);

      Process process = pb.start();

      try (BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          line = line.trim();
          if (!line.isEmpty()) {
            result.add(line);
          }
        }
      }

      process.waitFor();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOG.warn("Git command interrupted", e);
    }

    return result;
  }

  /** Find git root directory by walking up from current directory. */
  @Nullable
  private File findGitRoot(@NotNull File startDir) {
    File current = startDir.isDirectory() ? startDir : startDir.getParentFile();

    while (current != null) {
      if (new File(current, ".git").exists()) {
        return current;
      }
      current = current.getParentFile();
    }

    return null;
  }
}
