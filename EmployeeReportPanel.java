import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import org.bson.Document;

public class EmployeeReportPanel extends JPanel {

    private JTable employeeTable;
    private DefaultTableModel employeeModel;
    private JTextArea commentArea;
    private JTextArea commentHistory;
    private JLabel selectedUserLabel;

    public EmployeeReportPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Theme.COLOR_CREAM);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- HEADER ---
        JLabel title = new JLabel("Employee Performance Reporting");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.COLOR_DARK);
        add(title, BorderLayout.NORTH);

        // --- CENTER SPLIT ---
        JPanel center = new JPanel(new GridLayout(1, 2, 20, 0));
        center.setBackground(Theme.COLOR_CREAM);

        // 1. LEFT: Employee Stats Table
        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(BorderFactory.createTitledBorder("Staff Performance Metrics"));
        left.setBackground(Theme.COLOR_CREAM);

        // --- UPDATED COLUMNS HERE ---
        employeeModel = new DefaultTableModel(new String[]{"Username", "Role", "Incoming Processed", "Outgoing Processed"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        employeeTable = new JTable(employeeModel);
        employeeTable.setRowHeight(30);
        
        employeeTable.getSelectionModel().addListSelectionListener(e -> loadComments());

        left.add(new JScrollPane(employeeTable), BorderLayout.CENTER);

        // 2. RIGHT: Comments
        JPanel right = new JPanel(new BorderLayout(0, 10));
        right.setBorder(BorderFactory.createTitledBorder("Admin Reviews & Comments"));
        right.setBackground(Theme.COLOR_CREAM);

        selectedUserLabel = new JLabel("Select an employee to view/add comments");
        selectedUserLabel.setFont(Theme.FONT_BOLD);
        right.add(selectedUserLabel, BorderLayout.NORTH);

        commentHistory = new JTextArea();
        commentHistory.setEditable(false);
        commentHistory.setBackground(new Color(240, 240, 240));
        right.add(new JScrollPane(commentHistory), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        commentArea = new JTextArea(3, 20);
        commentArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        JButton submitBtn = new JButton("Add Note");
        submitBtn.setBackground(Theme.COLOR_GREEN);
        submitBtn.setForeground(Color.WHITE);
        submitBtn.addActionListener(e -> addComment());

        inputPanel.add(new JScrollPane(commentArea), BorderLayout.CENTER);
        inputPanel.add(submitBtn, BorderLayout.EAST);
        
        right.add(inputPanel, BorderLayout.SOUTH);

        center.add(left);
        center.add(right);
        add(center, BorderLayout.CENTER);

        // Initial Load
        loadData();
    }

    private void loadData() {
        employeeModel.setRowCount(0);
        List<Document> users = Database.getAllUsers();

        for (Document u : users) {
            String username = u.getString("username");
            
            // --- UPDATED LOGIC HERE ---
            // "Incoming" = Items added via AddProductPanel
            int incomingCount = Database.getActionCount(username, "Incoming");
            // "Outgoing" = Items exported via OutgoingPanel
            int outgoingCount = Database.getActionCount(username, "Export");

            employeeModel.addRow(new Object[]{
                username,
                u.getString("role"),
                incomingCount,
                outgoingCount
            });
        }
    }

    private void loadComments() {
        int row = employeeTable.getSelectedRow();
        if (row == -1) return;

        String username = (String) employeeModel.getValueAt(row, 0);
        selectedUserLabel.setText("Reviews for: " + username);
        
        commentHistory.setText("");
        List<Document> comments = Database.getEmployeeComments(username);
        for (Document c : comments) {
            commentHistory.append("[" + c.getDate("date").toString() + "] " + c.getString("admin") + ":\n");
            commentHistory.append("   " + c.getString("comment") + "\n\n");
        }
    }

    private void addComment() {
        int row = employeeTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select an employee first.");
            return;
        }

        String targetUser = (String) employeeModel.getValueAt(row, 0);
        String comment = commentArea.getText().trim();
        
        if (comment.isEmpty()) return;

        Database.saveEmployeeComment(targetUser, comment, Session.currentUsername);
        commentArea.setText("");
        loadComments(); 
    }
}