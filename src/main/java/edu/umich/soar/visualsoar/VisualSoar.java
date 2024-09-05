package edu.umich.soar.visualsoar;
import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackManager;
import edu.umich.soar.visualsoar.misc.Prefs;

import java.io.File;
import java.util.Vector;

/**
 * This is the class that the user runs, all it does is create an instance of the
 * main frame and sets it visible
 * @author Brad Jones 0.5a 5 Aug 1999
 * @author Jon Bauman
 * @author Brian Harleton
 * @version 4.0 5 Jun 2002
 */
public class VisualSoar {
///////////////////////////////////////////////////////////////////
// Methods
///////////////////////////////////////////////////////////////////

	/**
	 * Since this class can be run this is the starting point, it constructs an instance
	 * of the MainFrame
	 * @param args an array of strings representing command line arguments
	 */
	public static void main(String[] args) throws Exception {
		MainFrame mainFrame = new MainFrame("VisualSoar");
		MainFrame.setMainFrame(mainFrame);
		mainFrame.setVisible(true);

		//If user specified a command line argument, try to open it as a project
		if(args.length >= 1){
      try (FeedbackManager.AtomicContext ignored = mainFrame.getFeedbackManager().beginAtomicContext()) {
        mainFrame.openProject(new File(args[0]), false);
      }
    }

		//If nothing was specified on the command line, try
		//to open the most recent project as a courtesy
		else  {
			Vector<Prefs.RecentProjInfo> recentProjs = Prefs.getRecentProjs();
			int numRecent = recentProjs.size();
			if (numRecent > 0) {
				Prefs.RecentProjInfo mostRecent = recentProjs.get(numRecent - 1);
        try (FeedbackManager.AtomicContext ignored = mainFrame.getFeedbackManager().beginAtomicContext()) {
          mainFrame.openProject(mostRecent.file, mostRecent.isReadOnly);
        }
			}
		}
	}

}
