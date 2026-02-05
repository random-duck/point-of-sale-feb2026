import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;

public class CSVImporter {

    public static class ProductData {
        String name, category;
        double price, h, w, weight;
        int quantity;
    }

    public static class ConflictItem {
        ProductData newData;
        Document existingDoc;
        String resolution = "Skip"; 
        public ConflictItem(ProductData d, Document e) { newData = d; existingDoc = e; }
    }

    public static void importCSV(JFrame parent, File file) {
        List<ProductData> newItems = new ArrayList<>();
        List<ConflictItem> conflicts = new ArrayList<>();
        StringBuilder errorReport = new StringBuilder();
        int successCount = 0;
        int errorCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int rowNum = 0;
            
            while ((line = br.readLine()) != null) {
                rowNum++;
                // Skip empty lines
                if (line.trim().isEmpty()) continue;

                // --- THE FIX: CSV REGEX SPLIT ---
                // This complex regex splits by comma ONLY if it's not inside quotes
                String[] values = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                
                // Clean up quotes around values (e.g. "Sofa" -> Sofa)
                for(int i=0; i<values.length; i++) {
                    values[i] = values[i].trim().replace("\"", "");
                }

                // Skip Header
                if (values.length < 4 || values[0].equalsIgnoreCase("Name")) continue;

                try {
                    ProductData p = new ProductData();
                    p.name = values[0]; // Name
                    p.category = values[1]; // Category
                    
                    // Smart parse numbers (removes $ and ,)
                    p.price = cleanDouble(values[2]); 
                    p.quantity = cleanInt(values[3]);
                    
                    // Dimensions (Optional)
                    p.h = (values.length > 4) ? cleanDouble(values[4]) : 0;
                    p.w = (values.length > 5) ? cleanDouble(values[5]) : 0;
                    p.weight = (values.length > 6) ? cleanDouble(values[6]) : 0;

                    // Check Duplicates
                    Document existing = Database.findProductByName(p.name);
                    if (existing != null) {
                        conflicts.add(new ConflictItem(p, existing));
                    } else {
                        newItems.add(p);
                    }

                } catch (Exception ex) {
                    errorCount++;
                    errorReport.append("Row ").append(rowNum).append(": Failed to read column. (Check if Price/Qty has letters)\n");
                }
            }

            // --- 1. HANDLE CONFLICTS ---
            if (!conflicts.isEmpty()) {
                ImportResolutionDialog dialog = new ImportResolutionDialog(parent, conflicts);
                dialog.setVisible(true);
                
                if (!dialog.isConfirmed()) {
                    JOptionPane.showMessageDialog(parent, "Import Cancelled.");
                    return; 
                }
                
                for (ConflictItem item : conflicts) {
                    if (item.resolution.equals("Skip")) continue;
                    
                    if (item.resolution.equals("Replace")) {
                        Database.updateProduct(item.existingDoc.getObjectId("_id"), item.newData.name, item.newData.category, item.newData.price, item.newData.quantity, item.newData.h, item.newData.w, item.newData.weight);
                        successCount++;
                    } else if (item.resolution.equals("Update Stock (Add)")) {
                        int newTotal = item.existingDoc.getInteger("quantity") + item.newData.quantity;
                        Database.updateProduct(item.existingDoc.getObjectId("_id"), item.existingDoc.getString("name"), item.existingDoc.getString("category"), item.existingDoc.getDouble("price"), newTotal, item.existingDoc.get("dimensions", Document.class).getDouble("height"), item.existingDoc.get("dimensions", Document.class).getDouble("width"), item.existingDoc.get("dimensions", Document.class).getDouble("weight"));
                        successCount++;
                    }
                }
            }

            // --- 2. ADD NEW ITEMS ---
            for (ProductData p : newItems) {
                Database.addProduct(p.name, p.category, p.price, p.quantity, p.h, p.w, p.weight, ""); 
                successCount++;
            }

            // --- 3. REPORT ---
            String msg = "Import Complete!\nProcessed: " + successCount;
            if (errorCount > 0) msg += "\nSkipped " + errorCount + " rows (See error log).";
            JOptionPane.showMessageDialog(parent, msg);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent, "File Error: " + e.getMessage());
        }
    }

    private static double cleanDouble(String input) {
        if (input == null || input.trim().isEmpty()) return 0.0;
        return Double.parseDouble(input.replaceAll("[^0-9.]", ""));
    }

    private static int cleanInt(String input) {
        if (input == null || input.trim().isEmpty()) return 0;
        return Integer.parseInt(input.replaceAll("[^0-9]", ""));
    }
}