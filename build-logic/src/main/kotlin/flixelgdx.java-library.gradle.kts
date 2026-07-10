/**
 * Convention for FlixelGDX Java library modules.
 *
 * <p>Applies {@code flixelgdx.java-base} for shared setup, then layers on:
 * Java 17 toolchain, JPMS module attributes for Eclipse, Javadoc settings with doclint,
 * and the Vanniktech Maven publish pipeline targeting Sonatype Central Portal.
 */

import org.gradle.plugins.ide.eclipse.model.Library

plugins {
  id("flixelgdx.java-base")
  `java-library`
  id("com.vanniktech.maven.publish")
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(17)
  }
}

// Mark all library JARs as JPMS modules in Eclipse so automatic modules from libGDX resolve
// consistently with flixelgdx-core's module-path compile setup.
eclipse.classpath.file {
  whenMerged { classpath: org.gradle.plugins.ide.eclipse.model.Classpath ->
    classpath.entries.filterIsInstance<Library>().forEach {
      it.entryAttributes["module"] = "true"
    }
  }
}

tasks.withType<Javadoc>().configureEach {
  options.encoding = "UTF-8"
  (options as StandardJavadocDocletOptions).apply {
    charSet = "UTF-8"
    docEncoding = "UTF-8"
    memberLevel = JavadocMemberLevel.PUBLIC
    links("https://docs.oracle.com/en/java/javase/17/docs/api/")
    if (JavaVersion.current().isJava9Compatible) {
      addStringOption("Xdoclint:all,-missing", "-quiet")
    }
  }
  setFailOnError(true)
  modularity.inferModulePath = false
}

// JitPack rewrites Gradle module metadata and drops classifier compatibility data, causing
// consumers to resolve wrong libGDX variants. POMs stay correct; omit .module files so
// metadata is sourced from the POM alone.
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
