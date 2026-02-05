import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;

public class CSVImporter {

    // Helper class to store parsed data temporarily
    public static class ProductData {
        String name, category;
        double price, h, w, weight;
        int quantity;
    }

    // Helper class for conflicts
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
                String[] values = line.split(",");
                
                // Skip empty lines or header
                if (values.length < 4 || values[0].equalsIgnoreCase("Name")) continue;

                try {
                    ProductData p = new ProductData();
                    p.name = values[0].trim();
                    p.category = values[1].trim();
                    
                    // --- THE FIX: SMART PARSING ---
                    // We now use 'cleanDouble' and 'cleanInt' instead of standard parsing
                    p.price = cleanDouble(values[2]);
                    p.quantity = cleanInt(values[3]);
                    
                    // Dimensions (handle missing columns safely)
                    p.h = (values.length > 4) ? cleanDouble(values[4]) : 0;
                    p.w = (values.length > 5) ? cleanDouble(values[5]) : 0;
                    p.weight = (values.length > 6) ? cleanDouble(values[6]) : 0;

                    // CHECK FOR DUPLICATES
                    Document existing = Database.findProductByName(p.name);
                    if (existing != null) {
                        conflicts.add(new ConflictItem(p, existing));
                    } else {
                        newItems.add(p);
                    }

                } catch (Exception ex) {
                    errorCount++;
                    // More detailed error message for debugging
                    errorReport.append("Row ").append(rowNum).append(": Check '").append(values[0]).append("' values. (Err: ").append(ex.getMessage()).append(")\n");
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
                        Database.updateProduct(
                            item.existingDoc.getObjectId("_id"), 
                            item.newData.name, item.newData.category, 
                            item.newData.price, item.newData.quantity, 
                            item.newData.h, item.newData.w, item.newData.weight
                        );
                        successCount++;
                    } 
                    else if (item.resolution.equals("Update Stock (Add)")) {
                        int newTotal = item.existingDoc.getInteger("quantity") + item.newData.quantity;
                        Database.updateProduct(
                            item.existingDoc.getObjectId("_id"), 
                            item.existingDoc.getString("name"), item.existingDoc.getString("category"), 
                            item.existingDoc.getDouble("price"), newTotal, 
                            item.existingDoc.get("dimensions", Document.class).getDouble("height"),
                            item.existingDoc.get("dimensions", Document.class).getDouble("width"),
                            item.existingDoc.get("dimensions", Document.class).getDouble("weight")
                        );
                        successCount++;
                    }
                }
            }

            // --- 2. ADD NEW ITEMS ---
            for (ProductData p : newItems) {
                Database.addProduct(p.name, p.category, p.price, p.quantity, p.h, p.w, p.weight, ""); 
                successCount++;
            }

            // --- 3. FINAL REPORT ---
            String msg = "Import Complete!\nSuccessfully Imported/Updated: " + successCount;
            if (errorCount > 0) {
                msg += "\n\nSkipped " + errorCount + " rows due to errors:\n" + errorReport.toString();
            }
            JOptionPane.showMessageDialog(parent, msg);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent, "Error reading file: " + e.getMessage());
        }
    }

    // --- NEW HELPER METHODS ---

    /**
     * Cleans a string (removes $, PHP, commas) and converts to double.
     */
    private static double cleanDouble(String input) throws NumberFormatException {
        if (input == null || input.trim().isEmpty()) return 0.0;
        // Remove 'PHP', '$', spaces, and commas
        String clean = input.replaceAll("[^0-9.]", ""); 
        return Double.parseDouble(clean);
    }

    /**
     * Cleans a string and converts to integer.
     */
    private static int cleanInt(String input) throws NumberFormatException {
        if (input == null || input.trim().isEmpty()) return 0;
        // Remove everything that isn't a digit
        String clean = input.replaceAll("[^0-9]", "");
        return Integer.parseInt(clean);
    }
}