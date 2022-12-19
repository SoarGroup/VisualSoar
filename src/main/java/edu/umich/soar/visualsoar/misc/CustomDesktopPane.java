package edu.umich.soar.visualsoar.misc;
import edu.umich.soar.visualsoar.datamap.DataMap;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import java.util.*;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.awt.Point;

/**
 * This is the new editor pane so, we can add functionality
 * of moving windows around like cascading and the like
 * based up Graphic Java 2, Volume II pg 901
 *
 * @author Brad Jones Jon Bauman
 * @version 0.5a 6/29/1999
 */
public class CustomDesktopPane extends JDesktopPane {
	//amount of offset between cascaded windows
	public static final int X_OFFSET = 30;
	public static final int Y_OFFSET = 30;
	public static final int CASCADE_WIDTH = 250;
	public static final int CASCADE_HEIGHT = 350;


    CascadeAction 	cascadeAction = new CascadeAction();
    TileAction 		tileAction    = new TileAction();
    ReTileAction 	reTileAction  = new ReTileAction();

    // Added dataMaps member to keep track of which dataMaps are currently
    // open within the desktop.
    private final Hashtable<Integer, DataMap> dataMaps = new Hashtable<>();


	/**
	 * This will cascade all the windows currently in the 
	 * desktop pane
	 */
	class CascadeAction extends PerformableAction {
	    public CascadeAction() {
		super("Cascade");
	    }

	    public void perform() {
		JInternalFrame[] frames = getAllFrames();
		int x = 0, y = 0;

			for (JInternalFrame frame : frames) {
				if (!frame.isIcon()) {
					try {
						frame.setMaximum(false);
					} catch (java.beans.PropertyVetoException pve) {
						//should not happen
						continue;
					}
					frame.setBounds(x, y, CASCADE_WIDTH, CASCADE_HEIGHT);
					x += X_OFFSET;
					y += Y_OFFSET;
				}
			}
	    }
		
	    public void actionPerformed(ActionEvent e) {
		perform();
	    }
	}

    public void performTileAction() {
	tileAction.perform();
    }

    public void performReTileAction() {
	reTileAction.perform();
    }

    public void performCascadeAction() {
	cascadeAction.perform();
    }
		
    /**
	 * horizontalTile
	 *
	 * sorts the open rule editor windows into two columns
	 */
	public void horizontalTile() {
		int 				iconHeight = 0;
		int 				paneHeight = getHeight();
		int					paneWidth = getWidth();
		int 				numCols;
		int 				numRows;

		//Retrieve the panes to tile
		JInternalFrame[] 	allFrames = getAllFrames();
		if (allFrames == null || allFrames.length == 0) {
			return;
		}

		//Only tile the frames are not minimized (not icons)
		Vector<JInternalFrame> frames = new Vector<>();
		int numFrames = 0;	
		for (int i = allFrames.length - 1; i > -1; i--) {
			if (!allFrames[i].isIcon()) {
				frames.add(allFrames[i]);
				numFrames++;
			}
			else {
				iconHeight = 30;
			}
		}

		if(numFrames == 0) {
			return;  //nothing to tile
		}

		//Calculate arrangement based on number of frames
		switch (numFrames) {
			case 1:
				numCols = 1;
				numRows = 1;
				break;
			case 2:
				if (paneHeight > paneWidth) {
					numCols = 1;
					numRows = 2;
				}
				else {
					numCols = 2;
					numRows = 1;
				}
				break;
			case 3:
			case 4:
				numCols = 2;
				numRows = 2;
				break;
			default: // > 4
				numCols = 2;
				numRows = (int)Math.ceil(numFrames /  2.0);
				break;
		}

		//Calculate the new upper-left corner positions that will be used
		int colWidth = paneWidth / numCols;
		int rowHeight = (paneHeight - iconHeight) / numRows;
		Vector<Point> ulPoints = new Vector<>(); // all possible upper, left-hand points
		int x = 0;
		int y;
		for (int col = 0; col < numCols; col++) {
			y = 0;
			for (int row = 0; row < numRows; row++) {
				ulPoints.add(new Point(x,y));
				y += rowHeight;
			}
			x += colWidth;
		}		

		//Find the pane nearest each ul point and move it there
		CustomInternalFrame currentFrame;
		Enumeration<JInternalFrame> framesEnum = frames.elements();
		Enumeration<Point> ulpe;
		Point framePoint, ULPoint;
		int shortest = 0, current;
		int shortestPointIndex = 0;
		while (framesEnum.hasMoreElements()) { // find the closest match for each frame
			currentFrame = (CustomInternalFrame)framesEnum.nextElement();
			framePoint = currentFrame.getLocation();
			
			ulpe = ulPoints.elements();
			if (ulpe.hasMoreElements()) { // initialize shortest dist to 1st point
				ULPoint = ulpe.nextElement();
				shortest = (int)framePoint.distance(ULPoint);
				shortestPointIndex = 0;
			} 
			while (ulpe.hasMoreElements()) {
				ULPoint = ulpe.nextElement();
			
				current = (int)framePoint.distance(ULPoint);
				if (current < shortest) {
					shortest = current;
					shortestPointIndex = ulPoints.indexOf(ULPoint);
				}
			}
			
			try {
				currentFrame.setMaximum(false);
			}
			catch(java.beans.PropertyVetoException pve) {
				pve.printStackTrace();
			}
			ULPoint = ulPoints.elementAt(shortestPointIndex);
			currentFrame.setBounds((int)ULPoint.getX(), (int)ULPoint.getY(), colWidth, rowHeight);
			
			ulPoints.remove(shortestPointIndex);
		}//while

		//If there is an odd number of panes window one pane can be wider
		if (! ulPoints.isEmpty()) { // cover wasted space
			int wastedY = (int) ulPoints.elementAt(0).getY();
			
			framesEnum = frames.elements();
			while (framesEnum.hasMoreElements()) {
				currentFrame = (CustomInternalFrame)framesEnum.nextElement();
				if (currentFrame.getLocation().getY() == wastedY) {
					currentFrame.setBounds(0, wastedY, 2 * colWidth, rowHeight);
				}
			}
		}
	} // horizontalTile

