import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import org.bson.Document;

public class AnalyticsPanel extends JPanel {

    private JComboBox<String> rangeBox;
    
    // Tables
    private DefaultTableModel lowStockModel;
    private DefaultTableModel bestSellerModel;
    private DefaultTableModel worstSellerModel;

    public AnalyticsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Theme.COLOR_CREAM);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- 1. HEADER (Filter) ---
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.COLOR_CREAM);
        
        JLabel title = new JLabel("Inventory Intelligence");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.COLOR_DARK);
        
        JPanel filterPanel = new JPanel();
        filterPanel.setBackground(Theme.COLOR_CREAM);
        filterPanel.add(new JLabel("Analyze: "));
        String[] ranges = {"All Time", "Today", "Last 7 Days", "This Month"};
        rangeBox = new JComboBox<>(ranges);
        rangeBox.addActionListener(e -> refreshData());
        filterPanel.add(rangeBox);

        header.add(title, BorderLayout.WEST);
        header.add(filterPanel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // --- 2. MAIN CONTENT (3 Columns) ---
        JPanel grid = new JPanel(new GridLayout(1, 3, 20, 0)); // 3 Columns
        grid.setBackground(Theme.COLOR_CREAM);

        // Table 1: Low Stock (Critical)
        lowStockModel = new DefaultTableModel(new String[]{"⚠️ Low Stock Item", "Qty"}, 0);
        grid.add(createTablePanel("Critical Stock Level (<= 5)", lowStockModel, new Color(255, 235, 235)));

        // Table 2: Best Sellers
        bestSellerModel = new DefaultTableModel(new String[]{"🔥 Best Moving", "Sold"}, 0);
        grid.add(createTablePanel("Highest Demand Items", bestSellerModel, new Color(235, 255, 235)));

        // Table 3: Worst Sellers (Dead Stock)
        worstSellerModel = new DefaultTableModel(new String[]{"❄️ Slow / Dead Stock", "Sold"}, 0);
        grid.add(createTablePanel("Lowest Demand Items", worstSellerModel, new Color(235, 245, 255)));

        add(grid, BorderLayout.CENTER);

        // Initial Load
        refreshData();
    }

    // --- LOGIC ---
    private void refreshData() {
        // 1. Clear Tables
        lowStockModel.setRowCount(0);
        bestSellerModel.setRowCount(0);
        worstSellerModel.setRowCount(0);

        // 2. Load Sales History
        // If you deleted getSalesBetween, we use getSalesHistory and filter manually
        List<Document> allSales = Database.getSalesHistory(); 
        List<Document> filteredSales = filterSalesByDate(allSales, (String) rangeBox.getSelectedItem());

        // 3. Calculate Item Counts
        Map<String, Integer> productSalesCounts = new HashMap<>();
        
        // Fill map with sold items
        for (Document sale : filteredSales) {
            List<Document> items = (List<Document>) sale.get("items");
            if (items != null) {
                for (Document item : items) {
                    String name = item.getString("name");
                    int qty = item.getInteger("qty");
                    productSalesCounts.put(name, productSalesCounts.getOrDefault(name, 0) + qty);
                }
            }
        }

        // 4. Get Current Inventory for Low Stock & Worst Sellers
        List<Document> inventory = Database.getProducts("All");

        // --- POPULATE LOW STOCK ---
        for (Document p : inventory) {
            if (p.getInteger("quantity") <= 5) {
                lowStockModel.addRow(new Object[]{p.getString("name"), p.getInteger("quantity")});
            }
        }

        // --- POPULATE BEST SELLERS (Sort Map Descending) ---
        productSalesCounts.entrySet().stream()
            .sorted((k1, k2) -> -k1.getValue().compareTo(k2.getValue())) // High to Low
            .limit(20)
            .forEach(e -> bestSellerModel.addRow(new Object[]{e.getKey(), e.getValue()}));

        // --- POPULATE WORST SELLERS (Items with 0 or low sales) ---
        List<Object[]> worstList = new ArrayList<>();
        
        for (Document p : inventory) {
            String name = p.getString("name");
            int soldCount = productSalesCounts.getOrDefault(name, 0);
            worstList.add(new Object[]{name, soldCount});
        }
        
        // Sort Low to High
        worstList.sort(Comparator.comparingInt(o -> (int) o[1]));
        
        // Add top 20 worst to table
        for (int i = 0; i < Math.min(worstList.size(), 20); i++) {
            worstSellerModel.addRow(worstList.get(i));
        }
    }

    // --- HELPER: Filter Dates Manually ---
    private List<Document> filterSalesByDate(List<Document> sales, String range) {
        if (range.equals("All Time")) return sales;

        List<Document> filtered = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
        Date startOfToday = cal.getTime();

        if (range.equals("Today")) {
            // Start is today
        } else if (range.equals("Last 7 Days")) {
            cal.add(Calendar.DAY_OF_YEAR, -7);
        } else if (range.equals("This Month")) {
            cal.set(Calendar.DAY_OF_MONTH, 1);
        }
        Date cutOff = cal.getTime();

        for (Document d : sales) {
            Date saleDate = d.getDate("date");
            if (saleDate != null && !saleDate.before(cutOff)) {
                filtered.add(d);
            }
        }
        return filtered;
    }

    // --- UI HELPERS ---
    private JPanel createTablePanel(String title, DefaultTableModel model, Color headerColor) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        p.setBackground(Theme.COLOR_CREAM);

        JLabel lbl = new JLabel(title, SwingConstants.CENTER);
        lbl.setFont(Theme.FONT_BOLD);
        lbl.setOpaque(true);
        lbl.setBackground(headerColor);
        lbl.setBorder(BorderFactory.createLineBorder(Theme.COLOR_GREEN, 1));
        lbl.setPreferredSize(new Dimension(0, 30));

        JTable table = new JTable(model);
        table.setEnabled(false);
        table.setFillsViewportHeight(true);
        table.setRowHeight(25);
        
        p.add(lbl, BorderLayout.NORTH);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        return p;
    }
}   