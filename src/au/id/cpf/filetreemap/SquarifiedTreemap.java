package au.id.cpf.filetreemap;

import com.google.common.io.Resources;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.File;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by chris on 15/01/15.
 */
public class SquarifiedTreemap {

    public static void processDiskScan(File dbFile) throws Exception {
        List<FileNode> nodes = getTopLevelDirs(dbFile);


        ScriptEngineManager engineManager = new ScriptEngineManager();
        ScriptEngine engine = engineManager.getEngineByName("nashorn");
        String treemapLibScript = Resources.toString(Resources.getResource("au/id/cpf/filetreemap/treemap-squarify.js"), Charset.defaultCharset());
        engine.eval(treemapLibScript);
        Object json = engine.eval("JSON");
        Object treemap = engine.eval("Treemap");
        Invocable invocable = (Invocable) engine;

        StringBuilder sizesJsonArrayBuilder = new StringBuilder();
        sizesJsonArrayBuilder.append("[");
        for (int i = 0; i < nodes.size(); i++) {
            FileNode node = nodes.get(i);

            if (node.getSize() > 0) {
                sizesJsonArrayBuilder.append(node.getSize());

                //if (i < nodes.size() - 1) {
                    sizesJsonArrayBuilder.append(", ");
                //}
            }
        }
        sizesJsonArrayBuilder.append("]");

        Object data = engine.eval(sizesJsonArrayBuilder.toString());
        //Object data = engine.eval("[60000, 60000, 40000, 30000, 20000, 10000]");

        Object treemapNodes = invocable.invokeMethod(treemap, "generate", data, 1000, 1000);
        Object jsonTreemapNodes = invocable.invokeMethod(json, "stringify", treemapNodes);
        // These are top left and both right coordinates. Need to round then use to draw the boxes.

        System.out.println(treemapNodes.toString());
    }

    private static List<FileNode> getTopLevelDirs(File dbFile) throws Exception {
        // load the sqlite-JDBC driver using the current class loader
        Class.forName("org.sqlite.JDBC");

        List<FileNode> nodes = new ArrayList<FileNode>();

        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

        PreparedStatement stmt = connection.prepareStatement("SELECT id, name, sizeIncludingChildren FROM DiskScan WHERE level = 1 ORDER BY sizeIncludingChildren DESC;");

        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            int id = rs.getInt("id");
            String name = rs.getString("name");
            long size = rs.getLong("sizeIncludingChildren");

            nodes.add(new FileNode(id, name, size));
        }

        return nodes;
    }

}
