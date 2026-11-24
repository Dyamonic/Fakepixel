package club.abelix.commons.mysql.modern;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedList;

public class BufferedQuery extends LinkedList<BufferedRow> implements AutoCloseable {

    public BufferedQuery(ResultSet rs) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();

        while (rs.next()) {
            BufferedRow row = new BufferedRow();
            int columns = rsmd.getColumnCount();

            for (int i = 0; i < columns; i++) {
                int idx = i + 1;

                String name = rsmd.getColumnName(idx);
                Object object = rs.getObject(idx);

                if (object != null) {
                    row.put(name, object);
                }
            }

            add(row);
        }

        rs.close();
    }

    @Override
    public void close() throws Exception {

    }
}