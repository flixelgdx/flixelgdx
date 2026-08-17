/**
 * Convention for FlixelGDX Kotlin library modules.
 *
 * <p>Applies {@code flixelgdx.java-base} for shared setup (coordinates, repositories, Spotless,
 * IDE metadata), then layers on: the Kotlin JVM plugin targeting a Java 17 toolchain, the
 * {@code java-library} surface area, and the Vanniktech Maven publish pipeline used by every
 * other published module.
 *
 * <p>Unlike {@code flixelgdx.java-library} this convention does not configure the Javadoc task,
 * since Kotlin sources do not produce Javadoc through it.
 */

plugins {
  id("flixelgdx.java-base")
  id("org.jetbrains.kotlin.jvm")
  `java-library`
  id("com.vanniktech.maven.publish")
}

kotlin {
  jvmToolchain(17)
}

// JitPack rewrites Gradle module metadata and drops classifier compatibility data, causing
// consumers to resolve wrong variants. POMs stay correct; omit .module files so metadata is
// sourced from the POM alone. Mirrors flixelgdx.java-library.
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
