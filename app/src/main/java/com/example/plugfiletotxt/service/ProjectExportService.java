package com.example.plugfiletotxt.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.ui.CheckedTreeNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

@Service(Service.Level.PROJECT) // Light service — не требует регистрации в plugin.xml!
public final class ProjectExportService {

    private static final Logger LOG = Logger.getInstance(ProjectExportService.class);

    private static final Set<String> SOURCE_EXTENSIONS = Set.of("java", "kt", "xml", "gradle", "properties", "yml", "json", "md", "txt");
    private static final Set<String> JAVA_EXTENSIONS = Set.of("java", "kt");
    private static final Set<String> XML_EXTENSIONS = Set.of("xml");
    private static final Set<String> EXCLUDED_DIRS = Set.of(
            "build", "out", "gradle", "idea", ".gradle", ".idea", ".git",
            "node_modules", "target", "bin", "obj", "dist", "release", "debug",
            ".mvn", ".settings", ".classpath", ".project", ".DS_Store"
    );
    private static final int MAX_TREE_DEPTH = 50;

    private File currentRootFolder;
    private CheckedTreeNode rootNode;
    private final Set<String> selectedPaths = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<String> gitignorePatterns = new HashSet<>();
    private final Map<String, Long> fileTimestamps = new ConcurrentHashMap<>(); // Кэш временных меток файлов
    private long lastScanTime = 0;
    private static final long CACHE_TTL = 5000; // 5 секунд кэширования

    public static ProjectExportService getInstance(@NotNull Project project) {
        return project.getService(ProjectExportService.class);
    }

    @Nullable
    public File getCurrentRootFolder() {
        return currentRootFolder;
    }

    /**
     * Устанавливает новую корневую папку и ПОЛНОСТЬЮ очищает все данные
     */
    public void setCurrentRootFolder(@Nullable File folder) {
        // Если папка меняется (или null) — очищаем всё
        if (!Objects.equals(this.currentRootFolder, folder)) {
            this.currentRootFolder = folder;
            clearAllData();
        }
    }

    /**
     * Полная очистка всех данных — гарантирует, что старый проект не "зависнет"
     */
    public void clearAllData() {
        this.rootNode = null;
        this.selectedPaths.clear();
        this.gitignorePatterns.clear();
        this.fileTimestamps.clear();
        this.lastScanTime = 0;
    }

