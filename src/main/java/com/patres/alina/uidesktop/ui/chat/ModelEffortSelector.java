package com.patres.alina.uidesktop.ui.chat;

import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.ui.model.ModelEffortOption;
import com.patres.alina.uidesktop.ui.util.FxThreadRunner;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;

import java.util.List;
import java.util.Objects;

/**
 * Owns the model/effort selection state and its composer menu.
 */
final class ModelEffortSelector {

    private final Label modelLabel;
    private final Label effortLabel;
    private final ContextMenu modelMenu = new ContextMenu();
    private final ContextMenu effortMenu = new ContextMenu();

    private volatile String selectedModel;
    private volatile String selectedEffort;
    private List<String> availableModels = List.of();
    private List<String> availableEfforts = List.of();
    private long modelLoadSequence;
    private long effortLoadSequence;
    private boolean userChangedSelection;

    ModelEffortSelector(final Label modelLabel, final Label effortLabel) {
        this.modelLabel = modelLabel;
        this.effortLabel = effortLabel;
    }

    void initialize() {
        modelLabel.setOnMouseClicked(_ -> showModelMenu());
        effortLabel.setOnMouseClicked(_ -> showEffortMenu());

        Thread.startVirtualThread(() -> {
            final AssistantSettings settings = safeSettings();
            final List<String> models = safeModels();
            final String model = resolveModel(settings.resolveModelIdentifier(), models);
            final List<String> efforts = safeEfforts(model);
            final String effort = resolveEffort(settings.effort(), efforts);
            FxThreadRunner.run(() -> {
                if (!userChangedSelection) {
                    applyLoadedSelection(models, efforts, model, effort);
                }
            });
        });
    }

    String selectedModel() {
        return selectedModel;
    }

    String selectedEffort() {
        return selectedEffort;
    }

    void showModelMenu() {
        if (modelMenu.isShowing()) {
            modelMenu.hide();
            return;
        }
        effortLoadSequence++;
        effortMenu.hide();

        final long requestSequence = ++modelLoadSequence;
        Thread.startVirtualThread(() -> {
            final List<String> models = safeModels();
            FxThreadRunner.run(() -> {
                if (requestSequence != modelLoadSequence) {
                    return;
                }
                availableModels = models;
                rebuildModelMenu();
                modelMenu.show(modelLabel, javafx.geometry.Side.TOP, 0, 0);
            });
        });
    }

    private void showEffortMenu() {
        if (effortMenu.isShowing()) {
            effortMenu.hide();
            return;
        }
        modelLoadSequence++;
        modelMenu.hide();

        final String requestedModel = selectedModel;
        final long requestSequence = ++effortLoadSequence;
        Thread.startVirtualThread(() -> {
            final List<String> efforts = safeEfforts(requestedModel);
            FxThreadRunner.run(() -> {
                if (requestSequence != effortLoadSequence || !Objects.equals(requestedModel, selectedModel)) {
                    return;
                }
                availableEfforts = efforts;
                rebuildEffortMenu();
                effortMenu.show(effortLabel, javafx.geometry.Side.TOP, 0, 0);
            });
        });
    }

    private void applyLoadedSelection(final List<String> models,
                                      final List<String> efforts,
                                      final String model,
                                      final String effort) {
        availableModels = models;
        availableEfforts = efforts;
        selectedModel = model;
        selectedEffort = effort;
        updateLabels();
        rebuildMenus();
    }

    private void rebuildMenus() {
        rebuildModelMenu();
        rebuildEffortMenu();
    }

    private void rebuildModelMenu() {
        modelMenu.getItems().clear();
        for (final String model : availableModels) {
            final MenuItem item = new MenuItem(model);
            item.setOnAction(_ -> selectModel(model));
            modelMenu.getItems().add(item);
        }
    }

    private void rebuildEffortMenu() {
        effortMenu.getItems().clear();
        for (final ModelEffortOption option : ModelEffortOption.choices("Default", availableEfforts)) {
            final MenuItem item = new MenuItem(option.label());
            item.setOnAction(_ -> selectEffort(option));
            effortMenu.getItems().add(item);
        }
    }

    private void selectEffort(final ModelEffortOption option) {
        userChangedSelection = true;
        effortLoadSequence++;
        selectedEffort = option.value().isBlank() ? null : option.value();
        updateLabels();
        effortMenu.hide();
    }

    private void selectModel(final String model) {
        userChangedSelection = true;
        selectedModel = model;
        modelLabel.setText(model);
        modelMenu.hide();
        final long requestSequence = ++effortLoadSequence;
        Thread.startVirtualThread(() -> {
            final List<String> efforts = safeEfforts(model);
            FxThreadRunner.run(() -> {
                if (requestSequence != effortLoadSequence || !model.equals(selectedModel)) {
                    return;
                }
                availableEfforts = efforts;
                selectedEffort = resolveEffort(selectedEffort, efforts);
                updateLabels();
                rebuildEffortMenu();
            });
        });
    }

    private void updateLabels() {
        modelLabel.setText(displayModel());
        effortLabel.setText(displayEffort());
    }

    private String displayModel() {
        return selectedModel == null || selectedModel.isBlank() ? "Default" : selectedModel;
    }

    private String displayEffort() {
        return selectedEffort == null || selectedEffort.isBlank()
                ? "Default"
                : ModelEffortOption.humanize(selectedEffort);
    }

    private AssistantSettings safeSettings() {
        try {
            return BackendApi.getAssistantSettings();
        } catch (Exception ignored) {
            return new AssistantSettings();
        }
    }

    private List<String> safeModels() {
        try {
            return BackendApi.getChatModels();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<String> safeEfforts(final String model) {
        try {
            return BackendApi.getChatEfforts(model);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String resolveModel(final String configuredModel, final List<String> models) {
        if (models.isEmpty()) {
            return configuredModel;
        }
        if (models.contains(configuredModel)) {
            return configuredModel;
        }
        final String providerless = stripProvider(configuredModel);
        return models.contains(providerless) ? providerless : models.getFirst();
    }

    private String resolveEffort(final String configuredEffort, final List<String> efforts) {
        if (efforts.isEmpty()) {
            return configuredEffort;
        }
        return efforts.stream()
                .filter(effort -> effort.equalsIgnoreCase(configuredEffort))
                .findFirst()
                .orElse(efforts.getFirst());
    }

    private String stripProvider(final String model) {
        if (model == null || model.isBlank()) {
            return model;
        }
        final int slash = model.indexOf('/');
        return slash >= 0 ? model.substring(slash + 1).trim() : model;
    }

}
