package edu.umich.soar.visualsoar.mainframe;

import edu.umich.soar.visualsoar.ProjectModel;
import edu.umich.soar.visualsoar.datamap.DataMap;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.dialogs.*;
import edu.umich.soar.visualsoar.dialogs.find.FindDialog;
import edu.umich.soar.visualsoar.dialogs.find.FindInProjectDialog;
import edu.umich.soar.visualsoar.dialogs.find.ReplaceInProjectDialog;
import edu.umich.soar.visualsoar.files.Backup;
import edu.umich.soar.visualsoar.files.Cfg;
import edu.umich.soar.visualsoar.mainframe.actions.*;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackEntryOpNode;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackList;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackListEntry;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackManager;
import edu.umich.soar.visualsoar.misc.*;
import edu.umich.soar.visualsoar.operatorwindow.*;
import edu.umich.soar.visualsoar.ruleeditor.RuleEditor;
import edu.umich.soar.visualsoar.threepenny.SoarRuntimeSendRawCommandDialog;
import edu.umich.soar.visualsoar.util.ActionButtonAssociation;
import edu.umich.soar.visualsoar.util.KeyStrokeUtil;
import edu.umich.soar.visualsoar.util.MenuAdapter;
import edu.umich.soar.visualsoar.util.SoarUtils;
import org.jetbrains.annotations.NotNull;
import sml.Agent;
import sml.Kernel;
import sml.sml_Names;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.io.*;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;

import static edu.umich.soar.visualsoar.components.FontUtils.*;

// 3P

// The global application class

/**
 * This is the main project window of VisualSoar
 * @author Brad Jones
 */
public class MainFrame extends JFrame
{
/////////////////////////////////////////
// Constants
/////////////////////////////////////////
	private static final long serialVersionUID = 20221225L;

	//This is for the dividers between the operator pane and the desktop and also feedback pane
	public static final double MAX_DIV_POS = 0.95;
	public static final double DEFAULT_OPER_DIV_POS = 0.1;
	public static final double DEFAULT_FB_DIV_POS = 0.65;
	public static final double MIN_DIV_POS = 0.02;

/////////////////////////////////////////
// Static Members
/////////////////////////////////////////
	private static MainFrame s_mainFrame = null;
  private final TitledBorder feedbackListBorder = new TitledBorder(" Feedback ");

  ////////////////////////////////////////
// Data Members
////////////////////////////////////////
	private OperatorWindow operatorWindow;

	private final CustomDesktopPane desktopPane = new CustomDesktopPane();
	private final TemplateManager d_templateManager = new TemplateManager();
	private final JSplitPane operatorDesktopSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	private final JSplitPane feedbackDesktopSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	private final FeedbackList feedbackList = new FeedbackList();
  private final FeedbackManager feedbackManager;

	private String lastWindowViewOperation = "none"; // can also be "tile" or "cascade"

	private JMenu soarRuntimeAgentMenu = null;

	//Has the project been opened in read-only mode
	//NOTE:  do not set this variable directly.  Use setReadOnly()
	private boolean isReadOnly = false;

////////////////////////////////////////
// Access to data members
////////////////////////////////////////

// Dialogs
	AboutDialog aboutDialog = new AboutDialog(this);

	//Actions
	Action newProjectAction = new NewProjectAction(this);
	Action openProjectAction = new OpenProjectAction(this);
    Action openFileAction = new OpenFileAction(this);
	PerformableAction closeProjectAction = new CloseProjectAction(this);
	PerformableAction saveAllFilesAction = new SaveAllFilesAction(this);
	PerformableAction exportAgentAction = new ExportAgentAction(this);
	PerformableAction saveDataMapAndProjectAction = new SaveDataMapAndProjectAction(this);
	Action preferencesAction = new PreferencesAction(this);

	public static final String TOGGLE_RO_ON = "Turn On Read-Only";
	public static final String TOGGLE_RO_OFF = "Turn Off Read-Only";
	public static final String RO_LABEL = "Read-Only: ";

	//Note:  these menu items must be an instance variable because they are changed in read-only mode
	JMenuItem toggleReadOnlyItem = new JMenuItem(TOGGLE_RO_ON);
	JMenuItem saveItem = new JMenuItem("Save");
	JMenu openRecentMenu = new JMenu("Open Recent");

	Action toggleReadOnlyAction = new ToggleReadOnlyAction();
	Action exitAction = new ExitAction();
	Action closeAllWindowsAction = new CloseAllWindowsAction(this);
    Action cascadeAction = new CascadeAction();
    Action tileWindowsAction = new TileWindowsAction();
    Action reTileWindowsAction = new ReTileWindowsAction();
	PerformableAction verifyProjectAction = new VerifyProjectAction(this);
	Action checkSyntaxErrorsAction = new CheckSyntaxErrorsAction(this);
	Action loadTopStateDatamapAction = new LoadTopStateDatamapAction(this);
	Action linkDataMapAction = new LinkDataMapAction(this);
	CheckAllProductionsAction checkAllProductionsAction = new CheckAllProductionsAction(this);
    Action searchDataMapCreateAction = new SearchDataMapCreateAction(this);
    Action searchDataMapTestAction = new SearchDataMapTestAction(this);
    Action searchDataMapCreateNoTestAction = new SearchDataMapCreateNoTestAction(this);
    Action searchDataMapTestNoCreateAction = new SearchDataMapTestNoCreateAction(this);
    Action searchDataMapNoTestNoCreateAction = new SearchDataMapNoTestNoCreateAction(this);

    Action generateDataMapAction = new GenerateDataMapAction(this);
	Action saveProjectAsAction = new SaveProjectAsAction();
	Action contactUsAction = new ContactUsAction();
    Action viewKeyBindingsAction = new ViewKeyBindingsAction(this);
	Action findInProjectAction = new FindInProjectAction();
    Action replaceInProjectAction = new ReplaceInProjectAction();
	Action findAllProdsAction = new FindAllProdsAction();

	// 3P
    Kernel m_Kernel = null ;
    String m_ActiveAgent = null ;
	// Menu handlers for STI init, term, and "Send Raw Command"
	Action soarRuntimeInitAction = new SoarRuntimeInitAction();
	Action soarRuntimeTermAction = new SoarRuntimeTermAction();
	Action soarRuntimeSendRawCommandAction = new SoarRuntimeSendRawCommandAction();
	Action soarRuntimeSendAllFilesAction = new SendAllFilesToSoarAction() ;
  CommitAction commitAction =
      new CommitAction(
          this,
          saveAllFilesAction,
          exportAgentAction,
          saveDataMapAndProjectAction,
          checkAllProductionsAction);

	public Agent getActiveAgent()
	{
		if (m_Kernel == null || m_ActiveAgent == null)
			return null ;
		return m_Kernel.GetAgent(m_ActiveAgent) ;
	}

////////////////////////////////////////
// Constructors
////////////////////////////////////////

	/**
	 * Constructs the operatorWindow, the DesktopPane, the SplitPane, the menubar and the file chooser
	 * 3P Also initializes the STI library
	 * @param s the name of the window
	 */
	public MainFrame(String s)
    {
		// Set the Title of the window
		super(s);
		restorePositionAndSize();
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);  //see addWindowListener call below

		Container contentPane = getContentPane();
		operatorDesktopSplit.setRightComponent(desktopPane);
		operatorDesktopSplit.setOneTouchExpandable(true);
    operatorDesktopSplit.setContinuousLayout(true);

      JLabel statusBar = new JLabel("  Welcome to Visual Soar.");
      feedbackManager =
        new FeedbackManager(
            feedbackList, statusBar, (count) -> feedbackListBorder.setTitle(" Feedback (" + count + ") "));


		desktopSetup();
    resizeHandlerSetup();

		//Add the desktop to the window with status bar at the bottom
		Box vbox = Box.createVerticalBox();
		contentPane.add(vbox);
        vbox.add(feedbackDesktopSplit, BorderLayout.CENTER);
		vbox.add(statusBar);

		//Perform Exit actions on window close
		setJMenuBar(createMainMenu());
		addWindowListener(
            new WindowAdapter()
            {
                public void windowClosing(WindowEvent e)
                {
				exitAction.actionPerformed(
					new ActionEvent(e.getSource(),e.getID(),"Exit"));
                }
            });//addWindowListener()

        d_templateManager.load();

        List<Image> icons = new ArrayList<>();
		Toolkit tk = Toolkit.getDefaultToolkit();
        icons.add(tk.getImage(MainFrame.class.getResource("/vs.png")));
        icons.add(tk.getImage(MainFrame.class.getResource("/vs16.png")));
		this.setIconImages(icons);

