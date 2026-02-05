import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
public class main {
//========== [DATABASE CREDENTIALS] ==========
    static final String URL = "jdbc:mysql://localhost:3306/inventory_db";
    static final String USER = "root";
    static final String PASS = "root";
    public static boolean signupLogin(String username, String password) {
        boolean valid = false;
        try (Connection con = DriverManager.getConnection(URL, USER, PASS)) {
            String sql = "SELECT * FROM users WHERE username=? AND password=? AND role='Admin' AND status='Approved'";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, username);
            pst.setString(2, password);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) valid = true;
        } catch (Exception e) { e.printStackTrace(); }
        return valid;
    }
    public static boolean checkLogin(String username, String password, String role) {
    boolean valid = false;
    try {
        Connection con = DriverManager.getConnection(URL, USER, PASS);
        String sql = "SELECT * FROM users WHERE username=? AND password=? AND role=?";
        PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, username);
            pst.setString(2, password);
            pst.setString(3, role);
        ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                valid = true;
            }con.close();
    } catch (Exception e) {
            e.printStackTrace();
    }
    return valid;
}
//========== [METHODS] ==========
    static JPanel Panel(JFrame frame,int x, int y, int w, int h) {
        JPanel p = new JPanel();
        p.setBounds(x, y, w, h);
        p.setLayout(null);
        p.setBackground(Color.pink);
        p.setVisible(false);
        frame.add(p);
        return p;
    }
    static JLabel Background(JPanel pan, String image, int x, int y, int w, int h) {
        JLabel bg = new JLabel(new ImageIcon(image));
        bg.setBounds(x, y, w, h);
        pan.add(bg);
        pan.setComponentZOrder(bg, pan.getComponentCount() - 1);
        return bg;
}
    static JLabel Text (String text, int x, int y, int w, int h, String colorHex, String fontName, int fontStyle, int fontSize) {
        JLabel txt = new JLabel(text);
        txt.setForeground(Color.decode(colorHex));
        txt.setBounds(x, y, w, h);
        txt.setFont(new Font(fontName, fontStyle, fontSize));
        return txt;
    }
// Updated method signature to accept frame and the navigation map
static void addAdminTexts(JFrame frame, JPanel currentPanel, java.util.Map<String, JPanel> navMap) {
    // The order of buttons to appear
    String[] texts = {
        "VERIFY",
        "DASHBOARD", 
        "PRODUCTS",
        "SOFA",
        "CHAIR",
        "BED",
        "TABLE",
        "INCOMING ITEMS",
        "OUTGOING ITEMS",
        "LOW-STOCK ITEMS",
        "DAMAGED ITEMS",
        "SALES",
        "REPORTS"
    };
    int x = 55; // 
    int y = 110; // 
    int w = 250; //width
    int h = 35; //height
    int gap = 10; // Increased gap slightly for readability
    for (String t : texts) {
        JLabel label = Text(t, x, y, w, h, "#ffffff", "Impact", Font.PLAIN, 18);
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        // Hover Effect
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                label.setForeground(Color.YELLOW);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                label.setForeground(Color.WHITE);
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                // Look for the target panel in the map
                if (navMap.containsKey(t)) {
                    JPanel targetPanel = navMap.get(t);
                    showPanel(frame, targetPanel);
                } else {
                    System.out.println("No panel mapped for: " + t);
                }
            }
        });
        currentPanel.add(label);
        // Ensure the menu text is on top of the background image
        currentPanel.setComponentZOrder(label, 0); 
        y += h + gap;
    }
}
    static JButton Button(String text, int x, int y, int w, int h, String c, String font, int style, int size) {
        JButton btn = new JButton(text);
        btn.setForeground(Color.decode(c));
        btn.setBounds(x, y, w, h);
        btn.setFont(new Font (font, style, size));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        return btn;
    }
    static void showPanel(JFrame frame, JPanel panel) {
        for (Component c : frame.getContentPane().getComponents()) {
            if (c instanceof JPanel) c.setVisible(false);
        }
        panel.setVisible(true);
    }