	/**
	 * verticalTile
	 *
	 * sorts the open rule editor windows into two rows
	 */
	public void verticalTile() {
		int 				iconizedHeight = 0;
		int 				paneHeight = getHeight();
		int					paneWidth = getWidth();
		int 				numCols;
		int 				numRows;

		//Retrieve the panes to tile
		JInternalFrame[] 	allFrames = getAllFrames();
		if (allFrames == null || allFrames.length == 0) {
			return;
		}

		//Only tile the frames are not minimized (not icons)
		Vector<JInternalFrame> frames = new Vector<>();
		int numFrames = 0;	
		for (int i = allFrames.length - 1; i > -1; i--) {
			if (!allFrames[i].isIcon()) {
				frames.add(allFrames[i]);
				numFrames++;
			}
			else {
				iconizedHeight = 30;
			}
		}

		if(numFrames == 0) {
			return;  //nothing to tile
		}

		//Calculate arrangement based on number of frames
		switch (numFrames) {
			case 1:
				numCols = 1;
				numRows = 1;
				break;
			case 2:
				if (paneHeight < paneWidth) {
					numCols = 1;
					numRows = 2;
				}
				else {
					numCols = 2;
					numRows = 1;
				}
				break;
			case 3:
			case 4:
				numCols = 2;
				numRows = 2;
				break;
			default: // > 4
				numRows = 2;
				numCols = (int)Math.ceil(numFrames /  2.0);
				break;
		}

		//Calculate the new upper-left corner positions that will be used
		int colWidth = paneWidth / numCols;
		int rowHeight = (paneHeight - iconizedHeight) / numRows;
		Vector<Point> ulPoints = new Vector<>(); // all possible upper, left-hand points
		int x = 0;
		int y;
		for (int col = 0; col < numCols; col++) {
			y = 0;
			for (int row = 0; row < numRows; row++) {
				ulPoints.add(new Point(x,y));
				y += rowHeight;
			}
			x += colWidth;
		}

		//Find the pane nearest each ul point and move it there
		CustomInternalFrame currentFrame;	
		Enumeration<JInternalFrame> framesEnum = frames.elements();
		Enumeration<Point> ulpe;
		Point framePoint, ULPoint;
		int shortest = 0, current;
		int shortestPointIndex = 0;
		while (framesEnum.hasMoreElements()) { // find the closest match for each frame
			currentFrame = (CustomInternalFrame)framesEnum.nextElement();
			framePoint = currentFrame.getLocation();
			
			ulpe = ulPoints.elements();
			if (ulpe.hasMoreElements()) { // initialize shortest dist to 1st point
				ULPoint = ulpe.nextElement();
				shortest = (int)framePoint.distance(ULPoint);
				shortestPointIndex = 0;
			} 
			while (ulpe.hasMoreElements()) {
				ULPoint = ulpe.nextElement();
			
				current = (int)framePoint.distance(ULPoint);
				if (current < shortest) {
					shortest = current;
					shortestPointIndex = ulPoints.indexOf(ULPoint);
				}
			}
			
			try {
				currentFrame.setMaximum(false);
			}
			catch(java.beans.PropertyVetoException pve) {
				pve.printStackTrace();
			}
			ULPoint = ulPoints.elementAt(shortestPointIndex);
			currentFrame.setBounds((int)ULPoint.getX(), (int)ULPoint.getY(), colWidth, rowHeight);
			
			ulPoints.remove(shortestPointIndex);
		}//while

		//If there is an odd number of panes window one pane can be taller
		if (! ulPoints.isEmpty()) { // cover wasted space
			int wastedX = (int) ulPoints.elementAt(0).getX();
			
			framesEnum = frames.elements();
			while (framesEnum.hasMoreElements()) {
				currentFrame = (CustomInternalFrame)framesEnum.nextElement();
				if (currentFrame.getLocation().getX() == wastedX) {
					currentFrame.setBounds(wastedX, 0, colWidth, 2 * rowHeight);
				}
			}
		}
	} // verticalTile

