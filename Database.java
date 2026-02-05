import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

public class Database {

    // --- CONFIGURATION ---
    private static final String URI = "mongodb+srv://admin:1234567890987654321@cluster0.hadhdy5.mongodb.net/?retryWrites=true&w=majority";
    private static final String DB_NAME = "inventory_db";
    private static MongoClient mongoClient;

    public static MongoDatabase getDatabase() {
        if (mongoClient == null) {
            try {
                mongoClient = MongoClients.create(URI);
            } catch (Exception e) {
                return null;
            }
        }
        return mongoClient.getDatabase(DB_NAME);
    }

    // --- AUTH ---
    public static Document login(String username, String password) {
        MongoDatabase db = getDatabase();
        if (db == null) return null;
        // The DB knows the role! We don't need the dropdown.
        return db.getCollection("users").find(and(eq("username", username), eq("password", password))).first();
    }

    public static boolean registerUser(String username, String password, String role) {
        MongoDatabase db = getDatabase();
        if (db == null) return false;
        if (db.getCollection("users").find(eq("username", username)).first() != null) return false;
        db.getCollection("users").insertOne(new Document("username", username).append("password", password).append("role", role).append("status", "Pending"));
        return true;
    }
    
    public static List<Document> getAllUsers() {
        MongoDatabase db = getDatabase();
        if (db == null) return new ArrayList<>();
        return db.getCollection("users").find().into(new ArrayList<>());
    }

    public static List<Document> getPendingUsers() {
        MongoDatabase db = getDatabase();
        if (db == null) return new ArrayList<>();
        return db.getCollection("users").find(eq("status", "Pending")).into(new ArrayList<>());
    }

    public static void approveUser(ObjectId userId) {
        MongoDatabase db = getDatabase();
        if (db == null) return;
        db.getCollection("users").updateOne(eq("_id", userId), set("status", "Approved"));
    }

    public static void deleteUser(ObjectId userId) {
        MongoDatabase db = getDatabase();
        if (db == null) return;
        db.getCollection("users").deleteOne(eq("_id", userId));
    }

    // --- PRODUCTS ---
    public static Document findProductByName(String name) {
        MongoDatabase db = getDatabase();
        if (db == null) return null;
        Pattern regex = Pattern.compile("^" + Pattern.quote(name.trim()) + "$", Pattern.CASE_INSENSITIVE);
        return db.getCollection("products").find(eq("name", regex)).first();
    }

    public static boolean addProduct(String name, String category, double price, int quantity, double height, double width, double weight, String imagePath) {
        if (findProductByName(name) != null) return false; 
        MongoDatabase db = getDatabase();
        if (db == null) return false;
        String status = (quantity == 0) ? "No Stock" : (quantity <= 5 ? "Low Stock" : "In Stock");
        db.getCollection("products").insertOne(new Document("name", name.trim()).append("category", category).append("price", price).append("quantity", quantity)
                .append("dimensions", new Document("height", height).append("width", width).append("weight", weight)).append("status", status).append("imagePath", imagePath));
        return true;
    }

    public static List<Document> getProducts(String categoryFilter) {
        MongoDatabase db = getDatabase();
        if (db == null) return new ArrayList<>();
        if (categoryFilter == null || categoryFilter.equals("All")) return db.getCollection("products").find().into(new ArrayList<>());
        return db.getCollection("products").find(eq("category", categoryFilter)).into(new ArrayList<>());
    }

    public static boolean updateProduct(ObjectId id, String name, String category, double price, int quantity, double h, double w, double weight) {
        MongoDatabase db = getDatabase();
        if (db == null) return false;
        String status = (quantity == 0) ? "No Stock" : (quantity <= 5 ? "Low Stock" : "In Stock");
        Document updateDoc = new Document("name", name.trim()).append("category", category).append("price", price).append("quantity", quantity).append("status", status)
                .append("dimensions.height", h).append("dimensions.width", w).append("dimensions.weight", weight);
        try { db.getCollection("products").updateOne(eq("_id", id), new Document("$set", updateDoc)); return true; } catch (Exception e) { return false; }
    }
    
