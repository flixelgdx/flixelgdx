import com.android.build.gradle.LibraryExtension

plugins {
  id("flixelgdx.java-base")
  id("com.android.library")
  id("com.vanniktech.maven.publish")
  checkstyle
}

checkstyle {
  toolVersion = "10.21.0"
  configDirectory.set(rootProject.layout.projectDirectory.dir("gradle/checkstyle"))
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(17)
  }
}

// The Checkstyle plugin only creates per-source-set tasks for Java source sets, which Android
// modules do not have, so we register checkstyleMain by hand to match the other modules.
afterEvaluate {
  val android = extensions.getByType(LibraryExtension::class.java)
  val checkstyleMain = tasks.register("checkstyleMain", Checkstyle::class.java) {
    group = "verification"
    description = "Runs Checkstyle on the Android main source set."
    source(android.sourceSets.getByName("main").java.srcDirs)
    include("**/*.java")
    classpath = files()
  }
  tasks.named("check") {
    dependsOn(checkstyleMain)
  }
}

afterEvaluate {
  val android = extensions.getByType(LibraryExtension::class.java)
  tasks.register("javadoc", Javadoc::class.java) {
    group = "documentation"
    description = "Generates Javadoc for the Android release variant."
    source(android.sourceSets.getByName("main").java.srcDirs)
    classpath = configurations.getByName("releaseCompileClasspath")
      .plus(files(android.bootClasspath))
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).apply {
      charSet = "UTF-8"
      docEncoding = "UTF-8"
      memberLevel = JavadocMemberLevel.PUBLIC
      links("https://docs.oracle.com/en/java/javase/17/docs/api/")
      if (JavaVersion.current().isJava9Compatible) {
        addStringOption("Xdoclint:all,-missing", "-quiet")
        addStringOption("Werror")
      }
    }
    isFailOnError = true
  }
}

tasks.matching { it.name.startsWith("generateMetadataFileFor") }.configureEach {
  enabled = false
}

mavenPublishing {
  publishToMavenCentral()

  val hasSigning = findProperty("flixel.signing.enabled")?.toString() == "true"
    || findProperty("signing.keyId") != null
    || findProperty("signingInMemoryKeyId") != null
  if (hasSigning) {
    signAllPublications()
  }

  coordinates(project.group as String, project.name, project.version as String)

  pom {
    name = rootProject.property("pomName") as String
    description = rootProject.property("pomDescription") as String
    url = rootProject.property("pomUrl") as String
    licenses {
      license {
        name = rootProject.property("pomLicenseName") as String
        url = rootProject.property("pomLicenseUrl") as String
        distribution = "repo"
      }
    }
    developers {
      developer {
        id = rootProject.property("pomDeveloperId") as String
        name = rootProject.property("pomDeveloperName") as String
      }
    }
    scm {
      connection = rootProject.property("pomScmConnection") as String
      developerConnection = rootProject.property("pomScmDeveloperConnection") as String
      url = rootProject.property("pomScmUrl") as String
    }
  }
}