public static void main(String[] args) {
//========== [MAIN FRAME SETUP] ==========
    JFrame mainframe = new JFrame("INVENTORY SYSTEM");
        mainframe.setSize(1366, 810);
        mainframe.setLocationRelativeTo(null);
        mainframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainframe.setLayout(null);
    JPanel optionPANEL = Panel(mainframe, 0, 0, 1366, 768);
    JLabel optionBg = Background(optionPANEL, "1.png", 0, 0, 1366, 768);
    JButton option1 = Button("LOGIN",145, 500, 380, 50, "#ffffff", "Garet", Font.BOLD, 25);
    JButton option2 = Button("SIGN UP", 145, 590, 380, 50, "#ffffff", "Garet", Font.BOLD, 25);
    optionBg.add(option1); optionBg.add(option2);
//========== LOGIN PANEL ==========
    JPanel loginPANEL = Panel(mainframe, 0, 0, 1366, 768);
    JLabel loginBg = Background(loginPANEL, "2.png", 0, 0, 1366, 768);
    JTextField Username = new JTextField(); Username.setBounds(800, 245, 390, 35);
        Username.setOpaque(false);
        Username.setBackground(new Color(0, 0, 0, 0));
        Username.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.WHITE));
        Username.setForeground(Color.decode("#ffffff"));
        Username.setCaretColor(Color.WHITE);
        Username.setFont(new Font("Arial", Font.PLAIN, 20));
    JPasswordField Password = new JPasswordField(); Password.setBounds(800, 355, 390, 35);
        Password.setOpaque(false);
        Password.setBackground(new Color(0, 0, 0, 0));
        Password.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.WHITE));
        Password.setForeground(Color.decode("#ffffff"));
        Password.setCaretColor(Color.WHITE);
        Password.setFont(new Font("Arial", Font.PLAIN, 20));
    String[] roles = {"ADMIN", "STAFF"};  //hiiii
    JComboBox<String> login = new JComboBox<>(roles);
    login.setBounds(800, 420, 400, 40);
    Color customColor = Color.decode("#ded3c2");
        login.setFont(new Font("Garet", Font.PLAIN, 16));
        login.setForeground(Color.decode("#8e9472"));
        login.setBackground(customColor);
        login.setOpaque(true);
    JButton Login = Button("LOGIN", 780, 517, 200, 40, "#8e9472", "Garet", Font.BOLD, 20);
    JButton Back = Button("BACK", 1000, 517, 200, 40, "#8e9472", "Garet", Font.BOLD, 20);
    JButton SignUp = Button("SIGN UP", 790, 595, 400, 40, "#8e9472", "Garet", Font.BOLD, 20);//pacheck nabago ba?
    loginBg.add(Username); loginBg.add(Password); loginBg.add(login);
    loginBg.add(Login); loginBg.add(Back); loginBg.add(SignUp);
//========== SIGNUP PANEL ==========
    JPanel signupPANEL = Panel(mainframe, 0, 0, 1366, 768);
    JLabel signupBg = Background(signupPANEL, "3.png", 0, 0, 1366, 768);
    JTextField NEWUsername = new JTextField(); NEWUsername.setBounds(150, 220, 390, 35);
        NEWUsername.setOpaque(false);
        NEWUsername.setBackground(new Color(0, 0, 0, 0));
        NEWUsername.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.decode("#666549")));
        NEWUsername.setForeground(Color.decode("#3b4033"));
        NEWUsername.setCaretColor(Color.WHITE);
        NEWUsername.setFont(new Font("Arial", Font.PLAIN, 20));
    JPasswordField NEWPassword = new JPasswordField(); NEWPassword.setBounds(150, 340, 390, 35);
        NEWPassword.setOpaque(false);
        NEWPassword.setBackground(new Color(0, 0, 0, 0));
        NEWPassword.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.decode("#666549")));
        NEWPassword.setForeground(Color.decode("#3b4033"));
        NEWPassword.setCaretColor(Color.WHITE);
        NEWPassword.setFont(new Font("Arial", Font.PLAIN, 20));
    String[] newroles = {"ADMIN", "STAFF"};
    JComboBox<String> signup = new JComboBox<>(newroles); signup.setBounds(150, 400, 390, 35);
    Color newColor = Color.decode("#666549");
        signup.setBackground(newColor);
        signup.setFont(new Font("Garet", Font.PLAIN, 16));
        signup.setForeground(Color.WHITE);
        signup.setOpaque(true);
    JButton Create = Button("CREATE", 132, 511, 200, 40, "#ded3c2", "Garet", Font.BOLD, 20);
    JButton Back1 = Button("BACK", 380, 511, 200, 40, "#ded3c2", "Garet", Font.BOLD, 20);
    JButton login2 = Button("LOGIN",300, 511, 200, 40, "#ded3c2", "Garet", Font.BOLD, 20); //walapanglistener
    signupBg.add(NEWUsername); signupBg.add(NEWPassword); signupBg.add(signup);
    signupBg.add(Create); signupBg.add(Back1); signupBg.add(login2);
