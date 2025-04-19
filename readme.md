# VisualSoar

This is an IDE for [Soar](https://soar.eecs.umich.edu/) agent development. Please see the [user manual](./doc/usersman/VisualSoar_UsersManual.docx) for more information on how to use it.

## Soar Project Organization

A Soar project is organized into a hierarchy based on the agent's operators and sub-operators. VisualSoar then links the Soar source in these files to entries in a datamap, which is a specification of the structure and contents of working memory. Soar productions can then be automatically checked for errors.

### Project JSON Format

The project JSON format's canonical definition is in POJO classes processed by [Jackson](https://github.com/FasterXML/jackson). However, if you would like to work with the JSON in other tools, the format and contents are documented thoroughly in a JSON schema: [`project_schema.json`](./doc/project_schema.json).

### Datamap Validation

Normally users will run datamap validation within the IDE, but it is also possible to run the validation from the command line by adding the parameters `--check productionsAgainstDatamap` and `--project <path to project file>`. This will check the productions in the Soar agent against the datamap and report any errors. You can also pass in `--json` to get JSON output suitable for processing in other tools. The JSON structure follows the [LSP specification for diagnostics](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#diagnostic).

To run this from the project's Gradle build, you must wrap the arguments in a `--args` parameter, like so:

```bash
./gradlew run --args="--check productionsAgainstDatamap --project <path to project file> --json"
```

## Developing

To compile VisualSoar, you must put a copy of the latest version of sml.lib (from the SoarSuite) into the lib directory.

The project is defined and managed via [Gradle](https://gradle.org/). To open in IntelliJ,
simply open the project root folder, and IntelliJ will ask if you'd like to import the gradle
project that it found. Select "yes" and you'll be able to run and debug files normally, as well
as run the Gradle targets from IntelliJ. Eclipse understands Gradle projects, as well.

### Soar Parser

The Soar parser used in VisualSoar is specified in `src/main/javacc/soarparser.jj`, and the Java code is then
generated via [JavaCC](https://javacc.github.io/javacc/).

To regenerate the required files, run the top-level `compileJavacc` gradle task.

The following classes are generated:

* ParseException.java
* SimpleCharStream.java
* SoarParser.java
* SoarParserConstants.java
* SoarParserTokenManager.java
* Token.java
* TokenMgrError.java

### Building the jar for release with Soar

The slim jar we distribute with Soar:

    ./gradlew jar

The jar is then located under `./build/libs`. It will have a date suffix that needs to be removed for inclusion with the Soar release.

There is also an Ant script that does the same; it is outdated, however, and should not be used for distributing.
We will likely remove it in the future:

    ant build

### JLink application image

To use jlink to create a zip of the containing the application, a custom JRE, and appropriate start scripts:

    ./gradlew runtimeZip

### JPackage standalone application and installer

To create the standalone app and installer for your OS:

    ./gradlew jpackage

Unfortunately, the resulting application is not able to connect with Soar unless the user has placed the SOAR_HOME on
their system `path`. This is due to a confluence of missing features:

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
