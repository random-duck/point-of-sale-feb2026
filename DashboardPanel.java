import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;

public class DashboardPanel extends JPanel {

    // Internal CardLayout to swap between "Verify", "Products", etc.
    private CardLayout contentLayout = new CardLayout();
    private JPanel contentPanel = new JPanel(contentLayout);

    public DashboardPanel() {
        setLayout(new BorderLayout());

        // --- 1. SIDEBAR (Left) ---
        JPanel sidebar = new JPanel();
        sidebar.setBackground(Theme.COLOR_GREEN);
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        // Add Logo to Sidebar
        JLabel logo = new JLabel("MUWEBLES");
        logo.setFont(new Font("Serif", Font.BOLD, 24));
        logo.setForeground(Theme.COLOR_CREAM);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        sidebar.add(Box.createRigidArea(new Dimension(0, 30))); // Spacing
        sidebar.add(logo);
        sidebar.add(Box.createRigidArea(new Dimension(0, 50)));

        // Add Menu Items
        addMenuButton(sidebar, "VERIFY USERS");
        addMenuButton(sidebar, "DASHBOARD");
        addMenuButton(sidebar, "PRODUCTS");
        addMenuButton(sidebar, "INCOMING");
        addMenuButton(sidebar, "OUTGOING");
        addMenuButton(sidebar, "SUPPLY ORDERS");
        addMenuButton(sidebar, "SETTINGS");
        
        // Logout Button at bottom
        sidebar.add(Box.createVerticalGlue()); 
        JButton logoutBtn = new JButton("LOGOUT");
        logoutBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoutBtn.setBackground(Theme.COLOR_DARK);
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.addActionListener(e -> Main.getInstance().showScreen("LOGIN"));
        sidebar.add(logoutBtn);
        sidebar.add(Box.createRigidArea(new Dimension(0, 20)));


        // --- 2. CONTENT AREA (Center) ---
        
        // A. Verify Users Panel
        JPanel verifyPanel = createVerifyPanel();
        contentPanel.add(verifyPanel, "VERIFY USERS");

        // B. Real Panels
        contentPanel.add(new AnalyticsPanel(), "DASHBOARD");
        
        // Link the real panels we created
        contentPanel.add(new ProductGalleryPanel(), "PRODUCTS");
        contentPanel.add(new AddProductPanel(), "INCOMING");
        contentPanel.add(new OutgoingPanel(), "OUTGOING");
        contentPanel.add(new SupplyOrderPanel(), "SUPPLY ORDERS");
        contentPanel.add(new SettingsPanel(), "SETTINGS");

        add(sidebar, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
    }

    // --- HELPER: Create Sidebar Buttons ---
    private void addMenuButton(JPanel sidebar, String name) {
        JLabel btn = new JLabel(name);
        btn.setFont(Theme.FONT_BOLD);
        btn.setForeground(Theme.COLOR_CREAM);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover Effect & Click Action
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setForeground(Color.YELLOW); }
            public void mouseExited(MouseEvent e) { btn.setForeground(Theme.COLOR_CREAM); }
            public void mouseClicked(MouseEvent e) {
                // Switch the center view
                contentLayout.show(contentPanel, name);
            }
        });

        sidebar.add(btn);
        sidebar.add(Box.createRigidArea(new Dimension(0, 20))); // Spacing
    }

    // --- PANEL: Verify Users (Table Logic) ---
    private JPanel createVerifyPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.COLOR_CREAM);

        // Header
        JLabel header = new JLabel("  Admin Verification");
        header.setFont(Theme.FONT_TITLE);
        header.setForeground(Theme.COLOR_DARK);
        p.add(header, BorderLayout.NORTH);

        // Table Model
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Username", "Role", "Status"}, 0);
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setRowHeight(30);
        table.setFont(Theme.FONT_REGULAR);
        
        // Hide ID Column
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        
        // Scroll Pane
        JScrollPane scroll = new JScrollPane(table);
        p.add(scroll, BorderLayout.CENTER);

        // Load Data Button (Refresh)
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Theme.COLOR_CREAM);

        JButton refreshBtn = new JButton("Refresh Data");
        JButton approveBtn = new JButton("Approve Selected");

        buttonPanel.add(refreshBtn);
        buttonPanel.add(approveBtn);
        p.add(buttonPanel, BorderLayout.SOUTH);

        // --- ACTIONS ---
        
        // 1. Refresh Table
        refreshBtn.addActionListener(e -> {
            model.setRowCount(0); // Clear table
            
            // USE THE NEW DATABASE METHOD
            List<Document> users = Database.getAllUsers();
            
            for (Document doc : users) {
                model.addRow(new Object[]{
                    doc.getObjectId("_id"),
                    doc.getString("username"),
                    doc.getString("role"),
                    doc.getString("status")
                });
            }
        });

        // 2. Approve User
        approveBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a user to approve.");
                return;
            }

            // Get ID from table
            ObjectId userId = (ObjectId) table.getValueAt(row, 0);
            String currentStatus = (String) table.getValueAt(row, 3);

            if ("Approved".equals(currentStatus)) {
                JOptionPane.showMessageDialog(this, "User is already approved!");
                return;
            }

            // USE THE NEW DATABASE METHOD
            Database.approveUser(userId);
            
            JOptionPane.showMessageDialog(this, "User Approved!");
            refreshBtn.doClick(); // Auto-refresh table
        });

        // Load data immediately on creation
        refreshBtn.doClick();

        return p;
    }

    // --- HELPER: Placeholder for empty pages ---
    private JPanel createPlaceholder(String title) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.COLOR_CREAM);
        JLabel label = new JLabel(title + " (Under Construction)");
        label.setFont(Theme.FONT_TITLE);
        label.setForeground(Theme.COLOR_GREEN);
        p.add(label);
        return p;
    }
}