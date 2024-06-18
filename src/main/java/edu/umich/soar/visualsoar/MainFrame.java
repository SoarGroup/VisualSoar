package edu.umich.soar.visualsoar;

import edu.umich.soar.visualsoar.datamap.CheckBoxDataMap;
import edu.umich.soar.visualsoar.datamap.DataMap;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryModel;
import edu.umich.soar.visualsoar.datamap.SoarWorkingMemoryReader;
import edu.umich.soar.visualsoar.dialogs.*;
import edu.umich.soar.visualsoar.graph.NamedEdge;
import edu.umich.soar.visualsoar.misc.*;
import edu.umich.soar.visualsoar.operatorwindow.*;
import edu.umich.soar.visualsoar.parser.ParseException;
import edu.umich.soar.visualsoar.parser.SoarProduction;
import edu.umich.soar.visualsoar.parser.SuppParseChecks;
import edu.umich.soar.visualsoar.parser.TokenMgrError;
import edu.umich.soar.visualsoar.ruleeditor.RuleEditor;
import edu.umich.soar.visualsoar.threepenny.SoarRuntimeSendRawCommandDialog;
import edu.umich.soar.visualsoar.util.ActionButtonAssociation;
import edu.umich.soar.visualsoar.util.MenuAdapter;
import sml.Agent;
import sml.Kernel;
import sml.smlStringEventId;
import sml.sml_Names;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.io.*;
import java.util.List;
import java.util.*;

// 3P

// The global application class

/** 
 * This is the main project window of VisualSoar
 * @author Brad Jones
 */
public class MainFrame extends JFrame implements Kernel.StringEventInterface
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

////////////////////////////////////////
// Data Members
////////////////////////////////////////
	private OperatorWindow operatorWindow;

	private final CustomDesktopPane desktopPane = new CustomDesktopPane();
	private final TemplateManager d_templateManager = new TemplateManager();
	private final JSplitPane operatorDesktopSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	private final JSplitPane feedbackDesktopSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	public FeedbackList feedbackList = new FeedbackList();
    public JLabel statusBar = new JLabel("  Welcome to Visual Soar.");
	String lastWindowViewOperation = "none"; // can also be "tile" or "cascade"

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
	Action newProjectAction = new NewProjectAction();
	Action openProjectAction = new OpenProjectAction();
    Action openFileAction = new OpenFileAction();
	PerformableAction closeProjectAction = new CloseProjectAction();
	PerformableAction saveAllFilesAction = new SaveAllFilesAction();
	PerformableAction exportAgentAction = new ExportAgentAction();
	PerformableAction saveDataMapAndProjectAction = new SaveDataMapAndProjectAction();
	Action preferencesAction = new PreferencesAction();

	public static final String TOGGLE_RO_ON = "Turn On Read-Only...";
	public static final String TOGGLE_RO_OFF = "Turn Off Read-Only...";
	public static final String RO_LABEL = "Read-Only: ";

	//Note:  these menu items must be an instance variable because they are changed in read-only mode
	JMenuItem toggleReadOnlyItem = new JMenuItem(TOGGLE_RO_ON);
	JMenuItem saveItem = new JMenuItem("Save");
	JMenu openRecentMenu = new JMenu("Open Recent");

	Action toggleReadOnlyAction = new ToggleReadOnlyAction();
	PerformableAction commitAction = new CommitAction();
	Action exitAction = new ExitAction();
	Action closeAllWindowsAction = new CloseAllWindowsAction();
    Action cascadeAction = new CascadeAction();
    Action tileWindowsAction = new TileWindowsAction();
    Action reTileWindowsAction = new ReTileWindowsAction();
	PerformableAction verifyProjectAction = new VerifyProjectAction();
	Action checkSyntaxErrorsAction = new CheckSyntaxErrorsAction();
	Action loadTopStateDatamapAction = new LoadTopStateDatamapAction();
	Action linkDataMapAction = new LinkDataMapAction();
	PerformableAction checkAllProductionsAction = new CheckAllProductionsAction();
    Action searchDataMapCreateAction = new SearchDataMapCreateAction();
    Action searchDataMapTestAction = new SearchDataMapTestAction();
    Action searchDataMapCreateNoTestAction = new SearchDataMapCreateNoTestAction();
    Action searchDataMapTestNoCreateAction = new SearchDataMapTestNoCreateAction();
    Action searchDataMapNoTestNoCreateAction = new SearchDataMapNoTestNoCreateAction();

    Action generateDataMapAction = new GenerateDataMapAction();
	Action saveProjectAsAction = new SaveProjectAsAction();
	Action contactUsAction = new ContactUsAction();
    Action viewKeyBindingsAction = new ViewKeyBindingsAction();
	Action findInProjectAction = new FindInProjectAction();
    Action replaceInProjectAction = new ReplaceInProjectAction();
	Action findAllProdsAction = new FindAllProdsAction();

	// 3P
    Kernel m_Kernel = null ;
    String m_ActiveAgent = null ;
    long    m_EditProductionCallback = -1 ;
	// Menu handlers for STI init, term, and "Send Raw Command"
	Action soarRuntimeInitAction = new SoarRuntimeInitAction();
	Action soarRuntimeTermAction = new SoarRuntimeTermAction();
	Action soarRuntimeSendRawCommandAction = new SoarRuntimeSendRawCommandAction();
	Action soarRuntimeSendAllFilesAction = new SendAllFilesToSoarAction() ;

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

		JScrollPane sp = new JScrollPane(feedbackList);
		sp.setBorder(new TitledBorder("Feedback"));

		//Create the main desktop
		feedbackDesktopSplit.setTopComponent(operatorDesktopSplit);
        feedbackDesktopSplit.setBottomComponent(sp);
        feedbackDesktopSplit.setOneTouchExpandable(true);
		feedbackDividerSetup();

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
                    checkForUnsavedProjectOnClose();
                    
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

	}//MainFrame ctor

