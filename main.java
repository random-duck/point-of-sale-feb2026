import java.awt.*;
import javax.swing.*;

public class Main extends JFrame {

    // The "CardLayout" lets us stack panels like a deck of cards and show only one at a time
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);

    // Singleton instance: Allows other panels to access Main methods (like screen switching)
    private static Main instance;

    public Main() {
        // 1. Basic Frame Setup
        setTitle("MUWEBLES INVENTORY SYSTEM");
        setSize(1366, 768);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center on screen
        setResizable(true); // Let it be responsive!

        // 2. Add the Main Panel (The Container)
        add(mainPanel);

        // 3. INITIALIZE SCREENS (We will create these files next!)
        // For now, I added placeholder panels so you can run it.
        
        mainPanel.add(new JPanel() {{ 
            setBackground(Theme.COLOR_GREEN); 
            add(new JLabel("LOGIN SCREEN PLACEHOLDER")); 
        }}, "LOGIN");

        mainPanel.add(new JPanel() {{ 
            setBackground(Theme.COLOR_CREAM); 
            add(new JLabel("SIGNUP SCREEN PLACEHOLDER")); 
        }}, "SIGNUP");
        
        mainPanel.add(new JPanel() {{ 
            setBackground(Color.WHITE); 
            add(new JLabel("DASHBOARD PLACEHOLDER")); 
        }}, "DASHBOARD");

        mainPanel.add(new LoginPanel(), "LOGIN");
        mainPanel.add(new SignupPanel(), "SIGNUP");
        mainPanel.add(new DashboardPanel(), "DASHBOARD");
       
        // 4. Show the first screen
        showScreen("LOGIN");
        
        // Save instance so we can call it globally
        instance = this;
    }

    // --- GLOBAL METHOD TO SWITCH SCREENS ---
    // Call Main.getInstance().showScreen("DASHBOARD") from anywhere!
    public void showScreen(String screenName) {
        cardLayout.show(mainPanel, screenName);
    }

    public static Main getInstance() {
        return instance;
    }

    public static void main(String[] args) {
        // Run the GUI on the Event Dispatch Thread (Best Practice)
        SwingUtilities.invokeLater(() -> {
            new Main().setVisible(true);
        });
    }
}