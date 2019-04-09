plugins {
	`java-library`
	eclipse
	idea
	checkstyle
}

val mavenizedProjects: List<Project> by rootProject.extra
val buildDate: String by rootProject.extra
val buildTime: String by rootProject.extra
val buildRevision: Any by rootProject.extra
val builtByValue: String by rootProject.extra

val shadowed by configurations.creating
val extension = extensions.create<JavaLibraryExtension>("javaLibrary")

sourceSets {
	main {
		compileClasspath += shadowed
	}
	create("nine") {
		java {
			source(objects.sourceDirectorySet("main", "main without ModuleUtils").apply {
				setSrcDirs(sourceSets.main.get().java.srcDirs)
				exclude("**/ModuleUtils.java")
			})
		}
		compileClasspath += configurations["compileClasspath"] + shadowed + sourceSets["main"].output
	}
	test {
		runtimeClasspath += shadowed
	}
}

eclipse {
	classpath {
		plusConfigurations.add(shadowed)
	}
}

idea {
	module {
		scopes["PROVIDED"]!!["plus"]!!.add(shadowed)
	}
}

tasks.javadoc {
	classpath += shadowed
}

tasks.checkstyleMain {
	classpath += shadowed
}

if (project in mavenizedProjects) {

	apply(from = "$rootDir/gradle/publishing.gradle.kts")

	tasks.javadoc {
		options {
			memberLevel = JavadocMemberLevel.PROTECTED
			header = project.name
			encoding = "UTF-8"
			(this as StandardJavadocDocletOptions).apply {
				addBooleanOption("Xdoclint:html,syntax", true)
				addBooleanOption("html5", true)
				addBooleanOption("-no-module-directories", true)
				addMultilineStringsOption("tag").value = listOf(
						"apiNote:a:API Note:",
						"implNote:a:Implementation Note:"
				)
				use(true)
				noTimestamp(true)
			}
		}
	}

	val sourcesJar by tasks.creating(Jar::class) {
		dependsOn(tasks.classes)
		archiveClassifier.set("sources")
		from(sourceSets.main.get().allSource)
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	}

	val javadocJar by tasks.creating(Jar::class) {
		archiveClassifier.set("javadoc")
		from(tasks.javadoc)
	}

	tasks.withType<Jar> {
		from(rootDir) {
			include("LICENSE.md", "LICENSE-notice.md")
			into("META-INF")
		}
		into("META-INF/versions/9") {
			from(sourceSets.named<SourceSet>("nine").get().output) {
				includeEmptyDirs = false
				include("module-info.class")
				include("**/ModuleUtils*.class")
			}
		}
	}

	configure<PublishingExtension> {
		publications {
			named<MavenPublication>("maven") {
				from(components["java"])
				artifact(sourcesJar)
				artifact(javadocJar)
				pom {
					description.set(provider { "Module \"${project.name}\" of JUnit 5." })
				}
			}
		}
	}

} else {
	tasks {
		jar {
			enabled = false
		}
		javadoc {
			enabled = false
		}
	}
}

normalization {
	runtimeClasspath {
		// Ignore the JAR manifest when checking whether runtime classpath have changed
		// because it contains timestamps and the commit checksum. This is used when
		// checking whether a test task is up-to-date or can be loaded from the build cache.
		ignore("/META-INF/MANIFEST.MF")
	}
}

tasks.jar {
	manifest {
		attributes(
				"Created-By" to "${System.getProperty("java.version")} (${System.getProperty("java.vendor")} ${System.getProperty("java.vm.version")})",
				"Built-By" to builtByValue,
				"Build-Date" to buildDate,
				"Build-Time" to buildTime,
				"Build-Revision" to buildRevision,
				"Specification-Title" to project.name,
				"Specification-Version" to (project.version as String).substringBefore('-'),
				"Specification-Vendor" to "junit.org",
				"Implementation-Title" to project.name,
				"Implementation-Version" to project.version,
				"Implementation-Vendor" to "junit.org"
		)
	}
}

afterEvaluate {
	val automaticModuleName = extension.automaticModuleName
	if (automaticModuleName != null) {
		tasks.jar {
			manifest {
				attributes("Automatic-Module-Name" to automaticModuleName)
			}
		}
	}
	if (!sourceSets.named<SourceSet>("nine").get().output.isEmpty) {
		tasks.jar {
			manifest {
				attributes("Multi-Release" to "true")
			}
		}
	}
}

tasks.compileJava {
	options.encoding = "UTF-8"

	// See: https://docs.oracle.com/en/java/javase/11/tools/javac.html
	options.compilerArgs.addAll(listOf(
			"-Xlint", // Enables all recommended warnings.
			"-Werror" // Terminates compilation when warnings occur.
	))
}

afterEvaluate {
	tasks {
		compileJava {
			sourceCompatibility = extension.mainJavaVersion.majorVersion
			targetCompatibility = extension.mainJavaVersion.majorVersion // needed by asm
			// --release release
			// Compiles against the public, supported and documented API for a specific VM version.
			// Supported release targets are 6, 7, 8, 9, 10, and 11.
			// Note that if --release is added then -target and -source are ignored.
			options.compilerArgs.addAll(listOf("--release", extension.mainJavaVersion.majorVersion))
		}
		tasks.named<JavaCompile>("compileNineJava") {
			sourceCompatibility = extension.nineJavaVersion.majorVersion
			targetCompatibility = extension.nineJavaVersion.majorVersion
			options.encoding = "UTF-8"
			options.compilerArgs.addAll(listOf(
					"-Xlint",
					"--release", extension.nineJavaVersion.majorVersion
					// "-verbose"
			))
		}
		compileTestJava {
			options.encoding = "UTF-8"
			sourceCompatibility = extension.testJavaVersion.majorVersion
			targetCompatibility = extension.testJavaVersion.majorVersion

			// See: https://docs.oracle.com/en/java/javase/11/tools/javac.html
			options.compilerArgs.addAll(listOf(
					"-Xlint", // Enables all recommended warnings.
					"-Xlint:-overrides", // Disables "method overrides" warnings.
					"-parameters" // Generates metadata for reflection on method parameters.
			))
		}
	}
}

checkstyle {
	toolVersion = Versions.checkstyle
	configDir = rootProject.file("src/checkstyle")
}
tasks {
	checkstyleMain {
		configFile = rootProject.file("src/checkstyle/checkstyleMain.xml")
	}
	tasks.named<Checkstyle>("checkstyleNine") {
		configFile = rootProject.file("src/checkstyle/checkstyleMain.xml")
		exclude("module-info.java")
	}
	checkstyleTest {
		configFile = rootProject.file("src/checkstyle/checkstyleTest.xml")
	}
}
