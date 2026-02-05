import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import org.bson.Document;

public class LoginPanel extends JPanel {

    public LoginPanel() {
        setLayout(new GridLayout(1, 2)); // Split screen: Left (Logo) | Right (Form)

        // --- LEFT PANEL (Logo Area) ---
        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(Theme.COLOR_CREAM);
        leftPanel.setLayout(new GridBagLayout()); // Centers the logo

        JLabel logoLabel = new JLabel("MUWEBLES");
        logoLabel.setFont(new Font("Serif", Font.BOLD, 40));
        logoLabel.setForeground(Theme.COLOR_DARK);
        
        JLabel sloganLabel = new JLabel("Where Every Piece Counts.");
        sloganLabel.setFont(Theme.FONT_REGULAR);
        sloganLabel.setForeground(Theme.COLOR_GREEN);

        // Add logo and slogan to left panel
        GridBagConstraints gbcLeft = new GridBagConstraints();
        gbcLeft.gridx = 0; gbcLeft.gridy = 0;
        leftPanel.add(logoLabel, gbcLeft);
        gbcLeft.gridy = 1;
        gbcLeft.insets = new Insets(10, 0, 0, 0); // Spacing
        leftPanel.add(sloganLabel, gbcLeft);


        // --- RIGHT PANEL (Form Area) ---
        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(Theme.COLOR_GREEN);
        rightPanel.setLayout(new GridBagLayout());
        rightPanel.setBorder(new EmptyBorder(50, 50, 50, 50)); // Padding around edges

        // GridBagConstraints allows precise positioning
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 10, 0); // Vertical spacing between items
        gbc.fill = GridBagConstraints.HORIZONTAL; // Make text fields stretch
        gbc.gridx = 0; 
        
        // 1. Title
        JLabel title = new JLabel("LOGIN");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.COLOR_CREAM);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        
        gbc.gridy = 0;
        rightPanel.add(title, gbc);

        // 2. Username Field
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(Theme.FONT_REGULAR);
        userLabel.setForeground(Theme.COLOR_CREAM);
        
        JTextField userField = new JTextField(20);
        styleField(userField);

        gbc.gridy++; rightPanel.add(userLabel, gbc);
        gbc.gridy++; rightPanel.add(userField, gbc);

        // 3. Password Field
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(Theme.FONT_REGULAR);
        passLabel.setForeground(Theme.COLOR_CREAM);
        
        JPasswordField passField = new JPasswordField(20);
        styleField(passField);

        gbc.gridy++; rightPanel.add(passLabel, gbc);
        gbc.gridy++; rightPanel.add(passField, gbc);

        // 4. Role Selector
        JLabel roleLabel = new JLabel("Login As:");
        roleLabel.setForeground(Theme.COLOR_CREAM);
        String[] roles = {"ADMIN", "STAFF"};
        JComboBox<String> roleCombo = new JComboBox<>(roles);
        roleCombo.setBackground(Theme.COLOR_CREAM);
        
        gbc.gridy++; rightPanel.add(roleLabel, gbc);
        gbc.gridy++; rightPanel.add(roleCombo, gbc);

        // 5. Buttons (Login & Signup)
        JButton loginBtn = new JButton("LOGIN");
        styleButton(loginBtn);
        
        JButton signupBtn = new JButton("Create an Account");
        signupBtn.setBorderPainted(false);
        signupBtn.setContentAreaFilled(false);
        signupBtn.setForeground(Theme.COLOR_CREAM);
        signupBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        gbc.gridy++; 
        gbc.insets = new Insets(30, 0, 10, 0); // Extra space before button
        rightPanel.add(loginBtn, gbc);
        
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0);
        rightPanel.add(signupBtn, gbc);


        // --- ACTION LISTENERS (The Logic) ---

        // LOGIC: Login Button Click
        loginBtn.addActionListener((ActionEvent e) -> {
            String u = userField.getText();
            String p = new String(passField.getPassword());
            String role = roleCombo.getSelectedItem().toString();

            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all fields.");
                return;
            }

            // Call the Database class!
            Document user = Database.login(u, p);

            if (user != null) {
                // Check if the role matches what they selected
                String dbRole = user.getString("role");
                String status = user.getString("status");

                if (!dbRole.equalsIgnoreCase(role)) {
                     JOptionPane.showMessageDialog(this, "Invalid Role selected for this user.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                } else if ("Pending".equalsIgnoreCase(status)) {
                     JOptionPane.showMessageDialog(this, "Your account is still pending Admin approval.", "Access Denied", JOptionPane.WARNING_MESSAGE);
                } else {
                    // SUCCESS! Switch to Dashboard
                    JOptionPane.showMessageDialog(this, "Welcome back, " + u + "!");
                    Main.getInstance().showScreen("DASHBOARD");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Username or Password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        // LOGIC: Go to Signup Screen
        signupBtn.addActionListener(e -> {
            Main.getInstance().showScreen("SIGNUP");
        });

        // Add panels to the main container
        add(leftPanel);
        add(rightPanel);
    }

    // --- HELPER STYLING METHODS ---
    
    private void styleField(JTextField field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 16));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.COLOR_CREAM, 0), // No outer border
            BorderFactory.createEmptyBorder(10, 10, 10, 10) // Padding inside text box
        ));
        field.setBackground(Theme.COLOR_CREAM);
    }

    private void styleButton(JButton btn) {
        btn.setFont(Theme.FONT_BOLD);
        btn.setBackground(Theme.COLOR_DARK);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0)); // Padding
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
}