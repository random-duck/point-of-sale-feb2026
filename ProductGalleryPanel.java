import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import org.bson.Document;

public class ProductGalleryPanel extends JPanel {

    // Components
    private JComboBox<String> filterBox;
    private JPanel cardViewPanel; // Holds the grid of images
    private JTable tableView; // Holds the list data
    private DefaultTableModel tableModel;
    
    // Layout Manager for swapping views
    private CardLayout viewLayout = new CardLayout();
    private JPanel centerPanel = new JPanel(viewLayout);

    public ProductGalleryPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.COLOR_CREAM);

        // --- 1. TOP TOOLBAR (Filter & Toggle) ---
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setBackground(Theme.COLOR_CREAM);
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Theme.COLOR_GREEN));

        // Filter Label
        JLabel filterLbl = new JLabel("Category: ");
        filterLbl.setFont(Theme.FONT_BOLD);
        
        // Filter Dropdown
        String[] filters = {"All", "Furniture", "Utensils", "Appliances", "Electronics", "Decor", "N/A"};
        filterBox = new JComboBox<>(filters);
        filterBox.addActionListener(e -> loadData()); // Refresh when changed

        // Toggle Button
        JToggleButton toggleBtn = new JToggleButton("Switch to List View");
        toggleBtn.setBackground(Theme.COLOR_GREEN);
        toggleBtn.setForeground(Color.WHITE);
        toggleBtn.addActionListener(e -> {
            if (toggleBtn.isSelected()) {
                viewLayout.show(centerPanel, "TABLE");
                toggleBtn.setText("Switch to Gallery View");
            } else {
                viewLayout.show(centerPanel, "GALLERY");
                toggleBtn.setText("Switch to List View");
            }
        });

        toolbar.add(filterLbl);
        toolbar.add(filterBox);
        toolbar.add(Box.createHorizontalStrut(50)); // Spacer
        toolbar.add(toggleBtn);

        add(toolbar, BorderLayout.NORTH);


        // --- 2. CENTER PANEL (Views) ---
        
        // VIEW A: Gallery Grid
        cardViewPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20)); // Flow layout wraps items
        cardViewPanel.setBackground(Theme.COLOR_CREAM);
        JScrollPane galleryScroll = new JScrollPane(cardViewPanel);
        galleryScroll.getVerticalScrollBar().setUnitIncrement(16); // Smooth scrolling
        
        // VIEW B: Table List
        String[] cols = {"Name", "Category", "Price", "Qty", "Status", "Height", "Width", "Weight"};
        tableModel = new DefaultTableModel(cols, 0);
        tableView = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(tableView);

        // Add both to the CardLayout stack
        centerPanel.add(galleryScroll, "GALLERY");
        centerPanel.add(tableScroll, "TABLE");

        add(centerPanel, BorderLayout.CENTER);

        // Load initial data
        loadData();
    }

    // --- DATA LOADING LOGIC ---
    private void loadData() {
        // 1. Clear existing data
        cardViewPanel.removeAll();
        tableModel.setRowCount(0);

        // 2. Fetch from DB
        String selectedCat = filterBox.getSelectedItem().toString();
        List<Document> products = Database.getProducts(selectedCat);

        // 3. Populate Views
        for (Document doc : products) {
            // Add to Table
            addTableRow(doc);
            
            // Add to Gallery
            addProductCard(doc);
        }

        // 4. Refresh UI
        cardViewPanel.revalidate();
        cardViewPanel.repaint();
    }

    private void addTableRow(Document doc) {
        Document dims = (Document) doc.get("dimensions");
        tableModel.addRow(new Object[]{
            doc.getString("name"),
            doc.getString("category"),
            doc.getDouble("price"),
            doc.getInteger("quantity"),
            doc.getString("status"),
            dims != null ? dims.getDouble("height") : 0,
            dims != null ? dims.getDouble("width") : 0,
            dims != null ? dims.getDouble("weight") : 0
        });
    }

    private void addProductCard(Document doc) {
        // Create a mini panel for the product
        JPanel card = new JPanel();
        card.setPreferredSize(new Dimension(220, 300));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(Theme.COLOR_GREEN, 1));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Image
        JLabel imgLbl = new JLabel();
        imgLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        String path = doc.getString("imagePath");
        
        if (path != null && !path.isEmpty()) {
            // Load and scale image safely
            try {
                ImageIcon icon = new ImageIcon(path);
                Image img = icon.getImage().getScaledInstance(200, 180, Image.SCALE_SMOOTH);
                imgLbl.setIcon(new ImageIcon(img));
            } catch (Exception e) {
                imgLbl.setText("Image Error");
            }
        } else {
            imgLbl.setText("No Image");
            imgLbl.setPreferredSize(new Dimension(200, 180));
            imgLbl.setHorizontalAlignment(SwingConstants.CENTER);
        }

        // Text Info
        JLabel nameLbl = new JLabel(doc.getString("name"));
        nameLbl.setFont(Theme.FONT_BOLD);
        nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel priceLbl = new JLabel("PHP " + doc.getDouble("price"));
        priceLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel statusLbl = new JLabel(doc.getString("status"));
        statusLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        statusLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Color code status
        String status = doc.getString("status");
        if ("No Stock".equals(status)) statusLbl.setForeground(Color.RED);
        else if ("Low Stock".equals(status)) statusLbl.setForeground(Color.ORANGE);
        else statusLbl.setForeground(Theme.COLOR_GREEN);

        // Add to card
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(imgLbl);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(nameLbl);
        card.add(priceLbl);
        card.add(statusLbl);

        // CLICK EVENT: Open Details Popup
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(ProductGalleryPanel.this);
                ProductDetailDialog dialog = new ProductDetailDialog(parentFrame, doc);
                dialog.setVisible(true);

                // If user saved changes or deleted, reload the whole grid
                if (dialog.isDataChanged()) {
                    loadData();
                }
            }
        });

        cardViewPanel.add(card);
    }
}