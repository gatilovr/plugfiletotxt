package com.example.plugfiletotxt.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

public class ExportAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ExportAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        LOG.info("ExportAction actionPerformed called!");
        Project project = e.getProject();
        if (project == null) {
            LOG.warn("Project is null");
            return;
        }

        Messages.showInfoMessage(
                "Плагин успешно работает!\nВыбери файлы для экспорта",
                "Selective Code Exporter"
        );
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Для ToolsMenu важно: действие должно быть enabled, чтобы быть видимым
        Project project = e.getProject();
        boolean enabled = project != null;
        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setVisible(true); // visible всегда true

        LOG.info("ExportAction update called, enabled: " + enabled + ", project: " + project);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}