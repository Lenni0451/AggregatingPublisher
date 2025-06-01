package net.lenni0451.aggregatingpublisher.swingui;

import lombok.extern.slf4j.Slf4j;
import net.lenni0451.aggregatingpublisher.services.PublisherService;
import net.lenni0451.commons.swing.GBC;
import net.lenni0451.commons.swing.components.ScrollPaneSizedPanel;
import net.lenni0451.commons.swing.utils.SwingTheming;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class Window extends JFrame {

    private static final Window instance;

    static {
        SwingTheming.setSystemLookAndFeel();
        instance = new Window();
    }

    public static void open() {
        SwingUtilities.invokeLater(() -> instance.setVisible(true));
    }

    public static void addFile(final String path) {
        SwingUtilities.invokeLater(() -> {
            DefaultListModel<String> model = (DefaultListModel<String>) instance.filesList.getModel();
            model.addElement(path);
        });
    }

    public static void clearFiles() {
        SwingUtilities.invokeLater(() -> {
            DefaultListModel<String> model = (DefaultListModel<String>) instance.filesList.getModel();
            model.clear();
        });
    }


    private final ExecutorService executorService = Executors.newWorkStealingPool();
    private JList<String> filesList;

    private Window() {
        super("Aggregating Publisher");
        this.setIconImage(new ImageIcon(this.getClass().getResource("/icon.png")).getImage());
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setSize(600, 400);
        this.setMinimumSize(this.getSize());
        this.setLocationRelativeTo(null);
        this.init();
    }

    private void init() {
        JSplitPane root = new JSplitPane();
        root.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        root.setResizeWeight(0.6);
        this.setContentPane(root);

        JScrollPane left = new JScrollPane();
        this.filesList = new JList<>();
        this.filesList.setModel(new DefaultListModel<>());
        left.setViewportView(this.filesList);
        root.setTopComponent(left);

        JPanel right = new JPanel();
        right.setLayout(new GridBagLayout());
        root.setBottomComponent(right);
        GBC.create(right).nextRow().fill(GBC.BOTH).weight(1, 1).add(new JScrollPane(), scrollPane -> {
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            ScrollPaneSizedPanel buttonPanel = new ScrollPaneSizedPanel(scrollPane);
            scrollPane.setViewportView(buttonPanel);
            buttonPanel.setLayout(new GridBagLayout());
            for (PublisherService publisherService : Main.aggregatingPublisher.getPublisherServices()) {
                GBC.create(buttonPanel).nextRow().insets(5, 5, 0, 5).fill(GBC.HORIZONTAL).weightx(1).add(new JButton(publisherService.getName()), button -> {
                    button.addActionListener(e -> {
                        if (Main.fileCollector.isEmpty()) return;
                        button.setEnabled(false);
                        this.executorService.execute(() -> {
                            try {
                                publisherService.publish(Main.fileCollector.getFiles());
                            } catch (Throwable t) {
                                log.error("An error occurred while publishing files to {}", publisherService.getName(), t);
                                Popup.showException("An error occurred while publishing files to " + publisherService.getName(), t);
                            } finally {
                                button.setEnabled(true);
                            }
                        });
                    });
                });
            }
            GBC.fillVerticalSpace(buttonPanel);
        });
        GBC.create(right).nextRow().insets(5).fill(GBC.HORIZONTAL).weightx(1).add(new JButton("Clear and close"), button -> {
            button.addActionListener(e -> {
                Main.fileCollector.clearFiles();
                this.dispose();
            });
        });
    }

}
