import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class Database {

    // --- CONFIGURATION ---
    // 🔴 PASTE YOUR ACTUAL CONNECTION STRING HERE!
    private static final String URI = "mongodb+srv://admin:1234567890987654321@cluster0.hadhdy5.mongodb.net/";
    
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
        // Note: In a real production app, passwords should be hashed (e.g., BCrypt), not plain text!
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
    
    // --- HELPER METHODS ---
    
    // You can add more methods here later, like:
    // public static void addProduct(...)
    // public static List<Document> getAllProducts(...)
}