    public DataMap dmGetDataMap(int id) {
        return dataMaps.get(id);
    }

    /**
     *  Checks to see if a datamap is already open within the desktop
     *  @param dm the datamap to look for
     */
    public boolean hasDataMap(DataMap dm)
    {
        // Make sure this datamap isn't already there
    	return dataMaps.containsKey(dm.getId());
    }
    
  /**
   *  Adds a dataMap to the hashtable of datamaps that are open within the desktop
   *  @param id the id that denotes the correct datamap
   *  @param dm the datamap that is being stored in the hashtable
   */
    public void dmAddDataMap(int id, DataMap dm)
    {
        if (hasDataMap(dm)) return;
        
        //Try to add the new datamap
        try
        {
            dataMaps.put(id, dm);
        }
        catch (NullPointerException npe) {
            System.err.println("error-key or value was null");
        }
    }

  /**
   *  removes a datamap from the hashtable that holds all open datamaps within the desktop
   *  @param id the id that denotes the datamap to be removed
   */
  public void dmRemove(int id) {
    dataMaps.remove(id);
  }

    /**
	 * Create a version of the getAllFrames() method to return an array of 
	 * CustomInternalFrame.
	 */
    public CustomInternalFrame[] getAllCustomFrames()
    {
        JInternalFrame[] allFrames = getAllFrames();
        CustomInternalFrame[] allCustomFrames = new CustomInternalFrame[allFrames.length];
        for(int i = 0; i < allFrames.length; i++)
        {
            allCustomFrames[i] = (CustomInternalFrame)allFrames[i];
        }
        
        return allCustomFrames;
    }

	/**
	 * This will tile all the windows in the most fantastically efficient manner 
	 * imaginable (as far as desktop space in concerned of course; this may not be the
	 * most efficient imaginable as far as excecution in concerned). It will skip
	 * windows which are iconized.
	 */	
	class TileAction extends PerformableAction {
	    public TileAction() {
		super("Tile");
	    }		
	
	    public void perform() {
			if (Prefs.horizTile.getBoolean()) {
			    horizontalTile();
			}
			else {
			    verticalTile();
			}
	    }

	    public void actionPerformed(ActionEvent e) {
			perform();
	    }
	}
	
	class ReTileAction extends AbstractAction {
	    public ReTileAction() {
			super("ReTile");
	    }		
	
	    public void perform() {	
			if (Prefs.horizTile.getBoolean()) {
			    verticalTile();
			}
			else {
			    horizontalTile();
			}
	    }
		
	    public void actionPerformed(ActionEvent e) {
			perform();
	    }
	}

}
