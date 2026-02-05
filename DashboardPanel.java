import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;

public class DashboardPanel extends JPanel {

    private CardLayout contentLayout = new CardLayout();
    private JPanel contentPanel = new JPanel(contentLayout);
    private JPanel sidebar;
    private JLabel logo;

    public DashboardPanel() {
        setLayout(new BorderLayout());

        // --- SIDEBAR SETUP ---
        sidebar = new JPanel();
        sidebar.setBackground(Theme.COLOR_GREEN);
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        // --- CONTENT AREA SETUP ---
        // 1. Add all potential panels here
        contentPanel.add(new ProductGalleryPanel(), "PRODUCTS");
        contentPanel.add(new AddProductPanel(), "INCOMING");
        contentPanel.add(new OutgoingPanel(), "OUTGOING");
        
        // Admin Panels
        contentPanel.add(new AnalyticsPanel(), "DASHBOARD");
        contentPanel.add(new SupplyOrderPanel(), "SUPPLY ORDERS");
        contentPanel.add(new EmployeeReportPanel(), "EMPLOYEE REPORT");
        contentPanel.add(new VerifyUsersPanel(), "VERIFY USERS");
        contentPanel.add(new SettingsPanel(), "SETTINGS");

        add(sidebar, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        // --- AUTO-REFRESH & RESET ON SHOW ---
        this.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                refreshSidebar();
            }
        });
        
        refreshSidebar();
    }

    private void refreshSidebar() {
        sidebar.removeAll(); // Clear old buttons

        // Re-add Logo
        logo = new JLabel("MUWEBLES");
        logo.setFont(new Font("Serif", Font.BOLD, 24));
        logo.setForeground(Theme.COLOR_CREAM);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        sidebar.add(Box.createRigidArea(new Dimension(0, 30)));
        sidebar.add(logo);
        sidebar.add(Box.createRigidArea(new Dimension(0, 50)));

        // --- DYNAMIC BUTTONS ---
        boolean isAdmin = Session.isAdmin();
        System.out.println("DEBUG: Refreshing Sidebar. User: " + Session.currentUsername + " | Is Admin? " + isAdmin);

        // Common Buttons
        addMenuButton(sidebar, "PRODUCTS");
        addMenuButton(sidebar, "INCOMING");
        addMenuButton(sidebar, "OUTGOING");

        // Admin Only Buttons
        if (isAdmin) {
            JLabel adminLbl = new JLabel("ADMIN CONTROLS");
            adminLbl.setForeground(Color.YELLOW);
            adminLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            sidebar.add(Box.createRigidArea(new Dimension(0, 20)));
            sidebar.add(adminLbl);
            sidebar.add(Box.createRigidArea(new Dimension(0, 10)));

            addMenuButton(sidebar, "DASHBOARD");
            addMenuButton(sidebar, "SUPPLY ORDERS");
            addMenuButton(sidebar, "EMPLOYEE REPORT");
            addMenuButton(sidebar, "VERIFY USERS");
            addMenuButton(sidebar, "SETTINGS");
        }

        // Logout Button
        sidebar.add(Box.createVerticalGlue()); 
        String userLabel = (Session.currentUsername != null) ? Session.currentUsername : "Guest";
        JButton logoutBtn = new JButton("LOGOUT (" + userLabel + ")");
        
        logoutBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoutBtn.setBackground(Theme.COLOR_DARK);
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.addActionListener(e -> {
            Session.logout();
            Main.getInstance().showScreen("LOGIN");
        });
        sidebar.add(logoutBtn);
        sidebar.add(Box.createRigidArea(new Dimension(0, 20)));

        // --- THE FIX: ALWAYS RESET TO "PRODUCTS" ---
        // This forces the view to jump back to the main gallery every time the dashboard loads.
        // This prevents Staff from seeing the last screen the Admin was looking at.
        contentLayout.show(contentPanel, "PRODUCTS");
        // -------------------------------------------

        sidebar.revalidate();
        sidebar.repaint();
    }

    private void addMenuButton(JPanel sidebar, String name) {
        JLabel btn = new JLabel(name);
        btn.setFont(Theme.FONT_BOLD);
        btn.setForeground(Theme.COLOR_CREAM);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setForeground(Color.YELLOW); }
            public void mouseExited(MouseEvent e) { btn.setForeground(Theme.COLOR_CREAM); }
            public void mouseClicked(MouseEvent e) { contentLayout.show(contentPanel, name); }
        });
        
        sidebar.add(btn);
        sidebar.add(Box.createRigidArea(new Dimension(0, 20)));
    }
}