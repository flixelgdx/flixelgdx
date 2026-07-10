// Desktop backend for the FlixelGDX video extension. Bridges libvlc into the framework through
// JNA-registered JNI bindings and packages (optionally) the libvlc native libraries that
// downloadVlcNatives fetches from upstream.

import java.io.OutputStream
import java.net.URI
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.GZIPInputStream
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.tukaani.xz.XZInputStream

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        // Pure-JVM extraction of the upstream archives: 7z (Windows SDK .lib files)
        // and Debian .deb (ar + tar.xz) so the natives task needs no external tools
        // for Windows and Linux. Only the macOS .dmg needs 7z or hdiutil on PATH.
        classpath("org.apache.commons:commons-compress:${libs.versions.commonsCompress.get()}")
        classpath("org.tukaani:xz:${libs.versions.xz.get()}")
    }
}

plugins {
    id("flixelgdx.java-library")
}

val vlcVersion = libs.versions.vlc.get()

val vlcDownloads = listOf(
    mapOf(
        "name" to "vlc-${vlcVersion}-win64.zip",
        "url" to "https://download.videolan.org/pub/videolan/vlc/${vlcVersion}/win64/vlc-${vlcVersion}-win64.zip",
        "sha256" to "992d19dbd0b8a7cde9167d2f7780b1ef6f92acc8a71acfa736101a21f35181e1"
    ),
    mapOf(
        "name" to "vlc-${vlcVersion}-win64.7z",
        "url" to "https://download.videolan.org/pub/videolan/vlc/${vlcVersion}/win64/vlc-${vlcVersion}-win64.7z",
        "sha256" to "eb4fd8a28291da73608c733786a09610fea865fbe94113bcb60b91c1ebb8404a"
    ),
    mapOf(
        "name" to "vlc-${vlcVersion}-universal.dmg",
        "url" to "https://download.videolan.org/pub/videolan/vlc/${vlcVersion}/macosx/vlc-${vlcVersion}-universal.dmg",
        "sha256" to "56ee657c3aaf5c71b4ab7d6e4f4a77f6eca54633e0bf42a93b8116eb1d1f6ec9"
    ),
    mapOf(
        "name" to "libvlc5_${vlcVersion}-0+deb12u1_amd64.deb",
        "url" to "https://deb.debian.org/debian/pool/main/v/vlc/libvlc5_${vlcVersion}-0%2Bdeb12u1_amd64.deb",
        "sha256" to "07ad5c61dc41acf29c485224accf457b7632e68a910eee21badf30213e6ab359"
    ),
    mapOf(
        "name" to "libvlccore9_${vlcVersion}-0+deb12u1_amd64.deb",
        "url" to "https://deb.debian.org/debian/pool/main/v/vlc/libvlccore9_${vlcVersion}-0%2Bdeb12u1_amd64.deb",
        "sha256" to "a3114b86450777e4cbbd4620419b74d189367e5f5026286cc74c39c9e759bfa7"
    ),
    mapOf(
        "name" to "vlc-plugin-base_${vlcVersion}-0+deb12u1_amd64.deb",
        "url" to "https://deb.debian.org/debian/pool/main/v/vlc/vlc-plugin-base_${vlcVersion}-0%2Bdeb12u1_amd64.deb",
        "sha256" to "38b953a2a6355c5ba75e3e5d2015100d793fbf5a00211aaa78b2d705a9547cb1"
    ),
    mapOf(
        "name" to "vlc-plugin-video-output_${vlcVersion}-0+deb12u1_amd64.deb",
        "url" to "https://deb.debian.org/debian/pool/main/v/vlc/vlc-plugin-video-output_${vlcVersion}-0%2Bdeb12u1_amd64.deb",
        "sha256" to "de1d62487161efc62305b6e84cd364e71f109125d601401ee824d3291813e6e2"
    )
)

// Plugin categories no game playback path needs (interface, scripting, streaming out,
// discovery). Dropping them roughly halves the Windows natives payload.
val vlcPluginBlocklist = listOf(
    "gui", "lua", "control", "services_discovery", "visualization",
    "mux", "stream_out", "access_output", "meta_engine", "keystore", "logger"
)

val vlcDownloadDir = layout.buildDirectory.dir("vlc-downloads")
val vlcNativesDir = layout.projectDirectory.dir("natives")

