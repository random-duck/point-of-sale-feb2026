import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;

public class SupplyOrderPanel extends JPanel {

    private JTabbedPane tabs;
    
    // TAB 1: NEW ORDER
    private JComboBox<SupplierItem> supplierBox;
    private JTable orderTable;
    private DefaultTableModel orderModel;
    
    // TAB 2: SUPPLIERS
    private JTable supplierTable;
    private DefaultTableModel supplierModel;

    // TAB 3: HISTORY
    private JTable historyTable;
    private DefaultTableModel historyModel;

    public SupplyOrderPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.COLOR_CREAM);

        // Header
        JLabel title = new JLabel("  SUPPLY CHAIN MANAGEMENT");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.COLOR_DARK);
        title.setPreferredSize(new Dimension(0, 50));
        add(title, BorderLayout.NORTH);

        // Tabs
        tabs = new JTabbedPane();
        tabs.setFont(Theme.FONT_BOLD);
        
        tabs.addTab("Create Purchase Order", createOrderTab());
        tabs.addTab("Manage Suppliers", createSupplierTab());
        tabs.addTab("Order History", createHistoryTab());

        add(tabs, BorderLayout.CENTER);

        // Auto-refresh when opening tabs
        tabs.addChangeListener(e -> refreshAllData());
        refreshAllData();
    }

    // ==========================================
    // TAB 1: CREATE ORDER (With Quick Add Button)
    // ==========================================
    private JPanel createOrderTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.COLOR_CREAM);

        // --- TOP: Supplier Selection ---
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setBackground(Theme.COLOR_CREAM);
        
        JLabel lbl = new JLabel("Select Supplier: ");
        lbl.setFont(Theme.FONT_BOLD);
        top.add(lbl);

        supplierBox = new JComboBox<>();
        supplierBox.setPreferredSize(new Dimension(250, 30));
        top.add(supplierBox);

        // THE NEW "+" BUTTON (Quick Add)
        JButton quickAddBtn = new JButton("+");
        quickAddBtn.setBackground(Theme.COLOR_GREEN);
        quickAddBtn.setForeground(Color.WHITE);
        quickAddBtn.setToolTipText("Add New Supplier Instantly");
        quickAddBtn.addActionListener(e -> showQuickAddSupplierDialog());
        top.add(quickAddBtn);

        p.add(top, BorderLayout.NORTH);

        // --- CENTER: Items Table ---
        orderModel = new DefaultTableModel(new String[]{"ID", "Product Name", "Current Stock", "ORDER QTY"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 3; // Only Order Qty is editable
            }
        };
        orderTable = new JTable(orderModel);
        orderTable.setRowHeight(30);
        orderTable.getColumnModel().getColumn(0).setMinWidth(0);
        orderTable.getColumnModel().getColumn(0).setMaxWidth(0);
        
        // Highlight Editable Column
        orderTable.getColumnModel().getColumn(3).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(new Color(230, 240, 255)); 
                c.setFont(new Font("SansSerif", Font.BOLD, 14));
                return c;
            }
        });

        p.add(new JScrollPane(orderTable), BorderLayout.CENTER);

        // --- BOTTOM: Actions ---
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBackground(Theme.COLOR_CREAM);

        JButton autoFillBtn = new JButton("Auto-Fill Low Stock (Qty <= 5)");
        autoFillBtn.setBackground(Color.ORANGE);
        autoFillBtn.addActionListener(e -> autoFillLowStock());

        JButton sendBtn = new JButton("SEND ORDER TO SUPPLIER");
        sendBtn.setBackground(Theme.COLOR_GREEN);
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setFont(Theme.FONT_BOLD);
        sendBtn.setPreferredSize(new Dimension(250, 40));
        sendBtn.addActionListener(e -> sendOrder());

        bottom.add(autoFillBtn);
        bottom.add(Box.createHorizontalStrut(20));
        bottom.add(sendBtn);
        p.add(bottom, BorderLayout.SOUTH);

        return p;
    }

    // --- QUICK ADD POPUP ---
    private void showQuickAddSupplierDialog() {
        JTextField nameF = new JTextField();
        JTextField emailF = new JTextField();
        JTextField catF = new JTextField(); // New Category Field
        
        Object[] message = {
            "Supplier Name:", nameF,
            "Category (e.g. Furniture):", catF,
            "Email:", emailF,
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Quick Add Supplier", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            if (nameF.getText().isEmpty()) return;
            // Save with default placeholders for phone/address
            Database.addSupplier(nameF.getText(), emailF.getText(), "N/A", "N/A", catF.getText());
            refreshSupplierData(); // Reload dropdown
            
            // Select the new one automatically
            supplierBox.setSelectedIndex(supplierBox.getItemCount() - 1);
        }
    }

    private void autoFillLowStock() {
        refreshOrderTable();
        int count = 0;
        for (int i = 0; i < orderModel.getRowCount(); i++) {
            int currentStock = (Integer) orderModel.getValueAt(i, 2);
            if (currentStock <= 5) {
                int suggestedOrder = 20 - currentStock;
                orderModel.setValueAt(suggestedOrder, i, 3);
                count++;
            }
        }
        if (count > 0) JOptionPane.showMessageDialog(this, "Auto-filled " + count + " items.");
        else JOptionPane.showMessageDialog(this, "No low stock items found.");
    }

    private void sendOrder() {
        SupplierItem supplier = (SupplierItem) supplierBox.getSelectedItem();
        if (supplier == null) {
            JOptionPane.showMessageDialog(this, "Please select a supplier first.");
            return;
        }

        List<Document> orderItems = new ArrayList<>();
        StringBuilder receipt = new StringBuilder();
        receipt.append("PURCHASE ORDER\n");
        receipt.append("To: ").append(supplier.name).append(" (").append(supplier.category).append(")\n");
        receipt.append("Email: ").append(supplier.email).append("\n");
        receipt.append("Date: ").append(new Date()).append("\n");
        receipt.append("------------------------------------------------\n");
        receipt.append(String.format("%-20s %-10s\n", "Item", "Qty"));
        receipt.append("------------------------------------------------\n");

        boolean hasItems = false;
        for (int i = 0; i < orderModel.getRowCount(); i++) {
            Object val = orderModel.getValueAt(i, 3);
            int qty = (val instanceof String) ? Integer.parseInt((String)val) : (Integer)val;

            if (qty > 0) {
                hasItems = true;
                String name = (String) orderModel.getValueAt(i, 1);
                orderItems.add(new Document("name", name).append("qty", qty));
                receipt.append(String.format("%-20s %-10d\n", name, qty));
            }
        }

        if (!hasItems) {
            JOptionPane.showMessageDialog(this, "No items selected (Quantity is 0).");
            return;
        }

        Database.savePurchaseOrder(supplier.name, supplier.email, orderItems, 0.0);

        try {
            String filename = "PO_" + supplier.name.replaceAll(" ", "_") + "_" + System.currentTimeMillis() + ".txt";
            FileWriter writer = new FileWriter(new File(filename));
            writer.write(receipt.toString());
            writer.close();
            
            JOptionPane.showMessageDialog(this, "<html>Order Sent Successfully!<br>Receipt saved: " + filename + "</html>");
            refreshOrderTable();
            refreshHistoryTable();
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ==========================================
    // TAB 2: SUPPLIERS (Updated with Category)
    // ==========================================
    private JPanel createSupplierTab() {
        JPanel p = new JPanel(new BorderLayout());
        
        // Added "Category" Column
        supplierModel = new DefaultTableModel(new String[]{"ID", "Name", "Category", "Email", "Phone", "Address"}, 0);
        supplierTable = new JTable(supplierModel);
        supplierTable.getColumnModel().getColumn(0).setMinWidth(0);
        supplierTable.getColumnModel().getColumn(0).setMaxWidth(0);

        JPanel form = new JPanel(new GridLayout(2, 6)); // 2 Rows to fit everything
        
        JTextField nameF = new JTextField();
        JTextField catF = new JTextField(); // New Input
        JTextField emailF = new JTextField();
        JTextField phoneF = new JTextField();
        JTextField addrF = new JTextField();
        JButton addBtn = new JButton("ADD SUPPLIER");
        addBtn.setBackground(Theme.COLOR_GREEN);
        addBtn.setForeground(Color.WHITE);

        form.add(new JLabel("Name:")); form.add(nameF);
        form.add(new JLabel("Category:")); form.add(catF);
        form.add(new JLabel("Email:")); form.add(emailF);
        form.add(new JLabel("Phone:")); form.add(phoneF);
        form.add(new JLabel("Address:")); form.add(addrF);
        form.add(addBtn);

        p.add(new JScrollPane(supplierTable), BorderLayout.CENTER);
        p.add(form, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> {
            if (Database.addSupplier(nameF.getText(), emailF.getText(), phoneF.getText(), addrF.getText(), catF.getText())) {
                nameF.setText(""); emailF.setText(""); phoneF.setText(""); addrF.setText(""); catF.setText("");
                refreshSupplierData();
            }
        });

        // Right click delete logic (same as before)
        supplierTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = supplierTable.rowAtPoint(e.getPoint());
                    supplierTable.setRowSelectionInterval(row, row);
                    
                    JPopupMenu menu = new JPopupMenu();
                    JMenuItem del = new JMenuItem("Delete Supplier");
                    del.addActionListener(ev -> {
                        ObjectId id = (ObjectId) supplierModel.getValueAt(row, 0);
                        Database.deleteSupplier(id);
                        refreshSupplierData();
                    });
                    menu.add(del);
                    menu.show(supplierTable, e.getX(), e.getY());
                }
            }
        });

        return p;
    }

    // ==========================================
    // TAB 3: HISTORY
    // ==========================================
    private JPanel createHistoryTab() {
        JPanel p = new JPanel(new BorderLayout());
        historyModel = new DefaultTableModel(new String[]{"Date", "Supplier", "Items Count", "Status"}, 0);
        historyTable = new JTable(historyModel);
        p.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        return p;
    }

    // ==========================================
    // DATA HELPERS
    // ==========================================
    private void refreshAllData() {
        refreshOrderTable();
        refreshSupplierData();
        refreshHistoryTable();
    }

    private void refreshOrderTable() {
        orderModel.setRowCount(0);
        List<Document> products = Database.getProducts("All");
        for (Document d : products) {
            orderModel.addRow(new Object[]{
                d.getObjectId("_id"),
                d.getString("name"),
                d.getInteger("quantity"),
                0 
            });
        }
    }

    private void refreshSupplierData() {
        supplierModel.setRowCount(0);
        supplierBox.removeAllItems();
        
        List<Document> suppliers = Database.getSuppliers();
        for (Document d : suppliers) {
            String cat = d.getString("category"); // Retrieve Category
            if(cat == null) cat = "General";      // Default if missing
            
            supplierModel.addRow(new Object[]{ 
                d.getObjectId("_id"), 
                d.getString("name"), 
                cat, 
                d.getString("email"), 
                d.getString("phone"), 
                d.getString("address") 
            });
            
            supplierBox.addItem(new SupplierItem(d.getString("name"), d.getString("email"), cat));
        }
    }

    private void refreshHistoryTable() {
        historyModel.setRowCount(0);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        List<Document> orders = Database.getPurchaseOrders();
        for (Document d : orders) {
            List items = (List) d.get("items");
            historyModel.addRow(new Object[]{
                sdf.format(d.getDate("date")),
                d.getString("supplier"),
                items.size() + " items",
                d.getString("status")
            });
        }
    }

    class SupplierItem {
        String name, email, category;
        public SupplierItem(String n, String e, String c) { name = n; email = e; category = c; }
        public String toString() { return name + " (" + category + ")"; } // Shows "IKEA (Furniture)" in dropdown
    }
}