import org.javacc.plugin.gradle.javacc.CompileJavaccTask
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

version = "4.6.22"

plugins {
  java
  application
  id("org.beryx.runtime") version "1.13.1"
  id("org.javacc.javacc") version "4.0.1"
}

repositories {
  // Use Maven Central for resolving dependencies.
  mavenCentral()
}

dependencies {
  implementation("org.jetbrains:annotations:24.0.0")
  // load all the jars in the lib/ folder
  implementation(fileTree("lib") {
    include("*.jar")
  })

  testImplementation(libs.junit.jupiter)
  testImplementation("org.mockito:mockito-core:5.12.0")
//  testImplementation("org.junit.jupiter:junit-jupiter-params")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// For loading the SML JNI library
tasks.withType<JavaExec> {
  systemProperty("java.library.path", System.getenv("SOAR_HOME"))
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<JavaCompile> {
  options.encoding = "UTF-8"
}

// make an executable jar that will depend on other jars being available in the classpath
tasks.jar {
  manifest {
    attributes(
      mapOf(
        "Title" to "${project.name} ${project.version}",
        "Vendor" to "University of Michigan",
        "Date" to LocalDate.now().toString(),
        "Version" to "${project.version}",
        "Copyright" to "(c) The Regents of the University of Michigan, 2024",
        "Main-Class" to application.mainClass.get(),
        // Dynamically include all JARs in the lib/ directory
        "Class-Path" to (file("lib").takeIf { it.exists() && it.isDirectory }
          ?.listFiles()
          ?.filter { it.extension == "jar" }
          ?.joinToString(" ") { "java/${it.name}" } ?: "")
      )
    )
  }
  from(sourceSets.main.get().output)
  dependsOn(configurations.runtimeClasspath)
//	Do not include runtime dependencies in the jar (we want a slim jar, not a fat one):
//	from({
//		configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
//	})
}

tasks.test {
  useJUnitPlatform()

  reports {
    junitXml.required.set(true)
    junitXml.outputLocation.set(file("${layout.buildDirectory.get()}/reports/test-results"))
  }

  testLogging {
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
  }
}

application {
  mainClass = "edu.umich.soar.visualsoar.VisualSoar"
}

tasks.named<Test>("test") {
  // Use JUnit Platform for unit tests.
  useJUnitPlatform()
}

//////////////////////////////
// JavaCC-based Soar Parser //
//////////////////////////////

tasks.named<CompileJavaccTask>("compileJavacc") {
  inputDirectory = file("src/main/javacc")
  outputDirectory = file("src/main/java/edu/umich/soar/visualsoar/parser")
}


////////////////////////////////////
// JVM-included application build //
////////////////////////////////////

runtime {
  launcher {
    // Runtime plugin replaces {{}} with environment variables in generated scripts.
    // Works for JLink but not jpackage! Therefore any generated standalone apps or installers
    // won't be able to connect to soar unless SOAR_HOME in is in the users path already :(
    // See readme for details.
    jvmArgs.add("-Djava.library.path={{SOAR_HOME}}")
    noConsole = true
  }
}


/////////////////////////////////////////////////////////////////
// Generate version/datetime string to show in the application //
/////////////////////////////////////////////////////////////////

val generateVersionFile by tasks.registering {
  val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm:ss 'UTC'")
  dateFormat.timeZone = TimeZone.getTimeZone("UTC")
  val date = dateFormat.format(Date())
  val versionString = "${project.version} ($date)"
  val templateContext = mapOf("versionString" to versionString)
  // When versionString changes, the task is out of date
  inputs.properties(templateContext)
  // without doLast, the file is not consistently generated
  doLast {
    val versionFile = layout.buildDirectory.file("generated/resources/versionString.txt").get().asFile
    versionFile.parentFile.mkdirs()
    versionFile.writeText(versionString)
    logger.lifecycle("Generating version file at: ${versionFile.absolutePath}")
  }
}

tasks.withType<JavaCompile> {
  dependsOn(generateVersionFile)
}

// Add the extra resource directory to the main source set
sourceSets.main {
  resources.srcDir(layout.buildDirectory.dir("generated/resources"))
}
