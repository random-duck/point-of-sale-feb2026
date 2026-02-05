import java.awt.*;
import java.io.File;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

public class AddProductPanel extends JPanel {

    // Store the selected image path here
    private String selectedImagePath = "";
    private JLabel imagePathLabel;

    public AddProductPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.COLOR_CREAM);
        setBorder(new EmptyBorder(30, 50, 30, 50)); // Margin

        // --- HEADER ---
        JLabel title = new JLabel("INCOMING ITEM ENTRY");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.COLOR_DARK);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title, BorderLayout.NORTH);

        // --- FORM PANEL (GridBagLayout) ---
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Theme.COLOR_CREAM);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); // Spacing
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;

        // 1. Product Name
        addLabel(formPanel, "Product Name:", gbc);
        JTextField nameField = new JTextField(20);
        styleField(nameField);
        gbc.gridx = 1; formPanel.add(nameField, gbc);

        // 2. Category (Updated to be Broader + N/A)
        gbc.gridx = 0; gbc.gridy++;
        addLabel(formPanel, "Category:", gbc);
        // "Furniture" covers sofas/tables, "Utensils" covers kitchenware, "N/A" for others
        String[] cats = {"Furniture", "Utensils", "Appliances", "Electronics", "Decor", "N/A"};
        JComboBox<String> catBox = new JComboBox<>(cats);
        catBox.setBackground(Color.WHITE);
        gbc.gridx = 1; formPanel.add(catBox, gbc);

        // 3. Price (PHP) & Quantity
        gbc.gridx = 0; gbc.gridy++;
        addLabel(formPanel, "Price (PHP):", gbc);
        JTextField priceField = new JTextField(10);
        styleField(priceField);
        gbc.gridx = 1; formPanel.add(priceField, gbc);

        gbc.gridx = 0; gbc.gridy++;
        addLabel(formPanel, "Quantity:", gbc);
        JTextField qtyField = new JTextField(10);
        styleField(qtyField);
        gbc.gridx = 1; formPanel.add(qtyField, gbc);

        // 4. Dimensions (Height, Width, Weight) - Now Required Numbers
        gbc.gridx = 0; gbc.gridy++;
        addLabel(formPanel, "Height (cm):", gbc);
        JTextField hField = new JTextField(); styleField(hField);
        gbc.gridx = 1; formPanel.add(hField, gbc);

        gbc.gridx = 0; gbc.gridy++;
        addLabel(formPanel, "Width (cm):", gbc);
        JTextField wField = new JTextField(); styleField(wField);
        gbc.gridx = 1; formPanel.add(wField, gbc);

        gbc.gridx = 0; gbc.gridy++;
        addLabel(formPanel, "Weight (kg):", gbc);
        JTextField weightField = new JTextField(); styleField(weightField);
        gbc.gridx = 1; formPanel.add(weightField, gbc);

        // 5. Image Selection
        gbc.gridx = 0; gbc.gridy++;
        addLabel(formPanel, "Product Image:", gbc);
        
        JButton uploadBtn = new JButton("Select Image...");
        styleButtonSmall(uploadBtn);
        
        imagePathLabel = new JLabel("No file selected");
        imagePathLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        
        JPanel imagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        imagePanel.setBackground(Theme.COLOR_CREAM);
        imagePanel.add(uploadBtn);
        imagePanel.add(imagePathLabel);
        
        gbc.gridx = 1; formPanel.add(imagePanel, gbc);

        // --- IMAGE CHOOSER ACTION ---
        uploadBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Images (JPG, PNG)", "jpg", "png", "jpeg");
            chooser.setFileFilter(filter);
            
            int returnVal = chooser.showOpenDialog(this);
            if(returnVal == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                selectedImagePath = file.getAbsolutePath();
                imagePathLabel.setText(file.getName());
            }
        });

        // --- BUTTONS (Submit & Import) ---
        
        JButton submitBtn = new JButton("ADD ITEM");
        styleButtonLarge(submitBtn);

        // New Import Button
        JButton importBtn = new JButton("BULK IMPORT (CSV)");
        importBtn.setFont(Theme.FONT_TITLE);
        importBtn.setBackground(Theme.COLOR_ACCENT); // Different color to distinguish
        importBtn.setForeground(Color.WHITE);
        importBtn.setPreferredSize(new Dimension(250, 50));
        importBtn.setFocusPainted(false);

        
        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(Theme.COLOR_CREAM);
        bottomPanel.add(submitBtn);
        bottomPanel.add(importBtn); // Add the import button here
        add(bottomPanel, BorderLayout.SOUTH);

        // --- IMPORT LOGIC ---
        importBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select CSV File");
            fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));
            
            int option = fileChooser.showOpenDialog(this);
            if (option == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                // Call our helper class to process the CSV
                CSVImporter.importCSV((JFrame) SwingUtilities.getWindowAncestor(this), file);
            }
        });

        // --- SUBMIT LOGIC: VALIDATION & SAVE ---
        submitBtn.addActionListener(e -> {
            
            // 1. Check Text Fields (Name)
            if (nameField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Product Name is required!");
                return;
            }

            // 2. Validate Numbers (Price, Qty, Dimensions)
            double price, height, width, weight;
            int quantity;

            try {
                price = Double.parseDouble(priceField.getText());
                quantity = Integer.parseInt(qtyField.getText());
                height = Double.parseDouble(hField.getText());
                width = Double.parseDouble(wField.getText());
                weight = Double.parseDouble(weightField.getText());

                if (price < 0 || quantity < 0 || height < 0 || width < 0 || weight < 0) {
                      JOptionPane.showMessageDialog(this, "Values cannot be negative.");
                      return;
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Invalid Input!\n\n" +
                    "- Price, Height, Width, and Weight must be numbers (Decimals allowed).\n" +
                    "- Quantity must be a whole number.\n" +
                    "- No fields can be left empty.", 
                    "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 3. Get other values
            String name = nameField.getText();
            String cat = catBox.getSelectedItem().toString();

            // 4. Save to Database
            boolean success = Database.addProduct(name, cat, price, quantity, height, width, weight, selectedImagePath);

            if (success) {
                JOptionPane.showMessageDialog(this, "Item Added Successfully!");
                // Clear form
                nameField.setText("");
                priceField.setText("");
                qtyField.setText("");
                hField.setText("");
                wField.setText("");
                weightField.setText("");
                imagePathLabel.setText("No file selected");
                selectedImagePath = "";
            } else {
                JOptionPane.showMessageDialog(this, "Database Error: Could not save item.");
            }
        });
    }

    // --- STYLING HELPERS ---
    private void addLabel(JPanel p, String text, GridBagConstraints gbc) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.FONT_BOLD);
        l.setForeground(Theme.COLOR_GREEN);
        p.add(l, gbc);
    }

    private void styleField(JTextField f) {
        f.setFont(Theme.FONT_REGULAR);
        f.setBorder(BorderFactory.createLineBorder(Theme.COLOR_GREEN));
        f.setPreferredSize(new Dimension(200, 25)); // Make input boxes uniform size
    }

    private void styleButtonSmall(JButton b) {
        b.setBackground(Theme.COLOR_GREEN);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
    }

    private void styleButtonLarge(JButton b) {
        b.setFont(Theme.FONT_TITLE);
        b.setBackground(Theme.COLOR_DARK);
        b.setForeground(Color.WHITE);
        b.setPreferredSize(new Dimension(250, 50));
        b.setFocusPainted(false);
    }
}