    public static boolean deleteProduct(ObjectId id) {
        try { getDatabase().getCollection("products").deleteOne(eq("_id", id)); return true; } catch (Exception e) { return false; }
    }
    
    // --- SUPPLIERS ---
    public static boolean addSupplier(String name, String email, String phone, String address, String category) {
        MongoDatabase db = getDatabase();
        if (db == null) return false;
        try { db.getCollection("suppliers").insertOne(new Document("name", name).append("email", email).append("phone", phone).append("address", address).append("category", category)); return true; } catch (Exception e) { return false; }
    }

    public static List<Document> getSuppliers() {
        MongoDatabase db = getDatabase();
        if (db == null) return new ArrayList<>();
        return db.getCollection("suppliers").find().into(new ArrayList<>());
    }

    public static boolean deleteSupplier(ObjectId id) {
        try { getDatabase().getCollection("suppliers").deleteOne(eq("_id", id)); return true; } catch (Exception e) { return false; }
    }

    // --- PURCHASE ORDERS ---
    public static boolean savePurchaseOrder(String supplierName, String supplierEmail, List<Document> items, double totalEstimate, String username) {
        MongoDatabase db = getDatabase();
        if (db == null) return false;
        try { 
            db.getCollection("purchase_orders").insertOne(new Document("supplier", supplierName)
                .append("email", supplierEmail)
                .append("date", new Date())
                .append("items", items)
                .append("totalEstimate", totalEstimate)
                .append("status", "Sent")
                .append("createdBy", username)); 
            
            // Log Action (This adds 1 to the 'Orders Created' count)
            saveActionLog(username, "Supply Order", 1); 
            return true; 
        } catch (Exception e) { return false; }
    }

    public static List<Document> getPurchaseOrders() {
        MongoDatabase db = getDatabase();
        if (db == null) return new ArrayList<>();
        return db.getCollection("purchase_orders").find().sort(new Document("date", -1)).into(new ArrayList<>());
    }

    public static List<Document> getSalesHistory() {
        MongoDatabase db = getDatabase();
        if (db == null) return new ArrayList<>();
        try {
            return db.getCollection("sales").find().sort(new Document("date", -1)).into(new ArrayList<>());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // --- SETTINGS ---
    public static void saveStoreSettings(String name, String address, String phone) {
        MongoDatabase db = getDatabase();
        if (db == null) return;
        MongoCollection<Document> settings = db.getCollection("settings");
        settings.deleteMany(new Document()); 
        settings.insertOne(new Document("storeName", name).append("address", address).append("phone", phone));
    }

    public static Document getStoreSettings() {
        MongoDatabase db = getDatabase();
        if (db == null) return null;
        return db.getCollection("settings").find().first();
    }

    // --- EMPLOYEE REPORTING & LOGS (UPDATED FOR TRANSACTION COUNTING) ---
    public static void saveActionLog(String username, String actionType, int quantity) {
        MongoDatabase db = getDatabase();
        if (db == null) return;
        if (username == null) username = "Unknown";
        
        db.getCollection("action_logs").insertOne(new Document("username", username)
                .append("action", actionType)
                .append("quantity", quantity) // We still save the amount in case you need it later
                .append("date", new Date()));
    }

    // THE FIX: This now counts *entries* (Transactions) instead of summing quantities
    public static int getActionCount(String username, String actionType) {
        MongoDatabase db = getDatabase();
        if (db == null) return 0;
        
        // countDocuments returns the number of times this action happened
        long count = db.getCollection("action_logs").countDocuments(and(eq("username", username), eq("action", actionType)));
        return (int) count;
    }

    public static void saveEmployeeComment(String username, String comment, String adminName) {
        MongoDatabase db = getDatabase();
        if (db == null) return;
        db.getCollection("employee_comments").insertOne(new Document("targetUser", username)
                .append("comment", comment)
                .append("admin", adminName)
                .append("date", new Date()));
    }

    public static List<Document> getEmployeeComments(String username) {
        MongoDatabase db = getDatabase();
        if (db == null) return new ArrayList<>();
        return db.getCollection("employee_comments").find(eq("targetUser", username)).sort(new Document("date", -1)).into(new ArrayList<>());
    }
}