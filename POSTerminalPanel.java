import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;

public class POSTerminalPanel extends JPanel {

    // COMPONENTS
    private JTextField searchField;
    private JTable productTable; // Source (Inventory)
    private DefaultTableModel productModel;
    
    private JTable cartTable;    // Destination (Cart)
    private DefaultTableModel cartModel;
    
    private JLabel totalLabel;
    private double currentTotal = 0.0;

    public POSTerminalPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.COLOR_CREAM);

        // --- HEADER ---
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.COLOR_DARK);
        header.setPreferredSize(new Dimension(0, 60));
        
        JLabel title = new JLabel("  CASHIER TERMINAL");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);
        
        add(header, BorderLayout.NORTH);

        // --- SPLIT PANE (Left: Lookup, Right: Cart) ---
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createProductLookupPanel(), createCartPanel());
        split.setDividerLocation(500); // Give product list more space
        split.setResizeWeight(0.5);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        // Load initial data
        loadProducts("");
    }

    // ==========================================
    // LEFT PANEL: PRODUCT LOOKUP
    // ==========================================
    private JPanel createProductLookupPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createTitledBorder("1. Search & Add Items"));

        // Search Bar
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBackground(Color.WHITE);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        searchField = new JTextField();
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        searchField.putClientProperty("JTextField.placeholderText", "Scan barcode or type name...");
        
        // Search Logic (Real-time filtering)
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                loadProducts(searchField.getText());
            }
        });
        
        searchPanel.add(new JLabel("Search: "), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        p.add(searchPanel, BorderLayout.NORTH);

        // Product Table
        productModel = new DefaultTableModel(new String[]{"ID", "Name", "Price", "Stock"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        productTable = new JTable(productModel);
        productTable.setRowHeight(30);
        productTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
        productTable.getColumnModel().getColumn(0).setMinWidth(0);
        productTable.getColumnModel().getColumn(0).setMaxWidth(0); // Hide ID

        // Double Click to Add to Cart
        productTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    addToCart();
                }
            }
        });

        p.add(new JScrollPane(productTable), BorderLayout.CENTER);
        return p;
    }

    // ==========================================
    // RIGHT PANEL: CART & PAYMENT
    // ==========================================
    private JPanel createCartPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.COLOR_CREAM);
        p.setBorder(BorderFactory.createTitledBorder("2. Current Transaction"));

        // Cart Table
        cartModel = new DefaultTableModel(new String[]{"ID", "Item", "Qty", "Subtotal"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        cartTable = new JTable(cartModel);
        cartTable.setRowHeight(30);
        cartTable.getColumnModel().getColumn(0).setMinWidth(0);
        cartTable.getColumnModel().getColumn(0).setMaxWidth(0); // Hide ID

        p.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        // Total & Pay Section
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, Theme.COLOR_GREEN));
        bottom.setPreferredSize(new Dimension(0, 100));

        // Total Label
        totalLabel = new JLabel("Total: PHP 0.00", SwingConstants.CENTER);
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        totalLabel.setForeground(Theme.COLOR_DARK);
        bottom.add(totalLabel, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton clearBtn = new JButton("CLEAR CART");
        clearBtn.setBackground(Color.RED);
        clearBtn.setForeground(Color.WHITE);
        clearBtn.addActionListener(e -> clearCart());

        JButton payBtn = new JButton("PAY NOW");
        payBtn.setFont(Theme.FONT_BOLD);
        payBtn.setBackground(Theme.COLOR_GREEN);
        payBtn.setForeground(Color.WHITE);
        payBtn.addActionListener(e -> processPayment());

        btnPanel.add(clearBtn);
        btnPanel.add(payBtn);
        bottom.add(btnPanel, BorderLayout.SOUTH);

        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    // ==========================================
    // LOGIC
    // ==========================================
    
    private void loadProducts(String query) {
        productModel.setRowCount(0);
        List<Document> products = Database.getProducts("All"); // Simple filter
        
        for (Document d : products) {
            String name = d.getString("name");
            // Simple case-insensitive search
            if (query.isEmpty() || name.toLowerCase().contains(query.toLowerCase())) {
                productModel.addRow(new Object[]{
                    d.getObjectId("_id"),
                    name,
                    d.getDouble("price"),
                    d.getInteger("quantity")
                });
            }
        }
    }

    private void addToCart() {
        int row = productTable.getSelectedRow();
        if (row == -1) return;

        ObjectId id = (ObjectId) productModel.getValueAt(row, 0);
        String name = (String) productModel.getValueAt(row, 1);
        double price = (Double) productModel.getValueAt(row, 2);
        int currentStock = (Integer) productModel.getValueAt(row, 3);

        // Check if item already in cart
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            ObjectId cartId = (ObjectId) cartModel.getValueAt(i, 0);
            if (cartId.equals(id)) {
                int currentQty = (Integer) cartModel.getValueAt(i, 2);
                if (currentQty + 1 > currentStock) {
                    JOptionPane.showMessageDialog(this, "Not enough stock!");
                    return;
                }
                // Update Qty
                cartModel.setValueAt(currentQty + 1, i, 2);
                cartModel.setValueAt((currentQty + 1) * price, i, 3);
                updateTotal();
                return;
            }
        }

        // Add new row if not found
        if (currentStock > 0) {
            cartModel.addRow(new Object[]{id, name, 1, price});
            updateTotal();
        } else {
            JOptionPane.showMessageDialog(this, "Out of Stock!");
        }
    }

    private void updateTotal() {
        currentTotal = 0.0;
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            currentTotal += (Double) cartModel.getValueAt(i, 3);
        }
        totalLabel.setText(String.format("Total: PHP %.2f", currentTotal));
    }

    private void clearCart() {
        cartModel.setRowCount(0);
        updateTotal();
    }

    private void processPayment() {
        if (cartModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Cart is empty.");
            return;
        }

        // 1. Ask for Cash
        String input = JOptionPane.showInputDialog(this, "Total is PHP " + currentTotal + "\nEnter Cash Amount:");
        if (input == null || input.isEmpty()) return;

        try {
            double cash = Double.parseDouble(input);
            if (cash < currentTotal) {
                JOptionPane.showMessageDialog(this, "Insufficient Cash!");
                return;
            }

            double change = cash - currentTotal;

            // 2. Prepare Data for Database
            List<Document> soldItems = new ArrayList<>();
            for (int i = 0; i < cartModel.getRowCount(); i++) {
                ObjectId id = (ObjectId) cartModel.getValueAt(i, 0);
                String name = (String) cartModel.getValueAt(i, 1);
                int qty = (Integer) cartModel.getValueAt(i, 2);
                
                // Add to list for history
                soldItems.add(new Document("name", name).append("qty", qty).append("price", cartModel.getValueAt(i, 3)));
                
                // 3. DEDUCT INVENTORY (Important!)
                // We need to fetch the full product details again to update it properly
                // For simplicity, we assume other fields didn't change, but in production, be careful.
                Document product = Database.getDatabase().getCollection("products").find(new Document("_id", id)).first();
                if (product != null) {
                    int newStock = product.getInteger("quantity") - qty;
                    Document dims = (Document) product.get("dimensions");
                    Database.updateProduct(id, name, product.getString("category"), product.getDouble("price"), newStock, 
                        dims.getDouble("height"), dims.getDouble("width"), dims.getDouble("weight"));
                }
            }

            // 4. Save Sales Record
            boolean saved = Database.saveSale(soldItems, currentTotal, cash, change);

            if (saved) {
                JOptionPane.showMessageDialog(this, String.format("Transaction Success!\n\nCHANGE: PHP %.2f", change));
                clearCart();
                loadProducts(searchField.getText()); // Refresh stock levels in list
            } else {
                JOptionPane.showMessageDialog(this, "Error saving transaction.");
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Amount.");
        }
    }
}