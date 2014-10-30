package au.id.cpf.filetreemap;

import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.utils.AppHelper;
import org.jdesktop.application.utils.OSXAdapter;
import org.jdesktop.application.utils.PlatformType;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.ResourceBundle;

/**
 * Created by chris on 28/10/14.
 */
public class TreemapViewer extends SingleFrameApplication {

    public static void main(String[] args) {
        setupMacSystemProperties(TreemapViewer.class);
        launch(TreemapViewer.class, args);
    }

    public TreemapViewer() {

    }

    @Override
    protected void startup() {
        final JFrame mainFrame = getMainFrame();
        mainFrame.setTitle("FileTreemap");
        mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        JPanel rootPanel = new JPanel();
        rootPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rootPanel.setBackground(SystemColor.control);
        rootPanel.setLayout(new BorderLayout(0, 0));

        rootPanel.add(new TreemapCanvas(), BorderLayout.CENTER);

        JMenuBar menuBar = buildMenus();
        getMainView().setMenuBar(menuBar);

        show(rootPanel);
    }

    @Override
    protected void initialize(String[] args) {
        super.initialize(args);
    }

    private JMenuBar buildMenus() {
        ActionMap actionMap = getContext().getActionMap();

        JMenuBar menuBar = new JMenuBar();

        menuBar.add(buildFileMenu(actionMap));

        menuBar.add(buildHelpMenu(actionMap));

        return menuBar;
    }

    private JMenu buildFileMenu(ActionMap actionMap) {
        JMenu mnuFile = new JMenu("File");
        //mnuFile.setName("mnuFile");

        JMenuItem mnuItOpen = new JMenuItem("Open");
        //mnuItAbout.setAction(actionMap.get("mnuItHelpAbout"));
        mnuFile.add(mnuItOpen);

        return mnuFile;
    }

    private JMenu buildHelpMenu(ActionMap actionMap) {
        JMenu mnuHelp = new JMenu("Help");
        //mnuHelp.setName("mnuHelp");

        JMenuItem mnuItContents = new JMenuItem("Contents");
        //mnuItAbout.setAction(actionMap.get("mnuItHelpAbout"));
        mnuHelp.add(mnuItContents);

        if (isMac()) {
            configureMacAboutBox(actionMap.get("mnuItHelpAbout"));
        } else {
            JMenuItem mnuItAbout = new JMenuItem();
            mnuItAbout.setAction(actionMap.get("mnuItHelpAbout"));
            mnuHelp.add(mnuItAbout);
        }



        return mnuHelp;
    }

    public static void setupMacSystemProperties(Class<? extends Application> applicationClass) {
        if (isMac()) {

            // Unfortunately setting the application about name in a SAF lifecycle method is too late
            // which means we can't use the SAF resource injection.
            String packageName = applicationClass.getPackage().getName();
            String bundleName = packageName+"."+applicationClass.getSimpleName();

            ResourceBundle bundle = ResourceBundle.getBundle(bundleName);
            String name = bundle.getString("Application.shortName");

            System.setProperty("com.apple.mrj.application.apple.menu.about.name", name);
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.fileDialogForDirectories", "true");
        }

    }

    /**
     * Returns true if this application is running on a MAC.
     *
     */
    public static boolean isMac() {
        return PlatformType.OS_X.equals(AppHelper.getPlatform());
    }

    /** Action to invoke to display the about box */
    protected Action _showAboutAction;

    public void showAboutBox() {
        _showAboutAction.actionPerformed(null);
    }

    /**
     * Configures the MAC application menu item (About <application>) to invoke the supplied
     * Action.
     * @param aboutBoxAction the Action to invoke from the MAC about menu.
     */
    protected void configureMacAboutBox(Action aboutBoxAction) {
        _showAboutAction = aboutBoxAction;
        try {
            Method showAboutBox = TreemapViewer.class.getDeclaredMethod("showAboutBox", null);
            OSXAdapter.setAboutHandler(this, showAboutBox);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Overrides shutdown to prevent NullPointerExceptions being output in Ubuntu.
     * (Frames are opened maximised by default so the bounds listener never gets the
     * normal frame bounds which causes a null pointer when trying to save those bounds).
     */
    @Override
    protected void shutdown() {
        try {
            super.shutdown();
        }
        catch (Exception e) {

        }
    }

    class TreemapCanvas extends JPanel {

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            g.drawString("This is my custom Panel!",10,20);
            g.drawRect(5, 5, this.getWidth() - 10, this.getHeight() - 10);
        }
    }
}
