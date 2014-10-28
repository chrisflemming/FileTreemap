package au.id.cpf.filetreemap;

import com.google.common.base.Charsets;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by chris on 8/10/2014.
 */
public class Scanner {

    public static void main(String[] args) throws Exception {
        // load the sqlite-JDBC driver using the current class loader
        Class.forName("org.sqlite.JDBC");

        File outputDir = new File(args[0]);

        Connection connection = initDB(outputDir);

        File[] fsRoots = File.listRoots();
        for (File root : fsRoots) {
            try {
                Files.walkFileTree(root.toPath(), new DiskScannerVisitor(connection));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static Connection initDB(File outputDir) throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String filePrefix = dateFormat.format(new Date());
        File outputFile = new File(outputDir, filePrefix + ".fms");

        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + outputFile.getAbsolutePath());
        File createSchemaFile = new File(Scanner.class.getClassLoader().getResource("au/id/cpf/filetreemap/createSchema.sql").getFile());

        String createSchemaString = com.google.common.io.Files.toString(createSchemaFile, Charsets.UTF_8);
        PreparedStatement pStmt = connection.prepareStatement(createSchemaString);
        pStmt.execute();

        return connection;
    }

    private static class DiskScannerVisitor implements FileVisitor<Path> {

        private Connection connection;
        private int currentRowid;

        private List<String> directoriesToIgnore;

        private HashMap<Path, Integer> mapParentPathToRowId;
        private HashMap<Path, Long> recusiveDirectorySizes;

        public DiskScannerVisitor(Connection connection) {
            this.connection = connection;
            this.currentRowid = 0;
            mapParentPathToRowId = new HashMap<Path, Integer>();
            recusiveDirectorySizes = new HashMap<Path, Long>();

            directoriesToIgnore = new ArrayList<String>();
            directoriesToIgnore.add("/Volumes");
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            String pathString = dir.toString();
            if (directoriesToIgnore.contains(pathString)) {
                return FileVisitResult.SKIP_SUBTREE;
            } else {
                handleFileOrDirectory(dir, attrs);
                return FileVisitResult.CONTINUE;
            }
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            handleFileOrDirectory(file, attrs);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            System.err.println("Failed visiting " + file.toString());
            exc.printStackTrace();
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            // Update entry for this direct to include the recursive size (size including children)
            long recursiveSize = recusiveDirectorySizes.get(dir);
            int rowId = mapParentPathToRowId.get(dir);

            try {
                PreparedStatement pStmt = connection.prepareStatement("UPDATE DiskScan SET sizeIncludingChildren = ? WHERE id = ?;");
                pStmt.setLong(1, recursiveSize);
                pStmt.setInt(2, rowId);
                pStmt.execute();
            } catch (SQLException ex) {
                System.err.println("Failed writing record for " + dir.toString());
                ex.printStackTrace();
            }

            // Add the recursive size of this directory to the recursive size for the parent.
            if (dir.getNameCount() > 0) { // No parent if this is the filesystem root.
                long parentRecursiveSize = recusiveDirectorySizes.get(dir.getParent());
                parentRecursiveSize += recursiveSize;
                recusiveDirectorySizes.put(dir.getParent(), parentRecursiveSize);
            }

            // Remove stored row id and rec
            recusiveDirectorySizes.remove(dir);
            mapParentPathToRowId.remove(dir);

            return FileVisitResult.CONTINUE;
        }

        private void handleFileOrDirectory(Path fileOrDir, BasicFileAttributes attrs) {
            int level = fileOrDir.getNameCount();
            String pathString = fileOrDir.toString();
            String name;

            if (level == 0) {
                name = pathString;
            } else {
                name = fileOrDir.getName(level - 1).toString();
            }
            String extension = com.google.common.io.Files.getFileExtension(pathString);

            boolean isDirectory = attrs.isDirectory();
            long creationTime = attrs.creationTime().toMillis();
            long lastAccessTime = attrs.lastAccessTime().toMillis();
            long lastModifiedTime = attrs.lastModifiedTime().toMillis();
            long size = attrs.size();

            if (level == 1) {
                System.out.println(pathString);
            }

            try {
                if (level == 0) {
                    // This is a filesystem root.

                    PreparedStatement pStmt = connection.prepareStatement("INSERT INTO DiskScan VALUES (?, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");

                    //id
                    pStmt.setInt(1, currentRowid);

                    //parent Id null for the root

                    //level
                    pStmt.setInt(2, level);

                    //path
                    pStmt.setString(3, pathString);

                    //name
                    pStmt.setString(4, name);

                    //extension
                    pStmt.setString(5, extension);

                    //isDirectory (Boolean represented as integer in SQLite)
                    pStmt.setInt(6, isDirectory == true ? 1 : 0);

                    //creationTime
                    pStmt.setLong(7, creationTime);

                    //lastAccessTime
                    pStmt.setLong(8, lastAccessTime);

                    //lastModifiedTime
                    pStmt.setLong(9, lastModifiedTime);

                    //size
                    pStmt.setLong(10, size);

                    //size including children - updated later
                    pStmt.setLong(11, size);

                    pStmt.execute();

                    mapParentPathToRowId.put(fileOrDir, currentRowid);
                    recusiveDirectorySizes.put(fileOrDir, size);
                    currentRowid++;
                } else {
                    //PreparedStatement pStmt = connection.prepareStatement("INSERT INTO DiskScan VALUES (NULL, (SELECT id FROM DiskScan WHERE path = ? AND level = ?), ?, ?, ?, ?, ?, ?, ?);");
                    PreparedStatement pStmt = connection.prepareStatement("INSERT INTO DiskScan VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");

                    //id
                    pStmt.setInt(1, currentRowid);

                    //parent id
                    int parentId = mapParentPathToRowId.get(fileOrDir.getParent());
                    pStmt.setInt(2, parentId);

                    //level
                    pStmt.setInt(3, level);

                    //path
                    pStmt.setString(4, pathString);

                    //name
                    pStmt.setString(5, name);

                    //extension
                    pStmt.setString(6, extension);

                    //isDirectory (Boolean represented as integer in SQLite)
                    pStmt.setInt(7, isDirectory == true ? 1 : 0);

                    //creationTime
                    pStmt.setLong(8, creationTime);

                    //lastAccessTime
                    pStmt.setLong(9, lastAccessTime);

                    //lastModifiedTime
                    pStmt.setLong(10, lastModifiedTime);

                    //size
                    pStmt.setLong(11, size);

                    //size including children - updated later
                    pStmt.setLong(12, size);

                    pStmt.execute();

                    if (isDirectory) {
                        mapParentPathToRowId.put(fileOrDir, currentRowid);
                        recusiveDirectorySizes.put(fileOrDir, size);
                    } else {
                        // Add the size of this file to the recursive size for the parent.
                        long parentRecursiveSize = recusiveDirectorySizes.get(fileOrDir.getParent());
                        parentRecursiveSize += size;
                        recusiveDirectorySizes.put(fileOrDir.getParent(), parentRecursiveSize);
                    }

                    currentRowid++;
                }


            } catch (SQLException ex) {
                System.err.println("Failed writing record for " + fileOrDir.toFile().getAbsolutePath());
                ex.printStackTrace();
            }
        }
    }
}
