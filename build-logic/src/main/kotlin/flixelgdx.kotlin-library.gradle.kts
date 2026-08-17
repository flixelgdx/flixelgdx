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

  // Build against an older Kotlin baseline than the compiler so the published artifact stays
  // consumable by projects on older Kotlin toolchains. The compiler is 2.2.x (required by Gradle 9),
  // but emitting 2.0 metadata and depending on the 2.0 standard library keeps the module readable by
  // any consumer on Kotlin 2.0 or newer.
  coreLibrariesVersion = "2.0.21"

  compilerOptions {
    apiVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
    languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
  }
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
