package com.patres.alina.uidesktop.logs;

import atlantafx.base.theme.Styles;
import com.patres.alina.common.logs.LogsResponse;
import com.patres.alina.uidesktop.settings.ui.ApplicationModalPaneContent;
import javafx.scene.Node;
import javafx.scene.control.TextArea;

import java.util.List;

import static com.patres.alina.uidesktop.backend.AlinaRestApi.getAlinaRestClient;
import static com.patres.alina.uidesktop.ui.util.TranslatedComponentUtils.createTextSeparator;

public class LogsPane extends ApplicationModalPaneContent {

    public static final double JAVAFX_PANE_WORKAROUND_HEIGHT_FULL = 100000.0;
    private TextArea logsTextArea;

    public LogsPane(Runnable backFunction) {
        super(backFunction);
    }

    @Override
    public void reload() {
        final LogsResponse logs = getAlinaRestClient().getLogs();
        logsTextArea.clear();
        logs.lines().forEach(log -> logsTextArea.appendText(log + "\n"));
        logsTextArea.setPrefHeight(JAVAFX_PANE_WORKAROUND_HEIGHT_FULL);
        logsTextArea.positionCaret(logsTextArea.getText().length());
    }


    @Override
    protected List<Node> generateContent() {
        var header = createTextSeparator("logs.title", Styles.TITLE_3);
        logsTextArea = new TextArea();
        logsTextArea.setEditable(false);
        logsTextArea.getStyleClass().add("logs-text-area");
        return List.of(header, logsTextArea);
    }
}