    private void loadGitignorePatterns() {
        gitignorePatterns.clear();
        if (currentRootFolder == null) return;

        File gitignoreFile = new File(currentRootFolder, ".gitignore");
        if (gitignoreFile.exists()) {
            try {
                List<String> lines = Files.readAllLines(gitignoreFile.toPath());
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                        gitignorePatterns.add(trimmed);
                    }
                }
            } catch (IOException e) {
                // Логируем ошибку чтения .gitignore
                LOG.warn("Failed to read .gitignore file", e);
            }
        }
    }

    private boolean shouldIncludeFile(@NotNull File file, int filterIndex, boolean respectGitignore) {
        String name = file.getName();
        String ext = getExtension(name);

        if (respectGitignore && !gitignorePatterns.isEmpty()) {
            String relativePath = currentRootFolder == null
                    ? name
                    : Optional.ofNullable(FileUtil.getRelativePath(currentRootFolder, file)).orElse(name);
            for (String pattern : gitignorePatterns) {
                if (matchesGitignorePattern(relativePath, pattern)) {
                    return false;
                }
            }
        }

        if (filterIndex == 0) return true;
        if (filterIndex == 1) return ext != null && SOURCE_EXTENSIONS.contains(ext.toLowerCase());
        if (filterIndex == 2) return ext != null && JAVA_EXTENSIONS.contains(ext.toLowerCase());
        if (filterIndex == 3) return ext != null && XML_EXTENSIONS.contains(ext.toLowerCase());

        return true;
    }

    private boolean shouldExcludeDir(@NotNull String name) {
        return name.startsWith(".") || EXCLUDED_DIRS.contains(name);
    }

    @Nullable
    private String getExtension(@NotNull String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : null;
    }

    private boolean matchesGitignorePattern(@NotNull String relativePath, @NotNull String pattern) {
        String cleanPattern = pattern.trim().replace('\\', '/');
        String normalizedPath = relativePath.replace('\\', '/');
        if (cleanPattern.isEmpty() || cleanPattern.startsWith("#") || cleanPattern.startsWith("!")) {
            return false;
        }

        boolean dirOnly = cleanPattern.endsWith("/");
        if (dirOnly) {
            cleanPattern = cleanPattern.substring(0, cleanPattern.length() - 1);
        }
        if (cleanPattern.startsWith("/")) {
            cleanPattern = cleanPattern.substring(1);
        }
        if (cleanPattern.isEmpty()) {
            return false;
        }

        String regex = Pattern.quote(cleanPattern)
                .replace("\\*\\*", "§§DOUBLE_STAR§§")
                .replace("\\*", "[^/]*")
                .replace("\\?", "[^/]")
                .replace("§§DOUBLE_STAR§§", ".*");

        boolean anchored = pattern.startsWith("/");
        if (anchored) {
            regex = "^" + regex + "(?:/.*)?$";
        } else {
            regex = "^(?:.*/)?" + regex + "(?:/.*)?$";
        }

        if (dirOnly) {
            return normalizedPath.matches(regex) || normalizedPath.startsWith(cleanPattern + "/");
        }
        return normalizedPath.matches(regex);
    }

    @NotNull
    public boolean needsRefresh() {
        if (currentRootFolder == null || !currentRootFolder.exists()) return false;
        
        long now = System.currentTimeMillis();
        if (now - lastScanTime >= CACHE_TTL) {
            return true;
        }
        
        // Проверяем, изменились ли временные метки файлов
        try {
            AtomicBoolean needsRefresh = new AtomicBoolean(false);
            Files.walkFileTree(currentRootFolder.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String absPath = file.toAbsolutePath().toString();
                    long currentModified = attrs.lastModifiedTime().toMillis();
                    Long cachedModified = fileTimestamps.get(absPath);
                    
                    if (cachedModified == null || currentModified != cachedModified) {
                        needsRefresh.set(true);
                        return FileVisitResult.TERMINATE;
                    }
                    
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
            
            return needsRefresh.get();
        } catch (IOException e) {
            return true; // При ошибке сканирования — пересканируем
        }
    }
    
    public void incrementalUpdate(@Nullable ProgressIndicator indicator, int filterIndex, boolean respectGitignore) {
        if (currentRootFolder == null || !currentRootFolder.exists() || rootNode == null) {
            // Если нет данных — полное сканирование
            scanFolder(indicator, filterIndex, respectGitignore);
            return;
        }

        // Проверяем, нужно ли полное обновление
        if (!needsRefresh()) {
            // Данные актуальны, ничего не делаем
            return;
        }

        // Собираем текущие пути для сравнения
        Set<String> existingPaths = new HashSet<>();
        collectFilePaths(rootNode, existingPaths);

        // Создаём новое дерево
        CheckedTreeNode newRoot = new CheckedTreeNode(currentRootFolder.getName());
        newRoot.setUserObject(currentRootFolder);
        
        // Сбрасываем кэш временных меток
        Map<String, Long> newFileTimestamps = new ConcurrentHashMap<>();

        try {
            Files.walkFileTree(currentRootFolder.toPath(), new SimpleFileVisitor<Path>() {
                private int currentDepth = 0;

                @Override
                public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) {
                    if (indicator != null && indicator.isCanceled()) {
                        return FileVisitResult.TERMINATE;
                    }

                    if (currentDepth > MAX_TREE_DEPTH) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    File dirFile = dir.toFile();
                    String name = dirFile.getName();

                    if (shouldExcludeDir(name)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    if (!dir.equals(currentRootFolder.toPath())) {
                        addFileToTree(newRoot, dirFile);
                    }

                    currentDepth++;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NotNull FileVisitResult visitFile(@NotNull Path path, @NotNull BasicFileAttributes attrs) {
                    if (indicator != null && indicator.isCanceled()) {
                        return FileVisitResult.TERMINATE;
                    }

                    File file = path.toFile();
                    if (shouldIncludeFile(file, filterIndex, respectGitignore)) {
                        // Обновляем кэш временных меток
                        String absPath = file.getAbsolutePath();
                        newFileTimestamps.put(absPath, file.lastModified());
                        addFileToTree(newRoot, file);
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NotNull FileVisitResult postVisitDirectory(@NotNull Path dir, IOException exc) {
                    currentDepth--;
                    if (exc != null) {
                        LOG.warn("Error visiting directory: " + dir, exc);
                    }
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public @NotNull FileVisitResult visitFileFailed(@NotNull Path file, @NotNull IOException exc) {
                    LOG.warn("Failed to visit file: " + file, exc);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOG.warn("Error scanning directory: " + currentRootFolder, e);
        }

        // Обновляем данные
        this.rootNode = newRoot;
        this.fileTimestamps.clear();
        this.fileTimestamps.putAll(newFileTimestamps);
        this.lastScanTime = System.currentTimeMillis();
    }
    
    private void collectFilePaths(CheckedTreeNode node, Set<String> paths) {
        if (node.getUserObject() instanceof File file) {
            paths.add(file.getAbsolutePath());
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            collectFilePaths((CheckedTreeNode) node.getChildAt(i), paths);
        }
    }
    
    public CheckedTreeNode scanFolder(@Nullable ProgressIndicator indicator, int filterIndex, boolean respectGitignore) {
        // Всегда создаём новый корень, даже если папки нет — UI останется стабильным
        if (currentRootFolder == null || !currentRootFolder.exists()) {
            CheckedTreeNode emptyRoot = new CheckedTreeNode("No folder selected");
            this.rootNode = emptyRoot;
            return emptyRoot;
        }

        if (respectGitignore) {
            loadGitignorePatterns();
        }

        // Проверяем кэш перед полным сканированием
        long now = System.currentTimeMillis();
        if (now - lastScanTime < CACHE_TTL) {
            // Если кэш ещё актуален, возвращаем существующий корень
            if (rootNode != null) {
                return rootNode;
            }
        }

        // Полное сканирование только при необходимости
        CheckedTreeNode root = new CheckedTreeNode(currentRootFolder.getName());
        root.setUserObject(currentRootFolder);

        try {
            Files.walkFileTree(currentRootFolder.toPath(), new SimpleFileVisitor<Path>() {
                private int currentDepth = 0;

                @Override
                public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) {
                    if (indicator != null && indicator.isCanceled()) {
                        return FileVisitResult.TERMINATE;
                    }

                    if (currentDepth > MAX_TREE_DEPTH) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    File dirFile = dir.toFile();
                    String name = dirFile.getName();

                    if (shouldExcludeDir(name)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    if (!dir.equals(currentRootFolder.toPath())) {
                        addFileToTree(root, dirFile);
                    }

                    currentDepth++;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NotNull FileVisitResult visitFile(@NotNull Path path, @NotNull BasicFileAttributes attrs) {
                    if (indicator != null && indicator.isCanceled()) {
                        return FileVisitResult.TERMINATE;
                    }

                    File file = path.toFile();
                    if (shouldIncludeFile(file, filterIndex, respectGitignore)) {
                        // Обновляем кэш временных меток
                        fileTimestamps.put(file.getAbsolutePath(), file.lastModified());
                        addFileToTree(root, file);
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NotNull FileVisitResult postVisitDirectory(@NotNull Path dir, IOException exc) {
                    currentDepth--;
                    if (exc != null) {
                        LOG.warn("Error visiting directory: " + dir, exc);
                    }
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public @NotNull FileVisitResult visitFileFailed(@NotNull Path file, @NotNull IOException exc) {
                    LOG.warn("Failed to visit file: " + file, exc);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOG.warn("Error scanning directory: " + currentRootFolder, e);
        }

        lastScanTime = now;
        this.rootNode = root;
        return root;
    }

    private void addFileToTree(@NotNull CheckedTreeNode root, @NotNull File file) {
        if (currentRootFolder == null) return;

        String relativePath = FileUtil.getRelativePath(currentRootFolder, file);
        if (relativePath == null) return;

        String[] pathParts = relativePath.split(Pattern.quote(File.separator));

        CheckedTreeNode currentNode = root;
        StringBuilder currentPath = new StringBuilder();

        for (String part : pathParts) {
            if (!currentPath.isEmpty()) {
                currentPath.append(File.separator);
            }
            currentPath.append(part);

            CheckedTreeNode childNode = findChildNode(currentNode, part);
            if (childNode == null) {
                File partFile = new File(currentRootFolder, currentPath.toString());
                childNode = new CheckedTreeNode(partFile);
                currentNode.add(childNode);
                
                // Сохраняем временную метку для нового файла
                if (partFile.isFile()) {
                    fileTimestamps.put(partFile.getAbsolutePath(), partFile.lastModified());
                }
            }
            currentNode = childNode;
        }
    }

    private CheckedTreeNode findChildNode(@NotNull CheckedTreeNode parent, @NotNull String name) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            CheckedTreeNode child = (CheckedTreeNode) parent.getChildAt(i);
            Object userObject = child.getUserObject();
            if (userObject instanceof File file && file.getName().equals(name)) {
                return child;
            } else if (userObject instanceof String str && str.equals(name)) {
                return child;
            }
        }
        return null;
    }

    public void saveSelectedPaths(@NotNull CheckedTreeNode node, @NotNull String path) {
        if (node.getUserObject() instanceof File file) {
            String currentPath = path.isEmpty() ? file.getName() : path + "/" + file.getName();
            if (node.isChecked() && !file.isDirectory()) {
                selectedPaths.add(currentPath);
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                saveSelectedPaths((CheckedTreeNode) node.getChildAt(i), currentPath);
            }
        }
    }

    public void restoreSelectedPaths(@NotNull CheckedTreeNode node, @NotNull String path) {
        if (node.getUserObject() instanceof File file) {
            String currentPath = path.isEmpty() ? file.getName() : path + "/" + file.getName();
            node.setChecked(!file.isDirectory() && selectedPaths.contains(currentPath));
            for (int i = 0; i < node.getChildCount(); i++) {
                restoreSelectedPaths((CheckedTreeNode) node.getChildAt(i), currentPath);
            }
        }
    }

    public void collectSelectedFiles(@NotNull CheckedTreeNode node, @NotNull List<File> files) {
        if (node.getUserObject() instanceof File file && node.isChecked() && !file.isDirectory()) {
            files.add(file);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            collectSelectedFiles((CheckedTreeNode) node.getChildAt(i), files);
        }
    }

    public void exportFile(@NotNull File file, boolean flatMode, @NotNull File outputFolder) throws IOException {
        File targetFile;

        if (flatMode) {
            targetFile = new File(outputFolder, file.getName() + ".txt");
            int counter = 1;
            while (targetFile.exists()) {
                targetFile = new File(outputFolder, file.getName() + "_" + counter + ".txt");
                counter++;
            }
        } else {
            if (currentRootFolder == null) return;
            String relativePath = FileUtil.getRelativePath(currentRootFolder, file);
            if (relativePath == null) {
                relativePath = file.getName();
            }
            targetFile = new File(outputFolder, relativePath + ".txt");
        }

        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("Failed to create directory: " + parentDir.getPath());
        }

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
             BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(targetFile))) {
            bis.transferTo(bos);
        }
    }
}