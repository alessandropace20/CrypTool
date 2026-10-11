package code;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Interfaccia grafica Swing per la generazione di "Secure Properties"
 * (criptazione/decriptazione di valori tramite AES), ispirata al tool
 * "MuleSoft Secure Properties Generator".
 *
 * Utilizza la classe SecureAES per le operazioni crittografiche.
 */
public class Main extends JFrame {

    private JComboBox<String> operationCombo;
    private JComboBox<String> algorithmCombo;
    private JComboBox<String> stateCombo;
    private JTextField keyField;
    private JTextField valueField;
    private JCheckBox randomIvCheckBox;
    private JTextArea resultArea;

    public Main() {
        setTitle("Secure Properties Generator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 480);
        setMinimumSize(new Dimension(650, 420));
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 15));
        mainPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

        mainPanel.add(buildHeaderPanel(), BorderLayout.NORTH);
        mainPanel.add(buildFormPanel(), BorderLayout.CENTER);
        mainPanel.add(buildResultPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Secure Properties Generator");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel(
                "<html><div style='text-align:center;'>Strumento per criptare/decriptare "
                        + "proprieta' sicure (secure properties) per la tua applicazione, "
                        + "criptando o decriptando valori con AES.</div></html>");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(Color.DARK_GRAY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setBorder(new EmptyBorder(8, 0, 0, 0));

        header.add(title);
        header.add(subtitle);
        return header;
    }

    private JPanel buildFormPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                new EmptyBorder(20, 20, 20, 20)));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        operationCombo = new JComboBox<>(new String[]{"Encrypt", "Decrypt"});
        algorithmCombo = new JComboBox<>(new String[]{"AES (default)"});
        stateCombo = new JComboBox<>(new String[]{"CBC (default)"});
        keyField = new JTextField();
        valueField = new JTextField();

        // Riga 1: Operation, Algorithm, State
        c.gridx = 0; c.gridy = 0; c.weightx = 1;
        formPanel.add(labeledField("Operation", operationCombo), c);
        c.gridx = 1;
        formPanel.add(labeledField("Algorithm", algorithmCombo), c);
        c.gridx = 2;
        formPanel.add(labeledField("State", stateCombo), c);

        // Riga 2: Key, Value
        c.gridy = 1;
        c.gridx = 0; c.gridwidth = 1;
        formPanel.add(labeledField("Key", keyField), c);
        c.gridx = 1; c.gridwidth = 2;
        formPanel.add(labeledField("Value", valueField), c);
        c.gridwidth = 1;

        // Riga 3: checkbox Use random IV (equivalente al flag --use-random-iv del tool ufficiale)
        randomIvCheckBox = new JCheckBox("Use random IV");
        randomIvCheckBox.setToolTipText(
                "Se disattivato, l'IV viene derivato dalla Key (comportamento di default del tool MuleSoft). "
                        + "Se attivato, un IV casuale viene generato e anteposto al ciphertext.");
        c.gridx = 0; c.gridy = 2; c.gridwidth = 3;
        c.insets = new Insets(4, 6, 4, 6);
        formPanel.add(randomIvCheckBox, c);

        // Riga 4: bottone Generate
        JButton generateButton = new JButton("Generate");
        generateButton.setBackground(new Color(0, 122, 255));
        generateButton.setForeground(Color.BLACK);
        generateButton.setFocusPainted(false);
        generateButton.setFont(new Font("SansSerif", Font.BOLD, 15));
        generateButton.setPreferredSize(new Dimension(100, 42));
        generateButton.addActionListener(e -> onGenerate());

        c.gridx = 0; c.gridy = 3; c.gridwidth = 3;
        c.insets = new Insets(14, 6, 6, 6);
        formPanel.add(generateButton, c);

        return formPanel;
    }

    private JPanel labeledField(String label, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        panel.add(l, BorderLayout.NORTH);
        if (field instanceof JTextField) {
            ((JTextField) field).setColumns(15);
        }
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 30));
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildResultPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                new EmptyBorder(10, 15, 10, 15)));

        JLabel resultLabel = new JLabel("Result", SwingConstants.CENTER);
        resultLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        resultLabel.setBorder(new EmptyBorder(0, 0, 8, 0));

        resultArea = new JTextArea(3, 40);
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        resultArea.setBackground(new Color(248, 248, 248));

        JScrollPane scrollPane = new JScrollPane(resultArea);

        panel.add(resultLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void onGenerate() {
        String operation = (String) operationCombo.getSelectedItem();
        String key = keyField.getText();
        String value = valueField.getText();

        if (key == null || key.isEmpty()) {
            showError("Inserisci una Key valida.");
            return;
        }
        if (value == null || value.isEmpty()) {
            showError("Inserisci un Value valido.");
            return;
        }

        boolean useRandomIv = randomIvCheckBox.isSelected();

        try {
            String result;
            if ("Encrypt".equals(operation)) {
                result = SecureAES.encrypt(key, value, useRandomIv);
            } else {
                result = SecureAES.decrypt(key, value, useRandomIv);
            }
            resultArea.setForeground(Color.BLACK);
            resultArea.setText(result);
        } catch (Exception ex) {
            showError("Errore durante l'operazione: " + ex.getMessage());
        }
    }

    private void showError(String message) {
        resultArea.setForeground(new Color(200, 0, 0));
        resultArea.setText(message);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // se il look and feel di sistema non e' disponibile si usa quello di default
            }
            new Main().setVisible(true);
        });
    }
}