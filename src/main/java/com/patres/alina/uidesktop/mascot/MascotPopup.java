package com.patres.alina.uidesktop.mascot;

import com.patres.alina.common.interaction.AgentInteractionApprovalScope;
import com.patres.alina.uidesktop.Resources;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.IllegalComponentStateException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class MascotPopup implements MascotPopupView {

    private static final Logger logger = LoggerFactory.getLogger(MascotPopup.class);

    private static final int SCREEN_MARGIN = 18;
    private static final int TERMINAL_DURATION_MILLIS = 8_000;
    private static final int ENTRANCE_DURATION_MILLIS = 220;
    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
    private static final Runnable NO_ACTION = () -> {
    };

    private final Supplier<MascotPalette> paletteSupplier;
    private final Consumer<Runnable> swingScheduler;
    private final Runnable swingStarter;

    private ImageIcon permissionIcon;
    private ImageIcon completeIcon;
    private boolean assetsAvailable;
    private volatile boolean initialized;
    private volatile boolean closed;
    private MascotPalette palette = MascotPalette.calmDark();

    private JWindow window;
    private JPanel root;
    private RoundedPanel bubble;
    private AnimatedImageLabel mascotImage;
    private JLabel stateLabel;
    private JLabel titleLabel;
    private JTextArea detailArea;
    private JTextArea errorArea;
    private RoundedBadge queueBadge;
    private JPanel actions;
    private RoundedButton approveButton;
    private RoundedButton scopedButton;
    private RoundedButton denyButton;
    private JProgressBar progress;
    private Timer terminalTimer;
    private Timer entranceTimer;
    private Timer bobTimer;

    private Runnable approveAction = NO_ACTION;
    private Runnable approveScopedAction = NO_ACTION;
    private Runnable denyAction = NO_ACTION;
    private Runnable openThreadAction = NO_ACTION;
    private Runnable expiredAction = NO_ACTION;

    MascotPopup() {
        this(
                MascotPalette::calmDark,
                MascotPopup::scheduleOnSwing,
                MascotPopup::startSwingDispatcher
        );
    }

    MascotPopup(final Supplier<MascotPalette> paletteSupplier) {
        this(paletteSupplier, MascotPopup::scheduleOnSwing, MascotPopup::startSwingDispatcher);
    }

    MascotPopup(final Supplier<MascotPalette> paletteSupplier,
                final Consumer<Runnable> swingScheduler,
                final Runnable swingStarter) {
        this.paletteSupplier = Objects.requireNonNull(paletteSupplier);
        this.swingScheduler = Objects.requireNonNull(swingScheduler);
        this.swingStarter = Objects.requireNonNull(swingStarter);
        initializeAssets();
        runSwing(this::initializeWindowSafely);
        this.swingStarter.run();
    }

    @Override
    public void showApproval(final MascotNotification notification,
                             final int remainingCount,
                             final Runnable approveAction,
                             final Optional<Runnable> approveScopedAction,
                             final Runnable denyAction,
                             final Runnable openThreadAction) {
        runSwing(() -> {
            if (!isAvailable()) {
                return;
            }
            terminalTimer.stop();
            applyLayout(MascotPopupLayout.forType(MascotNotificationType.APPROVAL));
            applyPalette();
            setMode(MascotNotificationType.APPROVAL);
            mascotImage.setIcon(permissionIcon);
            stateLabel.setText(LanguageManager.getLanguageString("mascot.state.approval"));
            titleLabel.setText(fallback(notification.title(), "mascot.approval.title"));
            setText(detailArea, notification.message());
            hideResolutionError();
            queueBadge.setText("+" + remainingCount);
            queueBadge.setVisible(remainingCount > 0);
            progress.setVisible(false);
            this.approveAction = approveAction;
            this.approveScopedAction = approveScopedAction.orElse(NO_ACTION);
            this.denyAction = denyAction;
            this.openThreadAction = openThreadAction;
            configureApprovalActions(notification.approvalScope(), approveScopedAction.isPresent());
            setActionsEnabled(true);
            showWindow();
        });
    }

    @Override
    public void showTerminal(final MascotNotification notification,
                             final Runnable openThreadAction,
                             final Runnable expiredAction) {
        runSwing(() -> {
            if (!isAvailable()) {
                return;
            }
            applyLayout(MascotPopupLayout.forType(notification.type()));
            applyPalette();
            final boolean error = notification.type() == MascotNotificationType.ERROR;
            setMode(notification.type());
            mascotImage.setIcon(error ? permissionIcon : completeIcon);
            stateLabel.setText(LanguageManager.getLanguageString(
                    error ? "mascot.state.error" : "mascot.state.complete"
            ));
            titleLabel.setText(LanguageManager.getLanguageString(
                    error ? "mascot.error.title" : "mascot.complete.title"
            ));
            setText(detailArea, MascotTerminalText.detail(notification));
            hideResolutionError();
            queueBadge.setVisible(false);
            progress.setVisible(false);
            actions.setVisible(false);
            this.openThreadAction = openThreadAction;
            this.expiredAction = expiredAction;
            terminalTimer.restart();
            showWindow();
        });
    }

    @Override
    public void showProcessing() {
        runSwing(() -> {
            if (!isAvailable()) {
                return;
            }
            setActionsEnabled(false);
            progress.setVisible(true);
            hideResolutionError();
            refreshLayout();
        });
    }

    @Override
    public void showResolutionError(final String message) {
        runSwing(() -> {
            if (!isAvailable()) {
                return;
            }
            progress.setVisible(false);
            setActionsEnabled(true);
            final String error = message == null || message.isBlank()
                    ? LanguageManager.getLanguageString("mascot.approval.error")
                    : message;
            setText(errorArea, error);
            refreshLayout();
        });
    }

    @Override
    public void hide() {
        runSwing(() -> {
            if (!isAvailable()) {
                return;
            }
            stopAnimations();
            if (window.isVisible()) {
                window.setVisible(false);
            }
        });
    }

    @Override
    public void close() {
        closed = true;
        runSwing(() -> {
            if (!initialized) {
                return;
            }
            stopAnimations();
            window.dispose();
            initialized = false;
        });
    }

    private void initializeAssets() {
        try (InputStream permissionStream = Resources.getResourceAsStream("assets/mascot/alinka-permission.png");
             InputStream completeStream = Resources.getResourceAsStream("assets/mascot/alinka-complete.png")) {
            final BufferedImage permissionImage = ImageIO.read(permissionStream);
            final BufferedImage completeImage = ImageIO.read(completeStream);
            assetsAvailable = permissionImage != null && completeImage != null;
            if (assetsAvailable) {
                final int iconSize = MascotPopupLayout
                        .forType(MascotNotificationType.APPROVAL)
                        .mascotIconSize();
                permissionIcon = new ImageIcon(scaleImage(permissionImage, iconSize));
                completeIcon = new ImageIcon(scaleImage(completeImage, iconSize));
            }
        } catch (Exception e) {
            assetsAvailable = false;
            logger.error("Cannot load Alinka mascot assets", e);
        }
    }

    private void initializeWindowSafely() {
        if (!assetsAvailable || closed) {
            return;
        }
        try {
            initializeWindow();
            initialized = true;
            if (closed) {
                stopAnimations();
                window.dispose();
                initialized = false;
            }
        } catch (RuntimeException | LinkageError e) {
            initialized = false;
            if (window != null) {
                window.dispose();
            }
            logger.error("Cannot initialize Alinka notification window", e);
        }
    }

    private void initializeWindow() {
        window = new JWindow();
        window.setName("Alinka notification");
        window.setType(Window.Type.POPUP);
        window.setFocusableWindowState(false);
        window.setAlwaysOnTop(true);
        window.setBackground(TRANSPARENT);
        final MascotPopupLayout initialLayout = MascotPopupLayout.forType(MascotNotificationType.APPROVAL);
        window.setSize(initialLayout.width(), initialLayout.height());

        root = new JPanel(new BorderLayout());
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(6, 6, 6, 6));

        mascotImage = new AnimatedImageLabel();
        mascotImage.setHorizontalAlignment(SwingConstants.CENTER);
        mascotImage.setVerticalAlignment(SwingConstants.BOTTOM);
        mascotImage.setPreferredSize(new Dimension(initialLayout.mascotWidth(), initialLayout.mascotHeight()));

        bubble = new RoundedPanel();
        bubble.setLayout(new BorderLayout());
        bubble.setBorder(new EmptyBorder(
                initialLayout.bubbleTopPadding(),
                initialLayout.bubbleLeftPadding(),
                initialLayout.bubbleBottomPadding(),
                initialLayout.bubbleRightPadding()
        ));

        final JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        stateLabel = label(Font.BOLD, 10);
        titleLabel = label(Font.BOLD, 15);
        detailArea = textArea(11, 36);
        errorArea = textArea(10, 24);
        errorArea.setVisible(false);

        queueBadge = new RoundedBadge();
        queueBadge.setVisible(false);
        progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setBorderPainted(false);
        progress.setPreferredSize(new Dimension(28, 6));
        progress.setVisible(false);

        final JPanel trailing = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        trailing.setOpaque(false);
        trailing.add(queueBadge);
        trailing.add(progress);

        final JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(stateLabel, BorderLayout.WEST);
        header.add(trailing, BorderLayout.EAST);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        actions = new JPanel();
        actions.setOpaque(false);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        actions.setPreferredSize(new Dimension(300, 31));
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 31));
        approveButton = new RoundedButton();
        scopedButton = new RoundedButton();
        denyButton = new RoundedButton();
        approveButton.addActionListener(_ -> this.approveAction.run());
        scopedButton.addActionListener(_ -> this.approveScopedAction.run());
        denyButton.addActionListener(_ -> this.denyAction.run());

        content.add(header);
        content.add(Box.createVerticalStrut(2));
        content.add(titleLabel);
        content.add(Box.createVerticalStrut(2));
        content.add(detailArea);
        content.add(errorArea);
        content.add(Box.createVerticalGlue());
        content.add(actions);

        bubble.add(content, BorderLayout.CENTER);
        root.add(mascotImage, BorderLayout.WEST);
        root.add(bubble, BorderLayout.CENTER);
        window.setContentPane(root);

        installOpenAction(
                bubble,
                mascotImage,
                content,
                header,
                stateLabel,
                titleLabel,
                detailArea,
                errorArea,
                queueBadge
        );

        terminalTimer = new Timer(TERMINAL_DURATION_MILLIS, _ -> this.expiredAction.run());
        terminalTimer.setRepeats(false);
        bobTimer = new Timer(45, _ -> updateMascotBob());
        applyPalette();
    }

    private void applyLayout(final MascotPopupLayout layout) {
        window.setSize(layout.width(), layout.height());
        mascotImage.setPreferredSize(new Dimension(layout.mascotWidth(), layout.mascotHeight()));
        bubble.setBorder(new EmptyBorder(
                layout.bubbleTopPadding(),
                layout.bubbleLeftPadding(),
                layout.bubbleBottomPadding(),
                layout.bubbleRightPadding()
        ));
        final int detailWidth = Math.max(
                200,
                layout.width()
                        - layout.mascotWidth()
                        - layout.bubbleLeftPadding()
                        - layout.bubbleRightPadding()
                        - 24
        );
        detailArea.setPreferredSize(new Dimension(detailWidth, layout.detailHeight()));
        detailArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, layout.detailHeight()));
    }

    private JLabel label(final int style, final int size) {
        final JLabel label = new JLabel();
        label.setFont(new Font(Font.SANS_SERIF, style, size));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JTextArea textArea(final int size, final int height) {
        final JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, size));
        area.setBorder(BorderFactory.createEmptyBorder());
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        area.setPreferredSize(new Dimension(290, height));
        return area;
    }

    private void configureApprovalActions(final AgentInteractionApprovalScope approvalScope,
                                          final boolean showScopedAction) {
        actions.removeAll();
        actions.setLayout(new GridLayout(1, showScopedAction ? 3 : 2, 6, 0));
        approveButton.setText(LanguageManager.getLanguageString("mascot.approval.allowOnce"));
        actions.add(approveButton);
        if (showScopedAction) {
            final String labelKey = approvalScope == AgentInteractionApprovalScope.PERSISTENT
                    ? "mascot.approval.allowAlways"
                    : "mascot.approval.allowSession";
            scopedButton.setText(LanguageManager.getLanguageString(labelKey));
            actions.add(scopedButton);
        }
        denyButton.setText(LanguageManager.getLanguageString("mascot.approval.deny"));
        actions.add(denyButton);
        actions.setVisible(true);
    }

    private void setActionsEnabled(final boolean enabled) {
        approveButton.setEnabled(enabled);
        scopedButton.setEnabled(enabled);
        denyButton.setEnabled(enabled);
    }

    private void applyPalette() {
        try {
            palette = Objects.requireNonNullElseGet(paletteSupplier.get(), MascotPalette::calmDark);
        } catch (RuntimeException e) {
            logger.debug("Cannot refresh Alinka palette", e);
            palette = MascotPalette.calmDark();
        }
        titleLabel.setForeground(palette.text());
        detailArea.setForeground(palette.mutedText());
        errorArea.setForeground(palette.danger());
        bubble.setColors(palette.surface(), palette.border(), palette.shadow());
        queueBadge.setColors(palette.accentSoft(), palette.accent());
        approveButton.setColors(
                palette.accentStrong(),
                mix(palette.accentStrong(), palette.text(), 0.14),
                null,
                palette.primaryText()
        );
        scopedButton.setColors(
                palette.successSoft(),
                mix(palette.successSoft(), palette.success(), 0.12),
                palette.success(),
                palette.success()
        );
        denyButton.setColors(
                palette.dangerSoft(),
                mix(palette.dangerSoft(), palette.danger(), 0.12),
                palette.danger(),
                palette.danger()
        );
        progress.setBackground(palette.subtleSurface());
        progress.setForeground(palette.accent());
        root.repaint();
    }

    private void installOpenAction(final Component... components) {
        final MouseAdapter listener = new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent event) {
                openThreadAction.run();
            }
        };
        for (Component component : components) {
            component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            component.addMouseListener(listener);
        }
    }

    private void setMode(final MascotNotificationType type) {
        if (type == MascotNotificationType.ERROR) {
            stateLabel.setForeground(palette.danger());
        } else if (type == MascotNotificationType.COMPLETE) {
            stateLabel.setForeground(palette.success());
        } else {
            stateLabel.setForeground(palette.accent());
        }
    }

    private void showWindow() {
        refreshLayout();
        final MascotScreenPositioner.Position position = resolvePosition();
        if (window.isVisible()) {
            stopEntranceAnimation();
            window.setLocation((int) Math.round(position.x()), (int) Math.round(position.y()));
            setOpacity(1.0f);
        } else {
            startEntranceAnimation(position);
        }
        startMascotBob();
    }

    private MascotScreenPositioner.Position resolvePosition() {
        final List<MascotScreenPositioner.ScreenBounds> screens = new ArrayList<>();
        try {
            final Toolkit toolkit = Toolkit.getDefaultToolkit();
            for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
                final GraphicsConfiguration configuration = device.getDefaultConfiguration();
                final Rectangle bounds = configuration.getBounds();
                final Insets insets = toolkit.getScreenInsets(configuration);
                screens.add(new MascotScreenPositioner.ScreenBounds(
                        bounds.getX() + insets.left,
                        bounds.getY() + insets.top,
                        bounds.getWidth() - insets.left - insets.right,
                        bounds.getHeight() - insets.top - insets.bottom
                ));
            }
        } catch (Exception e) {
            logger.debug("Cannot resolve screen bounds for Alinka", e);
        }

        double cursorX = 0;
        double cursorY = 0;
        try {
            final Point cursor = MouseInfo.getPointerInfo().getLocation();
            cursorX = cursor.getX();
            cursorY = cursor.getY();
        } catch (Exception e) {
            logger.debug("Cannot resolve pointer screen for Alinka", e);
        }
        return MascotScreenPositioner.bottomRight(
                screens,
                cursorX,
                cursorY,
                window.getWidth(),
                window.getHeight(),
                SCREEN_MARGIN
        );
    }

    private void startEntranceAnimation(final MascotScreenPositioner.Position position) {
        stopEntranceAnimation();
        final int targetX = (int) Math.round(position.x());
        final int targetY = (int) Math.round(position.y());
        final long startedAt = System.nanoTime();
        window.setLocation(targetX, targetY + 14);
        setOpacity(0.0f);
        window.setVisible(true);
        entranceTimer = new Timer(16, _ -> {
            final double elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000.0;
            final double progress = Math.min(1.0, elapsedMillis / ENTRANCE_DURATION_MILLIS);
            final double eased = 1.0 - Math.pow(1.0 - progress, 3.0);
            window.setLocation(targetX, targetY + (int) Math.round(14.0 * (1.0 - eased)));
            setOpacity((float) eased);
            if (progress >= 1.0) {
                stopEntranceAnimation();
            }
        });
        entranceTimer.start();
    }

    private void setOpacity(final float opacity) {
        try {
            window.setOpacity(opacity);
        } catch (UnsupportedOperationException | IllegalComponentStateException e) {
            logger.debug("Window opacity animation is unavailable", e);
        }
    }

    private void startMascotBob() {
        mascotImage.setAnimationStartedAt(System.nanoTime());
        if (!bobTimer.isRunning()) {
            bobTimer.start();
        }
    }

    private void updateMascotBob() {
        final double seconds = (System.nanoTime() - mascotImage.getAnimationStartedAt()) / 1_000_000_000.0;
        mascotImage.setBobOffset((int) Math.round(-1.5 + 1.5 * Math.sin(seconds * Math.PI)));
    }

    private void stopAnimations() {
        terminalTimer.stop();
        stopEntranceAnimation();
        bobTimer.stop();
        mascotImage.setBobOffset(0);
    }

    private void stopEntranceAnimation() {
        if (entranceTimer != null) {
            entranceTimer.stop();
            entranceTimer = null;
        }
    }

    private void hideResolutionError() {
        errorArea.setText("");
        errorArea.setVisible(false);
    }

    private void setText(final JTextArea area, final String text) {
        final String normalized = text == null ? "" : text.trim();
        area.setText(normalized);
        area.setCaretPosition(0);
        area.setVisible(!normalized.isBlank());
    }

    private void refreshLayout() {
        root.revalidate();
        root.repaint();
        window.validate();
    }

    private String fallback(final String value, final String key) {
        return value == null || value.isBlank() ? LanguageManager.getLanguageString(key) : value;
    }

    private BufferedImage scaleImage(final BufferedImage source, final int size) {
        final BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source.getScaledInstance(size, size, Image.SCALE_SMOOTH), 0, 0, null);
        graphics.dispose();
        return scaled;
    }

    private static Color mix(final Color first, final Color second, final double secondShare) {
        final double firstShare = 1.0 - secondShare;
        return new Color(
                (int) Math.round(first.getRed() * firstShare + second.getRed() * secondShare),
                (int) Math.round(first.getGreen() * firstShare + second.getGreen() * secondShare),
                (int) Math.round(first.getBlue() * firstShare + second.getBlue() * secondShare),
                (int) Math.round(first.getAlpha() * firstShare + second.getAlpha() * secondShare)
        );
    }

    private void runSwing(final Runnable action) {
        swingScheduler.accept(action);
    }

    private static void scheduleOnSwing(final Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }

    private static void startSwingDispatcher() {
        Thread.ofPlatform()
                .name("alinka-swing-bootstrap")
                .daemon(true)
                .start(() -> {
                    try {
                        SwingUtilities.invokeAndWait(NO_ACTION);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        logger.error("Cannot start Alinka Swing dispatcher", e);
                    }
                });
    }

    private boolean isAvailable() {
        return assetsAvailable && initialized && !closed;
    }

    private static final class RoundedPanel extends JPanel {

        private Color backgroundColor = MascotPalette.calmDark().surface();
        private Color borderColor = MascotPalette.calmDark().border();
        private Color shadowColor = MascotPalette.calmDark().shadow();

        private RoundedPanel() {
            setOpaque(false);
        }

        private void setColors(final Color backgroundColor,
                               final Color borderColor,
                               final Color shadowColor) {
            this.backgroundColor = backgroundColor;
            this.borderColor = borderColor;
            this.shadowColor = shadowColor;
            repaint();
        }

        @Override
        protected void paintComponent(final Graphics graphics) {
            final Graphics2D copy = (Graphics2D) graphics.create();
            copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            final Shape shape = bubbleShape(getWidth(), getHeight());
            copy.setColor(shadowColor);
            copy.fill(AffineTransform.getTranslateInstance(2, 5).createTransformedShape(shape));
            copy.setColor(backgroundColor);
            copy.fill(shape);
            copy.setColor(borderColor);
            copy.setStroke(new BasicStroke(1.0f));
            copy.draw(shape);
            copy.dispose();
            super.paintComponent(graphics);
        }

        private Shape bubbleShape(final int width, final int height) {
            final int tailCenterY = Math.max(26, height - 34);
            final Area shape = new Area(new RoundRectangle2D.Double(
                    10,
                    1,
                    Math.max(1, width - 14),
                    Math.max(1, height - 9),
                    22,
                    22
            ));
            shape.add(new Area(new Polygon(
                    new int[]{2, 14, 14},
                    new int[]{tailCenterY, tailCenterY - 12, tailCenterY + 13},
                    3
            )));
            return shape;
        }
    }

    private static final class AnimatedImageLabel extends JLabel {

        private int bobOffset;
        private long animationStartedAt;

        private void setBobOffset(final int bobOffset) {
            this.bobOffset = bobOffset;
            repaint();
        }

        private long getAnimationStartedAt() {
            return animationStartedAt;
        }

        private void setAnimationStartedAt(final long animationStartedAt) {
            this.animationStartedAt = animationStartedAt;
        }

        @Override
        protected void paintComponent(final Graphics graphics) {
            final Graphics2D copy = (Graphics2D) graphics.create();
            copy.translate(0, bobOffset);
            super.paintComponent(copy);
            copy.dispose();
        }
    }

    private static final class RoundedBadge extends JLabel {

        private Color backgroundColor = MascotPalette.calmDark().accentSoft();

        private RoundedBadge() {
            setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(new EmptyBorder(2, 6, 2, 6));
        }

        private void setColors(final Color backgroundColor, final Color foregroundColor) {
            this.backgroundColor = backgroundColor;
            setForeground(foregroundColor);
            repaint();
        }

        @Override
        protected void paintComponent(final Graphics graphics) {
            final Graphics2D copy = (Graphics2D) graphics.create();
            copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            copy.setColor(backgroundColor);
            copy.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            copy.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class RoundedButton extends JButton {

        private Color background = TRANSPARENT;
        private Color hoverBackground = TRANSPARENT;
        private Color outline;
        private boolean hovered;

        private RoundedButton() {
            setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            setBorder(new EmptyBorder(6, 6, 6, 6));
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setFocusable(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent event) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(final MouseEvent event) {
                    hovered = false;
                    repaint();
                }
            });
        }

        private void setColors(final Color background,
                               final Color hoverBackground,
                               final Color outline,
                               final Color foreground) {
            this.background = background;
            this.hoverBackground = hoverBackground;
            this.outline = outline;
            setForeground(foreground);
            repaint();
        }

        @Override
        protected void paintComponent(final Graphics graphics) {
            final Graphics2D copy = (Graphics2D) graphics.create();
            copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (!isEnabled()) {
                copy.setComposite(AlphaComposite.SrcOver.derive(0.5f));
            }
            copy.setColor(hovered && isEnabled() ? hoverBackground : background);
            copy.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            if (outline != null) {
                copy.setColor(outline);
                copy.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            }
            copy.dispose();
            super.paintComponent(graphics);
        }
    }
}
