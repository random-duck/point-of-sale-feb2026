import javax.swing.*;
import java.awt.*;
import org.bson.Document;
import org.bson.types.ObjectId;

public class ProductDetailDialog extends JDialog {

    private boolean dataChanged = false; // To tell the main screen if it needs to refresh

    public ProductDetailDialog(JFrame parent, Document product) {
        super(parent, "Edit Product Details", true);
        setSize(500, 600);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.COLOR_CREAM);

        // --- DATA PREPARATION ---
        ObjectId id = product.getObjectId("_id");
        String name = product.getString("name");
        String category = product.getString("category");
        double price = product.getDouble("price");
        int quantity = product.getInteger("quantity");
        
        // Handle nested dimensions safely
        Document dim = (Document) product.get("dimensions");
        double h = dim != null ? dim.getDouble("height") : 0;
        double w = dim != null ? dim.getDouble("width") : 0;
        double weight = dim != null ? dim.getDouble("weight") : 0;
        
        String imgPath = product.getString("imagePath");

        // --- UI LAYOUT ---
        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.setBackground(Theme.COLOR_CREAM);
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Fields
        form.add(new JLabel("Name:"));
        JTextField nameField = new JTextField(name);
        form.add(nameField);

        form.add(new JLabel("Category:"));
        String[] cats = {"Furniture", "Utensils", "Appliances", "Electronics", "Decor", "N/A"};
        JComboBox<String> catBox = new JComboBox<>(cats);
        catBox.setSelectedItem(category);
        form.add(catBox);

        form.add(new JLabel("Price (PHP):"));
        JTextField priceField = new JTextField(String.valueOf(price));
        form.add(priceField);

        form.add(new JLabel("Quantity:"));
        JTextField qtyField = new JTextField(String.valueOf(quantity));
        form.add(qtyField);

        form.add(new JLabel("Height (cm):"));
        JTextField hField = new JTextField(String.valueOf(h));
        form.add(hField);

        form.add(new JLabel("Width (cm):"));
        JTextField wField = new JTextField(String.valueOf(w));
        form.add(wField);

        form.add(new JLabel("Weight (kg):"));
        JTextField weightField = new JTextField(String.valueOf(weight));
        form.add(weightField);

        // Image Preview (Non-editable here for simplicity)
        JLabel imgLabel = new JLabel();
        imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
        if (imgPath != null && !imgPath.isEmpty()) {
            ImageIcon icon = new ImageIcon(imgPath);
            Image scaled = icon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
            imgLabel.setIcon(new ImageIcon(scaled));
        } else {
            imgLabel.setText("No Image Available");
        }

        // Buttons
        JButton saveBtn = new JButton("SAVE CHANGES");
        saveBtn.setBackground(Theme.COLOR_GREEN);
        saveBtn.setForeground(Color.WHITE);

        JButton deleteBtn = new JButton("DELETE ITEM");
        deleteBtn.setBackground(Color.RED);
        deleteBtn.setForeground(Color.WHITE);

        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(Theme.COLOR_CREAM);
        btnPanel.add(saveBtn);
        btnPanel.add(deleteBtn);

        // Add to Dialog
        add(imgLabel, BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        // --- ACTIONS ---
        
        saveBtn.addActionListener(e -> {
            try {
                // Parse numbers
                double p = Double.parseDouble(priceField.getText());
                int q = Integer.parseInt(qtyField.getText());
                double nh = Double.parseDouble(hField.getText());
                double nw = Double.parseDouble(wField.getText());
                double nweight = Double.parseDouble(weightField.getText());

                boolean success = Database.updateProduct(id, nameField.getText(), catBox.getSelectedItem().toString(), p, q, nh, nw, nweight);
                
                if (success) {
                    JOptionPane.showMessageDialog(this, "Updated Successfully!");
                    dataChanged = true;
                    dispose(); // Close popup
                } else {
                    JOptionPane.showMessageDialog(this, "Update Failed.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid Number Format.");
            }
        });

        deleteBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this item?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (Database.deleteProduct(id)) {
                    JOptionPane.showMessageDialog(this, "Item Deleted.");
                    dataChanged = true;
                    dispose();
                }
            }
        });
    }

    public boolean isDataChanged() {
        return dataChanged;
    }
}