fun sha256Of(f: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    f.inputStream().use { stream ->
        val buf = ByteArray(65536)
        var n: Int
        while (stream.read(buf).also { n = it } > 0) {
            digest.update(buf, 0, n)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

fun fetchVlcArtifact(spec: Map<String, String>, dir: File): File {
    val target = File(dir, spec["name"]!!)
    if (target.exists() && sha256Of(target) == spec["sha256"]) {
        return target
    }
    dir.mkdirs()
    logger.lifecycle("Downloading ${spec["url"]}")
    URI(spec["url"]!!).toURL().openStream().use { input ->
        target.outputStream().use { output -> input.copyTo(output) }
    }
    val actual = sha256Of(target)
    if (actual != spec["sha256"]) {
        target.delete()
        throw GradleException("Checksum mismatch for ${spec["name"]}: expected ${spec["sha256"]}, got $actual")
    }
    return target
}

// Extracts a Debian package's data.tar.* into destDir using commons-compress only.
fun extractDeb(deb: File, destDir: File) {
    deb.inputStream().use { fis ->
        val ar = ArArchiveInputStream(fis)
        var entry = ar.nextEntry
        while (entry != null) {
            if (entry.name.startsWith("data.tar")) {
                val dataStream = when {
                    entry.name.endsWith(".xz") -> XZInputStream(ar)
                    entry.name.endsWith(".gz") -> GZIPInputStream(ar)
                    else -> ar
                }
                val tar = TarArchiveInputStream(dataStream)
                var tarEntry = tar.nextEntry
                while (tarEntry != null) {
                    if (tarEntry.isFile) {
                        val out = File(destDir, tarEntry.name)
                        out.parentFile.mkdirs()
                        out.outputStream().use { tar.copyTo(it) }
                    }
                    tarEntry = tar.nextEntry
                }
            }
            entry = ar.nextEntry
        }
    }
}

@Suppress("DEPRECATION")
fun extractLibsFrom7z(sevenZip: File, pathPrefix: String, destDir: File) {
    // Deprecated single-File constructor used intentionally: Gradle's own (older) commons-compress
    // wins on the buildscript classpath and may not have the newer Builder API.
    val sz = SevenZFile(sevenZip)
    try {
        var entry = sz.nextEntry
        while (entry != null) {
            val normalizedName = entry.name.replace('\\', '/')
            if (!entry.isDirectory && normalizedName.startsWith(pathPrefix)) {
                val out = File(destDir, normalizedName.substring(pathPrefix.length))
                out.parentFile.mkdirs()
                val content = ByteArray(entry.size.toInt())
                var off = 0
                while (off < content.size) {
                    off += sz.read(content, off, content.size - off)
                }
                out.writeBytes(content)
            }
            entry = sz.nextEntry
        }
    } finally {
        sz.close()
    }
}

tasks.register("downloadVlcNatives") {
    group = "flixelgdx"
    description = "Downloads libvlc $vlcVersion natives (Windows, macOS, Linux) into natives/."
    outputs.dir(vlcNativesDir)

    doLast {
        val dlDir = vlcDownloadDir.get().asFile
        val outRoot = vlcNativesDir.asFile
        val files = vlcDownloads.associate { spec ->
            @Suppress("UNCHECKED_CAST")
            val typedSpec = spec as Map<String, String>
            typedSpec["name"]!! to fetchVlcArtifact(typedSpec, dlDir)
        }

        // Windows x64: runtime DLLs and plugins from the zip, SDK import libraries from the 7z.
        val winDir = File(outRoot, "windows-amd64")
        if (!File(winDir, "libvlc.dll").exists()) {
            logger.lifecycle("Extracting Windows natives...")
            val zipRoot = "vlc-$vlcVersion/"
            copy {
                from(zipTree(files["vlc-$vlcVersion-win64.zip"]!!)) {
                    include("${zipRoot}libvlc.dll", "${zipRoot}libvlccore.dll", "${zipRoot}plugins/**")
                    vlcPluginBlocklist.forEach { exclude("${zipRoot}plugins/$it/**") }
                    eachFile { path = path.substring(zipRoot.length) }
                    includeEmptyDirs = false
                }
                into(winDir)
            }
        }
        if (!File(winDir, "sdk/libvlc.lib").exists()) {
            logger.lifecycle("Extracting Windows SDK import libraries...")
            extractLibsFrom7z(files["vlc-$vlcVersion-win64.7z"]!!, "vlc-$vlcVersion/sdk/lib/", File(winDir, "sdk"))
        }

        // Linux x64: Debian builds of the same upstream release.
        val linuxDir = File(outRoot, "linux-amd64")
        if (!File(linuxDir, "libvlc.so.5").exists()) {
            logger.lifecycle("Extracting Linux natives...")
            val debStage = File(dlDir, "deb-stage")
            delete(debStage)
            vlcDownloads.filter { it["name"]!!.endsWith(".deb") }.forEach { spec ->
                @Suppress("UNCHECKED_CAST")
                extractDeb(files[spec["name"]]!!, debStage)
            }
            val usrLib = File(debStage, "usr/lib/x86_64-linux-gnu")
            copy {
                from(usrLib) {
                    include("libvlc.so*", "libvlccore.so*", "vlc/plugins/**", "vlc/libvlc_*.so")
                    vlcPluginBlocklist.forEach { exclude("vlc/plugins/$it/**") }
                    includeEmptyDirs = false
                }
                into(linuxDir)
            }
            // The .deb data tar carries libvlc.so.5 as a symlink; materialize real files for jar packaging.
            mapOf("libvlc.so.5" to "libvlc.so.5.6.1", "libvlccore.so.9" to "libvlccore.so.9.0.1").forEach { (link, real) ->
                val linkFile = File(linuxDir, link)
                val realFile = File(linuxDir, real)
                if (realFile.exists() && (!linkFile.exists() || linkFile.length() == 0L)) {
                    linkFile.delete()
                    linkFile.writeBytes(realFile.readBytes())
                }
            }
        }

        // macOS universal: needs a dmg-capable extractor (7z on any OS, hdiutil on macOS).
        val macDir = File(outRoot, "macos-universal")
        if (!File(macDir, "lib/libvlc.dylib").exists()) {
            val dmg = files["vlc-$vlcVersion-universal.dmg"]!!
            val sevenZip = listOf("7z", "7za").firstOrNull { tool ->
                try {
                    ProcessBuilder(tool).start().waitFor()
                    true
                } catch (e: Exception) {
                    false
                }
            }
            if (sevenZip != null) {
                logger.lifecycle("Extracting macOS natives with 7z...")
                val dmgStage = File(dlDir, "dmg-stage")
                delete(dmgStage)
                dmgStage.mkdirs()
                val appPath = "VLC media player/VLC.app/Contents/MacOS"
                val proc = ProcessBuilder(
                    sevenZip, "x", "-y", "-o${dmgStage.absolutePath}", dmg.absolutePath,
                    "$appPath/lib/*", "$appPath/plugins/*"
                ).start()
                proc.inputStream.use { }
                proc.errorStream.use { }
                proc.waitFor()
                val macSrc = File(dmgStage, appPath)
                if (File(macSrc, "lib").exists()) {
                    copy {
                        from(macSrc) { include("lib/**", "plugins/**") }
                        into(macDir)
                    }
                } else {
                    logger.warn("7z could not read the dmg layout; macOS natives were skipped.")
                }
            } else if (System.getProperty("os.name", "").lowercase(Locale.ROOT).contains("mac")) {
                logger.lifecycle("Extracting macOS natives with hdiutil...")
                val mountPoint = File(dlDir, "dmg-mount")
                mountPoint.mkdirs()
                ProcessBuilder("hdiutil", "attach", dmg.absolutePath, "-nobrowse", "-readonly", "-mountpoint", mountPoint.absolutePath).start().waitFor()
                try {
                    copy {
                        from(File(mountPoint, "VLC.app/Contents/MacOS")) { include("lib/**", "plugins/**") }
                        into(macDir)
                    }
                } finally {
                    ProcessBuilder("hdiutil", "detach", mountPoint.absolutePath).start().waitFor()
                }
            } else {
                logger.warn("No 7z/hdiutil found; macOS natives were skipped. Install p7zip to extract the dmg.")
            }
        }

        logger.lifecycle("libvlc $vlcVersion natives ready under $outRoot")
    }
}

val hostVlcPlatform = run {
    val os = System.getProperty("os.name", "").lowercase(Locale.ROOT)
    when {
        os.contains("win") -> "windows-amd64"
        os.contains("mac") -> "macos-universal"
        else -> "linux-amd64"
    }
}

fun vlcNativesComplete(platformDir: File): Boolean {
    if (!platformDir.isDirectory) return false
    fun hasLibIn(dir: File) = dir.isDirectory && (dir.listFiles()?.any { it.name.startsWith("libvlc") } ?: false)
    val hasLib = hasLibIn(platformDir) || hasLibIn(File(platformDir, "lib"))
    val pluginsDir = if (File(platformDir, "plugins").isDirectory) File(platformDir, "plugins")
    else File(platformDir, "vlc/plugins")
    val hasPlugins = pluginsDir.isDirectory && (pluginsDir.listFiles()?.isNotEmpty() ?: false)
    return hasLib && hasPlugins
}

if ((findProperty("skipVlcNatives") ?: "false") != "true") {
    tasks.processResources {
        dependsOn("downloadVlcNatives")
        // Fail loudly if the host platform's natives did not make it into natives/. Without this
        // the jar is packaged silently and the missing libraries only surface at runtime as an
        // unavailable video, which is exactly the failure this module tries to prevent.
        doFirst {
            val hostDir = File(vlcNativesDir.asFile, hostVlcPlatform)
            if (!vlcNativesComplete(hostDir)) {
                throw GradleException(
                    "flixelgdx-video-lwjgl3: libvlc natives for this platform ($hostVlcPlatform) are " +
                        "missing or incomplete under $hostDir. Run './gradlew " +
                        ":flixelgdx-video-lwjgl3:downloadVlcNatives' (needs network access), or pass " +
                        "-PskipVlcNatives=true to build a slim jar that relies on a system VLC install."
                )
            }
        }
        from(vlcNativesDir) {
            into("org/flixelgdx/video/natives")
            exclude("**/sdk/**")
        }
    }
}

dependencies {
    api(project(":flixelgdx-video-core"))
    api(libs.jna)
    implementation(project(":flixelgdx-lwjgl3"))
    implementation(libs.gdx.backend.lwjgl3)
    implementation(libs.jetbrains.annotations)

    // Provides the Feature interface and RuntimeJNIAccess used by FlixelVideoGraalFeature.
    // Compile-only: the class runs inside native-image at build time, not in user runtimes.
    compileOnly(libs.graalvm.nativeimage)
}
