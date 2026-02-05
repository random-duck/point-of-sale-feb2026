import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;

public class OutgoingPanel extends JPanel {

    // --- COMPONENTS ---
    private JTabbedPane tabs;
    
    // TAB 1: CART MODE
    private DefaultTableModel inventoryModel; // Source
    private DefaultTableModel cartModel;      // Destination
    private JTable inventoryTable;
    private JTable cartTable;
    
    // TAB 2: QUICK MODE
    private DefaultTableModel quickModel;
    private JTable quickTable;

    public OutgoingPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.COLOR_CREAM);

        // --- HEADER ---
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.setBackground(Theme.COLOR_CREAM);
        JLabel title = new JLabel("OUTGOING / EXPORT");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.COLOR_DARK);
        header.add(title);
        add(header, BorderLayout.NORTH);

        // --- TABS ---
        tabs = new JTabbedPane();
        tabs.setFont(Theme.FONT_BOLD);
        
        tabs.addTab("Cart Mode (Select Items)", createCartPanel());
        tabs.addTab("Quick Mode (Bulk Edit)", createQuickPanel());
        
        add(tabs, BorderLayout.CENTER);

        // Load data initially
        refreshData();
    }

    // ==========================================
    // TAB 1: CART MODE (Split Screen)
    // ==========================================
    private JPanel createCartPanel() {
        JPanel p = new JPanel(new GridLayout(1, 2, 10, 0)); // Split Left/Right
        p.setBackground(Theme.COLOR_CREAM);

        // --- LEFT: INVENTORY SOURCE ---
        inventoryModel = new DefaultTableModel(new String[]{"ID", "Name", "Stock", "Price"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        inventoryTable = new JTable(inventoryModel);
        inventoryTable.getColumnModel().getColumn(0).setMinWidth(0); // Hide ID
        inventoryTable.getColumnModel().getColumn(0).setMaxWidth(0);

        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(BorderFactory.createTitledBorder("1. Double-click to Add"));
        left.add(new JScrollPane(inventoryTable));

        // Event: Double Click to Add
        inventoryTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    addToCart();
                }
            }
        });

        // --- RIGHT: EXPORT CART ---
        cartModel = new DefaultTableModel(new String[]{"ID", "Name", "Export Qty"}, 0);
        cartTable = new JTable(cartModel);
        cartTable.getColumnModel().getColumn(0).setMinWidth(0); // Hide ID
        cartTable.getColumnModel().getColumn(0).setMaxWidth(0);

        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(BorderFactory.createTitledBorder("2. Items to Export"));
        right.add(new JScrollPane(cartTable));

        // Export Button
        JButton exportBtn = new JButton("CONFIRM & EXPORT CSV");
        styleButton(exportBtn);
        exportBtn.addActionListener(e -> processExport(cartTable, 2)); // 2 is the Qty Column Index

        right.add(exportBtn, BorderLayout.SOUTH);

        p.add(left);
        p.add(right);
        return p;
    }

    // ==========================================
    // TAB 2: QUICK MODE (Single Table)
    // ==========================================
    private JPanel createQuickPanel() {
        JPanel p = new JPanel(new BorderLayout());
        
        // Columns: ID, Name, Current Stock, EXPORT QTY (Editable)
        quickModel = new DefaultTableModel(new String[]{"ID", "Name", "Current Stock", "EXPORT QTY"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 3; // Only allow editing the last column
            }
        };
        
        quickTable = new JTable(quickModel);
        quickTable.setRowHeight(30);
        // Hide ID column
        quickTable.getColumnModel().getColumn(0).setMinWidth(0);
        quickTable.getColumnModel().getColumn(0).setMaxWidth(0);
        
        // Style: Make the editable column look different
        quickTable.getColumnModel().getColumn(3).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(new Color(230, 255, 230)); // Light Green background for editable area
                c.setFont(new Font("SansSerif", Font.BOLD, 14));
                return c;
            }
        });

        JButton exportBtn = new JButton("PROCESS BULK EXPORT");
        styleButton(exportBtn);
        exportBtn.addActionListener(e -> processExport(quickTable, 3)); // 3 is Qty Column

        p.add(new JScrollPane(quickTable), BorderLayout.CENTER);
        p.add(exportBtn, BorderLayout.SOUTH);
        return p;
    }

    // ==========================================
    // LOGIC & ACTIONS
    // ==========================================

    private void refreshData() {
        List<Document> products = Database.getProducts("All");
        
        inventoryModel.setRowCount(0);
        quickModel.setRowCount(0);

        for (Document d : products) {
            String id = d.getObjectId("_id").toString();
            String name = d.getString("name");
            int stock = d.getInteger("quantity");
            double price = d.getDouble("price");

            // Add to Tab 1 Source
            inventoryModel.addRow(new Object[]{id, name, stock, price});
            
            // Add to Tab 2 Quick List (Default Export Qty = 0)
            quickModel.addRow(new Object[]{id, name, stock, 0});
        }
    }

    private void addToCart() {
        int row = inventoryTable.getSelectedRow();
        if (row == -1) return;

        String id = (String) inventoryModel.getValueAt(row, 0);
        String name = (String) inventoryModel.getValueAt(row, 1);
        
        String input = JOptionPane.showInputDialog(this, "Enter Quantity to Export for " + name + ":");
        if (input == null || input.isEmpty()) return;

        try {
            int qty = Integer.parseInt(input);
            if (qty <= 0) return;
            cartModel.addRow(new Object[]{id, name, qty});
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Number");
        }
    }

    /**
     * The Master Method: Handles validation, DB updates, and CSV generation
     * @param table The source table (Cart or Quick)
     * @param qtyColIndex Which column holds the "Export Amount"
     */
    private void processExport(JTable table, int qtyColIndex) {
        // 1. Gather Items with Qty > 0
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        ArrayList<ExportItem> itemsToExport = new ArrayList<>();
        boolean negativeStockDetected = false;
        StringBuilder warningMsg = new StringBuilder("Warning: The following items have insufficient stock:\n");

        for (int i = 0; i < model.getRowCount(); i++) {
            try {
                Object qtyObj = model.getValueAt(i, qtyColIndex);
                int exportQty = Integer.parseInt(qtyObj.toString());

                if (exportQty > 0) {
                    String idStr = (String) model.getValueAt(i, 0); // ID is always col 0
                    String name = (String) model.getValueAt(i, 1);  // Name is always col 1
                    
                    // Check Current Stock from DB (Most accurate)
                    Document doc = Database.getDatabase().getCollection("products").find(new Document("_id", new ObjectId(idStr))).first();
                    int currentStock = doc.getInteger("quantity");

                    if (exportQty > currentStock) {
                        negativeStockDetected = true;
                        warningMsg.append("- ").append(name).append(": Stock ").append(currentStock).append(", Exporting ").append(exportQty).append("\n");
                    }

                    // Save nested dimensions to re-save them later (since updateProduct requires them)
                    Document dims = (Document) doc.get("dimensions");
                    ExportItem item = new ExportItem();
                    item.id = new ObjectId(idStr);
                    item.name = name;
                    item.category = doc.getString("category");
                    item.price = doc.getDouble("price");
                    item.currentQty = currentStock;
                    item.exportQty = exportQty;
                    item.h = dims.getDouble("height");
                    item.w = dims.getDouble("width");
                    item.weight = dims.getDouble("weight");
                    
                    itemsToExport.add(item);
                }
            } catch (Exception ex) {
                // Skip rows with invalid numbers (or 0)
            }
        }

        if (itemsToExport.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No items selected to export.");
            return;
        }

        // 2. Validation Popup
        if (negativeStockDetected) {
            warningMsg.append("\nDo you want to proceed? Inventory will become negative.");
            int choice = JOptionPane.showConfirmDialog(this, warningMsg.toString(), "Insufficient Stock", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.NO_OPTION) {
                return; // Cancel
            }
        }

        // 3. Select Save Location
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Export File");
        chooser.setSelectedFile(new File("Export_" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".csv"));
        
        int userSelection = chooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = chooser.getSelectedFile();
            
            // 4. Update Database
            for (ExportItem item : itemsToExport) {
                int newQty = item.currentQty - item.exportQty;
                Database.updateProduct(item.id, item.name, item.category, item.price, newQty, item.h, item.w, item.weight);
            }

            // 5. Generate CSV (We create a temp table just for the CSV output)
            DefaultTableModel csvModel = new DefaultTableModel(new String[]{"Name", "Category", "Price", "Exported Qty", "Remaining Stock", "Date"}, 0);
            String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
            
            for (ExportItem item : itemsToExport) {
                csvModel.addRow(new Object[]{
                    item.name, item.category, item.price, item.exportQty, (item.currentQty - item.exportQty), dateStr
                });
            }
            JTable csvTable = new JTable(csvModel);
            
            boolean success = CSVExporter.exportTableToCSV(csvTable, fileToSave);
            
            if (success) {
                JOptionPane.showMessageDialog(this, "Export Successful!\nInventory Updated.");
                refreshData(); // Reload UI
                cartModel.setRowCount(0); // Clear cart
            } else {
                JOptionPane.showMessageDialog(this, "Error saving file.");
            }
        }
    }

    private void styleButton(JButton b) {
        b.setFont(Theme.FONT_BOLD);
        b.setBackground(Theme.COLOR_DARK);
        b.setForeground(Color.WHITE);
        b.setPreferredSize(new Dimension(200, 40));
    }

    // Helper class
    class ExportItem {
        ObjectId id;
        String name, category;
        double price, h, w, weight;
        int currentQty, exportQty;
    }
}