////////////////////////////////////////
// Methods
////////////////////////////////////////

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
    
    
	/**
     * Method updates the FeedBack list window.  If your message is a single string
	 * consider using the status bar to display your message instead.
	 *
     * @param v the vector list of feedback data
     */
	public void setFeedbackListData(Vector<FeedbackListEntry> v) {
		feedbackList.setListData(v);
	}

	/**
	 * Method updates the status bar text with a message
	 */
	public void setStatusBarMsg(String text) {
		//Extra spaces make text align better with feedback window above it
		statusBar.setForeground(Color.black);
		statusBar.setText("  " + text);
	}

	/**
	 * Method updates the status bar text with list of strings.  These
	 * are displayed on a single line.
	 */
	public void setStatusBarMsgList(List<String> msgs) {
		if (msgs.isEmpty()) return; //nop
		StringBuilder sb = new StringBuilder();
		for (String match : msgs) {
			sb.append("   ");
			sb.append(match);
		}
		statusBar.setText(sb.toString());
	}

	/**
	 * Method updates the status bar text with a message that indicates a user error
	 */
	public void setStatusBarError(String text)
	{
		//Extra spaces make text align better with feedback window above it
		statusBar.setForeground(Color.red);
		statusBar.setText("  " + text);
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

	/** VS periodically creates auto backups of open files.  This method can be called to delete all of them. */
	public void deleteAutoBackupFiles() {

		//Check for any auto-backups of rule files
		CustomInternalFrame[] frames = desktopPane.getAllCustomFrames();
		for (CustomInternalFrame frame : frames) {
			if (frame instanceof RuleEditor) {
				RuleEditor re = (RuleEditor) frame;
				String tempFN = re.getFile() + "~";
				File f = new File(tempFN);
				if (f.exists()) f.delete();
			}
		}

		//Check for auto-backups of the project files
		if (operatorWindow != null) {
			Object root = operatorWindow.getModel().getRoot();
			if (root instanceof OperatorRootNode) {
				OperatorRootNode orn = (OperatorRootNode) root;
				File projectBackupFile = new File(orn.getProjectFile() + "~");
				if (projectBackupFile.exists()) projectBackupFile.delete();
				File dataMapBackupFile = new File(orn.getDataMapFile() + "~");
				if (dataMapBackupFile.exists()) dataMapBackupFile.delete();
				File commentBackupFile = new File(dataMapBackupFile.getParent() + File.separator + "comment.dm~");
				if (commentBackupFile.exists()) commentBackupFile.delete();
			}
		}
	}

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
        saveItem.setAccelerator(KeyStroke.getKeyStroke("control S"));
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
		newProjectItem.setAccelerator(KeyStroke.getKeyStroke("control N"));
		newProjectItem.setMnemonic(KeyEvent.VK_N);
		openProjectItem.setAccelerator(KeyStroke.getKeyStroke("control O"));
		openProjectItem.setMnemonic(KeyEvent.VK_O);
        openFileItem.setAccelerator(KeyStroke.getKeyStroke("control F"));
        openFileItem.setMnemonic(KeyEvent.VK_F);

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
				recentItem.addActionListener(new TryOpenProjectAction(projEntry));
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
		findInProjectItem.setAccelerator(KeyStroke.getKeyStroke("control shift F"));
		replaceInProjectItem.setMnemonic(KeyEvent.VK_R);
		replaceInProjectItem.setAccelerator(KeyStroke.getKeyStroke("control shift R"));
		findAllProdsItem.setMnemonic(KeyEvent.VK_D);
		findAllProdsItem.setAccelerator(KeyStroke.getKeyStroke("control shift D"));

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

		JMenuItem linkDataMapItem = new JMenuItem("Link Entries from Another Project's  Datamap");
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
		//Close All Windows
		JMenuItem closeAllWindowsItem = new JMenuItem("Close All Windows");
		closeAllWindowsItem.addActionListener(closeAllWindowsAction);
		closeAllWindowsItem.addPropertyChangeListener(
				new ActionButtonAssociation(closeAllWindowsAction,closeAllWindowsItem));
		viewMenu.add(closeAllWindowsItem);


		// View Menu
		JMenuItem cascadeItem = new JMenuItem("Cascade Windows");
		cascadeItem.addActionListener(cascadeAction);
		cascadeItem.addPropertyChangeListener(
            new ActionButtonAssociation(cascadeAction,cascadeItem));
		viewMenu.add(cascadeItem);
		
		JMenuItem tileWindowItem = new JMenuItem("Tile Windows Horizontally");
		tileWindowItem.addActionListener(tileWindowsAction);
		tileWindowItem.addPropertyChangeListener(
            new ActionButtonAssociation(tileWindowsAction,tileWindowItem));
		viewMenu.add(tileWindowItem);
		
		JMenuItem reTileWindowItem = new JMenuItem("Tile Windows Vertically");
		reTileWindowItem.addActionListener(reTileWindowsAction);
		reTileWindowItem.addPropertyChangeListener(
            new ActionButtonAssociation(reTileWindowsAction,reTileWindowItem));
		viewMenu.add(reTileWindowItem);
				
		viewMenu.addMenuListener(
            new MenuListener() 
            {
                public void menuCanceled(MenuEvent e) {} 
                public void menuDeselected(MenuEvent e) 
                    {
                        for(int i = viewMenu.getMenuComponentCount() - 1; i > 2; --i) 
                        {
                            viewMenu.remove(i);
                        }
                    }
          	 	
                public void menuSelected(MenuEvent e) 
                    {
                        JInternalFrame[] frames = desktopPane.getAllFrames();
						for (JInternalFrame frame : frames) {
							JMenuItem menuItem = new JMenuItem(frame.getTitle());
							menuItem.addActionListener(new WindowMenuListener(frame));
							viewMenu.add(menuItem);
						}
                    }   	 		
            });		
		viewMenu.setMnemonic('V');
		//cascadeWindowItem.setMnemonic(KeyEvent.VK_C);
		tileWindowItem.setMnemonic(KeyEvent.VK_T);
		tileWindowItem.setAccelerator(KeyStroke.getKeyStroke("control T"));
		reTileWindowItem.setAccelerator(KeyStroke.getKeyStroke("control shift T"));
		return viewMenu;
	}

    /**
     * This function is called when the project is being closed (i.e.,
     * due to window close, opening new project or selecting "Close Project"
     * from the File menu).  It detects unsaved changes and asks the user
     * what to do about it.
     */
    void checkForUnsavedProjectOnClose()
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
                return;
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
        }//if
                    
    }//checkForUnsavedProjectOnClose()


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
		else if ( (Prefs.autoTileEnabled.getBoolean())
                  || (lastWindowViewOperation.equals("tile")) )
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
                try 
                {
                    if (jif[i].isIcon())
                    {
                        jif[i].setIcon(false);
                    }
                    jif[i].setSelected(true);
                    jif[i].moveToFront();
                    break;
                }
                catch (java.beans.PropertyVetoException pve)
                {
                    //Don't break;
                }				
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
			internalFrame.toFront();
			try 
            {
				internalFrame.setIcon(false);
				internalFrame.setSelected(true);
				internalFrame.moveToFront();
			} catch (java.beans.PropertyVetoException pve) { 
				System.err.println("Guess we can't do that"); }
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
	 * sets the divider position between the feedback pane and the desktop
	 * based upon the last user setting.
	 */
	private static boolean feedbackListenerSetup = false;  //avoid re-creating listener
	private void feedbackDividerSetup() {
		double position = Double.parseDouble(Prefs.fbDividerPosition.get());
		if ((position < MIN_DIV_POS) || (position > MAX_DIV_POS)) {
			position = DEFAULT_FB_DIV_POS;
			Prefs.fbDividerPosition.set("" + DEFAULT_FB_DIV_POS);
		}
		feedbackDesktopSplit.setDividerLocation( ((int) (getSize().getHeight() * position)) );

		//Whenever the user moves the divider, remember the user's preference
		if (!feedbackListenerSetup) {
			feedbackDesktopSplit.addPropertyChangeListener(
					JSplitPane.DIVIDER_LOCATION_PROPERTY,
					new PropertyChangeListener() {
						@Override
						public void propertyChange(PropertyChangeEvent evt) {
							//Retrieve the value as a double
							String valStr = evt.getNewValue().toString();
							int val = Integer.parseInt(valStr);

							//Convert it to a fraction of split pane's size
							int paneHeight = feedbackDesktopSplit.getHeight();
							double proportion = (double) val / (double) paneHeight;

							//Save the new value to Prefs (if sane)
							if ((proportion >= MIN_DIV_POS) && (proportion <= MAX_DIV_POS)) {
								Prefs.fbDividerPosition.set("" + proportion);
								Prefs.flush();
							}

							//Re-tile the windows as a result
							scheduleDelayedReTile();
						}
					}
			);
			feedbackListenerSetup = true;
		}//listener setup
	}//dividerSetup


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
		setStatusBarMsg("You must turn off Read-Only mode to edit this project.");
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
	 * ghettoFileChooser
	 * <p>
	 * Uses a FileDialog to select a file with a .vsa extension
	 * It's called "ghetto" because the original author of this code wrote
	 * this about it:
	 * 	  FIXME: This is totally ghetto
	 * 	    Using a JFileChooser seems to cause hangs on OS X (10.4, at least)
	 * 	    so I've converted the code to use a FileDialog instead
	 * 	    Unfortunately, FilenameFilters don't work on Windows XP, so I have
	 * 	    to set the file to *.vsa.  Yuck.
	 * <p>
	 * It's quite possible JFileChooser no longer crashes OS/X but why fix
	 * something that seems to be working fine?    -- Nuxoll, Jan 2024
	 *
	 * @return the file selected (or null if none)
	 */
	private File ghettoFileChooser() {

		FileDialog fileChooser = new FileDialog(MainFrame.this, "Open Project", FileDialog.LOAD);
		File dir = new File(Prefs.openFolder.get());
		if ((dir.exists()) && (dir.canRead())) {
			fileChooser.setDirectory(dir.getAbsolutePath());
		}
		fileChooser.setFilenameFilter(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith("vsa");
			}
		});
		fileChooser.setFile("*.vsa");
		fileChooser.setVisible(true);
		if (fileChooser.getFile() == null) return null;
		return new File(fileChooser.getDirectory(), fileChooser.getFile());
	}//ghettoFileChooser

	/**
	 * @return a File object representing where the project's .cfg file should be
	 */
	private File getCfgFile() {
		Object root = operatorWindow.getModel().getRoot();
		if (root instanceof OperatorRootNode) {
			OperatorRootNode orn = (OperatorRootNode)root;
			String cfgFN = orn.getFolderName() + File.separator + orn.getName() + ".cfg";
			return new File(cfgFN);
		}

		return null;
	}//getCfgFile

	/**
	 * readCfgFile
	 *
	 * is called when the project is loaded to read information about what files are open last time and restore them
	 */
	private void readCfgFile() {
		//Read file contents
		File cfgFile = getCfgFile();
		if ( (cfgFile == null) || (!cfgFile.exists()) ) return; //nothing to do
		Scanner scan;
		ArrayList<String> cfgLines = new ArrayList<>();
		try {
			 scan = new Scanner(cfgFile);
			 while(scan.hasNextLine()) cfgLines.add(scan.nextLine());
			 scan.close();
		}
		catch(FileNotFoundException fnfe) {
			this.setStatusBarMsg("Unable to read previous configuration from " + cfgFile.getName());
			return;
		}

		//Process each line
		for(String line : cfgLines) {
			String[] tokens = line.split(" ");
			if (tokens.length != 2) continue; //invalid line

			//Calculate the canonical path of the filename for equality comparison
			String target;
			try {
				target = (new File(tokens[1])).getCanonicalPath();
			}
			catch(IOException ioe) {
				continue; //fail quietly; should never happen
			}

			//Find the associated node
			Enumeration<TreeNode> bfe = operatorWindow.breadthFirstEnumeration();
			while(bfe.hasMoreElements()) {
				OperatorNode node = (OperatorNode) bfe.nextElement();
				if (node.getFileName() == null) continue; //skip the fluff

				//calculate the canonical path so equality comparison will work as intended
				String nodeFN;
				try {
					nodeFN = (new File(node.getFileName())).getCanonicalPath();
				}
				catch(IOException ioe) {
					continue; //fail quietly; should never happen
				}


				if (nodeFN.equals(target)) {
					if (tokens[0].equals("RULEEDITOR")) {
						node.openRules(this);
					}
					else if (tokens[0].equals("DATAMAP")) {
						node.openDataMap(operatorWindow.getDatamap(), this);
					}
				}
			}//for each operator node
		}//for each cfg file line



	}//readCfgFile

	/**
	 * writeCfgFile
	 *
	 * saves information about what files are open in the project so that they can be restored
	 * when the file is loaded.
	 *
	 * Each line in the config file is one of the following:
	 * RULEEDITOR &lt;filename&gt;
	 * DATAMAP &lt;filename&gt;
	 */
	private void writeCfgFile() {
		//Sanity check:  is a project currently open?
		if (operatorWindow == null) return;

		//Create the .cfg file contents
		ArrayList<String> cfgLines = new ArrayList<>();
		JInternalFrame[] jif = desktopPane.getAllFrames();
		for (JInternalFrame jInternalFrame : jif) {
			if (jInternalFrame instanceof RuleEditor) {
				RuleEditor re = (RuleEditor) jInternalFrame;
				String line = "RULEEDITOR " + re.getFile();
				cfgLines.add(line);
			}
			else if (jInternalFrame instanceof DataMap) {
				DataMap dm = (DataMap) jInternalFrame;
				int dmId = dm.getId();

				//Find the filename of the SoarOperatorNode associated with this id
				Enumeration<TreeNode> bfe = operatorWindow.breadthFirstEnumeration();
				while(bfe.hasMoreElements()) {
					OperatorNode node = (OperatorNode) bfe.nextElement();
					if (node instanceof SoarOperatorNode) {
						SoarOperatorNode son = (SoarOperatorNode) node;
						if (son.getDataMapIdNumber() == dmId) {
							String line = "DATAMAP " + son.getFileName();
							cfgLines.add(line);
							break;
						}
					}
				}//for each operator node
			}//if datamap
		}//for each internal frame

		//Write the .cfg file contents
		File cfgFile = getCfgFile();
		if ((cfgFile != null) && (cfgFile.exists())) {
			try {
				PrintWriter pw = new PrintWriter(cfgFile);
				for (String line : cfgLines) {
					pw.println(line);
				}
				pw.close();
			} catch (FileNotFoundException fnfe) {
				//the .cfg file is not essential so just report it if it can't be written
				this.setStatusBarMsg("Unable to save current configuration to " + cfgFile.getName());
			}
		}
	}//writeCfgFile


