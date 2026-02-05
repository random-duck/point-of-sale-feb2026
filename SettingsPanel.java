import javax.swing.*;
import java.awt.*;
import org.bson.Document;

public class SettingsPanel extends JPanel {

    private JTextField nameField, addressField, phoneField;

    public SettingsPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.COLOR_CREAM);

        // Header
        JLabel title = new JLabel("  System Settings");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.COLOR_DARK);
        title.setPreferredSize(new Dimension(0, 50));
        add(title, BorderLayout.NORTH);

        // Form
        JPanel form = new JPanel(new GridLayout(4, 2, 10, 10));
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        form.setBackground(Theme.COLOR_CREAM);

        nameField = new JTextField();
        addressField = new JTextField();
        phoneField = new JTextField();

        form.add(new JLabel("Store Name:")); form.add(nameField);
        form.add(new JLabel("Store Address:")); form.add(addressField);
        form.add(new JLabel("Phone Number:")); form.add(phoneField);
        
        JButton saveBtn = new JButton("SAVE SETTINGS");
        saveBtn.setBackground(Theme.COLOR_GREEN);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.addActionListener(e -> save());
        
        form.add(new JLabel("")); // Spacer
        form.add(saveBtn);

        // Center the form
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBackground(Theme.COLOR_CREAM);
        centerWrapper.add(form, BorderLayout.NORTH);
        
        add(centerWrapper, BorderLayout.CENTER);

        loadData();
    }

    private void loadData() {
        Document d = Database.getStoreSettings();
        if (d != null) {
            nameField.setText(d.getString("storeName"));
            addressField.setText(d.getString("address"));
            phoneField.setText(d.getString("phone"));
        } else {
            nameField.setText("My Store");
            addressField.setText("City, Country");
            phoneField.setText("000-0000");
        }
    }

    private void save() {
        Database.saveStoreSettings(nameField.getText(), addressField.getText(), phoneField.getText());
        JOptionPane.showMessageDialog(this, "Settings Saved Successfully!");
    }
}