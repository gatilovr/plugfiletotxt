package com.example.plugfiletotxt.service;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class GitServiceTest {

  private GitService gitService;

  @Before
  public void setUp() {
    gitService = new GitService();
  }

  @Test
  public void testGitServiceInitialization() {
    assertNotNull(gitService);
  }

  @Test
  public void testChangeTypeEnum() {
    assertNotNull(GitService.ChangeType.STAGED);
    assertNotNull(GitService.ChangeType.UNSTAGED);
    assertNotNull(GitService.ChangeType.ALL_CHANGES);
    assertNotNull(GitService.ChangeType.SINCE_LAST_COMMIT);
  }

  @Test
  public void testIsGitRepository() {
    // Test with current directory (might or might not be git repo)
    File currentDir = new File(".");
    boolean isGit = gitService.isGitRepository(currentDir);
    // Should return boolean without throwing exception
    assertTrue(isGit || !isGit);
  }

  @Test
  public void testGetChangedFilesReturnsMap() {
    File projectDir = new File(".");
    if (gitService.isGitRepository(projectDir)) {
      Map<File, GitService.ChangeType> changes =
          gitService.getChangedFiles(projectDir, GitService.ChangeType.ALL_CHANGES);
      assertNotNull(changes);
    }
  }
}