/**
* Runs through all the Rule Editors in the Desktop Pane and tells them to save
* themselves.
*/
	class SaveAllFilesAction extends PerformableAction {
		private static final long serialVersionUID = 20221225L;

		public SaveAllFilesAction()
        {
			super("Save All");
			setEnabled(false);
		}
		
		public void perform() 
        {
			//Save the list of currently open windows
			writeCfgFile();

			try
            {
				JInternalFrame[] jif = desktopPane.getAllFrames();
				for (JInternalFrame jInternalFrame : jif) {
					if (jInternalFrame instanceof RuleEditor) {
						RuleEditor re = (RuleEditor) jInternalFrame;
						re.write();
					}
				}
			}
			catch(java.io.IOException ioe) 
            {
				JOptionPane.showMessageDialog(MainFrame.this,
                                              ioe.getMessage(),
                                              "I/O Error",
                                              JOptionPane.ERROR_MESSAGE);
				return;
			}

			//Since save was successful, discard auto-backup files
			deleteAutoBackupFiles();

		}//perform SaveAllFilesAction

		public void actionPerformed(ActionEvent event) 
        {
			perform();
		}
		
	}

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
			JInternalFrame[] frames = desktopPane.getAllFrames();
			Prefs.flush();
			try 
            {
				savePositionAndSize();

				if (CustomInternalFrame.hasEverChanged()) {
					commitAction.perform();
				}
				else {
					writeCfgFile();
					deleteAutoBackupFiles();
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

	/**
     * Attempts to save the datamap
     * @see OperatorWindow#saveHierarchy()
     */
	class SaveDataMapAndProjectAction extends PerformableAction {
		private static final long serialVersionUID = 20221225L;

		public SaveDataMapAndProjectAction()
        {
			super("Save DataMap And Project Action");
		}
		
		public void perform() 
        {
			if(operatorWindow != null) 
            {
				operatorWindow.saveHierarchy();
			}
		}
		
		public void actionPerformed(ActionEvent event) 
        {
			perform();	
			setStatusBarMsg("DataMap and Project Saved");
		}
	}

    /**
     * Attempts to open a new project by creating a new OperatorWindow
     * @param file .vsa project file that is to be opened
     * @see OperatorWindow
     */
	public void tryOpenProject (File file, boolean readOnly) throws IOException
    {
		operatorWindow = new OperatorWindow(file, readOnly);

		operatorDesktopSplit.setLeftComponent(operatorWindow);
		
		projectActionsEnable(true);

		//Set and monitor the divider position
		operDividerSetup();

		//Update the title to include the project name
		setTitle(file.getName().replaceAll(".vsa", ""));

		//Reopen windows that were open last time
		readCfgFile();

		//Configure read-only status
		setReadOnly(readOnly);

	}//tryOpenProject


	public class TryOpenProjectAction extends PerformableAction {
		private final Prefs.RecentProjInfo projInfo;

		public TryOpenProjectAction(Prefs.RecentProjInfo rpi) {
			super("Attempt to load project file: " + rpi);
			this.projInfo = rpi;
		}

		@Override
		public void perform() {
			//Get rid of the old project (if it exists)
			if (operatorWindow != null) {
				closeProjectAction.perform();
			}

			try {
				tryOpenProject(projInfo.file, projInfo.isReadOnly);
			} catch(IOException ioe) {
				JOptionPane.showMessageDialog(MainFrame.this, "Unable to open file: " + projInfo);
			}
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			perform();
		}
	}
	
	/**
	 * Open Project Action
     * a filechooser is created to determine project file
     * Opens a project by creating a new OperatorWindow
     * @see OperatorWindow
     * @see SoarFileFilter
	 */
	class OpenProjectAction extends AbstractAction 
    {
		private static final long serialVersionUID = 20221225L;

		public OpenProjectAction()
        {
			super("Open Project...");
		}	
		public void actionPerformed(ActionEvent event) 
        {
			try 
            {
				File file = ghettoFileChooser();
				if(file != null) {
					//Get rid of the old project (if it exists)
					if (operatorWindow != null) {
						closeProjectAction.perform();
					}

					//Open the new project
					boolean readOnly = event.getActionCommand().contains("Read-Only");
					operatorWindow = new OperatorWindow(file, readOnly);
					if(file.getParent() != null) {
						Prefs.openFolder.set(file.getParentFile().getAbsolutePath());
					}
					operatorDesktopSplit.setLeftComponent(new JScrollPane(operatorWindow));

					projectActionsEnable(true);

					//Set and monitor the divider position
					operDividerSetup();

					//Verify project integrity
					verifyProjectAction.perform();

					//Reset tracking whether any change has been made to this project
					CustomInternalFrame.resetEverchanged();

					//Set the title bar to include the project name
					setTitle(file.getName().replaceAll(".vsa", ""));

					//Reopen windows that were open last time
					readCfgFile();

					//Configure read-only status
					setReadOnly(readOnly);
				}
			}
			catch(FileNotFoundException fnfe)
            {
				JOptionPane.showMessageDialog(MainFrame.this,
                                              fnfe.getMessage(),
                                              "File Not Found",
                                              JOptionPane.ERROR_MESSAGE);
			}
			catch(IOException ioe) 
            {
				JOptionPane.showMessageDialog(MainFrame.this,
                                              ioe.getMessage(),
                                              "I/O Exception",
                                              JOptionPane.ERROR_MESSAGE);
				ioe.printStackTrace();
			}
			catch(NumberFormatException nfe) 
            {
                nfe.printStackTrace();
                JOptionPane.showMessageDialog(MainFrame.this,
                                              "Error Reading File, Data Incorrectly Formatted",
                                              "Bad File",
                                              JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	/**
     * Open a text file unrelated to the project in a rule editor
     * Opened file is not necessarily part of project and not soar formatted
     */
    class OpenFileAction extends AbstractAction 
    {
		private static final long serialVersionUID = 20221225L;

		public OpenFileAction()
        {
            super("Open File...");
        }
        public void actionPerformed(ActionEvent event) 
        {
            try 
            {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileFilter(new TextFileFilter());
				File dir = new File(Prefs.openFolder.get());
				if ((dir.exists()) && (dir.canRead())) {
					fileChooser.setCurrentDirectory(dir);
				}
                int state = fileChooser.showOpenDialog(MainFrame.this);
                File file = fileChooser.getSelectedFile();
                if(file != null && state == JFileChooser.APPROVE_OPTION) 
                {
                    OpenFile(file);
                }

            }

			catch(NumberFormatException nfe) 
            {
                nfe.printStackTrace();
                JOptionPane.showMessageDialog(MainFrame.this,
                                              "Error Reading File, Data Incorrectly Formatted",
                                              "Bad File",
                                              JOptionPane.ERROR_MESSAGE);
			}

        }
    }


	/**
	 * New Project Action
     * Creates a dialog that gets the new project name and then creates the new 
     * project by creating a new Operator Window.
     * @see NewAgentDialog
     * @see OperatorWindow
	 */
	class NewProjectAction extends AbstractAction 
    {
		private static final long serialVersionUID = 20221225L;

		public NewProjectAction()
        {
			super("New Project...");
		}
		
		public void actionPerformed(ActionEvent event) 
        {
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
				String agentFileName = path + File.separator + agentName + ".vsa";
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
                
				operatorWindow = new OperatorWindow(agentName,agentFileName,true);
				
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
	}
	
	/**
	 * Close Project Action
     * Closes all open windows in the desktop pane
	 */
	class CloseProjectAction extends PerformableAction 
    {
		private static final long serialVersionUID = 20221225L;

		public CloseProjectAction()
        {
			super("Close Project");
			setEnabled(false);
		}
		
		public void perform()
        {
            checkForUnsavedProjectOnClose();

			JInternalFrame[] frames = desktopPane.getAllFrames();
			try 
            {
				if ( (!isReadOnly()) && (CustomInternalFrame.hasEverChanged()) ) commitAction.perform();
				deleteAutoBackupFiles();

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
		}//perform()

        
        public void actionPerformed(ActionEvent event) 
        {
            perform();
        }

	}
	
	/**
	 * Export Agent
	 * Writes all the <operator>_source.soar files necesary for sourcing agent
	 * files written in  into the TSI
	 */
	class ExportAgentAction extends PerformableAction 
    {
		private static final long serialVersionUID = 20221225L;

		public ExportAgentAction()
        {
			super("Export Agent");
		}
		
		public void perform() 
        {
			DefaultTreeModel tree = (DefaultTreeModel)operatorWindow.getModel();
			OperatorRootNode root = (OperatorRootNode)tree.getRoot();
			try 
            {
				root.startSourcing();
			}
			catch (IOException exception) 
            {
				JOptionPane.showMessageDialog(MainFrame.this,
                                              exception.getMessage(),
                                              "Agent Export Error",
                                              JOptionPane.ERROR_MESSAGE);
			}
		}
		
		public void actionPerformed(ActionEvent event)
        {
			perform();
			setStatusBarMsg("Export Finished");
		}
	}

	/**
	 * Creates and shows the preferences dialog
	 */
	static class PreferencesAction extends AbstractAction
	{
		private static final long serialVersionUID = 20221225L;

		public PreferencesAction()
		{
			super("Preferences Action");
		}

		public void actionPerformed(ActionEvent e)
		{
			PreferencesDialog	theDialog = new PreferencesDialog(MainFrame.getMainFrame());
			theDialog.setVisible(true);
		}//actionPerformed()
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
	 * This is where the user wants a list of keybindings.  The action
     * loads the docs/KeyBindings.txt file.
	 */	
	class ViewKeyBindingsAction extends AbstractAction 
    {
		private static final long serialVersionUID = 20221225L;

		public ViewKeyBindingsAction()
        {
			super("VisualSoar Keybindings");
		}
		
		public void actionPerformed(ActionEvent e) 
        {
			JOptionPane.showMessageDialog(
                MainFrame.this,
                "<html>View VisualSoar key bindings help " +
                "<a href=\"https://soar.eecs.umich.edu/articles/articles/documentation/76-visual-soar-key-bindings\">" +
                "on the wiki</a>.<br />" +
                "https://soar.eecs.umich.edu/articles/articles/documentation/76-visual-soar-key-bindings</html>",
                "Key Bindings Help",
                JOptionPane.INFORMATION_MESSAGE);
		}
  	}//class ViewKeyBindingsAction

    /**
     * Edits a production (in the project) based on its name.
     * @author ThreePenny
     */
	protected void EditProductionByName(String sProductionName)
	
    {
		// TODO: Should we match case?
		
		OperatorWindow ow = getOperatorWindow();
		
		if (ow != null)
		{
			// Find the rule and open it
			getOperatorWindow().findInProjectAndOpenRule(sProductionName, false /* match case */);
		
			// Bring our window to the front
			toFront();
		} else {
			JOptionPane.showMessageDialog(
                    MainFrame.this,
                    "Edit-production event ignored because there is no project loaded.",
                    "Edit-production event ignored",
                    JOptionPane.ERROR_MESSAGE);

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

	public String stringEventHandler(int eventID, Object userData, Kernel kernel, String callbackData)
	{
		if (eventID == smlStringEventId.smlEVENT_EDIT_PRODUCTION.swigValue())
		{

			if (callbackData != null)
			{
				EditProductionByName(callbackData) ;
			}
		}
		return "" ;
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
		m_EditProductionCallback = m_Kernel.RegisterForStringEvent(smlStringEventId.smlEVENT_EDIT_PRODUCTION, this, null) ;
		
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

		m_EditProductionCallback = -1 ;
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

		setFeedbackListData(v);
	}//reportResult



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
            // Get the agent
        	Agent agent = MainFrame.getMainFrame().getActiveAgent() ;
            if (agent == null)
            {
                JOptionPane.showMessageDialog(MainFrame.this,"Not connected to an agent.","Error",JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Generate the path to the top level source file
            OperatorRootNode root = (OperatorRootNode)(operatorWindow.getModel().getRoot());
            
            if (root == null)
            {
            	System.out.println("Couldn't find the top level project node") ;
            	return ;
            }
            
            String projectFilename = root.getProjectFile() ;	// Includes .vsa
            
            // Swap the extension from .vsa to .soar
            projectFilename = projectFilename.replaceFirst(".vsa", ".soar") ;
            
            // Call source in Soar
            String result = agent.ExecuteCommandLine("source " + "\"" + projectFilename + "\"", true) ;
            
            if (!agent.GetLastCommandLineResult())
            	result = agent.GetLastErrorDescription() ;
            
			MainFrame.getMainFrame().reportResult(result) ;
        }
    }//class SendFileToSoarAction

	
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

	/**
     * This is a generic class for scanning a set of entities for errors in a
     * separate thread and providing a progress dialog while you do so.  You
     * must subclass this class to use it.
     */
    abstract class UpdateThread extends Thread
    {
        Runnable update, finish;
        int value, min, max;
		JProgressBar progressBar;
		JDialog progressDialog;
        Vector<OperatorNode> vecEntities;
        Vector<FeedbackListEntry> vecErrors = new Vector<>();
        int entityNum = 0;

        public UpdateThread(Vector<OperatorNode> v, String title)
        {
            vecEntities = v;
            max = v.size();
			progressBar = new JProgressBar(0, max);
			progressDialog = new JDialog(MainFrame.this, title);
			progressDialog.getContentPane().setLayout(new FlowLayout());
			progressDialog.getContentPane().add(progressBar);
			progressBar.setStringPainted(true);
			progressDialog.setLocationRelativeTo(MainFrame.this);
			progressDialog.pack();
			progressDialog.setVisible(true);
            progressBar.getMaximum();
            progressBar.getMinimum();
				
            update = new Runnable() 
            {
                public void run() 
                {
                    value = progressBar.getValue() + 1;
                    updateProgressBar(value);
                }
            };
            finish = new Runnable() 
            {
                public void run() 
                {
                    updateProgressBar(min);
                    progressDialog.dispose();
                }
            };
        }

        public void run() 
        {
            checkEntities();
        }

        private void updateProgressBar(int value) 
        {
            progressBar.setValue(value);
        }

        /**
         * Override this function in your subclass.  It scans the given entity
         * for errors and places them in the vecErrors vector.  vecErrors can
         * either contain Strings or FeedbackListEntry objects
         * @param o object to scan
         * @return true if any errors were found
         */
        abstract public boolean checkEntity(Object o) throws IOException;

        public void checkEntities()
        {
            try 
            {
                boolean anyErrors = false;
                for(int i = 0; i < max; i++)
                {
                    boolean errDetected = checkEntity(vecEntities.elementAt(i));
                    if (errDetected)
                    {
                        anyErrors = true;
                    }
                    updateProgressBar(++entityNum);
                    SwingUtilities.invokeLater(update);
                }

                if(!anyErrors)
                {
                    vecErrors.add(new FeedbackListEntry("There were no errors detected in this project."));
                }
                setFeedbackListData(vecErrors);
                SwingUtilities.invokeLater(finish);
            }
            catch (IOException ioe) 
            {
                ioe.printStackTrace();
            }
        }//checkEntities()

    }//class UpdateThread


 	/**
     * This action verifies that a project is intact.  Specifically
     * it checks that all the project's files are present and can
     * be loaded.
     */
	class VerifyProjectAction extends PerformableAction
    {
		private static final long serialVersionUID = 20221225L;

		public VerifyProjectAction()
        {
			super("Verify Project Integrity");
			setEnabled(false);
		}

        public void perform()
        {
			Enumeration<TreeNode> bfe = operatorWindow.breadthFirstEnumeration();
            Vector<OperatorNode> vecNodes = new Vector<>(10, 50);
			while(bfe.hasMoreElements())
            {
				Object obj = bfe.nextElement();
				if (obj instanceof OperatorNode) {
					OperatorNode node = (OperatorNode)obj;
					vecNodes.add(node);
				}
			}
			(new VerifyProjectThread(vecNodes, "Verifiying Project...")).start();
        }
        
		public void actionPerformed(ActionEvent ae)
        {
            perform();
		}

        class VerifyProjectThread extends UpdateThread
        {
            public VerifyProjectThread(Vector<OperatorNode> v, String title)
            {
                super(v, title);
            }
                
            public boolean checkEntity(Object node) {
                OperatorNode opNode = (OperatorNode)node;

                //Only file nodes need to be examined
                if ( ! (opNode instanceof FileNode))
                {
                    return false;
                }
                
                File f = new File(opNode.getFileName());
                if (!f.canRead())
                {
                	String msg = "Error!  Project Corrupted:  Unable to open file: "
								+ opNode.getFileName();
                    vecErrors.add(new FeedbackListEntry(msg));
                    return true;
                }
                    
                if (!f.canWrite())
                {
					String msg ="Error!  Unable to write to file: "
								+ opNode.getFileName();
							vecErrors.add(new FeedbackListEntry(msg));
                    return true;
                }

                //We lie and say there are errors no matter what so that
                //the "there were no errors..." message won't appear.
                return true;
            }
        }//class VerifyProjectThread
	
	}//class VerifyProjectAction

	/**
	 * class LoadTopStateDatamapAction
	 *
	 * This action loads the top-state datamap
	 *
	 * @author Andrew Nuxoll
	 * @version 08 Sep 2022
	 */
	class LoadTopStateDatamapAction extends AbstractAction
	{
		private static final long serialVersionUID = 20221225L;

		public LoadTopStateDatamapAction()
		{
			super("Load Top-State Data Map");
			setEnabled(false);
		}

		public void actionPerformed(ActionEvent ae)
		{
			openTopStateDatamap();
		}

	}//class LoadTopStateDatamapAction

	/**
     * This action searches all productions in the project for syntax
     * errors only.   Operation status is displayed in a progress bar.
     * Results are displayed in the feedback list
     */
	class CheckSyntaxErrorsAction extends AbstractAction 
    {
		private static final long serialVersionUID = 20221225L;

		//a list of all production names seen is stored here so that duplicates can be found
		private final Vector<String> allProdNames = new Vector<>();

		public CheckSyntaxErrorsAction() 
        {
			super("Check All Productions for Syntax Errors");
			setEnabled(false);
		}
	
		public void actionPerformed(ActionEvent ae)
        {
        	//reset the list for the new duplicate name check
			this.allProdNames.clear();


			Enumeration<TreeNode> bfe = operatorWindow.breadthFirstEnumeration();
            Vector<OperatorNode> vecNodes = new Vector<>(10, 50);
			while(bfe.hasMoreElements())
            {
				vecNodes.add((OperatorNode)bfe.nextElement());
			}
			(new CheckSyntaxThread(vecNodes, "Checking Productions...")).start();
		}

        class CheckSyntaxThread extends UpdateThread
        {
            public CheckSyntaxThread(Vector<OperatorNode> v, String title)
            {
                super(v, title);
            }

            /** Check for duplicate production names */
            private void checkDuplicateProdNames(OperatorNode opNode) {
				Vector<String> prodNames = opNode.getProdNames();
				for(String prodName : prodNames) {
					for(String allName : CheckSyntaxErrorsAction.this.allProdNames) {
						if (allName.startsWith(prodName)) {
							//We *may* have a name conflict, but it's possible that
							//allName has a longer name.
							//trim allName to just the name and check for match
							String allNameOnly = allName.trim();
							int spaceIndex = allNameOnly.indexOf(" ");
							if (spaceIndex > 0) {
								allNameOnly = allName.substring(0,spaceIndex);
							}

							//now check for equality
							if (allNameOnly.equals(prodName)) {
								//Construct and add a FeedbackListObj
								String errStr = "Warning: " + allName + " name conflicts with " + prodName + " in " + opNode.getFileName();
								int lineNo = opNode.getLineNumForString(prodName);
								FeedbackListEntry flobj = new FeedbackEntryOpNode(opNode, lineNo, errStr);
								vecErrors.add(flobj);
							}
						}
					}
					//save each name in this file to check against future files
					allProdNames.add(prodName + " in " + opNode.getFileName());
				}//for
			}//checkDuplicateProdNames

			public boolean checkEntity(Object node) throws IOException
            {
                OperatorNode opNode = (OperatorNode)node;

                //do this check first since it only generates warnings
				checkDuplicateProdNames(opNode);

                try
                {
					//This is the main parsing here
                    Vector<SoarProduction> prods = opNode.parseProductions();

                    //Check for Supplemental Errors and Warnings
                    if ((prods != null) && (!prods.isEmpty())) {

						// Variable on RHS never created or tested
						for (SoarProduction sprod : prods) {
							FeedbackListEntry flobj = SuppParseChecks.checkUndefinedVarRHS(opNode, sprod);
							if (flobj != null) {
								vecErrors.add(flobj);
								return true;
							}
						}

						//angle brackets used in constants
						FeedbackListEntry flobj = SuppParseChecks.warnSuspiciousConstants(opNode, prods);
						if (flobj != null) {
							vecErrors.add(flobj);
							return true;
						}
					}


                }
                catch(ParseException pe)
                {
                    vecErrors.add(opNode.parseParseException(pe));
                    return true;
                }
                catch(TokenMgrError tme) 
                {
                    vecErrors.add(opNode.parseTokenMgrError(tme));
                    return true;
                }

                return false;
            }
        }//class CheckSyntaxThread
	
	}//class CheckSyntaxErrorsAction


    

 	/**
     * This class is responsible for comparing all productions in the project
     * with the project's model of working memory - the datamap.
     * Operation status is displayed in a progress bar.
     * Results are displayed in the feedback list
     */
	class CheckAllProductionsAction extends PerformableAction
    {
		private static final long serialVersionUID = 20221225L;

		public CheckAllProductionsAction()
        {
			super("Check All Productions");
			setEnabled(false);
		}

        //Same as actionPerformed() but this function waits for the thread to
        //complete before returning (i.e., it's effectively not threaded)
        public void perform()
        {
            Vector<OperatorNode> vecNodes = new Vector<>(10, 50);
			Enumeration<TreeNode> bfe = operatorWindow.breadthFirstEnumeration();
			while(bfe.hasMoreElements()) 
            {
				vecNodes.add((OperatorNode) bfe.nextElement());
			}

            CheckProductionsThread cpt =
                new CheckProductionsThread(vecNodes,
                                           "Checking Productions...");
            cpt.start();
        }

		public void actionPerformed(ActionEvent ae)
        {
            perform();
		}

        class CheckProductionsThread extends UpdateThread
        {
            public CheckProductionsThread(Vector<OperatorNode> v, String title)
            {
                super(v, title);
            }
                
            public boolean checkEntity(Object node) throws IOException
            {
                return ((OperatorNode)node).CheckAgainstDatamap(vecErrors);
            }
            
        }

	}//class CheckAllProductionsAction
    

	/**
     * This action provides a framework for searching all datamaps for errors.
     * It is intended to be subclassed.  Operation status is displayed in
     * a progress bar.  Results are displayed in the feedback list
     * Double-clicking on an item in the feedback list should display the rogue
     * node in the datamap.
     */
	abstract class SearchDataMapAction extends AbstractAction
    {
		private static final long serialVersionUID = 20221225L;

		int numNodes = 0;       // number of operator nodes in the project
        int numChecks = 0;      // number of nodes scanned so far
        
		public SearchDataMapAction() 
        {
			super("Check All Productions");
			setEnabled(false);
		}

		public void actionPerformed(ActionEvent ae) 
        {
            initializeEdges();
            numNodes = 0;
            numChecks = 0;
            
			Enumeration<TreeNode> bfe = operatorWindow.breadthFirstEnumeration();
            Vector<OperatorNode> vecNodes = new Vector<>(10, 50);
			while(bfe.hasMoreElements())
            {
				vecNodes.add((OperatorNode) bfe.nextElement());
                numNodes++;
			}

            //Add the nodes a second time because we'll be scanning them twice,
            //once to check productions against the datamap and again to check
            //the datamap for untested WMEs.  (See checkEntity() below.)
			bfe = operatorWindow.breadthFirstEnumeration();
			while(bfe.hasMoreElements())
            {
				vecNodes.add((OperatorNode) bfe.nextElement());
			}
            
			(new DatamapTestThread(vecNodes, "Scanning Datamap...")).start();

		}//actionPerformed()

        /**
             *  This initializes the status of all the edges to zero, which
             *  means that the edges have not been used by a production in any
             *  way.
             */
        public void initializeEdges()
        {
            Enumeration<NamedEdge> edges = operatorWindow.getDatamap().getEdges();
            while(edges.hasMoreElements()) 
            {
                NamedEdge currentEdge = edges.nextElement();
                currentEdge.resetTestedStatus();
                currentEdge.resetErrorNoted();
                // initialize the output-link as already tested
                if(currentEdge.getName().equals("output-link")) 
                {
                    currentEdge.setOutputLinkTested(operatorWindow.getDatamap());
                }
            }
        }// initializeEdges()

        //This function performs the actual error check
        //The datamap associated with the given operator node is scanned and a
        //list of errors is placed in the given Vector.
        abstract public void searchDatamap(OperatorNode opNode, Vector<FeedbackListEntry> v);
        
		class DatamapTestThread extends UpdateThread 
        {
			public DatamapTestThread(Vector<OperatorNode> v, String title)
            {
                super(v, title);
			}

            /**
             *  Search through the datamap and look for extra WMEs by looking at
             *  the status of the named edge (as determined by the check nodes
             *  function) and the edge's location within the datamap.  Extra
             *  WMEs are classified in this action by never being tested by a
             *  production, not including any item within the output-link.
             */
            public boolean checkEntity(Object node) throws IOException
            {
                OperatorNode opNode = (OperatorNode)node;

                //For the first run, do a normal production check
                if (numChecks < numNodes)
                {
                    Vector<FeedbackListEntry> v = new Vector<>();
                    boolean rc = opNode.CheckAgainstDatamap(v);
                    if (rc)
                    {
                    	String msg = "WARNING:  datamap errors were found in "
									+ opNode.getFileName()
									+ "'s productions.  This may invalidate the current scan.";
                        vecErrors.add(new FeedbackListEntry(msg));
                    }

                    numChecks++;
                    return rc;
                }//if

                //For the second run, do the requested datamap scan
                Vector<FeedbackListEntry> v = new Vector<>();
                searchDatamap(opNode, v);
                numChecks++;
                
                if (!v.isEmpty())
                {
                    vecErrors.addAll(v);
                    return true;
                }

                return false;
            }//checkEntity()
           
		}//class DatamapTestThread
	}// end of SearchDataMapAction

    
	/**
     * Search for WMEs that are never tested
     */
	class SearchDataMapTestAction extends SearchDataMapAction
    {
		private static final long serialVersionUID = 20221225L;

		public void searchDatamap(OperatorNode opNode, Vector<FeedbackListEntry> v)
        {
            opNode.searchTestDataMap(operatorWindow.getDatamap(), v);
        }//searchDatamap
	}// end of SearchDataMapTestAction

    
	/**
     * Search for WMEs that are never created
     */
	class SearchDataMapCreateAction extends SearchDataMapAction
    {
		private static final long serialVersionUID = 20221225L;

		public void searchDatamap(OperatorNode opNode, Vector<FeedbackListEntry> v)
        {
            opNode.searchCreateDataMap(operatorWindow.getDatamap(), v);
        }//searchDatamap
	}// class SearchDataMapCreateAction

	/**
     * Search for WMEs that are tested but never created
     */
	class SearchDataMapTestNoCreateAction extends SearchDataMapAction
    {
		private static final long serialVersionUID = 20221225L;

		public void searchDatamap(OperatorNode opNode, Vector<FeedbackListEntry> v)
        {
            opNode.searchTestNoCreateDataMap(operatorWindow.getDatamap(), v);
        }//searchDatamap
	}//class SearchDataMapTestNoCreateAction

	/**
     * Search for WMEs that are created but never tested
     */
	class SearchDataMapCreateNoTestAction extends SearchDataMapAction
    {
		private static final long serialVersionUID = 20221225L;

		public void searchDatamap(OperatorNode opNode, Vector<FeedbackListEntry> v)
        {
            opNode.searchCreateNoTestDataMap(operatorWindow.getDatamap(), v);
        }//searchDatamap
	}//class SearchDataMapCreateNoTestAction

	/**
     * Search for WMEs that are never created and never tested
     */
	class SearchDataMapNoTestNoCreateAction extends SearchDataMapAction
    {
		private static final long serialVersionUID = 20221225L;

		public void searchDatamap(OperatorNode opNode, Vector<FeedbackListEntry> v)
        {
            opNode.searchNoTestNoCreateDataMap(operatorWindow.getDatamap(), v);
        }//searchDatamap
	}//class SearchDataMapNoTestNoCreateAction


    /**
     * This class is responsible for comparing all productions in the project
     * with the project's datamaps and 'fixing' any discrepancies
     * by adding missing productions to the datamap.
     * Operation status is displayed in a progress bar.
     * Add productions in the datamap are displayed as green until the user validates them.
     * Results are displayed in the feedback list
     */
    class GenerateDataMapAction extends AbstractAction 
    {
		private static final long serialVersionUID = 20221225L;

		JProgressBar progressBar;
		JDialog progressDialog;

		public GenerateDataMapAction() 
        {
			super("Generate Datamap from Operator Hierarchy");
			setEnabled(false);
		}

		public void actionPerformed(ActionEvent ae) 
        {
			int numNodes = 0;
			Enumeration<TreeNode> bfe = operatorWindow.breadthFirstEnumeration();
			while(bfe.hasMoreElements()) 
            {
				numNodes++;
				bfe.nextElement();
			}
			System.out.println("Nodes: " + numNodes);
			progressBar = new JProgressBar(0, numNodes * 7);
			progressDialog = new JDialog(MainFrame.this, "Generating Datamap from Productions");
			progressDialog.getContentPane().setLayout(new FlowLayout());
			progressDialog.getContentPane().add(progressBar);
			progressBar.setStringPainted(true);
			progressDialog.setLocationRelativeTo(MainFrame.this);
			progressDialog.pack();
			progressDialog.setVisible(true);
			(new UpdateThread()).start();
		}

		class UpdateThread extends Thread 
        {
			Runnable update, finish;
			int value, min;
			 

			Vector<FeedbackListEntry> errors = new Vector<>();
			int repCount = 0;
			Enumeration<TreeNode> bfe = operatorWindow.breadthFirstEnumeration();
			OperatorNode current;
			Vector<FeedbackListEntry> vecErrors = new Vector<>();

			public UpdateThread() 
            {
				progressBar.getMaximum();
				progressBar.getMinimum();

				update = new Runnable() 
                {
					public void run() 
                    {
						value = progressBar.getValue() + 1;
						updateProgressBar(value);
						//System.out.println("Value is " + value);
					}
				};
				finish = new Runnable() 
                {
					public void run() 
                    {
						updateProgressBar(min);
						System.out.println("Done");
						progressDialog.dispose();
					}
				};
			}

			public void run() 
            {
                checkNodes();
                repCount = 0;

                JOptionPane.showMessageDialog(s_mainFrame,
						"DataMap Generation Completed",
						"DataMap Generator",
						JOptionPane.INFORMATION_MESSAGE);
			}

			private void updateProgressBar(int value) 
            {
				progressBar.setValue(value);
			}
						
			public void checkNodes() 
            {
                do 
                {
                    repCount++;
                    errors.clear();
                    bfe = operatorWindow.breadthFirstEnumeration();

                    while(bfe.hasMoreElements()) 
                    {
                        current = (OperatorNode)bfe.nextElement();

                        operatorWindow.generateDataMap(current, errors, vecErrors);

                        setFeedbackListData(vecErrors);
                        value = progressBar.getValue() + 1;
                        updateProgressBar(value);
                        SwingUtilities.invokeLater(update);
                    } // while parsing operator nodes
          
                } while(!(errors.isEmpty()) && repCount < 5);


                //Instruct all open datamap windows to display
                //the newly generated nodes
                JInternalFrame[] jif = desktopPane.getAllFrames();
				for (JInternalFrame jInternalFrame : jif) {
					if (jInternalFrame instanceof DataMap) {
						DataMap dm = (DataMap) jInternalFrame;
						dm.displayGeneratedNodes();
					}
				}

                SwingUtilities.invokeLater(finish);
                
			}//checkNodes

		}//class UpdateThread
	}//class GenerateDataMapAction

	/**
	 * class LinkDataMapAction
	 *
	 * This action loads a datamap from another project and allows the user to import
	 * items from it.  These items are "linked" can not be edited via this project
	 * but must be edited via the project they are imported from.
	 *
	 * @author Andrew Nuxoll
	 * @version 21 Jan 2024
	 */
	class LinkDataMapAction extends AbstractAction
	{
		private static final long serialVersionUID = 20240121L;
		SoarWorkingMemoryModel swmm = null;

		public LinkDataMapAction()
		{
			super("Link Items from Another Datamap");
			setEnabled(false);
		}


		public void actionPerformed(ActionEvent ae) {
			//Only one datamap window can be open at a time
			if (desktopPane.numDataMaps() > 0) {
				setStatusBarMsg("Can not import from foreign datamap while local datamap is being edited.");
				return;
			}

			//The user selects a datamap file to import from
			File vsaFile = ghettoFileChooser();
			if (vsaFile == null) return;

			//read the data from the foreign datamap into a local SWMM object
			this.swmm = new SoarWorkingMemoryModel(false, vsaFile.getName());
			String dmFilename = SoarWorkingMemoryReader.readDataIntoSWMMfromVSA(vsaFile, this.swmm);
			if (dmFilename == null) return;

			//Create a datamap with checkboxes
			CheckBoxDataMap dataMap = new CheckBoxDataMap(swmm, dmFilename);
			dataMap.setVisible(true);
			addDataMap(dataMap);

		}//actionPerformed



	}//class LinkDataMapAction





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
			if (operatorWindow == null) return;

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
			if (operatorWindow == null) return;

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
			if (operatorWindow == null) return;

			//Get all files
			Enumeration<TreeNode> bfe = operatorWindow.breadthFirstEnumeration();
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
			setFeedbackListData(vecFeedback);
		}//actionPerformed
	}//class FindAllProdsAction


	class CommitAction extends PerformableAction 
    {
		private static final long serialVersionUID = 20221225L;

		public CommitAction()
        {
			super("Commit");
			setEnabled(false);
		}
		
		public void perform() 
        {
			saveAllFilesAction.perform();
			if(operatorWindow != null) 
            {
				exportAgentAction.perform();
				saveDataMapAndProjectAction.perform();
			}
		}
		
		public void actionPerformed(ActionEvent e) 
        {
			perform();
			setStatusBarMsg("Save Finished");
		}
	}//class CommitAction
	
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
			SaveProjectAsDialog spad = new SaveProjectAsDialog(MainFrame.getMainFrame());
            spad.setVisible(true);

            OperatorRootNode root = (OperatorRootNode)(operatorWindow.getModel().getRoot());
            File oldProjectFile = new File(root.getProjectFile());
			String oldProjPath = root.getFolderName();

			if (spad.wasApproved()) 
            {
				String newName = spad.getNewAgentName();
                String newRootPath = spad.getNewAgentPath();
                String newProjPath = newRootPath + File.separator + newName;
				if(OperatorWindow.isProjectNameValid(newName))
                {
					operatorWindow.saveProjectAs(newName, newRootPath);

                    // Regenerate the *_source.soar files in the old project
                    try  {
                        OperatorWindow oldOpWin = new OperatorWindow(oldProjectFile, false);
                        OperatorRootNode oldOrn = (OperatorRootNode)oldOpWin.getModel().getRoot();
                        oldOrn.startSourcing();
                    }
                    catch (IOException exception) {
                        JOptionPane.showMessageDialog(MainFrame.this,
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
							oldRuleEditor.fileRenamed(newNode.getFileName());         // Update the Rule editor with the correct updated file name
						}
					}
                    saveAllFilesAction.perform();     // Save all open Rule Editors to the new project directory
                    exportAgentAction.perform();
                    saveDataMapAndProjectAction.perform();    // Save DataMap and Project file (.vsa)

                    //Set the title bar to include the project name
                    setTitle(newName);
                    
                }
				else 
                {
					JOptionPane.showMessageDialog(MainFrame.this,
                                                  "That is not a valid name for the project",
                                                  "Invalid Name",
                                                  JOptionPane.ERROR_MESSAGE);				
				}
			}
		}
	}//class SaveProjectAsAction


	class CloseAllWindowsAction extends AbstractAction {
		private static final long serialVersionUID = 20240426L;

		public CloseAllWindowsAction()
		{
			super("Close All Windows");
		}

		public void actionPerformed(ActionEvent e) {
			JInternalFrame[] frames = desktopPane.getAllFrames();
			for (JInternalFrame jif : frames) {
				try {
					jif.setClosed(true);
				} catch (PropertyVetoException ex) {
					/* should not happen.  ignore. nbd.*/
				}
			}
		}
	}

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
		