//========== ADMIN INTERFACE ========
    JFrame adminINTERFACEFRAME = new JFrame("ADMIN");
    adminINTERFACEFRAME.setSize(1070, 880);
    adminINTERFACEFRAME.setLocationRelativeTo(null);
    adminINTERFACEFRAME.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    adminINTERFACEFRAME.setLayout(null);
    // Admin Verification Panel
    JPanel verifyPanel = Panel(adminINTERFACEFRAME, 0, 0, 1050, 840);
    JLabel verifyBG = Background(verifyPanel, "verify.png", 0, 0, 1050, 840);
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "User", "Role", "Status"}, 0);
        JTable table = new JTable(model);
        try (Connection con = DriverManager.getConnection(URL, USER, PASS)) {
            ResultSet rs = con.createStatement().executeQuery("SELECT id, username, role, status FROM users");
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)});
            }
        } catch (Exception e) { e.printStackTrace(); }
        JButton approveBtn = new JButton("Approve Selected User");
        approveBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                String username = table.getValueAt(row, 1).toString();
                try (Connection con = DriverManager.getConnection(URL, USER, PASS)) {
                    PreparedStatement pst = con.prepareStatement("UPDATE users SET status='Approved' WHERE username=?");
                    pst.setString(1, username);
                    pst.executeUpdate();
                    JOptionPane.showMessageDialog(verifyBG, "User Approved!");
                    adminINTERFACEFRAME.dispose(); // Refresh by closing
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });
        verifyBG.add(new JScrollPane(table), BorderLayout.CENTER);
        verifyBG.add(approveBtn, BorderLayout.SOUTH);
        verifyBG.setVisible(true);
    // Admin/Dashboard Panel
    JPanel dashboardPanel = Panel(adminINTERFACEFRAME, 0, 0, 1050, 840);
    JLabel dashboardBG = Background(dashboardPanel, "4.png", 0, 0, 1050, 840); // Assuming full BG
    // Products Panel
    JPanel productPanel = Panel(adminINTERFACEFRAME, 0, 0, 1050, 840);
    JLabel productBG = Background(productPanel, "product.png", 0, 0, 1050, 840);
    // Sofa Panel
    JPanel sofaPanel = Panel(adminINTERFACEFRAME, 0, 0, 1050, 840);
    JLabel sofaBG = Background(sofaPanel, "sofa.png", 0, 0, 1050, 840);
    // Chair Panel
    JPanel chairPanel = Panel(adminINTERFACEFRAME, 0, 0, 1050, 840);
    JLabel chairBG = Background(chairPanel, "chair.png", 0, 0, 1050, 840);
    // Bed Panel
    JPanel bedPanel = Panel(adminINTERFACEFRAME, 0, 0, 1050, 840);
    JLabel bedBG = Background(bedPanel, "bed.png", 0, 0, 1050, 840);
    // Table Panel
    JPanel tablePanel = Panel(adminINTERFACEFRAME, 0, 0, 1050, 840);
    JLabel tableBG = Background(tablePanel, "table.png", 0, 0, 1050, 840);
    // Incoming Panel
    JPanel incomingPanel = Panel(adminINTERFACEFRAME, 0, 0, 1050, 840);
    JLabel incomingBG = Background(incomingPanel, "incoming.png", 0, 0, 1050, 840);
    // Outgoing Panel
    JPanel outgoingPanel = Panel(adminINTERFACEFRAME, 0, 0, 1050, 840);
    JLabel outgoingBG = Background(outgoingPanel, "outgoing.png", 0, 0, 1050, 840);
    // Low Stock Panel
    JPanel lowStockPanel = Panel(adminINTERFACEFRAME, 0, 0, 1050, 840);
    JLabel lowStockBG = Background(lowStockPanel, "low.png", 0, 0, 1050, 840);
    // Damaged Panel
    JPanel damagedPanel = Panel(adminINTERFACEFRAME, 0, 0, 1050, 840);
    JLabel damagedBG = Background(damagedPanel, "damaged.png", 0, 0, 1050, 840);
    // Sales Panel
    JPanel salesPanel = Panel(adminINTERFACEFRAME, 0, 0, 1050, 840);
    JLabel salesBG = Background(salesPanel, "sales.png", 0, 0, 1050, 840);
    // Reports Panel
    JPanel reportsPanel = Panel(adminINTERFACEFRAME, 0, 0, 1050, 840);
    JLabel reportsBG = Background(reportsPanel, "reports.png", 0, 0, 1050, 840);
    java.util.Map<String, JPanel> navMap = new java.util.HashMap<>();
    navMap.put("VERIFY", verifyPanel);
    navMap.put("DASHBOARD", dashboardPanel);
    navMap.put("PRODUCTS", productPanel);
    navMap.put("SOFA", sofaPanel);
    navMap.put("CHAIR", chairPanel);
    navMap.put("BED", bedPanel);
    navMap.put("TABLE", tablePanel);
    navMap.put("INCOMING ITEMS", incomingPanel);
    navMap.put("OUTGOING ITEMS", outgoingPanel);
    navMap.put("LOW-STOCK ITEMS", lowStockPanel);
    navMap.put("DAMAGED ITEMS", damagedPanel);
    navMap.put("SALES", salesPanel);
    navMap.put("REPORTS", reportsPanel);
    for (JPanel p : navMap.values()) {
        addAdminTexts(adminINTERFACEFRAME, p, navMap);
    }
