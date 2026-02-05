import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.JTable;
import javax.swing.table.TableModel;

public class CSVExporter {
    
    public static boolean exportTableToCSV(JTable table, File file) {
        try {
            TableModel model = table.getModel();
            FileWriter csv = new FileWriter(file);

            // 1. Write Headers
            for (int i = 0; i < model.getColumnCount(); i++) {
                csv.write(model.getColumnName(i) + ",");
            }
            csv.write("\n");

            // 2. Write Rows
            for (int i = 0; i < model.getRowCount(); i++) {
                for (int j = 0; j < model.getColumnCount(); j++) {
                    Object data = model.getValueAt(i, j);
                    String val = (data == null) ? "" : data.toString();
                    // Escape commas in data (e.g. "Sofa, Red")
                    csv.write("\"" + val + "\",");
                }
                csv.write("\n");
            }

            csv.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}