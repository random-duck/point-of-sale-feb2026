import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.bson.Document;
import org.bson.types.ObjectId;

public class ProductGalleryPanel extends JPanel {

    private JPanel gridPanel;
    private JTextField searchField;
    private JComboBox<String> filterBox;

    public ProductGalleryPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.COLOR_CREAM);

        // --- TOP BAR ---
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        topBar.setBackground(Theme.COLOR_CREAM);
        
        JLabel title = new JLabel("PRODUCT GALLERY");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.COLOR_DARK);

        searchField = new JTextField(20);
        JButton searchBtn = new JButton("Search");
        searchBtn.setBackground(Theme.COLOR_DARK);
        searchBtn.setForeground(Color.WHITE);
        
        String[] cats = {"All", "Furniture", "Utensils", "Appliances", "Electronics", "Decor", "N/A"};
        filterBox = new JComboBox<>(cats);
        
        topBar.add(title);
        topBar.add(new JLabel("   Filter:"));
        topBar.add(filterBox);
        topBar.add(new JLabel("   Search:"));
        topBar.add(searchField);
        topBar.add(searchBtn);
        
        add(topBar, BorderLayout.NORTH);

        // --- GRID AREA ---
        gridPanel = new JPanel(new GridLayout(0, 4, 10, 10)); 
        gridPanel.setBackground(Theme.COLOR_CREAM);
        gridPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // --- ACTIONS ---
        searchBtn.addActionListener(e -> loadProducts());
        filterBox.addActionListener(e -> loadProducts());
        
        this.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent e) { loadProducts(); }
        });

        loadProducts();
    }

    private void loadProducts() {
        gridPanel.removeAll();
        
        String cat = filterBox.getSelectedItem().toString();
        List<Document> products = Database.getProducts(cat);
        String search = searchField.getText().toLowerCase();

        for (Document p : products) {
            if (!search.isEmpty() && !p.getString("name").toLowerCase().contains(search)) continue;

            JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            wrapper.setBackground(Theme.COLOR_CREAM);
            wrapper.add(createProductCard(p));
            
            gridPanel.add(wrapper);
        }
        
        while (gridPanel.getComponentCount() < 4) {
            JPanel placeholder = new JPanel();
            placeholder.setOpaque(false);
            gridPanel.add(placeholder);
        }

        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private JPanel createProductCard(Document p) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        card.setPreferredSize(new Dimension(220, 280));

        // 1. IMAGE
        JLabel imgLabel = new JLabel();
        imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imgLabel.setPreferredSize(new Dimension(220, 150));
        
        String imageString = p.getString("imagePath");
        ImageIcon icon = null;
        if (imageString != null && !imageString.isEmpty()) {
            icon = ImageUtils.decodeImage(imageString);
        }

        if (icon == null) {
            imgLabel.setText("No Image");
            imgLabel.setBackground(Color.decode("#EEEEEE"));
            imgLabel.setOpaque(true);
        } else {
            imgLabel.setIcon(icon);
        }
        card.add(imgLabel, BorderLayout.CENTER);

        // 2. INFO
        JPanel info = new JPanel(new GridLayout(3, 1));
        info.setBackground(Color.WHITE);
        info.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));
        
        JLabel nameLbl = new JLabel(p.getString("name"));
        nameLbl.setFont(Theme.FONT_BOLD);
        
        JLabel priceLbl = new JLabel("PHP " + String.format("%,.2f", p.getDouble("price")));
        priceLbl.setForeground(Theme.COLOR_GREEN);
        
        JLabel stockLbl = new JLabel("Stock: " + p.getInteger("quantity"));
        if (p.getInteger("quantity") <= 5) stockLbl.setForeground(Color.RED);

        info.add(nameLbl);
        info.add(priceLbl);
        info.add(stockLbl);
        card.add(info, BorderLayout.SOUTH);
        
        // 3. CLICKS (THE FIX: Timer Logic)
        MouseAdapter listener = new MouseAdapter() {
            private Timer clickTimer; // To distinguish single vs double click

            public void mouseClicked(MouseEvent e) {
                if (!Session.isAdmin()) return; 

                if (SwingUtilities.isRightMouseButton(e)) {
                    // Right Click: DELETE
                    int choice = JOptionPane.showConfirmDialog(ProductGalleryPanel.this, "Delete " + p.getString("name") + "?");
                    if (choice == JOptionPane.YES_OPTION) {
                        Database.deleteProduct(p.getObjectId("_id"));
                        loadProducts();
                    }
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() == 2) {
                        // DOUBLE CLICK: Stop timer and show Full Edit
                        if (clickTimer != null && clickTimer.isRunning()) {
                            clickTimer.stop();
                        }
                        showFullEditDialog(p);
                    } else if (e.getClickCount() == 1) {
                        // SINGLE CLICK: Start timer, wait 300ms
                        clickTimer = new Timer(300, evt -> {
                            showQuickEditDialog(p);
                        });
                        clickTimer.setRepeats(false);
                        clickTimer.start();
                    }
                }
            }
        };
        
        card.addMouseListener(listener);
        imgLabel.addMouseListener(listener);
        info.addMouseListener(listener);
        nameLbl.addMouseListener(listener);

        return card;
    }

    // --- QUICK EDIT ---
    private void showQuickEditDialog(Document p) {
        JTextField priceField = new JTextField(String.valueOf(p.getDouble("price")));
        JTextField stockField = new JTextField(String.valueOf(p.getInteger("quantity")));
        
        Object[] message = {
            "Quick Edit: " + p.getString("name"),
            "New Price:", priceField,
            "New Stock:", stockField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Quick Edit", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                double newPrice = Double.parseDouble(priceField.getText());
                int newStock = Integer.parseInt(stockField.getText());
                Document dims = (Document) p.get("dimensions");
                
                Database.updateProduct(p.getObjectId("_id"), p.getString("name"), p.getString("category"), 
                                       newPrice, newStock, 
                                       dims.getDouble("height"), dims.getDouble("width"), dims.getDouble("weight"));
                
                loadProducts(); 
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid Numbers");
            }
        }
    }

    // --- FULL EDIT ---
    private void showFullEditDialog(Document p) {
        JTextField nameF = new JTextField(p.getString("name"));
        JTextField priceF = new JTextField(String.valueOf(p.getDouble("price")));
        JTextField qtyF = new JTextField(String.valueOf(p.getInteger("quantity")));
        
        String[] cats = {"Furniture", "Utensils", "Appliances", "Electronics", "Decor", "N/A"};
        JComboBox<String> catBox = new JComboBox<>(cats);
        catBox.setSelectedItem(p.getString("category"));

        Document dims = (Document) p.get("dimensions");
        JTextField hF = new JTextField(String.valueOf(dims.getDouble("height")));
        JTextField wF = new JTextField(String.valueOf(dims.getDouble("width")));
        JTextField weightF = new JTextField(String.valueOf(dims.getDouble("weight")));

        // Image Handling
        JLabel imgStatus = new JLabel("Current Image Kept");
        final String[] newImageCode = { p.getString("imagePath") }; 
        
        JButton changeImgBtn = new JButton("Change Image...");
        changeImgBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "png", "jpeg"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                newImageCode[0] = ImageUtils.encodeImage(f); 
                imgStatus.setText("New Image Selected: " + f.getName());
            }
        });

        Object[] message = {
            "Product Name:", nameF,
            "Category:", catBox,
            "Price:", priceF,
            "Stock:", qtyF,
            "Height:", hF,
            "Width:", wF,
            "Weight:", weightF,
            "Image:", changeImgBtn,
            imgStatus
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Full Product Details", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                double price = Double.parseDouble(priceF.getText());
                int qty = Integer.parseInt(qtyF.getText());
                double h = Double.parseDouble(hF.getText());
                double w = Double.parseDouble(wF.getText());
                double weight = Double.parseDouble(weightF.getText());
                
                // Re-add to update image
                Database.deleteProduct(p.getObjectId("_id"));
                Database.addProduct(nameF.getText(), catBox.getSelectedItem().toString(), 
                                    price, qty, h, w, weight, newImageCode[0]);
                
                loadProducts();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error updating product. Check your numbers.");
            }
        }
    }
}