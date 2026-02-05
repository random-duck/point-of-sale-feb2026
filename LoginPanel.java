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
        leftPanel.setLayout(new GridBagLayout());

        JLabel logoLabel = new JLabel("MUWEBLES");
        logoLabel.setFont(new Font("Serif", Font.BOLD, 40));
        logoLabel.setForeground(Theme.COLOR_DARK);
        
        JLabel sloganLabel = new JLabel("Where Every Piece Counts.");
        sloganLabel.setFont(Theme.FONT_REGULAR);
        sloganLabel.setForeground(Theme.COLOR_GREEN);

        GridBagConstraints gbcLeft = new GridBagConstraints();
        gbcLeft.gridx = 0; gbcLeft.gridy = 0;
        leftPanel.add(logoLabel, gbcLeft);
        gbcLeft.gridy = 1;
        gbcLeft.insets = new Insets(10, 0, 0, 0); 
        leftPanel.add(sloganLabel, gbcLeft);


        // --- RIGHT PANEL (Form Area) ---
        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(Theme.COLOR_GREEN);
        rightPanel.setLayout(new GridBagLayout());
        rightPanel.setBorder(new EmptyBorder(50, 50, 50, 50)); 

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 10, 0); 
        gbc.fill = GridBagConstraints.HORIZONTAL; 
        gbc.gridx = 0; 
        
        // 1. Title
        JLabel title = new JLabel("LOGIN");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.COLOR_CREAM);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        
        gbc.gridy = 0;
        rightPanel.add(title, gbc);

        // 2. Username
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(Theme.FONT_REGULAR);
        userLabel.setForeground(Theme.COLOR_CREAM);
        JTextField userField = new JTextField(20);
        styleField(userField);

        gbc.gridy++; rightPanel.add(userLabel, gbc);
        gbc.gridy++; rightPanel.add(userField, gbc);

        // 3. Password
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(Theme.FONT_REGULAR);
        passLabel.setForeground(Theme.COLOR_CREAM);
        JPasswordField passField = new JPasswordField(20);
        styleField(passField);

        gbc.gridy++; rightPanel.add(passLabel, gbc);
        gbc.gridy++; rightPanel.add(passField, gbc);

        // REMOVED: The Role Dropdown (It was causing the confusion)

        // 4. Buttons
        JButton loginBtn = new JButton("LOGIN");
        styleButton(loginBtn);
        
        JButton signupBtn = new JButton("Create an Account");
        signupBtn.setBorderPainted(false);
        signupBtn.setContentAreaFilled(false);
        signupBtn.setForeground(Theme.COLOR_CREAM);
        signupBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        gbc.gridy++; 
        gbc.insets = new Insets(30, 0, 10, 0); 
        rightPanel.add(loginBtn, gbc);
        
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0);
        rightPanel.add(signupBtn, gbc);


        // --- LOGIC ---
        loginBtn.addActionListener((ActionEvent e) -> {
            String u = userField.getText();
            String p = new String(passField.getPassword());

            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all fields.");
                return;
            }

            // We trust the database to tell us the role
            Document user = Database.login(u, p);

            if (user != null) {
                String status = user.getString("status");
                if ("Pending".equals(status)) {
                    JOptionPane.showMessageDialog(this, "Account pending approval.");
                } else {
                    Session.login(user); 
                    Main.getInstance().showScreen("DASHBOARD");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Username or Password");
            }
        });

        signupBtn.addActionListener(e -> {
            Main.getInstance().showScreen("SIGNUP");
        });

        add(leftPanel);
        add(rightPanel);
    }

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