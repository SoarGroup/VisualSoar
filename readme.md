# VisualSoar

This is an IDE for [Soar](https://soar.eecs.umich.edu/) agent development.

## Developing

To compile VisualSoar, you must put a copy of the latest version of sml.lib (from the SoarSuite) into the lib directory.

The project is defined and managed via [Gradle](https://gradle.org/). Eclipse and
IntelliJ can both use this project type.

### Building

The slim jar we distribute with Soar:

    ./gradlew jar

There is also an Ant script that does the same (we will likely remove it in the future):

    ant build

### Running

At runtime you need to have your SOAR_HOME environment variable set to the path to Soar's `bin/` directory.
The main class is called VisualSoar.

### Testing

    ./gradlew test

### Formatting

The project does not have a consistent style, but a proposal for now is to use
[google-java-format](https://github.com/google/google-java-format). There are plugins for Eclipse and IntelliJ.

We have also included a .editorconfig in the project, which is supported by all major editors.