    setFontSize(Prefs.editorFontSize.getInt());
    setMenuBarFontSize(getJMenuBar(), Prefs.editorFontSize.getInt());
    feedbackListBorder.setTitleFont(
        getResizedFont(feedbackListBorder.getTitleFont(), Prefs.editorFontSize.getInt()));
    Prefs.editorFontSize.addChangeListener(newValue -> setFontSize((int) newValue));
	}//MainFrame ctor

  ////////////////////////////////////////
  // Methods
  ////////////////////////////////////////

  private void setFontSize(int fontSize) {
    SwingUtilities.invokeLater(
        () -> {
          setMenuBarFontSize(getJMenuBar(), fontSize);
          feedbackListBorder.setTitleFont(
              getResizedFont(feedbackListBorder.getTitleFont(), fontSize));
          UIManager.getLookAndFeelDefaults()
              .forEach(
                  (key, value) -> {
                    if (key.toString().endsWith(".font") && value instanceof Font) {
                      Font oldFont = (Font) value;
                      Font newFont = new Font(oldFont.getName(), oldFont.getStyle(), fontSize);
                      UIManager.put(key, newFont);
                    }
                  });
        });
  }

  private boolean canAutoTile() {
    return Prefs.autoTileEnabled.getBoolean() || lastWindowViewOperation.equals("tile");
  }

	/**
     * This method scans open windows on the VisualSoar desktop for unsaved
     * changes.  It returns true if any are found.
     */
    public boolean isModified() {
        CustomInternalFrame[] frames = desktopPane.getAllCustomFrames();
		for (CustomInternalFrame frame : frames) {
			if (frame.isModified()) return true;
		}

        return false;
    }

    public FeedbackManager getFeedbackManager() {
      return feedbackManager;
    }

	/**
     * Gets the project TemplateManager
     * @return a <code>TemplateManager</code> in charge of all template matters.
     */
	public TemplateManager getTemplateManager()
    {
		return d_templateManager;
	}

	/**
     * Gets the project OperatorWindow
     * @return the project's only <code>OperatorWindow</code>.
     */
	public OperatorWindow getOperatorWindow()
    {
		return operatorWindow;
	}

    /**
     * Gets the project CustomDesktopPane
     * @see CustomDesktopPane
     * @return the project's only <code>CustomDesktopPane</code>.
     */
    public CustomDesktopPane getDesktopPane()
    {
        return desktopPane;
    }

	/**
	 * are we in read-only mode?
	 */
	public boolean isReadOnly() { return this.isReadOnly; }

  /**
	 * A helper function to create the file menu
	 * @return The file menu
	 */
	private JMenu createFileMenu()
    {
		JMenu fileMenu = new JMenu("File");

		//First create a new JMenuItem object for each item in the menu and
		//tie it to its associated handler
		JMenuItem newProjectItem = new JMenuItem("New Project...");
		newProjectItem.addActionListener(newProjectAction);
		newProjectAction.addPropertyChangeListener(
            new ActionButtonAssociation(newProjectAction,newProjectItem));

		JMenuItem openProjectItem = new JMenuItem("Open Project...");
		openProjectItem.addActionListener(openProjectAction);
		openProjectAction.addPropertyChangeListener(
				new ActionButtonAssociation(openProjectAction,openProjectItem));

		JMenuItem openProjectReadOnlyItem = new JMenuItem("Open Project Read-Only...");
		openProjectReadOnlyItem.addActionListener(openProjectAction);
		openProjectAction.addPropertyChangeListener(
				new ActionButtonAssociation(openProjectAction,openProjectReadOnlyItem));

		JMenuItem openFileItem = new JMenuItem("Open File...");
        openFileItem.addActionListener(openFileAction);
        openFileAction.addPropertyChangeListener(
            new ActionButtonAssociation(openFileAction, openFileItem));

		JMenuItem closeProjectItem = new JMenuItem("Close Project");
		closeProjectItem.addActionListener(closeProjectAction);
		closeProjectAction.addPropertyChangeListener(
            new ActionButtonAssociation(closeProjectAction,closeProjectItem));

		saveItem.addActionListener(commitAction);
    saveItem.setAccelerator(KeyStrokeUtil.getPlatformKeyStroke("S"));
		commitAction.addPropertyChangeListener(
            new ActionButtonAssociation(commitAction, saveItem));

		JMenuItem exitItem = new JMenuItem("Exit");
		exitItem.addActionListener(exitAction);
		exitAction.addPropertyChangeListener(
            new ActionButtonAssociation(exitAction,exitItem));

		JMenuItem saveProjectAsItem = new JMenuItem("Save Project As...");
		saveProjectAsItem.addActionListener(saveProjectAsAction);
		saveProjectAsAction.addPropertyChangeListener(
            new ActionButtonAssociation(saveProjectAsAction,saveProjectAsItem));

		//Add all the JMenuItem objects to the file menu
		fileMenu.add(newProjectItem);
		fileMenu.add(openProjectItem);
		fileMenu.add(openRecentMenu);
		fileMenu.add(openProjectReadOnlyItem);
        fileMenu.add(openFileItem);
		fileMenu.add(closeProjectItem);

		fileMenu.addSeparator();

		fileMenu.add(saveItem);
		fileMenu.add(saveProjectAsItem);

		fileMenu.addSeparator();

		fileMenu.add(exitItem);

    // register mnemonics and accelerators
    fileMenu.setMnemonic('F');
    newProjectItem.setAccelerator(KeyStrokeUtil.getPlatformKeyStroke("N"));
    newProjectItem.setMnemonic(KeyEvent.VK_N);
    openProjectItem.setAccelerator(KeyStrokeUtil.getPlatformKeyStroke("O"));
    openProjectItem.setMnemonic(KeyEvent.VK_O);
    openFileItem.setAccelerator(KeyStrokeUtil.getPlatformKeyStroke("F"));
    openFileItem.setMnemonic(KeyEvent.VK_F);

		// Note: on Mac a ≈ character gets inserted before the document is saved. See
		// https://github.com/SoarGroup/VisualSoar/issues/30
		exitItem.setAccelerator(KeyStroke.getKeyStroke("alt X"));
		exitItem.setMnemonic(KeyEvent.VK_X);

		updateRecentProjectsSubMenu();


		return fileMenu;
	}//createFileMenu

	/**
	 * changes the values in the "Open Recent" sub-menu to reflect the latest project loaded
	 */
	public void updateRecentProjectsSubMenu() {
		//Clear the old projects
		openRecentMenu.removeAll();

		//Populate the recent projects
		Vector<Prefs.RecentProjInfo> recentProjs = Prefs.getRecentProjs();
		if (recentProjs.isEmpty()) openRecentMenu.setEnabled(false);
		else {
			//iterate backwards so the most recent file is at the top
			for(int i = recentProjs.size() - 1; i >= 0; --i) {
				Prefs.RecentProjInfo projEntry = recentProjs.get(i);
				JMenuItem recentItem = new JMenuItem(projEntry.toString());
				recentItem.addActionListener(
          new OpenProjectAction(this, projEntry.file, projEntry.isReadOnly));
				openRecentMenu.add(recentItem);
			}
		}//else
	}

	/**
	 * A helper function to create the edit menu
	 * @return The edit menu
	 */
	private JMenu createEditMenu()
    {
		JMenu editMenu = new JMenu("Edit");

		JMenuItem preferencesItem = new JMenuItem("Preferences...");
		preferencesItem.addActionListener(preferencesAction);
		editMenu.add(preferencesItem);
    preferencesItem.setMnemonic('P');
    preferencesItem.setAccelerator(KeyStrokeUtil.getPlatformKeyStroke("COMMA"));

		//Note:  toggleReadOnlyItem must be an instance var so its text can be changed "On" <--> "Off" to match the isReadOnly boolean
		toggleReadOnlyItem.addActionListener(toggleReadOnlyAction);
		editMenu.add(toggleReadOnlyItem);

		return editMenu;
	}

	/**
	 * A helper function to create the search menu
	 * @return The search menu
	 */
	private JMenu createSearchMenu()
    {
		final JMenu searchMenu = new JMenu("Search");
		// View Menu
		JMenuItem findInProjectItem = new JMenuItem("Find in Project");
		findInProjectItem.addActionListener(findInProjectAction);
		findInProjectItem.addPropertyChangeListener(
				new ActionButtonAssociation(findInProjectAction,findInProjectItem));
		searchMenu.add(findInProjectItem);

		JMenuItem replaceInProjectItem = new JMenuItem("Replace in Project");
		replaceInProjectItem.addActionListener(replaceInProjectAction);
		replaceInProjectItem.addPropertyChangeListener(
				new ActionButtonAssociation(replaceInProjectAction,replaceInProjectItem));
		searchMenu.add(replaceInProjectItem);

		JMenuItem findAllProdsItem = new JMenuItem("Find All Productions");
		findAllProdsItem.addActionListener(findAllProdsAction);
		findAllProdsItem.addPropertyChangeListener(
				new ActionButtonAssociation(findAllProdsAction,findAllProdsItem));
		searchMenu.add(findAllProdsItem);

		searchMenu.setMnemonic('A');
		findInProjectItem.setMnemonic(KeyEvent.VK_F);
		findInProjectItem.setAccelerator(KeyStrokeUtil.getPlatformKeyStroke("shift F"));
		replaceInProjectItem.setMnemonic(KeyEvent.VK_R);
		replaceInProjectItem.setAccelerator(KeyStrokeUtil.getPlatformKeyStroke("shift R"));
		findAllProdsItem.setMnemonic(KeyEvent.VK_D);
		findAllProdsItem.setAccelerator(KeyStrokeUtil.getPlatformKeyStroke("shift D"));

		return searchMenu;

	}//createSearchMenu

	/**
	 * A helper function to create the Datamap menu
	 * @return The datamap menu
	 */
	private JMenu createDatamapMenu()
    {
		JMenu datamapMenu = new JMenu("Datamap");

		JMenuItem topStateDatamapItem = new JMenuItem("Display Top-State Datamap");
		topStateDatamapItem.addActionListener(loadTopStateDatamapAction);
		topStateDatamapItem.addPropertyChangeListener(
				new ActionButtonAssociation(loadTopStateDatamapAction,topStateDatamapItem));

		JMenuItem checkAllProductionsItem = new JMenuItem("Check All Productions Against the Datamap");
		checkAllProductionsItem.addActionListener(checkAllProductionsAction);
		checkAllProductionsAction.addPropertyChangeListener(
				new ActionButtonAssociation(checkAllProductionsAction,checkAllProductionsItem));

		JMenuItem checkSyntaxErrorsItem = new JMenuItem("Check All Productions for Syntax Errors");
		checkSyntaxErrorsItem.addActionListener(checkSyntaxErrorsAction);
		checkSyntaxErrorsAction.addPropertyChangeListener(
            new ActionButtonAssociation(checkSyntaxErrorsAction,checkSyntaxErrorsItem));

		JMenuItem generateDataMapItem = new JMenuItem("Generate the Datamap from the Current Operator Hierarchy");
		generateDataMapItem.addActionListener(generateDataMapAction);
		generateDataMapAction.addPropertyChangeListener(
				new ActionButtonAssociation(generateDataMapAction, generateDataMapItem));

		JMenuItem searchDataMapTestItem = new JMenuItem("Search the Datamap for WMEs that are Never Tested");
		searchDataMapTestItem.addActionListener(searchDataMapTestAction);
		searchDataMapTestAction.addPropertyChangeListener(
            new ActionButtonAssociation(searchDataMapTestAction,searchDataMapTestItem));

		JMenuItem searchDataMapCreateItem = new JMenuItem("Search the Datamap for WMEs that are Never Created");
		searchDataMapCreateItem.addActionListener(searchDataMapCreateAction);
		searchDataMapCreateAction.addPropertyChangeListener(
            new ActionButtonAssociation(searchDataMapCreateAction,searchDataMapCreateItem));

		JMenuItem searchDataMapTestNoCreateItem = new JMenuItem("Search the Datamap for WMEs that are Tested but Never Created");
		searchDataMapTestNoCreateItem.addActionListener(searchDataMapTestNoCreateAction);
		searchDataMapTestNoCreateAction.addPropertyChangeListener(
            new ActionButtonAssociation(searchDataMapTestNoCreateAction,searchDataMapTestNoCreateItem));

		JMenuItem searchDataMapCreateNoTestItem = new JMenuItem("Search the Datamap for WMEs that are Created but Never Tested");
		searchDataMapCreateNoTestItem.addActionListener(searchDataMapCreateNoTestAction);
		searchDataMapCreateNoTestAction.addPropertyChangeListener(
            new ActionButtonAssociation(searchDataMapCreateNoTestAction,searchDataMapCreateNoTestItem));

        JMenuItem searchDataMapNoTestNoCreateItem = new JMenuItem("Search the Datamap for WMEs that are Never Tested and Never Created");
		searchDataMapNoTestNoCreateItem.addActionListener(searchDataMapNoTestNoCreateAction);
		searchDataMapNoTestNoCreateAction.addPropertyChangeListener(
            new ActionButtonAssociation(searchDataMapNoTestNoCreateAction,searchDataMapNoTestNoCreateItem));

		JMenuItem linkDataMapItem = new JMenuItem("Link Entries from Another Project's Datamap");
		linkDataMapItem.addActionListener(linkDataMapAction);
		linkDataMapItem.addPropertyChangeListener(
				new ActionButtonAssociation(linkDataMapAction,linkDataMapItem));



		datamapMenu.add(topStateDatamapItem);
		datamapMenu.addSeparator();
		datamapMenu.add(checkAllProductionsItem);
		datamapMenu.add(checkSyntaxErrorsItem);
		datamapMenu.addSeparator();
        datamapMenu.add(generateDataMapItem);
		datamapMenu.addSeparator();
        datamapMenu.add(searchDataMapTestItem);
        datamapMenu.add(searchDataMapCreateItem);
        datamapMenu.add(searchDataMapTestNoCreateItem);
        datamapMenu.add(searchDataMapCreateNoTestItem);
        datamapMenu.add(searchDataMapNoTestNoCreateItem);
		datamapMenu.addSeparator();
		datamapMenu.add(linkDataMapItem);

		datamapMenu.setMnemonic(KeyEvent.VK_D);

        return datamapMenu;
    }

	/**
	 * A helper function to create the view menu
	 * @return The view menu
	 */
	private JMenu createViewMenu()
    {
		final JMenu viewMenu = new JMenu("View");
      int numberViewMenuItems = 0;
		//Close All Windows
		JMenuItem closeAllWindowsItem = new JMenuItem("Close All Windows");
		closeAllWindowsItem.addActionListener(closeAllWindowsAction);
		closeAllWindowsItem.addPropertyChangeListener(
				new ActionButtonAssociation(closeAllWindowsAction,closeAllWindowsItem));
		viewMenu.add(closeAllWindowsItem);
      numberViewMenuItems++;

		JMenuItem cascadeItem = new JMenuItem("Cascade Windows");
		cascadeItem.addActionListener(cascadeAction);
		cascadeItem.addPropertyChangeListener(
            new ActionButtonAssociation(cascadeAction,cascadeItem));
		viewMenu.add(cascadeItem);
      numberViewMenuItems++;

		JMenuItem tileWindowItem = new JMenuItem("Tile Windows Horizontally");
		tileWindowItem.addActionListener(tileWindowsAction);
		tileWindowItem.addPropertyChangeListener(
            new ActionButtonAssociation(tileWindowsAction,tileWindowItem));
		viewMenu.add(tileWindowItem);
      numberViewMenuItems++;

		JMenuItem reTileWindowItem = new JMenuItem("Tile Windows Vertically");
		reTileWindowItem.addActionListener(reTileWindowsAction);
		reTileWindowItem.addPropertyChangeListener(
            new ActionButtonAssociation(reTileWindowsAction,reTileWindowItem));
		viewMenu.add(reTileWindowItem);
      numberViewMenuItems++;

    JMenuItem increaseFontSizeItem = new JMenuItem("Increase Font Size");
    increaseFontSizeItem.addActionListener(
      new IncreaseFontSizeAction(this));
    increaseFontSizeItem.setAccelerator(KeyStrokeUtil.getPlatformKeyStroke("EQUALS"));
    viewMenu.add(increaseFontSizeItem);
    numberViewMenuItems++;

    // Add Decrease Font Size
    JMenuItem decreaseFontSizeItem = new JMenuItem("Decrease Font Size");
    decreaseFontSizeItem.addActionListener(
      new DecreaseFontSizeAction(this));
    decreaseFontSizeItem.setAccelerator(KeyStrokeUtil.getPlatformKeyStroke("MINUS"));
    viewMenu.add(decreaseFontSizeItem);
    numberViewMenuItems++;

      final int finalNumberViewMenuItems = numberViewMenuItems;
    viewMenu.addMenuListener(
        new MenuListener() {
          public void menuCanceled(MenuEvent e) {}

          public void menuDeselected(MenuEvent e) {
            for (int i = viewMenu.getMenuComponentCount() - 1;
                i > finalNumberViewMenuItems - 1;
                --i) {
              viewMenu.remove(i);
            }
          }

          public void menuSelected(MenuEvent e) {
            JInternalFrame[] frames = desktopPane.getAllFrames();
            for (JInternalFrame frame : frames) {
              JMenuItem menuItem = new JMenuItem(frame.getTitle());
              menuItem.addActionListener(new WindowMenuListener(frame));
              setMenuItemFontSize(menuItem, Prefs.editorFontSize.getInt());
              viewMenu.add(menuItem);
            }
          }
        });

		viewMenu.setMnemonic('V');
		//cascadeWindowItem.setMnemonic(KeyEvent.VK_C);
		tileWindowItem.setMnemonic(KeyEvent.VK_T);
		tileWindowItem.setAccelerator(KeyStrokeUtil.getPlatformKeyStroke("T"));
		reTileWindowItem.setAccelerator(KeyStrokeUtil.getPlatformKeyStroke("shift T"));


		return viewMenu;
	}

    /**
     * This function is called when the project is being closed (i.e.,
     * due to window close, opening new project or selecting "Close Project"
     * from the File menu).  It detects unsaved changes and asks the user
     * what to do about it.
	 * @return true if application should close, false otherwise
     */
    boolean checkForUnsavedProjectOnClose()
    {
        //If the user has modified files that have not been saved,
        //we need to ask the user what to do about it.
        if (isModified()) {
            String[] buttons = { "Save all files, then exit.",
                                 "Exit without Saving" };

            String selectedValue = (String)
                JOptionPane.showInputDialog(
                    getMainFrame(),
                    "This project has unsaved files.  What should I do?",
                    "Abandon Project",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    buttons,
                    buttons[0]);

            //If user hits Cancel button
            if (selectedValue == null)
            {
                return false;
            }

            //If user selects Save all files, then exit
            if (selectedValue.equals(buttons[0]))
            {
                saveAllFilesAction.perform();
            }

            //If user selects Exit without Saving
            if (selectedValue.equals(buttons[1]))
            {
                //Make all files as unchanged
                CustomInternalFrame[] frames = desktopPane.getAllCustomFrames();
				for (CustomInternalFrame frame : frames) {
					frame.setModified(false);
				}

            }
        }
		return true;
    }


    /**
     * This function opens a file (assumed to be a non-Soar file).
     */
    public void OpenFile(File file)
    {
        try
        {
            boolean oldPref = Prefs.highlightingEnabled.getBoolean();
            Prefs.highlightingEnabled.setBoolean(false);  // Turn off highlighting
            RuleEditor ruleEditor = new RuleEditor(file);
            ruleEditor.setVisible(true);
            addRuleEditor(ruleEditor);
            ruleEditor.setSelected(true);
            Prefs.highlightingEnabled.setBoolean(oldPref);  // Turn it back to what it was
        }
        catch(IOException IOE)
        {
            JOptionPane.showMessageDialog(MainFrame.this,
                                          IOE.getMessage(),
                                          "I/O Error Reading " + file.getName(),
                                          JOptionPane.ERROR_MESSAGE);
        }
        catch(java.beans.PropertyVetoException pve)
        {
            //No sweat. This just means the new window failed to get focus.
        }
    }//OpenFile()

	/**
	 * sets the VisualSoar window's position and size to a default
	 */
	private void useDefaultPositionAndSize() {
		// Use Java toolkit to access user's screen size and set VisualSoar window to 90% of that size
		Toolkit tk = Toolkit.getDefaultToolkit();
		Dimension d = tk.getScreenSize();
		setSize( ((int) (d.getWidth() * .9)), ((int) (d.getHeight() * .9)) );
	}

	/**
	 * restores the Visual Soar to the position and size it had last time
	 */
	private void restorePositionAndSize() {
		//If size/position data is missing then use the default size/pos
		String lastXStr = Prefs.lastXPos.get();
		String lastYStr = Prefs.lastYPos.get();
		String lastWidthStr = Prefs.lastWidth.get();
		String lastHeightStr = Prefs.lastHeight.get();
		if ((lastXStr == null)  || (lastYStr == null) || (lastWidthStr == null) || (lastHeightStr == null)) {
			useDefaultPositionAndSize();
			return;
		}

		//If the last pos/size data is not parseable use the default size/pos
		int lastX;
		int lastY;
		int lastWidth;
		int lastHeight;
		try {
			lastX = Integer.parseInt(lastXStr);
			lastY = Integer.parseInt(lastYStr);
			lastWidth = Integer.parseInt(lastWidthStr);
			lastHeight = Integer.parseInt(lastHeightStr);
		}
		catch(NumberFormatException nfe) {
			useDefaultPositionAndSize();
			return;
		}

		//if the window is off the screen (or CLOSE to it), then use the default size/pos
		int CLOSE = 30; //I arbitrarily choose 30 pixels as "close"
		Toolkit tk = Toolkit.getDefaultToolkit();
		Dimension d = tk.getScreenSize();
		if ((lastX < 0) || (lastY < 0) || (lastX + CLOSE > d.width) || (lastY + CLOSE > d.height)) {
			useDefaultPositionAndSize();
			return;
		}

		//Ok, we are ready to accept these previous values
		setSize(lastWidth, lastHeight);
		setLocation(lastX, lastY);
	}//restorePositionAndSize

	/** stores the current window position and size, so they can be restored
	 * when Visual Soar runs again */
	private void savePositionAndSize() {
		Point loc = getLocation();
		Prefs.lastXPos.set("" + loc.x);
		Prefs.lastYPos.set("" + loc.y);
		Dimension dim = getSize();
		Prefs.lastWidth.set("" + dim.width);
		Prefs.lastHeight.set("" + dim.height);

	}

  /**
   * Save the project and all Soar source files.
   *
   * @param checkDm perform the project datamap check if true and set in preferences, skip it if
   *     false
   */
  public void commit(boolean checkDm) {
    commitAction.perform(checkDm);
  }

  /**
     * When the Soar Runtime|Agent menu is selected, this listener
     * populates the menu with the agents that exist in the kernel
     * we're currently connected to through SML.
     * @author ThreePenny
     */
	class SoarRuntimeAgentMenuListener extends MenuAdapter
    {
		public void menuSelected(MenuEvent e)
        {
			// Remove our existing items
			soarRuntimeAgentMenu.removeAll();

			// Check to see if we have a connection
			if (m_Kernel == null)
            {
				// Add a "not connected" menu item
				JMenuItem menuItem=new JMenuItem("<not connected>");
				menuItem.setEnabled(false);

				soarRuntimeAgentMenu.add(menuItem);
				return;
			}

			// Get the connection names
			int nAgents = m_Kernel.GetNumberAgents() ;

			// If we don't have any connections then display the
			// appropriate menu item.
			if (nAgents == 0)

            {
				// Add the "no agents" menu item
				JMenuItem menuItem=new JMenuItem("<no agents>");
				menuItem.setEnabled(false);

				soarRuntimeAgentMenu.add(menuItem);
				return;
			}

			// Add each name
			int i;
			for (i=0; i < nAgents; i++)

            {
				// Get this agent's name
				sml.Agent agent = m_Kernel.GetAgentByIndex(i) ;

				if (agent == null)
					continue ;

				String name = agent.GetAgentName() ;

				// Create the connection menu and add a listener to it
				// which contains the connection index.
				JRadioButtonMenuItem connectionMenuItem=new JRadioButtonMenuItem(name);

				if (name.equals(m_ActiveAgent))
					connectionMenuItem.setSelected(true) ;

				connectionMenuItem.addActionListener(new AgentConnectionActionListener(name));

				// Add the menu item
				soarRuntimeAgentMenu.add(connectionMenuItem);
			}
		}
	}

    /**
     * Listener activated when the user clicks on an Agent in the Soar
     * Runtime |Agents menu.  When the user clicks on an agent in the menu,
     * it is activated/deactivated.
     * @author ThreePenny
     */
	class AgentConnectionActionListener implements ActionListener
    {
		// Index of this agent connection in the menu
		private final String m_sAgentConnectionName;

		// Constructor
		public AgentConnectionActionListener(String sAgentConnectionName)
        {
			m_sAgentConnectionName = sAgentConnectionName;
		}

		// Called when the action has been performed
		public void actionPerformed(ActionEvent e)

        {
			m_ActiveAgent  = m_sAgentConnectionName ;
		}
	}


    /**
     * Creates the "Soar Runtime" menu which appears in the MainFrame
     * @author ThreePenny
     * @return a <code>soarRuntimeMenu</code> JMenu
     */
	private JMenu createSoarRuntimeMenu()

    {
		// Add the menu as set the mnemonic
		JMenu soarRuntimeMenu = new JMenu("Soar Runtime");
		soarRuntimeMenu.setMnemonic('S');

		// Add the menu items
		JMenuItem connectMenuItem= soarRuntimeMenu.add(soarRuntimeInitAction);
		JMenuItem disconnectMenuItem= soarRuntimeMenu.add(soarRuntimeTermAction);
		JMenuItem sendAllFilesMenuItem= soarRuntimeMenu.add(soarRuntimeSendAllFilesAction);
		JMenuItem sendRawCommandMenuItem= soarRuntimeMenu.add(soarRuntimeSendRawCommandAction);

		// Build the "Connected Agents" menu
		soarRuntimeAgentMenu = new JMenu("Connected Agents");
		soarRuntimeAgentMenu.addMenuListener(
            new SoarRuntimeAgentMenuListener());
		soarRuntimeAgentMenu.setEnabled(false);

		// Add the "Connected Agents" menu
		soarRuntimeMenu.add(soarRuntimeAgentMenu);

		// Set the mnemonics
		connectMenuItem.setMnemonic('C');
		disconnectMenuItem.setMnemonic('D');
		sendAllFilesMenuItem.setMnemonic('S');
		sendRawCommandMenuItem.setMnemonic('R');
		soarRuntimeAgentMenu.setMnemonic('A');

		return soarRuntimeMenu;
	}

	/**
	 * A helper function to create the help menu
	 * @return The help menu
	 */
	private JMenu createHelpMenu()
    {
		JMenu helpMenu = new JMenu("Help");
		// Help menu
		helpMenu.add(contactUsAction);
        helpMenu.add(viewKeyBindingsAction);
		helpMenu.setMnemonic('H');
		return helpMenu;
	}

	/**
	 * A Helper function that creates the main menu bar
	 * @return The MenuBar just created
	 */
	private JMenuBar createMainMenu()
    {
		JMenuBar MenuBar = new JMenuBar();

		// The Main Menu Bar
		MenuBar.add(createFileMenu());
		MenuBar.add(createEditMenu());
		MenuBar.add(createSearchMenu());
        MenuBar.add(createDatamapMenu());
		MenuBar.add(createViewMenu());

		// 3P
		// Add the Soar Runtime menu
        MenuBar.add(createSoarRuntimeMenu());

		MenuBar.add(createHelpMenu());

		return MenuBar;
	}

    /**
     * Sets a new JInternalFrame's shape and position based upon the user's
     * apparent preference.
     */
    void setJIFShape(JInternalFrame newJIF)
    {
        JInternalFrame currJIF = desktopPane.getSelectedFrame();
        if ( (currJIF != null) && (currJIF.isMaximum()) )
        {
            try
            {
                newJIF.setMaximum(true);
            }
            catch (java.beans.PropertyVetoException pve)
            {
                //Unable to maximize window.  Oh, well.
            }
        }
		else if (canAutoTile())
        {
			desktopPane.performTileAction();
		}
        else if (lastWindowViewOperation.equals("cascade"))
        {
            desktopPane.performCascadeAction();
		}
        else if (currJIF != null)
        {
            Rectangle bounds = currJIF.getBounds();
            newJIF.reshape(bounds.x + 30,
                           bounds.y + 30,
                           bounds.width,
                           bounds.height);
        }
        else
        {
            newJIF.reshape(0, 0 , 400, 400);
        }

    }//setJIFShape

	/**
	 * Creates a rule window opening with the given file name
	 * @param re the rule editor file that the rule editor should open
	 */
	public void addRuleEditor(RuleEditor re)
    {
		desktopPane.add(re);
		re.moveToFront();
		desktopPane.revalidate();
        setJIFShape(re);
	}

	/**
	 * Creates a datamap window with the given datamap.
	 * @param dm the datamap that the window should open
     * @see DataMap
	 */
	public void addDataMap(DataMap dm)
    {
        if (!desktopPane.hasDataMap(dm))
        {
            desktopPane.add(dm);
            desktopPane.revalidate();
            setJIFShape(dm);
        }
        else
        {
            dm = desktopPane.dmGetDataMap(dm.getId());
        }

        try
        {
            dm.setSelected(true);
            dm.setIcon(false);
            dm.moveToFront();
        }
		catch (java.beans.PropertyVetoException pve)
        {
            //no sweat.
        }

	}

	/**
     * Makes the specified rule editor window the selected window
     * and brings the window to the front of the frame.
     * @param re the specified rule editor window
     */
	public void showRuleEditor(RuleEditor re)
    {
		try
        {
			if (re.isIcon()) {
				re.setIcon(false);
			}
			re.setSelected(true);
			re.moveToFront();
		}
		catch (java.beans.PropertyVetoException pve)
        {
            System.err.println("Guess we can't do that");
        }
	}

	/**
     * Selects a currently open editor window and brings it to the front.
     * If none are open then no action is taken.
     */
    public void selectNewInternalFrame()
    {
        JInternalFrame[] jif = desktopPane.getAllFrames();
		for(int i = jif.length-1; i >= 0; i--)
        {
            if(jif[i].isShowing())
            {
            	// Use SwingUtilities.invokeLater for better Windows compatibility
              final int iInternal = i;
              SwingUtilities.invokeLater(() -> {
	                try
	                {
	                    if (jif[iInternal].isIcon())
	                    {
	                        jif[iInternal].setIcon(false);
	                    }
	                    jif[iInternal].setSelected(true);
	                    jif[iInternal].moveToFront();
	                    // Request focus for better Windows behavior
	                    jif[iInternal].requestFocusInWindow();
	                }
	                catch (java.beans.PropertyVetoException pve)
	                {
	                    pve.printStackTrace();
	                }
            	});
                break;
            }
        }//for

	}//selectNewInternalFrame()


	/**
	 * This class is used to bring an internal frame to the front
	 * if it is selected from the view menu
	 */
	static class WindowMenuListener implements ActionListener
    {
		JInternalFrame internalFrame;

		public WindowMenuListener(JInternalFrame jif)
        {
			internalFrame = jif;
		}

		public void actionPerformed(ActionEvent e)
        {
			// Use SwingUtilities.invokeLater for better Windows compatibility
			SwingUtilities.invokeLater(() -> {
				internalFrame.toFront();
				try
	            {
					internalFrame.setIcon(false);
					internalFrame.setSelected(true);
					internalFrame.moveToFront();
					// Request focus for better Windows behavior
					internalFrame.requestFocusInWindow();
				} catch (java.beans.PropertyVetoException pve) {
					System.err.println("Guess we can't do that"); }
			});
		}
	}


  	/**
  	 * Sets the main window
  	 */
  	public static void setMainFrame(MainFrame mainFrame)
    {
  		s_mainFrame = mainFrame;
  	}

  	/**
  	 * Gets the main window
  	 */
  	public static MainFrame getMainFrame()
    {
  		return s_mainFrame;
  	}

	/**
	 * sets the divider position between the operator pane and the desktop
	 * based upon the last user setting.
	 */
	private static boolean operListenerSetup = false;  //avoid re-creating listener
	private void operDividerSetup() {
		double position = Double.parseDouble(Prefs.operDividerPosition.get());
		if ((position < MIN_DIV_POS) || (position > MAX_DIV_POS)) {
			position = DEFAULT_OPER_DIV_POS;
			Prefs.operDividerPosition.set("" + DEFAULT_OPER_DIV_POS);
		}
		operatorDesktopSplit.setDividerLocation(position);

		//Whenever the user moves the divider, remember the user's preference
		if (!operListenerSetup) {
			operatorDesktopSplit.addPropertyChangeListener(
					JSplitPane.DIVIDER_LOCATION_PROPERTY,
					new PropertyChangeListener() {
						@Override
						public void propertyChange(PropertyChangeEvent evt) {
							//Retrieve the value as a double
							String valStr = evt.getNewValue().toString();
							int val = Integer.parseInt(valStr);

							//Convert it to a fraction of split pane's size
							int paneWidth = operatorDesktopSplit.getWidth();
							double proportion = (double) val / (double) paneWidth;

							//Save the new value to Prefs (if sane)
							if ((proportion >= MIN_DIV_POS) && (proportion <= MAX_DIV_POS)) {
								Prefs.operDividerPosition.set("" + proportion);
								Prefs.flush();
							}

							//Also schedule re-tile
							scheduleDelayedReTile();

						}
					}



			);
			operListenerSetup = true;
		}//listener setup
	}//operDividerSetup

	/**
	 * For some bizarre reason, the divider-location-property change event
	 * seems to occur before the underlying pane is resized as a result of
	 * the event.  I'm guessing the event handlers are out of order, but
	 * I don't know how to fix that since the pane-resize code is in
	 * swing, not part of Visual Soar.  As a result, re-tiling the windows
	 * in the event handler doesn't work.  Any easy, super-kludgey fix
	 * is just to wait a split second and then re-tile.  This method
	 * does that.
	 *
	 * Note:  It might be better to use javax.swing.Timer for this?  For
	 *        now, I'm not inclined to fix something that's not broken.
	 */
	private void scheduleDelayedReTile() {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					/* don't care */
				}
				desktopPane.performTileAction();
			}
		});
		t.start();
	}//scheduleDelayedReTile

  /**
   */
  private void desktopSetup() {
    JScrollPane sp = new JScrollPane(feedbackList);
    sp.setBorder(feedbackListBorder);

    feedbackDesktopSplit.setTopComponent(operatorDesktopSplit);
    feedbackDesktopSplit.setBottomComponent(sp);
    feedbackDesktopSplit.setOneTouchExpandable(true);
    feedbackDesktopSplit.setContinuousLayout(true);

    setFeedbackDividerPosFromPrefs();
    // Whenever the user moves the divider, remember the user's preference
    feedbackDesktopSplit.addPropertyChangeListener(
        JSplitPane.DIVIDER_LOCATION_PROPERTY,
        evt -> {
          // Retrieve the value as a double
          String valStr = evt.getNewValue().toString();
          int dividerLocation = Integer.parseInt(valStr);

          // Convert it to a fraction of split pane's size
          double proportion = (double) dividerLocation / getHeight();

          // Save the new value to Prefs (if sane)
          if ((proportion >= MIN_DIV_POS) && (proportion <= MAX_DIV_POS)) {
            Prefs.fbDividerPosition.set("" + proportion);
            Prefs.flush();
          }

          feedbackPanelHeight = getHeight() - dividerLocation;

          // Re-tile the windows as a result
          scheduleDelayedReTile();
        });
  } // dividerSetup

  private int feedbackPanelHeight = -1;

  /**
   * Sets the divider position between the feedback pane and the desktop based upon the last user
   * setting.
   */
  private void setFeedbackDividerPosFromPrefs() {
    double position = Double.parseDouble(Prefs.fbDividerPosition.get());
    if ((position < MIN_DIV_POS) || (position > MAX_DIV_POS)) {
      position = DEFAULT_FB_DIV_POS;
      Prefs.fbDividerPosition.set("" + DEFAULT_FB_DIV_POS);
    }
    int newHeight = ((int) (getHeight() * position));
    feedbackDesktopSplit.setDividerLocation(newHeight);
    feedbackPanelHeight = getHeight() - newHeight;
  }

  private void resizeHandlerSetup() {
    ComponentAdapter resizeListener =
        new ComponentAdapter() {
          @Override
          public void componentResized(ComponentEvent e) {
            if (canAutoTile()) {
              desktopPane.performTileAction();
            }
            // make the feedback panel stick remain the same size (stick to bottom of the window)
            feedbackDesktopSplit.setDividerLocation(getHeight() - feedbackPanelHeight);
          }
        };
    addComponentListener(resizeListener);
  }

	/**
	 * setReadOnly
	 *
	 * configures the UI to properly reflect a new read-only status of the project
	 *
	 * @param  status true=read-only  false=editable
	 */
	private void setReadOnly(boolean status) {
		this.isReadOnly = status;

		//Change the read-only status of all rule editor windows
		CustomInternalFrame[] frames = desktopPane.getAllCustomFrames();
		for (CustomInternalFrame frame : frames) {
			frame.setReadOnly(status);
		}


		//Set the project's title bar to reflect this status
		if (this.isReadOnly) {
			toggleReadOnlyItem.setText(TOGGLE_RO_OFF);
		}
		else {
			toggleReadOnlyItem.setText(TOGGLE_RO_ON);
		}

		//Reset the title so that the proper read-only status appears
		setTitle(getTitle());

		//The user may not save the project in read-only mode
		saveItem.setEnabled(! status);
	}//readOnlySetup

	/**
	 * call this method when the user attempts to edit the project
	 * when it is in read-only mode.  It notifies the user of
	 * the error.
	 */
	public void rejectForReadOnly() {
		getFeedbackManager().setStatusBarMsg("You must turn off Read-Only mode to edit this project.");
		getToolkit().beep();
	}

	/** override this method to guarantee the current read-only status is in the title */
	@Override
	public void setTitle(String title) {
		//Remove read-only status if present so title is reset to its base state
		title = title.replace(RO_LABEL, "");

		//Then re-add it if need be
		if (this.isReadOnly) {
			//Add "Read-Only" prefix to the title bar
			title = RO_LABEL + title;
		}

		//Then set it
		super.setTitle(title);
	}



	/**
  	 * enables the corresponding actions for when a project is opened
  	 */
  	private void projectActionsEnable(boolean areEnabled)
    {
  		// Enable various actions
		saveAllFilesAction.setEnabled(areEnabled);
		toggleReadOnlyItem.setEnabled(areEnabled);
		loadTopStateDatamapAction.setEnabled(areEnabled);
		checkAllProductionsAction.setEnabled(areEnabled);
		checkSyntaxErrorsAction.setEnabled(areEnabled);
        searchDataMapTestAction.setEnabled(areEnabled);
        searchDataMapCreateAction.setEnabled(areEnabled);
        searchDataMapTestNoCreateAction.setEnabled(areEnabled);
        searchDataMapCreateNoTestAction.setEnabled(areEnabled);
        searchDataMapNoTestNoCreateAction.setEnabled(areEnabled);
		linkDataMapAction.setEnabled(areEnabled);
        generateDataMapAction.setEnabled(areEnabled);
		closeProjectAction.setEnabled(areEnabled);
		commitAction.setEnabled(areEnabled);
		saveProjectAsAction.setEnabled(areEnabled);

		/*
		 * Note:  The following menu items are NOT in the list above and, thus, remain
		 * enabled even when there is no project currently open.
		 *  - (re)tile windows
		 *  - find in project
		 *  - replace in project
		 *  - find all productions
		 *
		 * Why???  Because there's a quirky behavior in Swing:  When an action is
		 * disabled, it's associated menu item's accelerator is also disabled as would
		 * be expected.  However, when it is re-enabled the accelerator is not re-instated
		 * UNTIL the menu is used manually by the user via the mouse.  It's an odd behavior
		 * that seems to be a bug in Swing afaict. Apparently it's possible to workaround it
		 * by having the menu ask for focus or resetting the accelerator.  I tried to
		 * the former, but it didn't work for me.  The latter seems inferior to the workaround
		 * I've chosen (below).
		 *
		 * Here are some relevant Stack Overflow articles:
		 *   https://stackoverflow.com/questions/22507505/keystroke-accelerator-not-working-after-disabling-jmenuitem
		 *   https://stackoverflow.com/questions/71976058/accelerator-on-jmenuitem-doesnt-work-if-the-jmenu-is-not-open-but-only-a-few
		 *
		 * Since I would expect the user will be annoyed if certain hot keys don't work right
		 * after a project is loaded, I've worked around this error by making the above actions
		 * detect when there is no project currently open and just silently abort in their
		 * actionPerformed() methods. Thus, I can keep them (and their hotkeys) active at all times.
		 *
		 * -:AMN:  30 Mar 2023
		 */
  	}

	/**
	 * opens the top-state datamap in a window.  If it's already open, the existing window will be used.
	 */
	public void openTopStateDatamap() {
		OperatorRootNode root = (OperatorRootNode)(operatorWindow.getModel().getRoot());
		SoarWorkingMemoryModel dataMap = MainFrame.this.operatorWindow.getDatamap();
		root.openDataMap(dataMap, MainFrame.this);
	}


