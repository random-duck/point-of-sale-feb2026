import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.List;
import java.util.Map;
import org.bson.Document;

public class ImportResolutionDialog extends JDialog {

    private boolean confirmed = false;
    private JTable table;
    private DefaultTableModel model;
    
    // This list will hold the user's final decisions
    private List<CSVImporter.ConflictItem> conflictItems;

    public ImportResolutionDialog(JFrame parent, List<CSVImporter.ConflictItem> conflicts) {
        super(parent, "Import Conflicts Detected", true);
        this.conflictItems = conflicts;
        
        setSize(900, 500);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.COLOR_CREAM);

        // --- HEADER ---
        JLabel lbl = new JLabel("<html>The following items already exist in the database.<br>Please choose an action for each:</html>");
        lbl.setFont(Theme.FONT_BOLD);
        lbl.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        add(lbl, BorderLayout.NORTH);

        // --- TABLE ---
        String[] cols = {"Product Name", "Current Status", "New CSV Data", "Action"};
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 3; // Only the "Action" column is editable
            }
        };

        for (CSVImporter.ConflictItem item : conflicts) {
            Document existing = item.existingDoc;
            CSVImporter.ProductData newData = item.newData;

            String currentInfo = String.format("Qty: %d | Price: %.2f", existing.getInteger("quantity"), existing.getDouble("price"));
            String newInfo = String.format("Qty: %d | Price: %.2f", newData.quantity, newData.price);

            model.addRow(new Object[]{newData.name, currentInfo, newInfo, "Skip"}); // Default to Skip
        }

        table = new JTable(model);
        table.setRowHeight(30);
        
        // Add Dropdown to the Action Column
        TableColumn actionCol = table.getColumnModel().getColumn(3);
        JComboBox<String> comboBox = new JComboBox<>(new String[]{"Skip", "Update Stock (Add)", "Replace"});
        actionCol.setCellEditor(new DefaultCellEditor(comboBox));

        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- BUTTONS ---
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(Theme.COLOR_CREAM);
        
        JButton okBtn = new JButton("CONFIRM IMPORT");
        okBtn.setBackground(Theme.COLOR_GREEN);
        okBtn.setForeground(Color.WHITE);
        
        JButton cancelBtn = new JButton("CANCEL ALL");
        cancelBtn.setBackground(Color.RED);
        cancelBtn.setForeground(Color.WHITE);

        btnPanel.add(okBtn);
        btnPanel.add(cancelBtn);
        add(btnPanel, BorderLayout.SOUTH);

        // --- LOGIC ---
        okBtn.addActionListener(e -> {
            // Save choices back to the list
            for (int i = 0; i < table.getRowCount(); i++) {
                String action = (String) table.getValueAt(i, 3);
                conflictItems.get(i).resolution = action;
            }
            confirmed = true;
            dispose();
        });

        cancelBtn.addActionListener(e -> dispose());
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}