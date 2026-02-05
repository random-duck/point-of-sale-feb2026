import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class SignupPanel extends JPanel {

    public SignupPanel() {
        setLayout(new GridLayout(1, 2)); // Split screen layout

        // --- LEFT PANEL (Branding) ---
        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(Theme.COLOR_CREAM);
        leftPanel.setLayout(new GridBagLayout()); 

        JLabel logoLabel = new JLabel("JOIN US");
        logoLabel.setFont(new Font("Serif", Font.BOLD, 40));
        logoLabel.setForeground(Theme.COLOR_DARK);
        
        JLabel sloganLabel = new JLabel("Create your Muwebles account.");
        sloganLabel.setFont(Theme.FONT_REGULAR);
        sloganLabel.setForeground(Theme.COLOR_GREEN);

        GridBagConstraints gbcLeft = new GridBagConstraints();
        gbcLeft.gridx = 0; gbcLeft.gridy = 0;
        leftPanel.add(logoLabel, gbcLeft);
        gbcLeft.gridy = 1;
        gbcLeft.insets = new Insets(10, 0, 0, 0); 
        leftPanel.add(sloganLabel, gbcLeft);


        // --- RIGHT PANEL (Signup Form) ---
        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(Theme.COLOR_GREEN);
        rightPanel.setLayout(new GridBagLayout());
        rightPanel.setBorder(new EmptyBorder(50, 50, 50, 50));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; 

        // 1. Title
        JLabel title = new JLabel("SIGN UP");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.COLOR_CREAM);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        
        gbc.gridy = 0;
        rightPanel.add(title, gbc);

        // 2. Username
        JLabel userLabel = new JLabel("New Username:");
        userLabel.setForeground(Theme.COLOR_CREAM);
        JTextField userField = new JTextField(20);
        styleField(userField);

        gbc.gridy++; rightPanel.add(userLabel, gbc);
        gbc.gridy++; rightPanel.add(userField, gbc);

        // 3. Password
        JLabel passLabel = new JLabel("New Password:");
        passLabel.setForeground(Theme.COLOR_CREAM);
        JPasswordField passField = new JPasswordField(20);
        styleField(passField);

        gbc.gridy++; rightPanel.add(passLabel, gbc);
        gbc.gridy++; rightPanel.add(passField, gbc);

        // 4. Role Selection
        JLabel roleLabel = new JLabel("Select Role:");
        roleLabel.setForeground(Theme.COLOR_CREAM);
        String[] roles = {"STAFF", "ADMIN"}; // Defaulting to Staff usually safer
        JComboBox<String> roleCombo = new JComboBox<>(roles);
        roleCombo.setBackground(Theme.COLOR_CREAM);

        gbc.gridy++; rightPanel.add(roleLabel, gbc);
        gbc.gridy++; rightPanel.add(roleCombo, gbc);

        // 5. Buttons
        JButton createBtn = new JButton("CREATE ACCOUNT");
        styleButton(createBtn);
        
        JButton backBtn = new JButton("Already have an account? Login");
        backBtn.setBorderPainted(false);
        backBtn.setContentAreaFilled(false);
        backBtn.setForeground(Theme.COLOR_CREAM);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        gbc.gridy++; 
        gbc.insets = new Insets(30, 0, 10, 0); 
        rightPanel.add(createBtn, gbc);
        
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0);
        rightPanel.add(backBtn, gbc);

        // --- LOGIC ---

        // CREATE CLICK
        createBtn.addActionListener((ActionEvent e) -> {
            String u = userField.getText();
            String p = new String(passField.getPassword());
            String role = roleCombo.getSelectedItem().toString();

            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Fields cannot be empty!");
                return;
            }

            // Attempt to register in MongoDB
            boolean success = Database.registerUser(u, p, role);

            if (success) {
                JOptionPane.showMessageDialog(this, "Account Request Sent!\nPlease wait for Admin approval.");
                // Clear fields
                userField.setText("");
                passField.setText("");
                // Go back to login automatically
                Main.getInstance().showScreen("LOGIN");
            } else {
                JOptionPane.showMessageDialog(this, "Username already taken.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // BACK CLICK
        backBtn.addActionListener(e -> {
            Main.getInstance().showScreen("LOGIN");
        });

        add(leftPanel);
        add(rightPanel);
    }

    // Reuse the styling helper
    private void styleField(JTextField field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 16));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.COLOR_CREAM, 0),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        field.setBackground(Theme.COLOR_CREAM);
    }

    private void styleButton(JButton btn) {
        btn.setFont(Theme.FONT_BOLD);
        btn.setBackground(Theme.COLOR_DARK);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
}