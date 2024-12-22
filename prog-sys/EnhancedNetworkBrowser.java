import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

public class EnhancedNetworkBrowser extends JFrame {
    private JEditorPane displayArea;
    private JTextField addressBar;
    private JButton goButton;
    private JButton backButton;
    private JButton forwardButton;
    private JButton refreshButton;
    private JButton homeButton;
    private JProgressBar loadingProgress;
    private JLabel statusLabel;
    private List<String> history = new ArrayList<>();
    private int currentHistoryIndex = -1;
    private static String protocol = "http://";
    private static final String HOME_PAGE = protocol + "";
    private static final Color TOOLBAR_GRADIENT_START = new Color(60, 63, 65);
    private static final Color TOOLBAR_GRADIENT_END = new Color(43, 43, 43);
    private static final Color BUTTON_HOVER_COLOR = new Color(70, 73, 75);
    private static final Color BUTTON_PRESSED_COLOR = new Color(50, 53, 55);

    public EnhancedNetworkBrowser() {
        setTitle("Enhanced Network Browser");
        setSize(1024, 768);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(new Color(245, 245, 245));

        JPanel toolbarPanel = createToolbarPanel();
        JPanel addressPanel = createAddressPanel();

        JPanel navigationPanel = new JPanel(new BorderLayout());
        navigationPanel.setBackground(TOOLBAR_GRADIENT_START);
        navigationPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        navigationPanel.add(toolbarPanel, BorderLayout.NORTH);
        navigationPanel.add(addressPanel, BorderLayout.SOUTH);

        setupDisplayArea();
        JPanel statusBar = createStatusBar();

        mainContainer.add(navigationPanel, BorderLayout.NORTH);
        mainContainer.add(new JScrollPane(displayArea), BorderLayout.CENTER);
        mainContainer.add(statusBar, BorderLayout.SOUTH);

        setContentPane(mainContainer);
        setupActionListeners();
        updateNavigationButtons();
        showHomePage();
    }

