/**
 * Convention for FlixelGDX Android library modules.
 *
 * <p>Applies {@code flixelgdx.java-base} for shared IDE and Spotless setup, then layers on:
 * the Android library plugin and the Vanniktech Maven publish pipeline targeting Sonatype
 * Central Portal.
 */

import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("flixelgdx.java-base")
    id("com.android.library")
    id("com.vanniktech.maven.publish")
}

// JitPack rewrites Gradle module metadata and drops classifier compatibility data; omit .module
// files so metadata is sourced from the POM alone.
tasks.matching { it.name.startsWith("generateMetadataFileFor") }.configureEach {
    enabled = false
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

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
