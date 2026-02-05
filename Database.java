import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class Database {

    // --- CONFIGURATION ---
    // 🔴 IMPORTANT: Ensure this matches your actual MongoDB Atlas connection string
    private static final String URI = "mongodb+srv://admin:1234567890987654321@cluster0.hadhdy5.mongodb.net/?retryWrites=true&w=majority";
    
    private static final String DB_NAME = "inventory_db";
    private static final String USERS_COLLECTION = "users";

    // We keep one static client to reuse connections (Much faster!)
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

    // --- AUTHENTICATION METHODS ---

    /**
     * Checks if a user exists with the given credentials.
     * @return The User Document if found (contains role, status, etc.), or null if failed.
     */
    public static Document login(String username, String password) {
        MongoDatabase db = getDatabase();
        if (db == null) return null;

        MongoCollection<Document> users = db.getCollection(USERS_COLLECTION);

        // Find a user where 'username' matches AND 'password' matches
        Document user = users.find(and(
            eq("username", username),
            eq("password", password)
        )).first();

        return user;
    }

    /**
     * Registers a new user.
     * @return true if successful, false if username already exists.
     */
    public static boolean registerUser(String username, String password, String role) {
        MongoDatabase db = getDatabase();
        if (db == null) return false;

        MongoCollection<Document> users = db.getCollection(USERS_COLLECTION);

        // 1. Check if username already exists
        if (users.find(eq("username", username)).first() != null) {
            return false; // User already exists
        }

        // 2. Create the new user document
        Document newUser = new Document("username", username)
                .append("password", password)
                .append("role", role)
                .append("status", "Pending"); // Default status for approval logic

        users.insertOne(newUser);
        return true;
    }
    
    // --- PRODUCT METHODS ---
    
    /**
     * Adds a new product to the inventory.
     * Status is calculated automatically based on quantity.
     */
    public static boolean addProduct(String name, String category, double price, int quantity, 
                                     double height, double width, double weight, String imagePath) {
        MongoDatabase db = getDatabase();
        if (db == null) return false;

        // This saves to the "products" collection, separate from "users"
        MongoCollection<Document> products = db.getCollection("products");

        // 1. Calculate Status Logic
        String status;
        if (quantity == 0) {
            status = "No Stock";
        } else if (quantity <= 5) {
            status = "Low Stock";
        } else {
            status = "In Stock";
        }

        // 2. Create Document
        Document newProduct = new Document("name", name)
                .append("category", category)
                .append("price", price)
                .append("quantity", quantity)
                .append("dimensions", new Document("height", height)
                                            .append("width", width)
                                            .append("weight", weight))
                .append("status", status)
                .append("imagePath", imagePath); // We save the file path string

        try {
            products.insertOne(newProduct);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    // --- NEW METHODS FOR GALLERY & EDITING ---

    /**
     * Fetches products. If category is "All", returns everything.
     */
    public static java.util.List<Document> getProducts(String categoryFilter) {
        MongoDatabase db = getDatabase();
        if (db == null) return new java.util.ArrayList<>();

        MongoCollection<Document> products = db.getCollection("products");
        
        if (categoryFilter == null || categoryFilter.equals("All")) {
            return products.find().into(new java.util.ArrayList<>());
        } else {
            return products.find(eq("category", categoryFilter)).into(new java.util.ArrayList<>());
        }
    }

    /**
     * Updates an existing product.
     */
    public static boolean updateProduct(org.bson.types.ObjectId id, String name, String category, 
                                        double price, int quantity, double h, double w, double weight) {
        MongoDatabase db = getDatabase();
        if (db == null) return false;

        String status = (quantity == 0) ? "No Stock" : (quantity <= 5 ? "Low Stock" : "In Stock");

        Document updateDoc = new Document("name", name)
                .append("category", category)
                .append("price", price)
                .append("quantity", quantity)
                .append("status", status)
                .append("dimensions.height", h)
                .append("dimensions.width", w)
                .append("dimensions.weight", weight);
        // Note: We are not updating the image path here for simplicity, but you could add it.

        try {
            db.getCollection("products").updateOne(eq("_id", id), new Document("$set", updateDoc));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes a product.
     */
    public static boolean deleteProduct(org.bson.types.ObjectId id) {
        try {
            getDatabase().getCollection("products").deleteOne(eq("_id", id));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}