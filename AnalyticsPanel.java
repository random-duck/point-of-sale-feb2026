import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import org.bson.Document;

public class AnalyticsPanel extends JPanel {

    // Controls
    private JComboBox<String> rangeBox;
    private JLabel totalSalesCard;
    private JLabel totalTransCard;
    
    // Tables
    private DefaultTableModel lowStockModel;
    private DefaultTableModel topProdModel;
    
    // Chart
    private BarChartPanel chartPanel;

    public AnalyticsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Theme.COLOR_CREAM);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- 1. TOP BAR (Filter) ---
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.setBackground(Theme.COLOR_CREAM);
        
        JLabel lbl = new JLabel("Time Period: ");
        lbl.setFont(Theme.FONT_BOLD);
        
        String[] ranges = {"Today", "Last 7 Days", "This Month", "This Year"};
        rangeBox = new JComboBox<>(ranges);
        rangeBox.addActionListener(e -> refreshData());

        topBar.add(lbl);
        topBar.add(rangeBox);
        add(topBar, BorderLayout.NORTH);

        // --- 2. MAIN CONTENT (Split Top/Bottom) ---
        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 10, 10)); // 2 Rows
        centerPanel.setBackground(Theme.COLOR_CREAM);

        // ROW A: CARDS + CHART
        JPanel rowA = new JPanel(new BorderLayout(10, 0));
        rowA.setBackground(Theme.COLOR_CREAM);

        // Left: Big Number Cards
        JPanel cardsPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        cardsPanel.setPreferredSize(new Dimension(250, 0));
        cardsPanel.setBackground(Theme.COLOR_CREAM);
        
        totalSalesCard = createCard("Total Sales", "PHP 0.00", Color.decode("#4CAF50")); // Green
        totalTransCard = createCard("Transactions", "0", Color.decode("#2196F3")); // Blue
        
        cardsPanel.add(totalSalesCard);
        cardsPanel.add(totalTransCard);
        
        // Right: Bar Chart
        chartPanel = new BarChartPanel();
        
        rowA.add(cardsPanel, BorderLayout.WEST);
        rowA.add(chartPanel, BorderLayout.CENTER);

        // ROW B: TABLES (Low Stock & Top Products)
        JPanel rowB = new JPanel(new GridLayout(1, 2, 10, 0));
        rowB.setBackground(Theme.COLOR_CREAM);

        // Table 1: Low Stock
        lowStockModel = new DefaultTableModel(new String[]{"Low Stock Item", "Qty"}, 0);
        JTable lowStockTable = new JTable(lowStockModel);
        lowStockTable.setEnabled(false); // Read only
        JPanel lowStockPanel = createTablePanel("⚠️ Low Stock Alerts", lowStockTable);

        // Table 2: Top Products
        topProdModel = new DefaultTableModel(new String[]{"Top Product", "Sold"}, 0);
        JTable topProdTable = new JTable(topProdModel);
        topProdTable.setEnabled(false);
        JPanel topProdPanel = createTablePanel("🏆 Best Sellers", topProdTable);

        rowB.add(lowStockPanel);
        rowB.add(topProdPanel);

        centerPanel.add(rowA);
        centerPanel.add(rowB);

        add(centerPanel, BorderLayout.CENTER);

        // Initial Load
        refreshData();
    }

    // --- LOGIC: REFRESH ALL DATA ---
    private void refreshData() {
        String range = (String) rangeBox.getSelectedItem();
        Date start = new Date();
        Date end = new Date();
        
        // 1. Calculate Date Range
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0); 
        cal.set(Calendar.MINUTE, 0); 
        cal.set(Calendar.SECOND, 0); // Start of today

        if (range.equals("Today")) {
            start = cal.getTime();
        } else if (range.equals("Last 7 Days")) {
            cal.add(Calendar.DAY_OF_YEAR, -7);
            start = cal.getTime();
        } else if (range.equals("This Month")) {
            cal.set(Calendar.DAY_OF_MONTH, 1);
            start = cal.getTime();
        } else if (range.equals("This Year")) {
            cal.set(Calendar.DAY_OF_YEAR, 1);
            start = cal.getTime();
        }
        
        // 2. Fetch Sales
        List<Document> sales = Database.getSalesBetween(start, end);
        
        // 3. Update Cards
        double totalSales = 0;
        int totalTrans = sales.size();
        Map<String, Integer> productCounts = new HashMap<>();

        for (Document sale : sales) {
            totalSales += sale.getDouble("total");
            
            // Count products for "Best Sellers"
            List<Document> items = (List<Document>) sale.get("items");
            for (Document item : items) {
                String name = item.getString("name");
                int qty = item.getInteger("qty");
                productCounts.put(name, productCounts.getOrDefault(name, 0) + qty);
            }
        }
        
        updateCardValue(totalSalesCard, "PHP " + String.format("%,.2f", totalSales));
        updateCardValue(totalTransCard, String.valueOf(totalTrans));

        // 4. Update Chart
        chartPanel.updateData(sales, range);

        // 5. Update Top Products Table
        topProdModel.setRowCount(0);
        productCounts.entrySet().stream()
            .sorted((k1, k2) -> -k1.getValue().compareTo(k2.getValue())) // Sort Descending
            .limit(10) // Top 10
            .forEach(e -> topProdModel.addRow(new Object[]{e.getKey(), e.getValue()}));

        // 6. Update Low Stock (Separate DB Call)
        lowStockModel.setRowCount(0);
        List<Document> allProducts = Database.getProducts("All");
        for (Document p : allProducts) {
            if (p.getInteger("quantity") <= 5) {
                lowStockModel.addRow(new Object[]{p.getString("name"), p.getInteger("quantity")});
            }
        }
    }

    // --- UI HELPERS ---
    
    private JLabel createCard(String title, String value, Color color) {
        JLabel card = new JLabel("<html><center>" + title + "<br><font size='6'>" + value + "</font></center></html>", SwingConstants.CENTER);
        card.setOpaque(true);
        card.setBackground(color);
        card.setForeground(Color.WHITE);
        card.setFont(new Font("SansSerif", Font.BOLD, 14));
        card.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        return card;
    }

    private void updateCardValue(JLabel card, String value) {
        String title = card.getText().split("<br>")[0].replace("<html><center>", "");
        card.setText("<html><center>" + title + "<br><font size='6'>" + value + "</font></center></html>");
    }

    private JPanel createTablePanel(String title, JTable table) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.setBackground(Theme.COLOR_CREAM);
        p.add(new JScrollPane(table));
        return p;
    }

    // --- INNER CLASS: BAR CHART ---
    private class BarChartPanel extends JPanel {
        private Map<String, Double> data = new LinkedHashMap<>();

        public BarChartPanel() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(Color.GRAY));
        }

        public void updateData(List<Document> sales, String range) {
            data.clear();
            SimpleDateFormat sdf;
            
            // Format keys based on range (e.g., "Jan 1", "Jan 2")
            if (range.contains("Year")) sdf = new SimpleDateFormat("MMM"); // Jan, Feb
            else sdf = new SimpleDateFormat("dd"); // 01, 02 (Day of month)

            for (Document sale : sales) {
                String key = sdf.format(sale.getDate("date"));
                double amount = sale.getDouble("total");
                data.put(key, data.getOrDefault(key, 0.0) + amount);
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data.isEmpty()) {
                g.drawString("No Sales Data for this Period", getWidth()/2 - 50, getHeight()/2);
                return;
            }

            Graphics2D g2 = (Graphics2D) g;
            int width = getWidth();
            int height = getHeight();
            int barWidth = (width / data.size()) - 10;
            double maxVal = data.values().stream().max(Double::compare).orElse(1.0);

            int x = 10;
            for (Map.Entry<String, Double> entry : data.entrySet()) {
                int barHeight = (int) ((entry.getValue() / maxVal) * (height - 40));
                
                // Draw Bar
                g2.setColor(Theme.COLOR_GREEN);
                g2.fillRect(x, height - barHeight - 20, barWidth, barHeight);
                
                // Draw Text (Value)
                g2.setColor(Color.BLACK);
                //g2.drawString(String.valueOf(entry.getValue().intValue()), x, height - barHeight - 25);
                
                // Draw Label (Date)
                g2.drawString(entry.getKey(), x + (barWidth/4), height - 5);
                
                x += barWidth + 10;
            }
        }
    }
}