import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

public class Database {

    // --- CONFIGURATION ---
    private static final String URI = "mongodb+srv://admin:1234567890987654321@cluster0.hadhdy5.mongodb.net/?retryWrites=true&w=majority";
    
    private static final String DB_NAME = "inventory_db";
    private static final String USERS_COLLECTION = "users";
    private static final String PRODUCTS_COLLECTION = "products";

    private static MongoClient mongoClient;

    // --- CONNECTION HANDLING ---
    public static MongoDatabase getDatabase() {
        if (mongoClient == null) {
            try {
                mongoClient = MongoClients.create(URI);
                System.out.println("✅ Connected to MongoDB Cloud successfully.");
            } catch (Exception e) {
                System.err.println("❌ Database Connection Failed: " + e.getMessage());
                return null;
            }
        }
        return mongoClient.getDatabase(DB_NAME);
    }

    // --- AUTH METHODS ---
    public static Document login(String username, String password) {
        MongoDatabase db = getDatabase();
        if (db == null) return null;
        return db.getCollection(USERS_COLLECTION).find(and(eq("username", username), eq("password", password))).first();
    }

    public static boolean registerUser(String username, String password, String role) {
        MongoDatabase db = getDatabase();
        if (db == null) return false;
        MongoCollection<Document> users = db.getCollection(USERS_COLLECTION);
        if (users.find(eq("username", username)).first() != null) return false;

        Document newUser = new Document("username", username)
                .append("password", password)
                .append("role", role)
                .append("status", "Pending");
        users.insertOne(newUser);
        return true;
    }
    
    // --- ADMIN USER METHODS (New!) ---
    
    public static List<Document> getAllUsers() {
        MongoDatabase db = getDatabase();
        if (db == null) return new ArrayList<>();
        return db.getCollection(USERS_COLLECTION).find().into(new ArrayList<>());
    }

    public static void approveUser(ObjectId userId) {
        MongoDatabase db = getDatabase();
        if (db == null) return;
        db.getCollection(USERS_COLLECTION).updateOne(eq("_id", userId), set("status", "Approved"));
    }

    // --- PRODUCT METHODS ---
    
    public static Document findProductByName(String name) {
        MongoDatabase db = getDatabase();
        if (db == null) return null;
        // Symbol-safe regex search
        Pattern regex = Pattern.compile("^" + Pattern.quote(name.trim()) + "$", Pattern.CASE_INSENSITIVE);
        return db.getCollection(PRODUCTS_COLLECTION).find(eq("name", regex)).first();
    }

    public static boolean addProduct(String name, String category, double price, int quantity, 
                                     double height, double width, double weight, String imagePath) {
        if (findProductByName(name) != null) return false; // Duplicate check

        MongoDatabase db = getDatabase();
        if (db == null) return false;

        String status = (quantity == 0) ? "No Stock" : (quantity <= 5 ? "Low Stock" : "In Stock");

        Document newProduct = new Document("name", name.trim())
                .append("category", category)
                .append("price", price)
                .append("quantity", quantity)
                .append("dimensions", new Document("height", height)
                                            .append("width", width)
                                            .append("weight", weight))
                .append("status", status)
                .append("imagePath", imagePath);

        try {
            db.getCollection(PRODUCTS_COLLECTION).insertOne(newProduct);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<Document> getProducts(String categoryFilter) {
        MongoDatabase db = getDatabase();
        if (db == null) return new ArrayList<>();
        
        MongoCollection<Document> products = db.getCollection(PRODUCTS_COLLECTION);
        if (categoryFilter == null || categoryFilter.equals("All")) {
            return products.find().into(new ArrayList<>());
        } else {
            return products.find(eq("category", categoryFilter)).into(new ArrayList<>());
        }
    }

    public static boolean updateProduct(ObjectId id, String name, String category, 
                                        double price, int quantity, double h, double w, double weight) {
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
            db.getCollection(PRODUCTS_COLLECTION).updateOne(eq("_id", id), new Document("$set", updateDoc));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public static boolean deleteProduct(ObjectId id) {
        try {
            getDatabase().getCollection(PRODUCTS_COLLECTION).deleteOne(eq("_id", id));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    // --- SUPPLIER METHODS (Updated with Category) ---
    
    public static boolean addSupplier(String name, String email, String phone, String address, String category) {
        MongoDatabase db = getDatabase();
        if (db == null) return false;
        
        Document doc = new Document("name", name)
                .append("email", email)
                .append("phone", phone)
                .append("address", address)
                .append("category", category); // Now includes category
        
        try {
            db.getCollection("suppliers").insertOne(doc);
            return true;
        } catch (Exception e) {
            return false;
        }
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
        } catch (Exception e) {
            return false;
        }
    }

    // --- PURCHASE ORDER METHODS ---

    public static boolean savePurchaseOrder(String supplierName, String supplierEmail, List<Document> items, double totalEstimate) {
        MongoDatabase db = getDatabase();
        if (db == null) return false;

        Document order = new Document("supplier", supplierName)
                .append("email", supplierEmail)
                .append("date", new java.util.Date())
                .append("items", items) // Nested list of what we bought
                .append("totalEstimate", totalEstimate)
                .append("status", "Sent");

        try {
            db.getCollection("purchase_orders").insertOne(order);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<Document> getPurchaseOrders() {
        MongoDatabase db = getDatabase();
        if (db == null) return new ArrayList<>();
        // Sort by date descending (newest first)
        return db.getCollection("purchase_orders").find().sort(new Document("date", -1)).into(new ArrayList<>());
    }
    // --- POS / SALES METHODS ---

    public static boolean saveSale(List<Document> items, double totalAmount, double cashReceived, double change) {
        MongoDatabase db = getDatabase();
        if (db == null) return false;

        Document sale = new Document("date", new java.util.Date())
                .append("items", items) // List of what was sold
                .append("total", totalAmount)
                .append("cash", cashReceived)
                .append("change", change)
                .append("paymentMethod", "Cash"); // Default to Cash for now

        try {
            db.getCollection("sales").insertOne(sale);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<Document> getSalesHistory() {
        MongoDatabase db = getDatabase();
        if (db == null) return new ArrayList<>();
        // Get newest first
        return db.getCollection("sales").find().sort(new Document("date", -1)).into(new ArrayList<>());
    }
}