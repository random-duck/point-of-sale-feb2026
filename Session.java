import org.bson.Document;

public class Session {
    public static String currentUsername = null;
    public static String currentRole = null; // "ADMIN" or "STAFF"

    public static void login(Document user) {
        currentUsername = user.getString("username");
        currentRole = user.getString("role");
        System.out.println("Session Started: " + currentUsername + " (" + currentRole + ")");
    }

    public static void logout() {
        System.out.println("Logging out: " + currentUsername);
        currentUsername = null;
        currentRole = null;
    }

    public static boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(currentRole);
    }
}