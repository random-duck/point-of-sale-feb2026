import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;

public class VerifyUsersPanel extends JPanel {

    private JTable table;
    private DefaultTableModel model;

    public VerifyUsersPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.COLOR_CREAM);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JLabel title = new JLabel("Pending User Approvals");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.COLOR_DARK);
        add(title, BorderLayout.NORTH);

        // Table
        model = new DefaultTableModel(new String[]{"ID", "Username", "Role", "Status"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(model);
        table.setRowHeight(30);
        table.getColumnModel().getColumn(0).setMinWidth(0); // Hide ID
        table.getColumnModel().getColumn(0).setMaxWidth(0);

        // Right-Click Context Menu
        JPopupMenu popup = new JPopupMenu();
        JMenuItem approveItem = new JMenuItem("Approve User");
        JMenuItem rejectItem = new JMenuItem("Reject / Delete");
        popup.add(approveItem);
        popup.add(rejectItem);

        approveItem.addActionListener(e -> processUser(true));
        rejectItem.addActionListener(e -> processUser(false));

        table.setComponentPopupMenu(popup);
        
        // Also allow selection with left click
        table.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < table.getRowCount()) {
                        table.setRowSelectionInterval(row, row);
                    }
                }
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);
        
        // Refresh Button
        JButton refreshBtn = new JButton("Refresh List");
        refreshBtn.addActionListener(e -> loadData());
        add(refreshBtn, BorderLayout.SOUTH);

        // Auto-load
        loadData();
        
        // Auto-refresh when tab shown
        this.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent e) { loadData(); }
        });
    }

    private void loadData() {
        model.setRowCount(0);
        List<Document> pending = Database.getPendingUsers(); // Uses the new method
        
        if (pending.isEmpty()) {
            model.addRow(new Object[]{null, "No pending users", "-", "-"});
        } else {
            for (Document d : pending) {
                model.addRow(new Object[]{
                    d.getObjectId("_id"),
                    d.getString("username"),
                    d.getString("role"),
                    d.getString("status")
                });
            }
        }
    }

    private void processUser(boolean approve) {
        int row = table.getSelectedRow();
        if (row == -1) return;

        Object idObj = model.getValueAt(row, 0);
        if (idObj == null) return; // Ignore the "No pending users" row

        ObjectId userId = (ObjectId) idObj;
        String username = (String) model.getValueAt(row, 1);

        if (approve) {
            Database.approveUser(userId);
            JOptionPane.showMessageDialog(this, username + " has been approved!");
        } else {
            Database.deleteUser(userId);
            JOptionPane.showMessageDialog(this, username + " has been deleted.");
        }
        loadData();
    }
}