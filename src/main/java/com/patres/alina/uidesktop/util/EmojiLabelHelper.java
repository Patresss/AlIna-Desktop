package com.patres.alina.uidesktop.util;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders Label text with color emoji on macOS by splitting content into
 * regular {@link Text} nodes (using the Label's CSS font) and emoji
 * {@link Text} nodes (using "Apple Color Emoji"). The resulting
 * {@link TextFlow} is set as the Label's graphic.
 * <p>
 * When no emoji are detected the Label is used normally — no graphic overhead.
 */
public final class EmojiLabelHelper {

    private static final String EMOJI_FONT = "Apple Color Emoji";
    private static final String EMOJI_MARKER = "emoji";
    private static final String CLIP_PROP = "emoji-label-clip";
    private static final String FLOW_PROP = "emoji-label-flow";
    private static final String NODES_PROP = "emoji-label-nodes";
    private static final String GROUP_PROP = "emoji-label-group";
    private static final String EXPANDED_PROP = "emoji-label-expanded";

    /** Same ranges as {@code MarkdownToHtmlConverter.EMOJI_PATTERN}, plus variation selectors. */
    private static final Pattern EMOJI_PATTERN = Pattern.compile(
            "(" +
            "[\\x{1F600}-\\x{1F64F}" +
            "\\x{1F300}-\\x{1F5FF}" +
            "\\x{1F680}-\\x{1F6FF}" +
            "\\x{1F900}-\\x{1F9FF}" +
            "\\x{1FA00}-\\x{1FA6F}" +
            "\\x{1FA70}-\\x{1FAFF}" +
            "\\x{2600}-\\x{26FF}" +
            "\\x{2700}-\\x{27BF}" +
            "\\x{200D}" +
            "\\x{20E3}" +
            "\\x{FE0F}" +
            "\\x{FE0E}" +
            "\\x{1F1E0}-\\x{1F1FF}" +
            "]+" +
            ")"
    );

    private EmojiLabelHelper() { }

    /**
     * Sets the label's text with color emoji support. If the text contains
     * emoji, a {@link TextFlow} graphic replaces the label text; otherwise
     * the label is used normally.
     * <p>
     * The TextFlow is wrapped in a {@link Group} so that it lays out as a
     * single unbroken line (Group does not impose width constraints on its
     * children). A clip {@link Rectangle} on the Group hides any horizontal
     * overflow, mimicking the ellipsis/truncation look of a normal Label.
     */
    public static void applyEmojiText(final Label label, final String text) {
        if (text == null || text.isEmpty() || !EMOJI_PATTERN.matcher(text).find()) {
            label.setText(text);
            return;
        }

        final List<Text> nodes = buildTextNodes(text, label.getFont());
        final TextFlow flow = new TextFlow(nodes.toArray(Node[]::new));

        // Bind non-emoji fill to the label's CSS text-fill
        for (final Text node : nodes) {
            if (!EMOJI_MARKER.equals(node.getUserData())) {
                node.fillProperty().bind(label.textFillProperty());
            }
        }

        // Keep fonts in sync when CSS is applied or theme changes
        label.fontProperty().addListener((obs, oldFont, newFont) -> syncFonts(nodes, newFont));

        // Wrap in Group — Group does NOT constrain child width, so TextFlow
        // stays as a single long line and never wraps.
        final Group wrapper = new Group(flow);

        // Clip: width follows label, height is one text line
        final Rectangle clip = new Rectangle();
        clip.widthProperty().bind(label.widthProperty());
        clip.setHeight(computeLineHeight(label.getFont()));
        wrapper.setClip(clip);

        // Update clip height when font/size changes
        label.fontProperty().addListener((obs, old, f) -> {
            if (wrapper.getClip() != null) {
                clip.setHeight(computeLineHeight(f));
            }
        });

        label.setText(null);
        label.setGraphic(wrapper);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        label.setMinWidth(0);

        // Store references for toggleWrap
        label.getProperties().put(CLIP_PROP, clip);
        label.getProperties().put(FLOW_PROP, flow);
        label.getProperties().put(NODES_PROP, nodes);
        label.getProperties().put(GROUP_PROP, wrapper);
    }

    /**
     * Toggles between clipped single-line and wrapped multi-line display.
     * Works for both emoji-enhanced labels and plain labels.
     * <p>
     * For emoji labels, expanding swaps the Label out of its parent container
     * and inserts the TextFlow directly. This lets the layout engine compute
     * the multi-line height naturally, since TextFlow is a Region that
     * properly participates in parent layout (unlike a Label's graphic).
     * Collapsing reverses the swap: the TextFlow goes back into the Group
     * wrapper inside the Label, and the Label is re-inserted into the parent.
     */
    @SuppressWarnings("unchecked")
    public static void toggleWrap(final Label label) {
        final TextFlow flow = (TextFlow) label.getProperties().get(FLOW_PROP);
        if (flow == null) {
            // Plain label without emoji — simple wrapText toggle, no extra constraints needed.
            label.setWrapText(!label.isWrapText());
            return;
        }

        final boolean isExpanded = Boolean.TRUE.equals(label.getProperties().get(EXPANDED_PROP));

        if (!isExpanded) {
            // ── Expand: swap Label → TextFlow in the parent Pane ──
            final Parent parent = label.getParent();
            if (parent instanceof Pane pane) {
                final int index = pane.getChildren().indexOf(label);
                if (index >= 0) {
                    // Detach flow from Group (it can only have one parent)
                    final Group wrapper = (Group) label.getProperties().get(GROUP_PROP);
                    if (wrapper != null) {
                        wrapper.getChildren().remove(flow);
                    }

                    // Copy the label's style class and padding to the TextFlow
                    flow.setPadding(label.getPadding());
                    flow.setMaxWidth(Double.MAX_VALUE);

                    // Transfer the mouse click handler so the user can collapse by clicking
                    flow.setOnMouseClicked(label.getOnMouseClicked());

                    // Insert TextFlow where the Label was
                    pane.getChildren().set(index, flow);
                    if (pane instanceof HBox) {
                        HBox.setHgrow(flow, Priority.ALWAYS);
                    }

                    flow.setCursor(javafx.scene.Cursor.HAND);
                    label.getProperties().put(EXPANDED_PROP, Boolean.TRUE);
                }
            }
        } else {
            // ── Collapse: swap TextFlow → Label back ──
            final Parent parent = flow.getParent();
            if (parent instanceof Pane pane) {
                final int index = pane.getChildren().indexOf(flow);
                if (index >= 0) {
                    // Remove click handler from flow
                    flow.setOnMouseClicked(null);

                    // Put TextFlow back into the Group wrapper for single-line clip
                    final Group wrapper = (Group) label.getProperties().get(GROUP_PROP);
                    if (wrapper != null) {
                        // Remove flow from current parent first (Pane)
                        pane.getChildren().remove(index);
                        wrapper.getChildren().setAll(flow);
                        final Rectangle clip = (Rectangle) label.getProperties().get(CLIP_PROP);
                        if (clip != null) {
                            clip.setHeight(computeLineHeight(label.getFont()));
                        }
                        label.setGraphic(wrapper);
                    }

                    // Re-insert label where the TextFlow was
                    pane.getChildren().add(index, label);
                    if (pane instanceof HBox) {
                        HBox.setHgrow(label, Priority.ALWAYS);
                    }

                    label.getProperties().put(EXPANDED_PROP, Boolean.FALSE);
                }
            }
        }
    }

    // ── internals ────────────────────────────────────────────────

    private static double computeLineHeight(final Font font) {
        final Text measure = new Text("Xg\u2600");
        measure.setFont(font);
        return Math.ceil(measure.getBoundsInLocal().getHeight()) + 2;
    }

    private static List<Text> buildTextNodes(final String text, final Font baseFont) {
        final List<Text> nodes = new ArrayList<>();
        final Matcher matcher = EMOJI_PATTERN.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                final Text t = new Text(text.substring(lastEnd, matcher.start()));
                t.setFont(baseFont);
                nodes.add(t);
            }
            final Text emoji = new Text(matcher.group());
            emoji.setFont(Font.font(EMOJI_FONT, baseFont.getSize()));
            emoji.setUserData(EMOJI_MARKER);
            nodes.add(emoji);
            lastEnd = matcher.end();
        }

        if (lastEnd < text.length()) {
            final Text t = new Text(text.substring(lastEnd));
            t.setFont(baseFont);
            nodes.add(t);
        }
        return nodes;
    }

    private static void syncFonts(final List<Text> nodes, final Font newFont) {
        for (final Text node : nodes) {
            if (EMOJI_MARKER.equals(node.getUserData())) {
                node.setFont(Font.font(EMOJI_FONT, newFont.getSize()));
            } else {
                node.setFont(newFont);
            }
        }
    }
}
