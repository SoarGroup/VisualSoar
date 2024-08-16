import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

version = "4.6.22"

plugins {
  java
  application
  id("org.beryx.runtime") version "1.13.1"
}

repositories {
  // Use Maven Central for resolving dependencies.
  mavenCentral()
}

dependencies {
  testImplementation(libs.junit.jupiter)
  testImplementation("org.mockito:mockito-core:5.12.0")

  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  // load all the jars in the lib/ folder
  implementation(fileTree("lib") {
    include("*.jar")
  })
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
        "Class-Path" to ". java/sml.jar bin/java/sml.jar lib/sml.jar"
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
}

application {
  mainClass = "edu.umich.soar.visualsoar.VisualSoar"
}

tasks.named<Test>("test") {
  // Use JUnit Platform for unit tests.
  useJUnitPlatform()
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
