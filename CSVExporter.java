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
                csv.write(escapeCSV(model.getColumnName(i)));
                if (i < model.getColumnCount() - 1) csv.write(",");
            }
            csv.write("\n");

            // 2. Write Rows
            for (int i = 0; i < model.getRowCount(); i++) {
                for (int j = 0; j < model.getColumnCount(); j++) {
                    Object data = model.getValueAt(i, j);
                    String val = (data == null) ? "" : data.toString();
                    
                    csv.write(escapeCSV(val));
                    
                    if (j < model.getColumnCount() - 1) csv.write(",");
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

    /**
     * Standard CSV Escaping:
     * - Wrap content in quotes
     * - If content has quotes, double them (" -> "")
     */
    private static String escapeCSV(String val) {
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}