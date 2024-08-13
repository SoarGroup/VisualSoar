import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.time.LocalDate

plugins {
	java
	application
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

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(11)
	}
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
