package edu.umich.soar.visualsoar.parser;

import java.util.*;

public class ActionSide {
	// Data Members
	private final List<Action> d_actions = new LinkedList<>();
	
	// Constructors
	public ActionSide() {}
	
	// Accessors
	public final void add(Action action) {
		d_actions.add(action);
	}
	
	public final Iterator<Action> getActions() {
		return d_actions.iterator();
	}	
}
