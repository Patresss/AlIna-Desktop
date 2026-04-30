package com.patres.alina.uidesktop.ui.dashboard;

import com.patres.alina.common.event.CalendarAiPromptEvent;
import com.patres.alina.common.event.Event;
import com.patres.alina.server.command.CommandConstants;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Shared factory for the AI prompt button used across all dashboard widgets.
 * The button is hidden by default and revealed on row hover via CSS
 * ({@code .workspace-calendar-ai-button} / parent {@code :hover} rule).
 */
public final class DashboardAiButton {

    private static final String STYLE_AI_ICON = "workspace-calendar-ai-icon";
    private static final String STYLE_AI_BUTTON = "workspace-calendar-ai-button";
    private static final int SLOT_WIDTH = 16;

    private DashboardAiButton() {
        // Utility class
    }

    /**
     * Creates an AI button or an empty placeholder of the same width.
     *
     * @param promptTemplate the raw prompt template from settings (may be empty)
     * @param arguments      the context-specific arguments to substitute for {@code $ARGUMENTS}
     * @return a {@link Region} – either a clickable AI label or a fixed-width placeholder
     */
    public static Region createSlot(final String promptTemplate, final String arguments) {
        if (promptTemplate == null || promptTemplate.isEmpty()) {
            return createPlaceholder();
        }
        final FontIcon aiIcon = new FontIcon(Feather.CPU);
        aiIcon.getStyleClass().add(STYLE_AI_ICON);
        final Label aiButton = new Label();
        aiButton.setGraphic(aiIcon);
        aiButton.getStyleClass().add(STYLE_AI_BUTTON);
        aiButton.setOnMouseClicked(e -> {
            final String resolved = resolvePrompt(promptTemplate, arguments);
            Event.publish(new CalendarAiPromptEvent(resolved));
        });
        return aiButton;
    }

    /**
     * Replaces {@code $ARGUMENTS} in the template, or appends arguments after a newline
     * if the placeholder is missing.
     */
    static String resolvePrompt(final String template, final String arguments) {
        if (template.contains(CommandConstants.ARGUMENTS_PLACEHOLDER)) {
            return template.replace(CommandConstants.ARGUMENTS_PLACEHOLDER, arguments);
        }
        return template + "\n" + arguments;
    }

    private static Region createPlaceholder() {
        final Region placeholder = new Region();
        placeholder.setMinWidth(SLOT_WIDTH);
        placeholder.setPrefWidth(SLOT_WIDTH);
        placeholder.setMaxWidth(SLOT_WIDTH);
        return placeholder;
    }
}
