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
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.Updates.set;

public class Database {

    // --- CONFIGURATION ---
    private static final String URI = "mongodb+srv://admin:1234567890987654321@cluster0.hadhdy5.mongodb.net/?retryWrites=true&w=majority";
    private static final String DB_NAME = "inventory_db";
    private static MongoClient mongoClient;

    // --- CONNECTION HANDLING ---
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

    // --- AUTH METHODS ---
    public static Document login(String username, String password) {
        MongoDatabase db = getDatabase();
        if (db == null) return null;
        return db.getCollection("users").find(and(eq("username", username), eq("password", password))).first();
    }

    public static boolean registerUser(String username, String password, String role) {
        MongoDatabase db = getDatabase();
        if (db == null) return false;
        if (db.getCollection("users").find(eq("username", username)).first() != null) return false;
        
        db.getCollection("users").insertOne(new Document("username", username)
                .append("password", password)
                .append("role", role)
                .append("status", "Pending"));
        return true;
    }
    
    // --- ADMIN USER METHODS ---
    public static List<Document> getAllUsers() {
        MongoDatabase db = getDatabase();
        if (db == null) return new ArrayList<>();
        return db.getCollection("users").find().into(new ArrayList<>());
    }

    public static void approveUser(ObjectId userId) {
        MongoDatabase db = getDatabase();
        if (db == null) return;
        db.getCollection("users").updateOne(eq("_id", userId), set("status", "Approved"));
    }

    // --- PRODUCT METHODS ---
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
        
        Document doc = new Document("name", name.trim())
                .append("category", category)
                .append("price", price)
                .append("quantity", quantity)
                .append("dimensions", new Document("height", height).append("width", width).append("weight", weight))
                .append("status", status)
                .append("imagePath", imagePath);
        
        db.getCollection("products").insertOne(doc);
        return true;
    }

    public static List<Document> getProducts(String categoryFilter) {
        MongoDatabase db = getDatabase();
        if (db == null) return new ArrayList<>();
        
        if (categoryFilter == null || categoryFilter.equals("All")) 
            return db.getCollection("products").find().into(new ArrayList<>());
        
        return db.getCollection("products").find(eq("category", categoryFilter)).into(new ArrayList<>());
    }

    public static boolean updateProduct(ObjectId id, String name, String category, double price, int quantity, double h, double w, double weight) {
        MongoDatabase db = getDatabase();
        if (db == null) return false;
        
        String status = (quantity == 0) ? "No Stock" : (quantity <= 5 ? "Low Stock" : "In Stock");
        
        Document updateDoc = new Document("name", name.trim())
                .append("category", category)
                .append("price", price)
                .append("quantity", quantity)
                .append("status", status)
                .append("dimensions.height", h)
                .append("dimensions.width", w)
                .append("dimensions.weight", weight);
        
        try { 
            db.getCollection("products").updateOne(eq("_id", id), new Document("$set", updateDoc)); 
            return true; 
        } catch (Exception e) { return false; }
    }
    
    public static boolean deleteProduct(ObjectId id) {
        try { 
            getDatabase().getCollection("products").deleteOne(eq("_id", id)); 
            return true; 
        } catch (Exception e) { return false; }
    }
    
    // --- SUPPLIER METHODS ---
    public static boolean addSupplier(String name, String email, String phone, String address, String category) {
        MongoDatabase db = getDatabase();
        if (db == null) return false;
        
        try { 
            db.getCollection("suppliers").insertOne(new Document("name", name)
                .append("email", email)
                .append("phone", phone)
                .append("address", address)
                .append("category", category)); 
            return true; 
        } catch (Exception e) { return false; }
    }

    public static List<Document> getSuppliers() {
        MongoDatabase db = getDatabase();
        if (db == null) return new ArrayList<>();
        return db.getCollection("suppliers").find().into(new ArrayList<>());
    }

    public static boolean deleteSupplier(ObjectId id) {
        try { 
            getDatabase().getCollection("suppliers").deleteOne(eq("_id", id)); 
            return true; 
        } catch (Exception e) { return false; }
    }

    // --- PURCHASE ORDERS ---
    public static boolean savePurchaseOrder(String supplierName, String supplierEmail, List<Document> items, double totalEstimate) {
        MongoDatabase db = getDatabase();
        if (db == null) return false;
        
        try { 
            db.getCollection("purchase_orders").insertOne(new Document("supplier", supplierName)
                .append("email", supplierEmail)
                .append("date", new Date())
                .append("items", items)
                .append("totalEstimate", totalEstimate)
                .append("status", "Sent")); 
            return true; 
        } catch (Exception e) { return false; }
    }

    public static List<Document> getPurchaseOrders() {
        MongoDatabase db = getDatabase();
        if (db == null) return new ArrayList<>();
        return db.getCollection("purchase_orders").find().sort(new Document("date", -1)).into(new ArrayList<>());
    }

    // --- SALES / ANALYTICS (Kept for Analytics Tab) ---
    public static boolean saveSale(List<Document> items, double totalAmount, double cashReceived, double change) {
        MongoDatabase db = getDatabase();
        if (db == null) return false;
        try { 
            db.getCollection("sales").insertOne(new Document("date", new Date())
                .append("items", items)
                .append("total", totalAmount)
                .append("cash", cashReceived)
                .append("change", change)
                .append("paymentMethod", "Cash")); 
            return true; 
        } catch (Exception e) { return false; }
    }

    public static List<Document> getSalesHistory() {
        MongoDatabase db = getDatabase();
        if (db == null) return new ArrayList<>();
        return db.getCollection("sales").find().sort(new Document("date", -1)).into(new ArrayList<>());
    }

    public static List<Document> getSalesBetween(Date start, Date end) {
        MongoDatabase db = getDatabase();
        if (db == null) return new ArrayList<>();
        return db.getCollection("sales").find(and(gte("date", start), lte("date", end))).into(new ArrayList<>());
    }

    // --- SETTINGS ---
    public static void saveStoreSettings(String name, String address, String phone) {
        MongoDatabase db = getDatabase();
        if (db == null) return;
        MongoCollection<Document> settings = db.getCollection("settings");
        settings.deleteMany(new Document()); // Clear old settings
        settings.insertOne(new Document("storeName", name).append("address", address).append("phone", phone));
    }

    public static Document getStoreSettings() {
        MongoDatabase db = getDatabase();
        if (db == null) return null;
        return db.getCollection("settings").find().first();
    }
}