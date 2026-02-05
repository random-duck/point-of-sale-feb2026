import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

// ==========================================
// IMPORT MONGODB DRIVERS
// You MUST add the "mongodb-driver-sync" JAR files to your VS Code project!
// ==========================================
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.types.ObjectId;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

import java.util.Map;
import java.util.HashMap;

public class main {
    
    // ==========================================
    // [DATABASE CONFIGURATION]
    // ==========================================
    
    // 🔴 IMPORTANT: Replace 'REPLACE_WITH_YOUR_PASSWORD' with your actual MongoDB password.
    // If your password has special characters (like @, :, /), you might need to "URL Encode" them.
    static final String URI = "mongodb+srv://admin:REPLACE_WITH_YOUR_PASSWORD@cluster0.hadhdy5.mongodb.net/?retryWrites=true&w=majority";
    
    static final String DB_NAME = "inventory_db";
    static final String COLL_NAME = "users";

    // ==========================================
    // [LOGIN LOGIC]
    // ==========================================
    
    public static boolean checkLogin(String username, String password, String role) {
        boolean valid = false;
        try (MongoClient mongoClient = MongoClients.create(URI)) {
            MongoDatabase database = mongoClient.getDatabase(DB_NAME);
            MongoCollection<Document> collection = database.getCollection(COLL_NAME);

            // Find user matching all 3 criteria
            Document found = collection.find(and(
                    eq("username", username),
                    eq("password", password),
                    eq("role", role)
            )).first();

            if (found != null) {
                // Optional: You can check found.getString("status") here if needed
                valid = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Connection Error: " + e.getMessage());
        }
        return valid;
    }

    // ==========================================
    // [UI BUILDER METHODS]
    // ==========================================
    
    static JPanel Panel(JFrame frame, int x, int y, int w, int h) {
        JPanel p = new JPanel();
        p.setBounds(x, y, w, h);
        p.setLayout(null);
        p.setBackground(Color.pink);
        p.setVisible(false);
        frame.add(p);
        return p;
    }

    static JLabel Background(JPanel pan, String image, int x, int y, int w, int h) {
        // Ensure image files (1.png, 2.png, etc.) are in your project folder
        JLabel bg = new JLabel(new ImageIcon(image)); 
        bg.setBounds(x, y, w, h);
        pan.add(bg);
        pan.setComponentZOrder(bg, pan.getComponentCount() - 1);
        return bg;
    }

    static JLabel Text(String text, int x, int y, int w, int h, String colorHex, String fontName, int fontStyle, int fontSize) {
        JLabel txt = new JLabel(text);
        txt.setForeground(Color.decode(colorHex));
        txt.setBounds(x, y, w, h);
        txt.setFont(new Font(fontName, fontStyle, fontSize));
        return txt;
    }

    static JButton Button(String text, int x, int y, int w, int h, String c, String font, int style, int size) {
        JButton btn = new JButton(text);
        btn.setForeground(Color.decode(c));
        btn.setBounds(x, y, w, h);
        btn.setFont(new Font(font, style, size));
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

    // Creates the Admin Sidebar Menu
    static void addAdminTexts(JFrame frame, JPanel currentPanel, Map<String, JPanel> navMap) {
        String[] texts = {
            "VERIFY", "DASHBOARD", "PRODUCTS", "SOFA", "CHAIR", 
            "BED", "TABLE", "INCOMING ITEMS", "OUTGOING ITEMS", 
            "LOW-STOCK ITEMS", "DAMAGED ITEMS", "SALES", "REPORTS"
        };

        int x = 55; int y = 110; int w = 250; int h = 35; int gap = 10; 

        for (String t : texts) {
            JLabel label = Text(t, x, y, w, h, "#ffffff", "Impact", Font.PLAIN, 18);
            label.setCursor(new Cursor(Cursor.HAND_CURSOR));

            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) { label.setForeground(Color.YELLOW); }
                @Override
                public void mouseExited(MouseEvent e) { label.setForeground(Color.WHITE); }
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (navMap.containsKey(t)) {
                        showPanel(frame, navMap.get(t));
                    }
                }
            });
            currentPanel.add(label);
            currentPanel.setComponentZOrder(label, 0); 
            y += h + gap;
        }
    }

    // ==========================================
    // [MAIN APPLICATION]
    // ==========================================
    
    public static void main(String[] args) {
        
        // --- 1. Main Frame ---
        JFrame mainframe = new JFrame("INVENTORY SYSTEM (MongoDB Cloud)");
        mainframe.setSize(1366, 810);
        mainframe.setLocationRelativeTo(null);
        mainframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainframe.setLayout(null);

        // --- 2. Landing Page ---
        JPanel optionPANEL = Panel(mainframe, 0, 0, 1366, 768);
        JLabel optionBg = Background(optionPANEL, "1.png", 0, 0, 1366, 768);
        JButton option1 = Button("LOGIN", 145, 500, 380, 50, "#ffffff", "Garet", Font.BOLD, 25);
        JButton option2 = Button("SIGN UP", 145, 590, 380, 50, "#ffffff", "Garet", Font.BOLD, 25);
        optionBg.add(option1); optionBg.add(option2);

        // --- 3. Login Page ---
        JPanel loginPANEL = Panel(mainframe, 0, 0, 1366, 768);
        JLabel loginBg = Background(loginPANEL, "2.png", 0, 0, 1366, 768);
        
        JTextField Username = new JTextField(); 
        Username.setBounds(800, 245, 390, 35);
        Username.setOpaque(false);
        Username.setBackground(new Color(0,0,0,0));
        Username.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.WHITE));
        Username.setForeground(Color.WHITE);
        Username.setCaretColor(Color.WHITE);
        Username.setFont(new Font("Arial", Font.PLAIN, 20));

        JPasswordField Password = new JPasswordField(); 
        Password.setBounds(800, 355, 390, 35);
        Password.setOpaque(false);
        Password.setBackground(new Color(0,0,0,0));
        Password.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.WHITE));
        Password.setForeground(Color.WHITE);
        Password.setCaretColor(Color.WHITE);
        Password.setFont(new Font("Arial", Font.PLAIN, 20));

        String[] roles = {"ADMIN", "STAFF"};
        JComboBox<String> loginSelector = new JComboBox<>(roles);
        loginSelector.setBounds(800, 420, 400, 40);
        loginSelector.setFont(new Font("Garet", Font.PLAIN, 16));
        loginSelector.setForeground(Color.decode("#8e9472"));
        loginSelector.setBackground(Color.decode("#ded3c2"));

        JButton LoginBtn = Button("LOGIN", 780, 517, 200, 40, "#8e9472", "Garet", Font.BOLD, 20);
        JButton BackBtn = Button("BACK", 1000, 517, 200, 40, "#8e9472", "Garet", Font.BOLD, 20);
        JButton SignUpBtn = Button("SIGN UP", 790, 595, 400, 40, "#8e9472", "Garet", Font.BOLD, 20);

        loginBg.add(Username); loginBg.add(Password); loginBg.add(loginSelector);
        loginBg.add(LoginBtn); loginBg.add(BackBtn); loginBg.add(SignUpBtn);

        // --- 4. Signup Page ---
        JPanel signupPANEL = Panel(mainframe, 0, 0, 1366, 768);
        JLabel signupBg = Background(signupPANEL, "3.png", 0, 0, 1366, 768);
        
        JTextField NEWUsername = new JTextField(); 
        NEWUsername.setBounds(150, 220, 390, 35);
        NEWUsername.setOpaque(false);
        NEWUsername.setBackground(new Color(0,0,0,0));
        NEWUsername.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.decode("#666549")));
        NEWUsername.setForeground(Color.decode("#3b4033"));
        NEWUsername.setFont(new Font("Arial", Font.PLAIN, 20));

        JPasswordField NEWPassword = new JPasswordField(); 
        NEWPassword.setBounds(150, 340, 390, 35);
        NEWPassword.setOpaque(false);
        NEWPassword.setBackground(new Color(0,0,0,0));
        NEWPassword.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.decode("#666549")));
        NEWPassword.setForeground(Color.decode("#3b4033"));
        NEWPassword.setFont(new Font("Arial", Font.PLAIN, 20));

        JComboBox<String> signupSelector = new JComboBox<>(roles); 
        signupSelector.setBounds(150, 400, 390, 35);
        signupSelector.setBackground(Color.decode("#666549"));
        signupSelector.setFont(new Font("Garet", Font.PLAIN, 16));
        signupSelector.setForeground(Color.WHITE);

        JButton CreateBtn = Button("CREATE", 132, 511, 200, 40, "#ded3c2", "Garet", Font.BOLD, 20);
        JButton Back1Btn = Button("BACK", 380, 511, 200, 40, "#ded3c2", "Garet", Font.BOLD, 20);
        JButton Login2Btn = Button("LOGIN", 300, 511, 200, 40, "#ded3c2", "Garet", Font.BOLD, 20);

        signupBg.add(NEWUsername); signupBg.add(NEWPassword); signupBg.add(signupSelector);
        signupBg.add(CreateBtn); signupBg.add(Back1Btn); signupBg.add(Login2Btn);

        // --- 5. Admin Interface (Verify Panel) ---
        JFrame adminINTERFACEFRAME = new JFrame("ADMIN DASHBOARD");
        adminINTERFACEFRAME.setSize(1070, 880);
        adminINTERFACEFRAME.setLocationRelativeTo(null);
        adminINTERFACEFRAME.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        adminINTERFACEFRAME.setLayout(null);

        JPanel verifyPanel = Panel(adminINTERFACEFRAME, 0, 0, 1050, 840);
        JLabel verifyBG = Background(verifyPanel, "verify.png", 0, 0, 1050, 840);
        
        // Table Model
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "User", "Role", "Status"}, 0);
        JTable table = new JTable(model);
        
        // Load existing users from Atlas to Table
        try (MongoClient client = MongoClients.create(URI)) {
            MongoDatabase db = client.getDatabase(DB_NAME);
            MongoCollection<Document> collection = db.getCollection(COLL_NAME);
            
            for (Document doc : collection.find()) {
                String id = doc.getObjectId("_id").toString();
                String uName = doc.getString("username");
                String uRole = doc.getString("role");
                String uStatus = doc.getString("status");
                model.addRow(new Object[]{id, uName, uRole, uStatus});
            }
        } catch (Exception e) { 
            // If the connection fails (e.g., bad password), this will print why
            System.err.println("Failed to load table: " + e.getMessage());
        }

        JButton approveBtn = new JButton("Approve Selected User");
        approveBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                String username = table.getValueAt(row, 1).toString();
                try (MongoClient client = MongoClients.create(URI)) {
                    MongoDatabase db = client.getDatabase(DB_NAME);
                    MongoCollection<Document> collection = db.getCollection(COLL_NAME);
                    
                    collection.updateOne(eq("username", username), set("status", "Approved"));
                    JOptionPane.showMessageDialog(verifyBG, "User Approved!");
                    adminINTERFACEFRAME.dispose(); // Simple refresh strategy
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });
        
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBounds(50, 50, 900, 600); 
        verifyBG.add(scroll);
        approveBtn.setBounds(400, 670, 200, 50);
        verifyBG.add(approveBtn);

        // --- 6. Other Admin Panels (Placeholders) ---
        JPanel dashboardPanel = Panel(adminINTERFACEFRAME, 0, 0, 1050, 840);
        Background(dashboardPanel, "4.png", 0, 0, 1050, 840);

        JPanel productPanel = Panel(adminINTERFACEFRAME, 0, 0, 1050, 840);
        Background(productPanel, "product.png", 0, 0, 1050, 840);

        // Map panels for navigation
        Map<String, JPanel> navMap = new HashMap<>();
        navMap.put("VERIFY", verifyPanel);
        navMap.put("DASHBOARD", dashboardPanel);
        navMap.put("PRODUCTS", productPanel);

        // Add Sidebar to all panels
        for (JPanel p : navMap.values()) {
            addAdminTexts(adminINTERFACEFRAME, p, navMap);
        }

        // --- 7. Button Actions ---
        option1.addActionListener(e -> showPanel(mainframe, loginPANEL));
        option2.addActionListener(e -> showPanel(mainframe, signupPANEL));
        SignUpBtn.addActionListener(e -> showPanel(mainframe, signupPANEL));
        Back1Btn.addActionListener(e -> showPanel(mainframe, optionPANEL));
        BackBtn.addActionListener(e -> showPanel(mainframe, optionPANEL));
        Login2Btn.addActionListener(e -> showPanel(mainframe, loginPANEL));

        // LOGIN CLICK
        LoginBtn.addActionListener(e -> {
            String u = Username.getText();
            String p = new String(Password.getPassword());
            String r = loginSelector.getSelectedItem().toString();

            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please fill all fields!");
            } else {
                if (checkLogin(u, p, r)) {
                    if (r.equals("ADMIN")) {
                        JOptionPane.showMessageDialog(null, "ADMIN LOGIN SUCCESS");
                        mainframe.setVisible(false);
                        adminINTERFACEFRAME.setVisible(true);
                        showPanel(adminINTERFACEFRAME, dashboardPanel);
                    } else {
                        JOptionPane.showMessageDialog(null, "STAFF LOGIN SUCCESS");
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "INVALID CREDENTIALS", "ERROR", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // CREATE/SIGNUP CLICK
        CreateBtn.addActionListener(e -> {
            String newU = NEWUsername.getText();
            String newP = new String(NEWPassword.getPassword());
            String newR = signupSelector.getSelectedItem().toString();

            if (newU.isEmpty() || newP.isEmpty()) {
                JOptionPane.showMessageDialog(mainframe, "Fields cannot be empty!");
                return;
            }

            try (MongoClient client = MongoClients.create(URI)) {
                MongoDatabase db = client.getDatabase(DB_NAME);
                MongoCollection<Document> collection = db.getCollection(COLL_NAME);

                // Check duplicate
                if (collection.find(eq("username", newU)).first() != null) {
                    JOptionPane.showMessageDialog(mainframe, "User already exists!");
                    return;
                }

                // Insert
                Document newUser = new Document("username", newU)
                        .append("password", newP)
                        .append("role", newR)
                        .append("status", "Pending");

                collection.insertOne(newUser);
                JOptionPane.showMessageDialog(mainframe, "Account Requested! Wait for Admin approval.");
                showPanel(mainframe, loginPANEL);

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(mainframe, "Database Error: " + ex.getMessage());
            }
        });

        // Launch Application
        mainframe.setVisible(true);
        optionPANEL.setVisible(true);
    }
}