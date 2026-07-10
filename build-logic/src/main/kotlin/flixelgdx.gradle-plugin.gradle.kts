/**
 * Convention for FlixelGDX Gradle plugin modules.
 *
 * <p>Applies {@code flixelgdx.java-base} for shared setup, then layers on:
 * Java 17 toolchain, the {@code java-gradle-plugin} surface, and the Vanniktech Maven publish
 * pipeline targeting Sonatype Central Portal.
 */

plugins {
  id("flixelgdx.java-base")
  `java-gradle-plugin`
  id("com.vanniktech.maven.publish")
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(17)
  }
}

// JitPack rewrites Gradle module metadata and drops classifier compatibility data; omit .module
// files so metadata is sourced from the POM alone.
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
