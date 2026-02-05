import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;

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
        String resolution = "Skip"; // Default choice
        
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
            
            // Skip Header if it exists (simple check: if first col is "Name")
            // Assuming strict format: Name, Category, Price, Qty, Height, Width, Weight
            
            while ((line = br.readLine()) != null) {
                rowNum++;
                String[] values = line.split(",");
                
                // Skip empty lines or header
                if (values.length < 4 || values[0].equalsIgnoreCase("Name")) continue;

                try {
                    ProductData p = new ProductData();
                    p.name = values[0].trim();
                    p.category = values[1].trim();
                    p.price = Double.parseDouble(values[2].trim());
                    p.quantity = Integer.parseInt(values[3].trim());
                    
                    // Dimensions (Optional in CSV? Let's assume 0 if missing)
                    p.h = (values.length > 4) ? Double.parseDouble(values[4].trim()) : 0;
                    p.w = (values.length > 5) ? Double.parseDouble(values[5].trim()) : 0;
                    p.weight = (values.length > 6) ? Double.parseDouble(values[6].trim()) : 0;

                    // CHECK FOR DUPLICATES
                    Document existing = Database.findProductByName(p.name);
                    if (existing != null) {
                        conflicts.add(new ConflictItem(p, existing));
                    } else {
                        newItems.add(p);
                    }

                } catch (Exception ex) {
                    errorCount++;
                    errorReport.append("Row ").append(rowNum).append(": Invalid Data (Check Numbers)\n");
                }
            }

            // --- 1. HANDLE CONFLICTS ---
            if (!conflicts.isEmpty()) {
                ImportResolutionDialog dialog = new ImportResolutionDialog(parent, conflicts);
                dialog.setVisible(true);
                
                if (!dialog.isConfirmed()) {
                    JOptionPane.showMessageDialog(parent, "Import Cancelled.");
                    return; // Stop everything
                }
                
                // Process resolved conflicts
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
                        // Keep old price/details, just update stock
                        // Note: You might want a specific method for this, but updateProduct works if we reuse old values
                        Database.updateProduct(
                            item.existingDoc.getObjectId("_id"), 
                            item.existingDoc.getString("name"), item.existingDoc.getString("category"), 
                            item.existingDoc.getDouble("price"), newTotal, // Updated Qty
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
                // Image path is empty for bulk import (Option A)
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
}