/*########################################################################################
  Actions
  ########################################################################################*/

  /**
	 * Exit command
	 * First closes all the RuleEditor windows
	 * if all the closes go successfully, then it closes
	 * the operator hierarchy then exits
	 */
	class ExitAction extends AbstractAction {
		private static final long serialVersionUID = 20221225L;

		public ExitAction()
        {
			super("Exit");
		}
		public void actionPerformed(ActionEvent event)
        {
			perform();
		}

		public void perform() {
			if (!checkForUnsavedProjectOnClose()) {
				return;
			}
			JInternalFrame[] frames = desktopPane.getAllFrames();
			Prefs.flush();
			try
            {
				savePositionAndSize();

				if (CustomInternalFrame.hasEverChanged()) {
//          TODO: ask user first
					commitAction.perform();
				}
				else {
//          TODO: shouldn't this be done in above if statement, too?
					Cfg.writeCfgFile(MainFrame.this);
					Backup.deleteAutoBackupFiles(MainFrame.this);
				}

				for (JInternalFrame frame : frames) {
					frame.setClosed(true);
				}

				// 3P
				// Close down the STI library
				SoarRuntimeTerm();

				dispose();
				System.exit(0);
			}
			catch (PropertyVetoException pve) { /* ignore */ }
		}
	}

  public void closeProject() {
    checkForUnsavedProjectOnClose();

    JInternalFrame[] frames = desktopPane.getAllFrames();
    try
    {
//      TODO: ask user first
//      TODO: what about cfg?
      // TODO: can we call this from exitAction? What's the difference between closeProject and exit?
      if ( (!isReadOnly()) && (CustomInternalFrame.hasEverChanged()) ) commitAction.perform();
      Backup.deleteAutoBackupFiles(MainFrame.this);

      for (JInternalFrame frame : frames) {
        frame.setClosed(true);
      }
      operatorDesktopSplit.setLeftComponent(null);

      projectActionsEnable(false);

      feedbackList.clearListData();

      //This acts as a flag to indicate there is no project
      operatorWindow = null;
    }
    catch (java.beans.PropertyVetoException pve) { /* ignore */ }

    setTitle("VisualSoar");
  }

  public boolean projectIsOpen() {
    return operatorWindow != null;
  }

  public void openProject(@NotNull File vsaFile, boolean readOnly) {
    // Get rid of the old project (if it exists)
    if (projectIsOpen()) {
      closeProjectAction.perform();
    }

    try {
      //Open the new project
      operatorWindow = new OperatorWindow(vsaFile, readOnly);
      if(vsaFile.getParent() != null) {
        Prefs.openFolder.set(vsaFile.getParentFile().getAbsolutePath());
      }
      operatorDesktopSplit.setLeftComponent(new JScrollPane(operatorWindow));

      projectActionsEnable(true);

      //Set and monitor the divider position
      operDividerSetup();

      //Verify project integrity
      verifyProjectAction.perform();

      //Reset tracking whether any change has been made to this project
      CustomInternalFrame.resetEverchanged();

      // Set the title bar to include the project name
      setTitle(vsaFile.getName().replaceAll(".vsa.json", "").replaceAll(".vsa", ""));

      //Reopen windows that were open last time
      Cfg.readCfgFile(this);

      //Configure read-only status
      setReadOnly(readOnly);
    } catch (FileNotFoundException fnfe) {
      JOptionPane.showMessageDialog(
        this, fnfe.getMessage(), "File Not Found", JOptionPane.ERROR_MESSAGE);
      getFeedbackManager().setStatusBarError("Failed to open " + vsaFile);
    } catch (IOException ioe) {
      getFeedbackManager().setStatusBarError("Failed to open " + vsaFile);
      JOptionPane.showMessageDialog(
        this, ioe.getMessage(), "I/O Exception", JOptionPane.ERROR_MESSAGE);
      ioe.printStackTrace();
    } catch (NumberFormatException nfe) {
      // TODO: find where this is getting thrown and change it to a (possibly custom) checked
      // exception
      nfe.printStackTrace();
      getFeedbackManager().setStatusBarError("Failed to open " + vsaFile);
      JOptionPane.showMessageDialog(
        this,
        "Error Reading File, Data Incorrectly Formatted",
        "Bad File",
        JOptionPane.ERROR_MESSAGE);
    } catch (Throwable e) {
      e.printStackTrace();
      getFeedbackManager().setStatusBarError("Failed to open " + vsaFile);
      JOptionPane.showMessageDialog(
        this,
        "Error: Failed to read project due to error: " + e.getMessage(),
        "Bad File",
        JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Creates a dialog that gets the new project name and then creates the new
   * project by creating a new Operator Window.
   * @see NewAgentDialog
   * @see OperatorWindow
   */
  public void newProject() {
    // redo this a dialog should just pass back data to the main window for processing
    NewAgentDialog newAgentDialog = new NewAgentDialog(MainFrame.this);
    newAgentDialog.setVisible(true);
    if (newAgentDialog.wasApproved())
    {
      //Verify that the path exists
      String path = newAgentDialog.getNewAgentPath();
      File pathFile = new File(path);
      if (! pathFile.exists())
      {
        int choice = JOptionPane.showConfirmDialog(
          getMainFrame(),
          path + " does not exist.\nShould I create it for you?",
          path + " Does Not Exist",
          JOptionPane.OK_CANCEL_OPTION);

        if (choice == JOptionPane.CANCEL_OPTION)
        {
          return;
        }

        pathFile.mkdirs();
      }//if

      //Verify that the project doesn't already exist
      String agentName = newAgentDialog.getNewAgentName();
      String agentFileName = path + File.separator + agentName + ".vsa.json";
      File agentNameFile = new File(agentFileName);
      if (agentNameFile.exists())
      {
        JOptionPane.showMessageDialog(
          getMainFrame(),
          agentName + " already exists. Please try again with a different project name or path.",
          agentName + " already exists!",
          JOptionPane.ERROR_MESSAGE);

        return;
      }

      operatorWindow = new OperatorWindow(agentName, Paths.get(agentFileName), true);

      Prefs.openFolder.set(path);
      operatorDesktopSplit.setLeftComponent(new JScrollPane(operatorWindow));

      projectActionsEnable(true);
      commitAction.perform();

      //Set and monitor the divider position
      operDividerSetup();

      //Reset tracking whether any change has been made to this project
      CustomInternalFrame.resetEverchanged();

      //Set the title bar to include the project name
      setTitle(agentName);

      //Add this new agent to the recently opened project list
      Prefs.addRecentProject(agentNameFile, false);
    }
  }

  /**
	 * Toggles the project in/out of Read-Only Mode
	 */
	class ToggleReadOnlyAction extends AbstractAction
	{
		private static final long serialVersionUID = 20240117L;

		public ToggleReadOnlyAction()
		{
			super("Toggle Read-Only Action");
			toggleReadOnlyItem.setEnabled(false);  //kept off until a project is loaded
		}

		public void actionPerformed(ActionEvent e) {
			setReadOnly(!isReadOnly);
		}//actionPerformed()
	}

	/**
	 * This is where the user wants some info about the authors
     * @see AboutDialog
	 */
	class ContactUsAction extends AbstractAction
    {
		private static final long serialVersionUID = 20221225L;

		public ContactUsAction()
        {
			super("About VisualSoar");
		}

		public void actionPerformed(ActionEvent e)
        {
			aboutDialog.setVisible(true);
		}
  	}

  /**
     * Handles Soar Runtime|Connect menu option
     * @author ThreePenny
     */
	class SoarRuntimeInitAction extends AbstractAction
    {
		private static final long serialVersionUID = 20221225L;

		public SoarRuntimeInitAction()

        {
			super("Connect");
		}

		public void actionPerformed(ActionEvent e)

        {
			// Initialize the soar runtime
			boolean ok = false ;
			String message = "Error connecting to remote kernel" ;

			try
			{
				ok = SoarRuntimeInit() ;
			}
			catch (Throwable ex)
			{
				ex.printStackTrace();
				message = "Exception when initializing the SML library.  Check that sml.jar is on the path along with soar-library." ;
				Throwable cause = ex.getCause();
				if (cause != null) {
					String causeMsg = cause.toString();
					StringBuilder cmChopped = new StringBuilder("\n\n");
					while(causeMsg.length() > 40) {
						int spaceIndex = causeMsg.indexOf(" ", 30);
						if (spaceIndex == -1) break;
						cmChopped.append("\n").append(causeMsg, 0, spaceIndex);
						causeMsg = causeMsg.substring(spaceIndex);
					}
					cmChopped.append(causeMsg);
					cmChopped = new StringBuilder(cmChopped.toString().trim());
					message += cmChopped;
				}
			}

			if (!ok)
            {
				JOptionPane.showMessageDialog(MainFrame.this,
											  message,
                                              "SML Connection Error",
                                              JOptionPane.ERROR_MESSAGE);
			}
		}
	}

    /**
     * Initializes the Soar Tool Interface (STI) object and enabled/disables
     * menu items as needed.
     *
     * @author ThreePenny
     */
	boolean SoarRuntimeInit() {
		if (m_Kernel != null)
		{
			SoarRuntimeTerm() ;
			m_Kernel = null ;
		}

		// This may throw if we can't find the SML libraries.
		m_Kernel = Kernel.CreateRemoteConnection(true, null) ;

		if (m_Kernel.HadError())
		{
			m_Kernel = null ;

			// Disable all related menu items
			soarRuntimeTermAction.setEnabled(false);
			soarRuntimeInitAction.setEnabled(true);	// Allow us to try again later
			soarRuntimeSendRawCommandAction.setEnabled(false);
			soarRuntimeAgentMenu.setEnabled(false);
			soarRuntimeSendAllFilesAction.setEnabled(false) ;

			return false ;
		}

		if (m_Kernel.GetNumberAgents() > 0)
		{
			// Select the first agent if there is any as our current agent
			m_ActiveAgent = m_Kernel.GetAgentByIndex(0).GetAgentName() ;
		}
		soarRuntimeTermAction.setEnabled(true);
		soarRuntimeInitAction.setEnabled(false);
		soarRuntimeSendRawCommandAction.setEnabled(true);
		soarRuntimeAgentMenu.setEnabled(true);
		soarRuntimeSendAllFilesAction.setEnabled(true) ;

		m_Kernel.SetConnectionInfo("visual-soar", sml_Names.getKStatusReady(), sml_Names.getKStatusReady()) ;

		return true ;
	}


    /**
     * Terminates the Soar Tool Interface (STI) object and enabled/disables
     * menu items as needed.
     * @author ThreePenny
     */
	void SoarRuntimeTerm()
    {
		try
		{
			if (m_Kernel != null)
				m_Kernel.delete() ;
		}
		catch (Throwable ex)
		{
			// Trouble shutting down.
			ex.printStackTrace();
		}
		m_Kernel = null ;

		// Enable/Disable menu items
		soarRuntimeTermAction.setEnabled(false);
		soarRuntimeInitAction.setEnabled(true);
		soarRuntimeSendRawCommandAction.setEnabled(false);
		soarRuntimeAgentMenu.setEnabled(false);
		soarRuntimeSendAllFilesAction.setEnabled(false) ;
	}

	/**
	 * reportResult
	 *
	 * Used to report a multi-line result of a command to the user
	 * in the feedback window.  This was originally created to display
	 * the output from commands sent to the SoarJavaDebugger but
	 * can be used for any similar output.
	 *
	 * @param result  Text to display in feedback window
	 */
	public void reportResult(String result)
	{
		String[] lines = result.split("\n") ;

		Vector<FeedbackListEntry> v = new Vector<>();

		for (String line : lines) {
			v.add(new FeedbackListEntry(line));
		}

    getFeedbackManager().showFeedback(v);
	}



	/**
     * Handles Soar Runtime|Disconnect menu option
     * @author ThreePenny
     */
	class SoarRuntimeTermAction extends AbstractAction
    {
		private static final long serialVersionUID = 20221225L;

		public SoarRuntimeTermAction()
        {
			// Set the name and default to being disabled
			super("Disconnect");
			setEnabled(false);
		}

		public void actionPerformed(ActionEvent e)
        {
			// Terminate the soar runtime
			SoarRuntimeTerm();
		}
	}//class SoarRuntimeTermAction

	// Handles the "Runtime|Send All Files" menu item
    class SendAllFilesToSoarAction extends AbstractAction
    {
		private static final long serialVersionUID = 20221225L;

		public SendAllFilesToSoarAction()
        {
            super("Send All Files");
			setEnabled(false);
        }

        public void actionPerformed(ActionEvent e)
        {
            // Generate the path to the top level source file
            OperatorRootNode root = (OperatorRootNode)(operatorWindow.getModel().getRoot());

            if (root == null)
            {
              MainFrame.getMainFrame().reportResult("VisualSoar error: Couldn't find the top-level project node");
            	return ;
            }

            String projectFilename = root.getProjectFile() ;	// Includes .vsa

            // Swap the extension from .vsa(.json) to .soar
            projectFilename = projectFilename.replaceFirst("\\.vsa(\\.json)?", ".soar") ;

            SoarUtils.sourceFile(projectFilename, MainFrame.this);
        }
    }//class SendAllFilesToSoarAction


    /**
     * Handles Soar Runtime|Send Raw Command menu option
     * @author ThreePenny
     */
	class SoarRuntimeSendRawCommandAction extends AbstractAction
    {
		private static final long serialVersionUID = 20221225L;

		public SoarRuntimeSendRawCommandAction()

        {
			// Set the name and default to being disabled
			super("Send Raw Command");
			setEnabled(false);
		}

		public void actionPerformed(ActionEvent e)

        {
			if (m_Kernel != null)
			{
				Agent agent = getActiveAgent() ;

				if (agent == null)
				{
					JOptionPane.showMessageDialog(MainFrame.getMainFrame(), "No agent is currently selected, so we can't send the command.\n\nPlease use the Connected Agent menu to select one.") ;
					return ;
				}

				SoarRuntimeSendRawCommandDialog theDialog = new SoarRuntimeSendRawCommandDialog(MainFrame.this, m_Kernel, getActiveAgent());
				theDialog.setVisible(true);
			}
		}
	}


  class FindInProjectAction extends AbstractAction
    {
		private static final long serialVersionUID = 20221225L;

		public FindInProjectAction()
        {
  			super("Find in Project");
			setEnabled(true);  //see the comment in projectActionsEnable()
  		}

        /**
         * Prompts the user for a string to find in the project
         * finds all instances of the string in the project
         * and displays a FindInProjectList in the DesktopPane with all instances
         * or tells the user that no instances were found
         * @see FindDialog
         */
		public void actionPerformed(ActionEvent e)
        {
			//If the user invokes this action when no project is open just ignore it
			//For more info on why this is necessary, see the comment in projectActionsEnable()
			if (!projectIsOpen()) return;

			FindInProjectDialog theDialog =
                new FindInProjectDialog(MainFrame.this,
                                        operatorWindow,
                                        (OperatorNode)operatorWindow.getModel().getRoot());
			theDialog.setVisible(true);
		}
	}//class FindInProjectAction


    class ReplaceInProjectAction extends AbstractAction
    {
		private static final long serialVersionUID = 20221225L;

		public ReplaceInProjectAction()
        {
            super("Replace in Project");
            setEnabled(true);    //see the comment in projectActionsEnable()
        }

        /**
         * Prompts the user for a string to find in the project
         * and a string to replace the found string with.
         * Goes through all found instances and opens rules editors
         * for all files with matching strings.
         * @see ReplaceInProjectDialog
         */
        public void actionPerformed(ActionEvent e)
        {
			//If the user invokes this action when no project is open just ignore it
			//For more info on why this is necessary, see the comment in projectActionsEnable()
			if (!projectIsOpen()) return;

			//If the project is in Read-Only mode reject the action
			if (isReadOnly) {
				rejectForReadOnly();
				return;
			}

			ReplaceInProjectDialog replaceDialog =
                new ReplaceInProjectDialog(MainFrame.this,
                                           operatorWindow,
                                           (OperatorNode)operatorWindow.getModel().getRoot());
            replaceDialog.setVisible(true);
        }
    }//class ReplaceInProjectAction

	/** action to find all productions in the project */
	class FindAllProdsAction extends AbstractAction
	{
		private static final long serialVersionUID = 20230330L;

		public FindAllProdsAction()
		{
			super("Find All Productions");
			setEnabled(true);    //see the comment in projectActionsEnable()
		}

		public void actionPerformed(ActionEvent e)
		{
			//If the user invokes this action when no project is open just ignore it
			//For more info on why this is necessary, see the comment in projectActionsEnable()
			if (!projectIsOpen()) return;

			//Get all files
			Enumeration<TreeNode> bfe = operatorWindow.getProjectModel().breadthFirstEnumeration();
			Vector<OperatorNode> vecNodes = new Vector<>(10, 50);
			while(bfe.hasMoreElements())
			{
				vecNodes.add((OperatorNode)bfe.nextElement());
			}

			//Extract production names/locations from each file
			Vector<FeedbackListEntry> vecFeedback = new Vector<>();
			for(OperatorNode opNode : vecNodes) {
				Vector<String> prodNames = opNode.getProdNames();
				for(String prodName : prodNames) {
					int lineNo = opNode.getLineNumForString(prodName);
					FeedbackListEntry flobj = new FeedbackEntryOpNode(opNode, lineNo, prodName);
					vecFeedback.add(flobj);
				}
			}

			//Share the final list with the user
      getFeedbackManager().showFeedback(vecFeedback);
		}//actionPerformed
	}//class FindAllProdsAction

  class SaveProjectAsAction extends AbstractAction
    {
		private static final long serialVersionUID = 20221225L;

		public SaveProjectAsAction()
        {
			super("Save Project As");
			setEnabled(false);
		}

		/** I'm flabbergasted that a method like this
		 * isn't available in the base Java distribution.
		 * @param orig copy the contents of this file...
		 * @param dest ...into this file
		 */
		public void copyFile(File orig, File dest) throws IOException {
			try {
				FileInputStream fis = new FileInputStream(orig);
				FileOutputStream fos = new FileOutputStream(dest);
				Scanner scan = new Scanner(fis);
				PrintWriter pw = new PrintWriter(fos);
				while(scan.hasNextLine()) {
					String line = scan.nextLine();
					pw.println(line);
				}
				scan.close();
				pw.close();
			}
			catch(IOException ioe) {
				String message = "Save As Failed!\n";
				message += "Exception copying file " + orig + "\n to " + dest;
				JOptionPane.showMessageDialog(MainFrame.getMainFrame(),message,"Save As Failed",JOptionPane.ERROR_MESSAGE);
				ioe.printStackTrace();
				throw ioe;
			}
		}//copyFile

		/**
		 * copyAllFiles
		 *
		 * copies all files associated with the children of a given
		 * OperatorNode.  This method recurses into all descendants (not just
		 * direct children).
		 *
		 * Note:  I suspect there is a better way to recurse the file
		 *        structure, but I'm not seeing it.  (Nuxoll - 20 Aug '22)
		 *
		 * @param oldFolder  folder to copy files from
		 * @param newFolder  folder associated with the given node
		 * @param node the given node
		 */
		public void copyAllFiles(String oldFolder,
								 String newFolder,
								 OperatorNode node) {

			int childCount = node.getChildCount();
			for(int i = 0; i < childCount; ++i)
			{
				OperatorNode child = (OperatorNode)node.getChildAt(i);

				//If the child is a file copy it
				String newNodeName = child.getFileName();
				if (newNodeName != null) {
					String oldNodeName = newNodeName.replace(newFolder, oldFolder);
					File orig = new File(oldNodeName);
					File dest = new File(newNodeName);
					if (orig.exists() && orig.isFile()) {
						try {
							copyFile(orig, dest);
						} catch(IOException ioe) {
							//no need to report anything as copyFile() already has
							return;
						}
					}
				}

				//if child is a sub-folder recurse into it
				//note:  child could be both file and sub-folder!
				String recurseNewFolder = child.getFolderName();
				if (! recurseNewFolder.equals(newFolder)) {
					String recurseOldFolder = recurseNewFolder.replace(newFolder, oldFolder);
					copyAllFiles(recurseOldFolder, recurseNewFolder, child);
				}
			}//for
		}//copyAllFiles


		public void actionPerformed(ActionEvent e)
        {
      try (FeedbackManager.AtomicContext ignored = getFeedbackManager().beginAtomicContext()) {
        SaveProjectAsDialog spad = new SaveProjectAsDialog(MainFrame.getMainFrame());
        spad.setVisible(true);

        OperatorRootNode root = (OperatorRootNode) (operatorWindow.getModel().getRoot());
        File oldProjectFile = new File(root.getProjectFile());
        String oldProjPath = root.getFolderName();

        if (spad.wasApproved()) {
          String newName = spad.getNewAgentName();
          String newRootPath = spad.getNewAgentPath();
          String newProjPath = newRootPath + File.separator + newName;
          if (ProjectModel.isProjectNameValid(newName)) {
            try {
            operatorWindow.saveProjectAs(newName, newRootPath);
            } catch (IOException exception) {
              JOptionPane.showMessageDialog(
                MainFrame.this,
                exception.getMessage(),
                "Project Save Error",
                JOptionPane.ERROR_MESSAGE);
              return;
            }

            // Regenerate the *_source.soar files in the old project
            try {
              OperatorWindow oldOpWin = new OperatorWindow(oldProjectFile, false);
              OperatorRootNode oldOrn = (OperatorRootNode) oldOpWin.getModel().getRoot();
              oldOrn.startSourcing();
            } catch (IOException exception) {
              JOptionPane.showMessageDialog(
                  MainFrame.this,
                  exception.getMessage(),
                  "Agent Export Error",
                  JOptionPane.ERROR_MESSAGE);
              return;
            }

            copyAllFiles(oldProjPath, newProjPath, root);

            JInternalFrame[] jif = desktopPane.getAllFrames();
            for (JInternalFrame jInternalFrame : jif) {
              if (jInternalFrame instanceof RuleEditor) {
                RuleEditor oldRuleEditor = (RuleEditor) jInternalFrame;
                OperatorNode newNode = oldRuleEditor.getNode();
                oldRuleEditor.fileRenamed(
                    newNode
                        .getFileName()); // Update the Rule editor with the correct updated file
                                         // name
              }
            }
            saveAllFilesAction.perform(); // Save all open Rule Editors to the new project directory
            exportAgentAction.perform();
            saveDataMapAndProjectAction.perform(); // Save DataMap and Project file (.vsa)

            // Set the title bar to include the project name
            setTitle(newName);

          } else {
            JOptionPane.showMessageDialog(
                MainFrame.this,
                "That is not a valid name for the project",
                "Invalid Name",
                JOptionPane.ERROR_MESSAGE);
          }
        }
      }
		}
	}//class SaveProjectAsAction


  class TileWindowsAction extends AbstractAction
    {
		private static final long serialVersionUID = 20221225L;

		public TileWindowsAction()
        {
            super("Tile Windows");
        }

        public void actionPerformed(ActionEvent e)
        {
            desktopPane.performTileAction();
            lastWindowViewOperation = "tile";
        }
    }

    class ReTileWindowsAction extends AbstractAction
    {
		private static final long serialVersionUID = 20221225L;

		public ReTileWindowsAction()
        {
            super("Re-Tile Windows");
        }

        public void actionPerformed(ActionEvent e)
        {
            desktopPane.performReTileAction();
            lastWindowViewOperation = "tile";
        }
    }//class TileWindowsAction

    class CascadeAction extends AbstractAction
    {
		private static final long serialVersionUID = 20221225L;

		public CascadeAction()
        {
            super("Cascade Windows");
        }

        public void actionPerformed(ActionEvent e)
        {
            desktopPane.performCascadeAction();
            lastWindowViewOperation = "cascade";
        }
    }//class CascadeAction

}//class MainFrame

