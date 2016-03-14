package au.id.cpf.filetreemap;

/**
 * Created by chris on 14/03/2016.
 */
public class FileNode {
    private int id;
    private String name;
    private long size;

    private int xOffset;
    private int yOffset;
    private int width;
    private int height;

    public FileNode(int id, String name, long size) {
        this.size = size;
        this.id = id;
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getxOffset() {
        return xOffset;
    }

    public void setxOffset(int xOffset) {
        this.xOffset = xOffset;
    }

    public int getyOffset() {
        return yOffset;
    }

    public void setyOffset(int yOffset) {
        this.yOffset = yOffset;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
