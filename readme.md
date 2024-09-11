# VisualSoar

This is an IDE for [Soar](https://soar.eecs.umich.edu/) agent development.

## Developing

To compile VisualSoar, you must put a copy of the latest version of sml.lib (from the SoarSuite) into the lib directory.

The project is defined and managed via [Gradle](https://gradle.org/). To open in IntelliJ,
simply open the project root folder, and IntelliJ will ask if you'd like to import the gradle
project that it found. Select "yes" and you'll be able to run and debug files normally, as well
as run the Gradle targets from IntelliJ. Eclipse understands Gradle projects, as well.

### Building the jar for release with Soar

The slim jar we distribute with Soar:

    ./gradlew jar

The jar is then located under `./build/libs`. It will have a date suffix that needs to be removed for inclusion with the Soar release.

There is also an Ant script that does the same; it is outdated, however, and should not be used for distributing.
We will likely remove it in the future:

    ant build

### JLink application image

To use jlink to create a zip of the containing the application, a custom JRE, and appropriate start scripts:

    ./gradle runtimeZip

### JPackage standalone application and installer

To create the standalone app for your OS:

    ./gradlew jpackageImage

To create an installer for your OS:

    ./gradlew jpackageImage

Unfortunately, these are not able to connect with Soar unless the user has placed the SOAR_HOME on their
system `path`. This is due to a confluence of missing features:

* To load the SML native library, Soar's Java bindings call `System.loadLibrary(name)`, which requires the library to be in `java.library.path`. This is called in a static block in the `sml` package, and I'm not sure how to mock that out.
* Java doesn't allow modifying the library path at runtime
* JPackage doesn't support using environment variables to form JVM arguments.
  * Also, it's not straightforward to load the user's environment variables in a standalone application; on Mac, for example, running a .app file doesn't run my .bash_profile first, etc.

I intend to revisit this after fixing the first point above in Soar: https://github.com/SoarGroup/Soar/issues/491.

### Running

At runtime you need to have your SOAR_HOME environment variable set to the path to Soar's `bin/` directory.
The main class is called VisualSoar.

### Testing

    ./gradlew test

### Formatting

The project does not have a consistent style, but a proposal for now is to use
[google-java-format](https://github.com/google/google-java-format). There are plugins for Eclipse and IntelliJ.

We have also included a .editorconfig in the project, which is supported by all major editors.
