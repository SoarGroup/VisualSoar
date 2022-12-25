package edu.umich.soar.visualsoar.misc;

/**
 * This class manages/encapsulates working with templates
 * <p>
 * Refactored by Dave Ray... this is pretty boring now because most of its
 * functionality has been moved into the Template class...
 *
 * @author Brad Jones Jon Bauman
 */

public class TemplateManager {


    ///////////////////////////////////
    // Data Members
    ///////////////////////////////////
    private Template rootTemplate;

    ///////////////////////////////////
    // Accessors
    //////////////////////////////////
    public Template getRootTemplate() {
        return rootTemplate;
    }

    ////////////////////////////////////////////////////////////
    // Modifiers
    ////////////////////////////////////////////////////////////

    /**
     * Loads in files from the jar file resources
     */
    public void load() {
        rootTemplate = Template.loadFromJar();
    }
}