    private JPanel createToolbarPanel() {
        JPanel toolbarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                int w = getWidth();
                int h = getHeight();
                GradientPaint gp = new GradientPaint(
                        0, 0, TOOLBAR_GRADIENT_START,
                        0, h, TOOLBAR_GRADIENT_END);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
                g2d.dispose();
            }
        };
        toolbarPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 2));
        toolbarPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        toolbarPanel.setPreferredSize(new Dimension(getWidth(), 50));

        backButton = createToolbarButton("[b]", "Back");
        forwardButton = createToolbarButton("[f]", "Forward");
        refreshButton = createToolbarButton("[r]", "Refresh");
        homeButton = createToolbarButton("[h]", "Home");

        toolbarPanel.add(backButton);
        toolbarPanel.add(forwardButton);
        toolbarPanel.add(refreshButton);
        toolbarPanel.add(homeButton);

        return toolbarPanel;
    }

    private JButton createToolbarButton(String text, String tooltip) {
        JButton button = new JButton(text) {
            {
                setContentAreaFilled(false);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Background
                if (getModel().isPressed()) {
                    g2d.setColor(BUTTON_PRESSED_COLOR);
                } else if (getModel().isRollover()) {
                    g2d.setColor(BUTTON_HOVER_COLOR);
                } else {
                    g2d.setColor(new Color(0, 0, 0, 0));
                }
                g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 5, 5);

                // Text
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 18));
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2d.drawString(text, x, y);

                g2d.dispose();
            }
        };

        button.setPreferredSize(new Dimension(40, 40));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setToolTipText(tooltip);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
    }

    private JPanel createAddressPanel() {
        JPanel addressPanel = new JPanel(new BorderLayout(5, 0));
        addressPanel.setBackground(TOOLBAR_GRADIENT_END);
        addressPanel.setBorder(new EmptyBorder(5, 5, 10, 5));

        addressBar = new JTextField();
        addressBar.setPreferredSize(new Dimension(0, 30));
        addressBar.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(70, 73, 75)),
                new EmptyBorder(5, 5, 5, 5)));
        addressBar.setFont(new Font("Arial", Font.PLAIN, 14));
        addressBar.setBackground(new Color(50, 53, 55));
        addressBar.setForeground(Color.WHITE);
        addressBar.setCaretColor(Color.WHITE);

        goButton = new JButton("Go") {
            {
                setContentAreaFilled(false);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2d.setColor(new Color(41, 128, 185));
                } else if (getModel().isRollover()) {
                    g2d.setColor(new Color(52, 152, 219));
                } else {
                    g2d.setColor(new Color(41, 128, 185));
                }

                g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 5, 5);

                // Text
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 14));
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth("Go")) / 2;
                int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2d.drawString("Go", x, y);

                g2d.dispose();
            }
        };
        goButton.setPreferredSize(new Dimension(50, 30));
        goButton.setFocusPainted(false);
        goButton.setBorderPainted(false);
        goButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        addressPanel.add(addressBar, BorderLayout.CENTER);
        addressPanel.add(goButton, BorderLayout.EAST);

        return addressPanel;
    }

    private void setupDisplayArea() {
        displayArea = new JEditorPane();
        displayArea.setEditable(false);
        displayArea.setContentType("text/html");
        displayArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        displayArea.setFont(new Font("Arial", Font.PLAIN, 14));
        displayArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        displayArea.setBackground(Color.WHITE);

        displayArea.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                navigateTo(e.getURL().toString());
            } else if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ENTERED) {
                statusLabel.setText(e.getURL().toString());
                displayArea.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.EXITED) {
                statusLabel.setText(" ");
                displayArea.setCursor(Cursor.getDefaultCursor());
            }
        });
    }

    private static final String WELCOME_HTML = "<html>" +
            "<head>" +
            "<style>" +
            "body { font-family: Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 40px; }" +
            ".container { max-width: 800px; margin: 0 auto; background: white; padding: 40px; " +
            "border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
            "h1 { color: #2c3e50; text-align: center; margin-bottom: 30px; }" +
            "p { color: #34495e; line-height: 1.6; font-size: 16px; text-align: center; }" +
            ".highlight { color: #3498db; font-weight: bold; }" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<div class='container'>" +
            "<h1>Bienvenue dans votre navigateur</h1>" +
            "<p>Pour commencer, <span class='highlight'>connectez-vous au serveur</span> en utilisant la barre d'adresse ci-dessus.</p>"
            +
            "</div>" +
            "</body>" +
            "</html>";

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(new EmptyBorder(3, 5, 3, 5));
        statusBar.setBackground(new Color(240, 240, 240));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        loadingProgress = new JProgressBar();
        loadingProgress.setPreferredSize(new Dimension(100, 15));
        loadingProgress.setVisible(false);

        statusBar.add(statusLabel, BorderLayout.CENTER);
        statusBar.add(loadingProgress, BorderLayout.EAST);

        return statusBar;
    }

    private void setupActionListeners() {
        goButton.addActionListener(e -> navigateTo(addressBar.getText()));
        backButton.addActionListener(e -> navigateBack());
        forwardButton.addActionListener(e -> navigateForward());
        refreshButton.addActionListener(e -> refreshPage());
        homeButton.addActionListener(e -> showHomePage());
        addressBar.addActionListener(e -> navigateTo(addressBar.getText()));
    }

    private void navigateTo(String urlString) {
        loadingProgress.setVisible(true);
        loadingProgress.setIndeterminate(true);
        statusLabel.setText("Loading " + urlString + "...");

        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                try {
                    URL url = new URL(urlString);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);

                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line).append("\n");
                        }
                        return response.toString();
                    }
                } catch (Exception e) {
                    return "Error: " + e.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String content = get();
                    displayArea.setText(content);
                    updateHistory(urlString);
                } catch (Exception e) {
                    displayArea.setText("Error: " + e.getMessage());
                }
                loadingProgress.setVisible(false);
                statusLabel.setText(" ");
            }
        };

        worker.execute();
    }

    private void showHomePage() {
        displayArea.setContentType("text/html");
        displayArea.setText(WELCOME_HTML);
        updateHistory("home");
        addressBar.setText(HOME_PAGE);
    }

    private void navigateBack() {
        if (currentHistoryIndex > 0) {
            currentHistoryIndex--;
            addressBar.setText(history.get(currentHistoryIndex));
            navigateTo(history.get(currentHistoryIndex));
        }
    }

    private void navigateForward() {
        if (currentHistoryIndex < history.size() - 1) {
            currentHistoryIndex++;
            addressBar.setText(history.get(currentHistoryIndex));
            navigateTo(history.get(currentHistoryIndex));
        }
    }

    private void refreshPage() {
        if (!history.isEmpty()) {
            navigateTo(addressBar.getText());
        }
    }

    private void updateHistory(String urlString) {
        if (currentHistoryIndex < history.size() - 1) {
            history = history.subList(0, currentHistoryIndex + 1);
        }
        history.add(urlString);
        currentHistoryIndex++;
        updateNavigationButtons();
    }

    private void updateNavigationButtons() {
        backButton.setEnabled(currentHistoryIndex > 0);
        forwardButton.setEnabled(currentHistoryIndex < history.size() - 1);
        refreshButton.setEnabled(!history.isEmpty());
        homeButton.setEnabled(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EnhancedNetworkBrowser browser = new EnhancedNetworkBrowser();
            browser.setVisible(true);
        });
    }
}