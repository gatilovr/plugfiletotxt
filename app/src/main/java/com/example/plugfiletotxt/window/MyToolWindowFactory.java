package com.example.plugfiletotxt.window;

import com.example.plugfiletotxt.service.ProjectExportService;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckboxTreeBase;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import org.jetbrains.annotations.NotNull;

public class MyToolWindowFactory implements ToolWindowFactory {

  private static final String LAST_PROJECT_FOLDER_KEY =
      "com.example.plugfiletotxt.lastProjectFolder";
  private static final String LAST_OUTPUT_FOLDER_KEY = "com.example.plugfiletotxt.lastOutputFolder";
  private static final String[] FILTER_OPTIONS = {
    "All Files", "Source Files (.java, .kt, .xml)", "Java/Kotlin Only", "XML Only"
  };
  private static final String[] HUGE_FILE_OPTIONS = {
    "Skip huge files", "Partial export (first 1 MB)", "Export full file"
  };

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    ProjectExportService service = ProjectExportService.getInstance(project);
    ToolWindowPanel panel = new ToolWindowPanel(project, toolWindow, service);

    ContentFactory contentFactory = ContentFactory.getInstance();
    Content content = contentFactory.createContent(panel, "", false);
    toolWindow.getContentManager().removeAllContents(true);
    toolWindow.getContentManager().addContent(content);
  }

  private static class ToolWindowPanel extends JPanel {
    private final Project project;
    private final ToolWindow toolWindow;
    private final ProjectExportService service;

    // UI Components — НЕ FINAL, инициализируем в конструкторе
    private final TextFieldWithBrowseButton projectFolderField;
    private final TextFieldWithBrowseButton outputFolderField;
    private final JPanel centerPanel;
    private ComboBox<String> filterComboBox;
    private JCheckBox respectGitignoreCheckBox;
    private JCheckBox flatExportCheckBox;
    private JCheckBox openFolderCheckBox;
    private ComboBox<String> hugeFileModeComboBox;
    private JButton exportButton;
    private JButton refreshButton;
    private JProgressBar progressBar;
    private JLabel statsLabel;

    // Текущее состояние
    private CheckboxTree fileTree;
    private CheckedTreeNode rootNode;
    private String currentProjectPath = "";

    public ToolWindowPanel(
        @NotNull Project project,
        @NotNull ToolWindow toolWindow,
        @NotNull ProjectExportService service) {
      this.project = project;
      this.toolWindow = toolWindow;
      this.service = service;

      setLayout(new BorderLayout(JBUI.scale(10), JBUI.scale(10)));
      setBorder(JBUI.Borders.empty(12));

      // Создаём поля для выбора папок
      projectFolderField = new TextFieldWithBrowseButton();
      outputFolderField = new TextFieldWithBrowseButton();

      // Загружаем последние использованные папки
      loadLastFolders();

      // Верхняя панель — создаётся один раз и не меняется
      add(createTopPanel(), BorderLayout.NORTH);

      // Центральная панель — будет обновляться, но сама панель живёт вечно
      centerPanel = new JPanel(new BorderLayout());
      centerPanel.setBorder(BorderFactory.createTitledBorder("Project Files"));
      showInitialMessage();
      add(centerPanel, BorderLayout.CENTER);

      // Нижняя панель — тоже стабильна
      add(createBottomPanel(), BorderLayout.SOUTH);
    }

    private void loadLastFolders() {
      PropertiesComponent props = PropertiesComponent.getInstance(project);

      String lastProject = props.getValue(LAST_PROJECT_FOLDER_KEY);
      if (lastProject != null && !lastProject.isEmpty()) {
        projectFolderField.setText(lastProject);
        currentProjectPath = lastProject;
        File folder = new File(lastProject);
        if (folder.exists()) {
          service.setCurrentRootFolder(folder);
        }
      }

      String lastOutput = props.getValue(LAST_OUTPUT_FOLDER_KEY);
      if (lastOutput != null && !lastOutput.isEmpty()) {
        outputFolderField.setText(lastOutput);
      }
    }

    private JPanel createTopPanel() {
      JPanel topPanel = new JPanel();
      topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

      // Панель выбора проекта
      JPanel projectPanel = new JPanel(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.insets = JBUI.insets(2);

      gbc.gridx = 0;
      gbc.gridy = 0;
      gbc.weightx = 0;
      projectPanel.add(new JLabel("Project Folder:"), gbc);

      gbc.gridx = 1;
      gbc.weightx = 1;
      projectPanel.add(projectFolderField, gbc);

      gbc.gridx = 2;
      gbc.weightx = 0;
      JButton projectBrowseButton = new JButton("Browse...", AllIcons.Actions.MenuOpen);
      projectBrowseButton.addActionListener(e -> chooseProjectFolder());
      projectPanel.add(projectBrowseButton, gbc);

      topPanel.add(projectPanel);
      topPanel.add(Box.createVerticalStrut(JBUI.scale(8)));

      // Панель вывода
      JPanel outputPanel = new JPanel(new GridBagLayout());

      gbc.gridx = 0;
      gbc.gridy = 0;
      gbc.weightx = 0;
      outputPanel.add(new JLabel("Output Folder:"), gbc);

      gbc.gridx = 1;
      gbc.weightx = 1;
      outputPanel.add(outputFolderField, gbc);

      gbc.gridx = 2;
      gbc.weightx = 0;
      JButton outputBrowseButton = new JButton("Browse...", AllIcons.Actions.MenuOpen);
      outputBrowseButton.addActionListener(e -> chooseOutputFolder());
      outputPanel.add(outputBrowseButton, gbc);

      topPanel.add(outputPanel);
      topPanel.add(Box.createVerticalStrut(JBUI.scale(8)));

      // Панель фильтров
      JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

      filterPanel.add(new JLabel("Filter:"));
      filterComboBox = new ComboBox<>(FILTER_OPTIONS);
      filterComboBox.addActionListener(e -> refreshFileTree());
      filterPanel.add(filterComboBox);

      filterPanel.add(Box.createHorizontalStrut(10));

      respectGitignoreCheckBox = new JCheckBox("Respect .gitignore");
      respectGitignoreCheckBox.addActionListener(e -> refreshFileTree());
      filterPanel.add(respectGitignoreCheckBox);

      topPanel.add(filterPanel);
      topPanel.add(Box.createVerticalStrut(JBUI.scale(8)));

      // Панель кнопок дерева
      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

      JButton selectAllButton = new JButton("Select All", AllIcons.Actions.Selectall);
      selectAllButton.addActionListener(e -> setAllChecked(true));
      buttonPanel.add(selectAllButton);

      JButton clearAllButton = new JButton("Clear All", AllIcons.Actions.Unselectall);
      clearAllButton.addActionListener(e -> setAllChecked(false));
      buttonPanel.add(clearAllButton);

      buttonPanel.add(new JToolBar.Separator());

      JButton expandAllButton = new JButton("Expand All", AllIcons.Actions.Expandall);
      expandAllButton.addActionListener(e -> expandAllNodes());
      buttonPanel.add(expandAllButton);

      JButton collapseAllButton = new JButton("Collapse All", AllIcons.Actions.Collapseall);
      collapseAllButton.addActionListener(e -> collapseAllNodes());
      buttonPanel.add(collapseAllButton);

      buttonPanel.add(new JToolBar.Separator());

      refreshButton = new JButton("Refresh", AllIcons.Actions.Refresh);
      refreshButton.addActionListener(e -> refreshFileTree());
      buttonPanel.add(refreshButton);

      topPanel.add(buttonPanel);
      topPanel.add(Box.createVerticalStrut(JBUI.scale(8)));

      // Панель экспорта
      JPanel exportPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

      exportButton = new JButton("Export", AllIcons.Actions.Upload);
      exportButton.setEnabled(false);
      exportButton.addActionListener(e -> exportFiles());
      exportPanel.add(exportButton);

      flatExportCheckBox = new JCheckBox("Without folders");
      exportPanel.add(flatExportCheckBox);

      openFolderCheckBox = new JCheckBox("Open after export");
      openFolderCheckBox.setSelected(true);
      exportPanel.add(openFolderCheckBox);

      exportPanel.add(Box.createHorizontalStrut(10));
      exportPanel.add(new JLabel("Huge files:"));
      hugeFileModeComboBox = new ComboBox<>(HUGE_FILE_OPTIONS);
      hugeFileModeComboBox.setSelectedIndex(1);
      exportPanel.add(hugeFileModeComboBox);

      topPanel.add(exportPanel);

      return topPanel;
    }

    private JPanel createBottomPanel() {
      JPanel bottomPanel = new JPanel(new BorderLayout(JBUI.scale(5), 0));
      bottomPanel.setBorder(JBUI.Borders.emptyTop(8));

      statsLabel = new JLabel(" ");
      statsLabel.setFont(UIUtil.getToolTipFont());
      statsLabel.setForeground(UIUtil.getInactiveTextColor());
      bottomPanel.add(statsLabel, BorderLayout.WEST);

      progressBar = new JProgressBar();
      progressBar.setStringPainted(true);
      progressBar.setVisible(false);
      progressBar.setPreferredSize(new Dimension(-1, JBUI.scale(20)));
      bottomPanel.add(progressBar, BorderLayout.SOUTH);

      return bottomPanel;
    }

    private void showInitialMessage() {
      centerPanel.removeAll();
      JLabel label = new JLabel("Select a project folder to begin", SwingConstants.CENTER);
      label.setPreferredSize(new Dimension(300, 200));
      centerPanel.add(label, BorderLayout.CENTER);
      centerPanel.revalidate();
      centerPanel.repaint();
    }

    private void showLoading() {
      centerPanel.removeAll();
      JLabel label = new JLabel("Loading files...", SwingConstants.CENTER);
      label.setPreferredSize(new Dimension(300, 200));
      centerPanel.add(label, BorderLayout.CENTER);
      centerPanel.revalidate();
      centerPanel.repaint();
    }

    private void showTree() {
      if (fileTree == null) return;

      centerPanel.removeAll();
      JScrollPane scrollPane = new JBScrollPane(fileTree);
      scrollPane.setBorder(null);
      centerPanel.add(scrollPane, BorderLayout.CENTER);
      centerPanel.revalidate();
      centerPanel.repaint();
    }

    private CheckboxTree createCheckboxTree() {
      CheckboxTreeBase.CheckPolicy checkPolicy =
          new CheckboxTreeBase.CheckPolicy(true, true, true, true);

      return new CheckboxTree(
          new CheckboxTree.CheckboxTreeCellRenderer() {
            @Override
            public void customizeRenderer(
                JTree tree,
                Object value,
                boolean selected,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus) {
              super.customizeRenderer(tree, value, selected, expanded, leaf, row, hasFocus);

              if (value instanceof CheckedTreeNode node
                  && node.getUserObject() instanceof File file) {
                getTextRenderer().append(file.getName());
                getTextRenderer()
                    .setIcon(file.isDirectory() ? AllIcons.Nodes.Folder : AllIcons.FileTypes.Text);
              }
            }
          },
          rootNode,
          checkPolicy) {
        @Override
        protected void onNodeStateChanged(CheckedTreeNode node) {
          super.onNodeStateChanged(node);
          updateStats();
        }
      };
    }

    private void chooseProjectFolder() {
      var descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
      descriptor.setTitle("Select Project Folder");

      VirtualFile folder = FileChooser.chooseFile(descriptor, project, null);
      if (folder != null) {
        String path = folder.getPath();

        if (path.equals(currentProjectPath)) {
          refreshFileTree();
          return;
        }

        currentProjectPath = path;
        projectFolderField.setText(path);

        PropertiesComponent.getInstance(project).setValue(LAST_PROJECT_FOLDER_KEY, path);

        service.clearAllData();
        loadProjectFolder(new File(path));
      }
    }

    private void loadProjectFolder(File folder) {
      showLoading();
      progressBar.setVisible(true);
      progressBar.setIndeterminate(true);

      service.setCurrentRootFolder(folder);

      int filterIndex = filterComboBox.getSelectedIndex();
      boolean respectGitignore = respectGitignoreCheckBox.isSelected();

      new Task.Backgroundable(project, "Scanning folder...", true) {
        private CheckedTreeNode newRoot;

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          indicator.setText("Scanning: " + folder.getPath());
          newRoot = service.scanFolder(indicator, filterIndex, respectGitignore);
        }

        @Override
        public void onSuccess() {
          if (newRoot != null) {
            rootNode = newRoot;

            fileTree = createCheckboxTree();
            fileTree.setRootVisible(false);
            fileTree.setShowsRootHandles(true);

            showTree();
            expandAllNodes();
            updateStats();
          }
          progressBar.setVisible(false);
          progressBar.setIndeterminate(false);
        }

        @Override
        public void onCancel() {
          progressBar.setVisible(false);
          progressBar.setIndeterminate(false);
          showInitialMessage();
        }
      }.queue();
    }

    private void chooseOutputFolder() {
      var descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
      descriptor.setTitle("Select Output Folder");

      VirtualFile folder = FileChooser.chooseFile(descriptor, project, null);
      if (folder != null) {
        String path = folder.getPath();
        outputFolderField.setText(path);
        PropertiesComponent.getInstance(project).setValue(LAST_OUTPUT_FOLDER_KEY, path);
        updateStats();
      }
    }

    private void refreshFileTree() {
      if (service.getCurrentRootFolder() == null) {
        showInitialMessage();
        return;
      }

      // Проверяем, нужно ли полное сканирование или инкрементальное обновление
      if (service.needsRefresh()) {
        // Если кэш устарел или файлов много — полное сканирование с прогрессом
        loadProjectFolder(service.getCurrentRootFolder());
      } else {
        // Быстрое инкрементальное обновление
        performIncrementalUpdate();
      }
    }

    private void performIncrementalUpdate() {
      progressBar.setVisible(true);
      progressBar.setIndeterminate(true);

      int filterIndex = filterComboBox.getSelectedIndex();
      boolean respectGitignore = respectGitignoreCheckBox.isSelected();

      new Task.Backgroundable(project, "Updating files...", true) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          indicator.setText("Checking for changes...");
          service.incrementalUpdate(indicator, filterIndex, respectGitignore);
        }

        @Override
        public void onSuccess() {
          CheckedTreeNode updatedRoot = service.scanFolder(null, filterIndex, respectGitignore);
          if (updatedRoot != null) {
            rootNode = updatedRoot;
            fileTree = createCheckboxTree();
            fileTree.setRootVisible(false);
            fileTree.setShowsRootHandles(true);
            showTree();
          }
          updateStats();
          progressBar.setVisible(false);
          progressBar.setIndeterminate(false);
        }

        @Override
        public void onCancel() {
          progressBar.setVisible(false);
          progressBar.setIndeterminate(false);
        }
      }.queue();
    }

    private void updateStats() {
      List<File> selected = getSelectedFiles();
      boolean hasSelectedFiles = !selected.isEmpty();
      File outputFolder = getOutputFolderFromField();
      exportButton.setEnabled(hasSelectedFiles && outputFolder != null && outputFolder.exists());

      long totalSize = 0;
      long totalLines = 0;
      long totalTokens = 0;
      int hugeFiles = 0;

      for (File file : selected) {
        totalSize += file.length();
        if (service.isHugeFile(file)) {
          hugeFiles++;
        }
        try {
          ProjectExportService.FileStats stats = service.countFileStats(file);
          totalLines += stats.lines;
          totalTokens += stats.tokens;
        } catch (IOException ignored) {
        }
      }

      double sizeKB = totalSize / 1024.0;
      double hugeThresholdMb = service.getHugeFileThresholdBytes() / (1024.0 * 1024.0);
      String stats =
          String.format(
              "Selected: %d files | %.2f KB | %d lines | ~%d tokens | huge(>%.0fMB): %d",
              selected.size(), sizeKB, totalLines, totalTokens, hugeThresholdMb, hugeFiles);
      statsLabel.setText(stats);
    }

    private ProjectExportService.HugeFileMode getHugeFileMode() {
      return switch (hugeFileModeComboBox.getSelectedIndex()) {
        case 0 -> ProjectExportService.HugeFileMode.SKIP;
        case 2 -> ProjectExportService.HugeFileMode.FULL;
        default -> ProjectExportService.HugeFileMode.PARTIAL;
      };
    }

    private File getOutputFolderFromField() {
      String path = outputFolderField.getText();
      return path.isEmpty() ? null : new File(path);
    }

    private List<File> getSelectedFiles() {
      List<File> selected = new ArrayList<>();
      if (rootNode != null) {
        service.collectSelectedFiles(rootNode, selected);
      }
      return selected;
    }

    private void setAllChecked(boolean checked) {
      if (rootNode == null) return;
      setNodeChecked(rootNode, checked);
      if (fileTree != null) {
        fileTree.repaint();
      }
      updateStats();
    }

    private void setNodeChecked(CheckedTreeNode node, boolean checked) {
      if (node.getUserObject() instanceof File) {
        node.setChecked(checked);
      }
      for (int i = 0; i < node.getChildCount(); i++) {
        setNodeChecked((CheckedTreeNode) node.getChildAt(i), checked);
      }
    }

    private void expandAllNodes() {
      if (fileTree == null) return;
      for (int i = 0; i < fileTree.getRowCount(); i++) {
        fileTree.expandRow(i);
      }
    }

    private void collapseAllNodes() {
      if (fileTree == null) return;
      fileTree.collapseRow(0);
      for (int i = 1; i < fileTree.getRowCount(); i++) {
        fileTree.collapseRow(i);
      }
    }

    private void exportFiles() {
      List<File> selectedFiles = getSelectedFiles();
      if (selectedFiles.isEmpty()) {
        Messages.showWarningDialog(project, "No files selected for export.", "Export");
        return;
      }

      File outputFolder = getOutputFolderFromField();
      if (outputFolder == null || !outputFolder.exists()) {
        Messages.showWarningDialog(project, "Please select a valid output folder.", "Export");
        return;
      }

      boolean flatMode = flatExportCheckBox.isSelected();
      File finalOutputFolder = outputFolder;
      ProjectExportService.HugeFileMode hugeFileMode = getHugeFileMode();

      new Task.Backgroundable(project, "Exporting files...", true) {
        private int processedFiles = 0;
        private final int totalFiles = selectedFiles.size();
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          indicator.setIndeterminate(false);
          indicator.setFraction(0.0);

          for (File file : selectedFiles) {
            if (indicator.isCanceled()) break;

            String progressText =
                String.format(
                    "Exporting: %s (%d/%d)", file.getName(), processedFiles + 1, totalFiles);
            indicator.setText(progressText);
            indicator.setFraction((double) processedFiles / totalFiles);

            try {
              String warning =
                  service.exportFile(file, flatMode, finalOutputFolder, hugeFileMode, indicator);
              if (warning != null) {
                warnings.add(warning);
              }
            } catch (ProcessCanceledException canceled) {
              throw canceled;
            } catch (Exception e) {
              errors.add("Failed to export " + file.getName() + ": " + e.getMessage());
            }

            processedFiles++;
            indicator.setFraction((double) processedFiles / totalFiles);
          }

          indicator.setFraction(1.0);
        }

        @Override
        public void onSuccess() {
          StringBuilder message = new StringBuilder();
          message.append("Export completed: ").append(processedFiles).append(" files processed.\n");

          if (!errors.isEmpty()) {
            message.append("\nErrors occurred:\n");
            for (String error : errors) {
              message.append("- ").append(error).append("\n");
            }
          }
          if (!warnings.isEmpty()) {
            message.append("\nWarnings:\n");
            for (String warning : warnings) {
              message.append("- ").append(warning).append("\n");
            }
          }

          Messages.showMessageDialog(
              project,
              message.toString(),
              "Export Result",
              errors.isEmpty() ? Messages.getInformationIcon() : Messages.getErrorIcon());

          if (errors.isEmpty() && openFolderCheckBox.isSelected()) {
            openFolder(finalOutputFolder);
          }
        }

        @Override
        public void onCancel() {
          Messages.showWarningDialog(project, "Export was cancelled.", "Export Cancelled");
        }
      }.queue();
    }

    private void openFolder(File folder) {
      try {
        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.OPEN)) {
          desktop.open(folder);
        } else {
          String os = System.getProperty("os.name").toLowerCase();
          Runtime runtime = Runtime.getRuntime();

          if (os.contains("win")) {
            runtime.exec(new String[] {"explorer", folder.getAbsolutePath()});
          } else if (os.contains("mac")) {
            runtime.exec(new String[] {"open", folder.getAbsolutePath()});
          } else if (os.contains("nix") || os.contains("nux")) {
            runtime.exec(new String[] {"xdg-open", folder.getAbsolutePath()});
          }
        }
      } catch (Exception ignored) {
        Messages.showInfoMessage(
            project, "Files exported to:\n" + folder.getAbsolutePath(), "Export Location");
      }
    }
  }

  @Override
  public boolean isDumbAware() {
    return true;
  }
}
