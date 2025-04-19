package edu.umich.soar.visualsoar;
import edu.umich.soar.visualsoar.mainframe.MainFrame;
import edu.umich.soar.visualsoar.mainframe.actions.CheckAllProductionsAction;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackListEntry;
import edu.umich.soar.visualsoar.mainframe.feedback.FeedbackManager;
import edu.umich.soar.visualsoar.misc.Prefs;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Vector;
import org.apache.commons.cli.*;

/**
 * This is the class that the user runs. Unless command line args are given, all it does is create
 * an instance of the main frame and make it visible.
 *
 * @author Brad Jones 0.5a 5 Aug 1999
 * @author Jon Bauman
 * @author Brian Harleton
 * @version 4.0 5 Jun 2002
 */
public class VisualSoar {

  private static final HelpFormatter HELP_FORMATTER = new HelpFormatter();
  private static final String CHECK_OPT = "check";
  private static final String CHECK_PRODS_AGAINST_DM = "productionsAgainstDatamap";
  private static final List<String> CHECK_TYPES = List.of(CHECK_PRODS_AGAINST_DM);
  private static final String HELP_OPT = "help";
  private static final String JSON_OPT = "json";
  private static final String PROJECT_OPT = "project";

  private static final Options CMD_OPTIONS;

  static {
    Option checkOption =
        new Option(
            CHECK_OPT.substring(0, 1),
            CHECK_OPT,
            true,
            "Perform the specified checks. Options are: " + String.join(", " + CHECK_TYPES));
    checkOption.setArgs(CHECK_TYPES.size());

    Option jsonOption =
      new Option(
        JSON_OPT.substring(0, 1),
        JSON_OPT,
        false,
        "Output diagnostics as JSON lines if set; otherwise human-readable text. The JSON structure follows the LSP specification for diagnostics.");


    Option projectOption =
        new Option(
            PROJECT_OPT.substring(0, 1),
            PROJECT_OPT,
            true,
            "Path to the project .vsa or .vsa.json file to to run a specified action against.");

    Option helpOption =
        new Option(HELP_OPT.substring(0, 1), HELP_OPT, false, "Print this help text and exit.");

    CMD_OPTIONS = new Options();
    CMD_OPTIONS.addOption(checkOption);
    CMD_OPTIONS.addOption(jsonOption);
    CMD_OPTIONS.addOption(projectOption);
    CMD_OPTIONS.addOption(helpOption);
  }

  private static void printHelp() {
    HELP_FORMATTER.printHelp("VisualSoar", CMD_OPTIONS);
  }

  private static void exitWithError(String message) {
    System.err.println(message);
    printHelp();
    System.exit(1);
  }

  private static void reportFeedback(List<FeedbackListEntry> feedback, String successMessage, boolean jsonFormat) {
    if (feedback.isEmpty()) {
      if (jsonFormat) {
        System.out.println(
        "{\"message\": \""
          + successMessage
          + "\", \"severity\": 3, "
          + "\"source\": \"VisualSoar\"}");
      }else {
        System.out.println(successMessage);
      }
      return;
    }
    for (FeedbackListEntry entry : feedback) {
      if (jsonFormat) {
        System.out.println(entry.toJson());
      } else {
        String emoji = entry.isError() ? "❌ " : "ℹ️ ";
        System.out.println(emoji + entry);
      }
    }
    System.exit(1);
  }

  /**
   * Since this class can be run this is the starting point, it constructs an instance of the
   * MainFrame
   *
   * @param args an array of strings representing command line arguments
   */
  public static void main(String[] args) throws ParseException {
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(CMD_OPTIONS, args);

    if (cmd.hasOption(HELP_OPT)) {
      printHelp();
      return;
    }

    boolean jsonOutput = cmd.hasOption(JSON_OPT);

    if (cmd.hasOption(CHECK_OPT)) {
      ProjectModel pm = null;
      String projectParameter = cmd.getOptionValue(PROJECT_OPT);
      if (projectParameter == null) {
        exitWithError("Please specify the project .vsa or .vsa.json path with --" + PROJECT_OPT + ".");
      }
      try {
        Path projectPath = Paths.get(projectParameter);
        pm = ProjectModel.openExistingProject(projectPath);
      } catch (IOException e) {
        exitWithError("Could not open project file for checking: " + e);
      }
      String checkName = cmd.getOptionValue(CHECK_OPT);
      switch (checkName) {
        case CHECK_PRODS_AGAINST_DM:
          {
            try {
              List<FeedbackListEntry> feedback = CheckAllProductionsAction.checkAllProductions(pm);
              reportFeedback(feedback, "✅ No datamap issues found!", jsonOutput);
            } catch (IOException e) {
              exitWithError("I/O error while checking productions against the datamap: " + e);
            }
            break;
          }
        default:
          {
            exitWithError("Unknown --" + CHECK_OPT + " argument: " + checkName);
          }
      }
      return;
    }

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