//========== BUTTON FUNCTION ============
    option1.addActionListener(e -> showPanel(mainframe, loginPANEL));
    option2.addActionListener(e -> showPanel (mainframe, signupPANEL));
    SignUp.addActionListener(e -> showPanel(mainframe, signupPANEL));
    Back1.addActionListener(e -> showPanel(mainframe, optionPANEL));
    Back.addActionListener(e -> showPanel(mainframe, optionPANEL));
//========== LOGIN AND CREATE ACCOUNT ==========
Login.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent e) {
        String username = Username.getText();
        String password = new String(Password.getPassword());
            if (username.isEmpty() && password.isEmpty()) {
                JOptionPane.showMessageDialog(null, "USERNAME AND PASSWORD REQUIRED!", "ERROR", JOptionPane.ERROR_MESSAGE);
            } else if (username.isEmpty()) {
                JOptionPane.showMessageDialog(null, "USERNAME REQUIRED!", "ERROR", JOptionPane.ERROR_MESSAGE);
            } else if (password.isEmpty()) {
                JOptionPane.showMessageDialog(null, "PASSWORD REQUIRED!", "ERROR", JOptionPane.ERROR_MESSAGE);
            } else {
                if (checkLogin(username, password, "Admin")) {
                    JOptionPane.showMessageDialog(null, "ADMIN LOGIN SUCCESS", "SUCCESS", JOptionPane.INFORMATION_MESSAGE);
                    mainframe.setVisible(false);
                    adminINTERFACEFRAME.setVisible(true);
                    showPanel(adminINTERFACEFRAME, dashboardPanel);
                } else if (checkLogin(username, password, "Staff")) {
                    JOptionPane.showMessageDialog(null, "STAFF LOGIN SUCCESS", "SUCCESS", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "INVALID ADMIN CREDENTIALS", "ERROR", JOptionPane.ERROR_MESSAGE);
                }
}}});
Create.addActionListener(e -> {
    if (NEWUsername.getText().isEmpty()) {
        JOptionPane.showMessageDialog(mainframe, "Username required!");
        return;
    }
    try (Connection con = DriverManager.getConnection(URL, USER, PASS)) {
        String sql = "INSERT INTO users (username, password, role, status) VALUES (?, ?, ?, 'Pending')";
        PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, NEWUsername.getText());
            pst.setString(2, new String(NEWPassword.getPassword()));
            pst.setString(3, signup.getSelectedItem().toString());
            pst.executeUpdate();
        JOptionPane.showMessageDialog(mainframe, "Account Requested! Wait for Admin approval.");
            showPanel(mainframe, loginPANEL);
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(mainframe, "Error: User already exists or DB error.");
    }
});
mainframe.setVisible(true);
optionPANEL.setVisible(true);
} // main method ends here
//========== ADMIN DASHBOARD FUNCTION ==========
    static void showAdminDashboard(JPanel verifyPanel) {
    }
}   