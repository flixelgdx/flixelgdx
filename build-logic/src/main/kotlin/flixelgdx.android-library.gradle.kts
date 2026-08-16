/**
 * Convention for FlixelGDX Android library modules.
 *
 * <p>Applies {@code flixelgdx.java-base} for shared IDE and Spotless setup, then layers on:
 * the Android library plugin, a {@code javadoc} task over the release variant, and the Vanniktech
 * Maven publish pipeline targeting Sonatype Central Portal.
 */

import com.android.build.gradle.LibraryExtension

plugins {
  id("flixelgdx.java-base")
  id("com.android.library")
  id("com.vanniktech.maven.publish")
}

// The android plugin does not register a javadoc task, so we add one here against the release
// variant. AGP resolves bootClasspath and the compile configuration only after evaluation.
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
