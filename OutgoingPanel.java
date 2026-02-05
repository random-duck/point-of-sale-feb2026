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

        // --- AUTO-REFRESH LISTENER ---
        this.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                refreshData();
            }
        });
    }

    // ==========================================
    // TAB 1: CART MODE
    // ==========================================
    private JPanel createCartPanel() {
        JPanel p = new JPanel(new GridLayout(1, 2, 10, 0)); 
        p.setBackground(Theme.COLOR_CREAM);

        // --- LEFT: INVENTORY SOURCE ---
        inventoryModel = new DefaultTableModel(new String[]{"ID", "Name", "Stock", "Price"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        inventoryTable = new JTable(inventoryModel);
        inventoryTable.getColumnModel().getColumn(0).setMinWidth(0);
        inventoryTable.getColumnModel().getColumn(0).setMaxWidth(0);

        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(BorderFactory.createTitledBorder("1. Double-click to Add"));
        left.add(new JScrollPane(inventoryTable));

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
        cartTable.getColumnModel().getColumn(0).setMinWidth(0);
        cartTable.getColumnModel().getColumn(0).setMaxWidth(0);

        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(BorderFactory.createTitledBorder("2. Items to Export"));
        right.add(new JScrollPane(cartTable));

        JButton exportBtn = new JButton("CONFIRM & EXPORT CSV");
        styleButton(exportBtn);
        exportBtn.addActionListener(e -> processExport(cartTable, 2));

        right.add(exportBtn, BorderLayout.SOUTH);

        p.add(left);
        p.add(right);
        return p;
    }

    // ==========================================
    // TAB 2: QUICK MODE
    // ==========================================
    private JPanel createQuickPanel() {
        JPanel p = new JPanel(new BorderLayout());
        
        quickModel = new DefaultTableModel(new String[]{"ID", "Name", "Current Stock", "EXPORT QTY"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 3; 
            }
        };
        
        quickTable = new JTable(quickModel);
        quickTable.setRowHeight(30);
        quickTable.getColumnModel().getColumn(0).setMinWidth(0);
        quickTable.getColumnModel().getColumn(0).setMaxWidth(0);
        
        quickTable.getColumnModel().getColumn(3).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(new Color(230, 255, 230)); 
                c.setFont(new Font("SansSerif", Font.BOLD, 14));
                return c;
            }
        });

        JButton exportBtn = new JButton("PROCESS BULK EXPORT");
        styleButton(exportBtn);
        exportBtn.addActionListener(e -> processExport(quickTable, 3)); 

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

            inventoryModel.addRow(new Object[]{id, name, stock, price});
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

    private void processExport(JTable table, int qtyColIndex) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        ArrayList<ExportItem> itemsToExport = new ArrayList<>();
        boolean negativeStockDetected = false;
        StringBuilder warningMsg = new StringBuilder("Warning: The following items have insufficient stock:\n");

        for (int i = 0; i < model.getRowCount(); i++) {
            try {
                Object qtyObj = model.getValueAt(i, qtyColIndex);
                int exportQty = Integer.parseInt(qtyObj.toString());

                if (exportQty > 0) {
                    String idStr = (String) model.getValueAt(i, 0); 
                    String name = (String) model.getValueAt(i, 1);  
                    
                    Document doc = Database.getDatabase().getCollection("products").find(new Document("_id", new ObjectId(idStr))).first();
                    int currentStock = doc.getInteger("quantity");

                    if (exportQty > currentStock) {
                        negativeStockDetected = true;
                        warningMsg.append("- ").append(name).append(": Stock ").append(currentStock).append(", Exporting ").append(exportQty).append("\n");
                    }

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
            } catch (Exception ex) { }
        }

        if (itemsToExport.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No items selected to export.");
            return;
        }

        if (negativeStockDetected) {
            warningMsg.append("\nDo you want to proceed? Inventory will become negative.");
            int choice = JOptionPane.showConfirmDialog(this, warningMsg.toString(), "Insufficient Stock", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.NO_OPTION) return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Export File");
        chooser.setSelectedFile(new File("Export_" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".csv"));
        
        int userSelection = chooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = chooser.getSelectedFile();
            
            for (ExportItem item : itemsToExport) {
                int newQty = item.currentQty - item.exportQty;
                Database.updateProduct(item.id, item.name, item.category, item.price, newQty, item.h, item.w, item.weight);
            }

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
                // --- CRITICAL PERFORMANCE TRACKING LOGIC ---
                int totalExportedItems = 0;
                for (ExportItem item : itemsToExport) {
                    totalExportedItems += item.exportQty;
                }
                // This saves the log to the database. Without this, the Employee Report will be 0.
                Database.saveActionLog(Session.currentUsername, "Export", totalExportedItems);
                // -------------------------------------------

                JOptionPane.showMessageDialog(this, "Export Successful!\nInventory Updated.");
                refreshData(); 
                cartModel.setRowCount(0); 
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

    class ExportItem {
        ObjectId id;
        String name, category;
        double price, h, w, weight;
        int currentQty, exportQty;
    }
}