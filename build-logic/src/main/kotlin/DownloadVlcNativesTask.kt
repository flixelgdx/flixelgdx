import org.apache.commons.compress.archivers.ar.ArArchiveInputStream
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.tukaani.xz.XZInputStream
import java.io.File
import java.net.URI
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.GZIPInputStream
import javax.inject.Inject

/**
 * Downloads and extracts libvlc native libraries for Windows, Linux, and macOS into a project
 * directory so they can be packaged into the flixelgdx-video-lwjgl3 JAR.
 *
 * <p>All archive extraction (zip, 7z, deb) is done in-process using commons-compress and xz-java,
 * so no external tools are required on Windows or Linux. macOS extraction falls back to the
 * system 7z or hdiutil when available.
 *
 * <p>The task is incremental: if {@link #nativesDir} already contains the expected library files
 * for a given platform, that platform's extraction is skipped.
 */
abstract class DownloadVlcNativesTask @Inject constructor(
    private val fs: FileSystemOperations,
    private val archives: ArchiveOperations
) : DefaultTask() {

  /** The VLC version string to download, for example {@code "3.0.23"}. */
  @get:Input
  abstract val vlcVersion: Property<String>

  /**
   * Ordered list of download specs, each a map with {@code name}, {@code url}, and {@code sha256}
   * keys describing one archive to fetch.
   */
  @get:Input
  abstract val downloadSpecs: ListProperty<Map<String, String>>

  /**
   * VLC plugin subdirectory names to exclude from the packaged output.
   * Dropping unneeded categories (GUI, scripting, streaming) roughly halves the payload.
   */
  @get:Input
  abstract val pluginBlocklist: ListProperty<String>

  /** Local directory used to cache downloaded archives between builds. Not an output. */
  @get:Internal
  abstract val downloadCacheDir: DirectoryProperty

  /** Directory where extracted native libraries are written, organized by platform. */
  @get:OutputDirectory
  abstract val nativesDir: DirectoryProperty

  @TaskAction
  fun execute() {
    val version = vlcVersion.get()
    val dlDir = downloadCacheDir.get().asFile
    val outRoot = nativesDir.get().asFile

    val files = downloadSpecs.get().associate { spec ->
      spec["name"]!! to fetchArtifact(spec, dlDir)
    }

    extractWindows(version, files, outRoot, dlDir)
    extractLinux(version, files, outRoot, dlDir)
    extractMacOS(version, files, outRoot, dlDir)

    logger.lifecycle("libvlc $version natives ready under $outRoot")
  }

  private fun sha256Of(f: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    f.inputStream().use { stream ->
      val buf = ByteArray(65536)
      var n: Int
      while (stream.read(buf).also { n = it } > 0) digest.update(buf, 0, n)
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
  }

  private fun fetchArtifact(spec: Map<String, String>, dir: File): File {
    val target = File(dir, spec["name"]!!)
    if (target.exists() && sha256Of(target) == spec["sha256"]) return target
    dir.mkdirs()
    logger.lifecycle("Downloading ${spec["url"]}")
    URI(spec["url"]!!).toURL().openStream().use { input ->
      target.outputStream().use { output -> input.copyTo(output) }
    }
    val actual = sha256Of(target)
    if (actual != spec["sha256"]) {
      target.delete()
      throw GradleException(
        "Checksum mismatch for ${spec["name"]}: expected ${spec["sha256"]}, got $actual"
      )
    }
    return target
  }

  private fun extractDeb(deb: File, destDir: File) {
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

  private fun extractLibsFrom7z(sevenZip: File, pathPrefix: String, destDir: File) {
    SevenZFile.Builder().setFile(sevenZip).get().use { sz ->
      var entry = sz.nextEntry
      while (entry != null) {
        val normalizedName = entry.name.replace('\\', '/')
        if (!entry.isDirectory && normalizedName.startsWith(pathPrefix)) {
          val out = File(destDir, normalizedName.substring(pathPrefix.length))
          out.parentFile.mkdirs()
          val content = ByteArray(entry.size.toInt())
          var off = 0
          while (off < content.size) off += sz.read(content, off, content.size - off)
          out.writeBytes(content)
        }
        entry = sz.nextEntry
      }
    }
  }

  private fun extractWindows(version: String, files: Map<String, File>, outRoot: File, dlDir: File) {
    val blocklist = pluginBlocklist.get()
    val winDir = File(outRoot, "windows-amd64")
    val zipRoot = "vlc-$version/"
    if (!File(winDir, "libvlc.dll").exists()) {
      logger.lifecycle("Extracting Windows natives...")
      fs.copy {
        from(archives.zipTree(files["vlc-$version-win64.zip"]!!)) {
          include("${zipRoot}libvlc.dll", "${zipRoot}libvlccore.dll", "${zipRoot}plugins/**")
          blocklist.forEach { exclude("${zipRoot}plugins/$it/**") }
          eachFile { path = path.substring(zipRoot.length) }
          includeEmptyDirs = false
        }
        into(winDir)
      }
    }
    if (!File(winDir, "sdk/libvlc.lib").exists()) {
      logger.lifecycle("Extracting Windows SDK import libraries...")
      extractLibsFrom7z(files["vlc-$version-win64.7z"]!!, "vlc-$version/sdk/lib/", File(winDir, "sdk"))
    }
  }

  private fun extractLinux(version: String, files: Map<String, File>, outRoot: File, dlDir: File) {
    val blocklist = pluginBlocklist.get()
    val linuxDir = File(outRoot, "linux-amd64")
    if (!File(linuxDir, "libvlc.so.5").exists()) {
      logger.lifecycle("Extracting Linux natives...")
      val debStage = File(dlDir, "deb-stage")
      fs.delete { delete(debStage) }
      downloadSpecs.get().filter { it["name"]!!.endsWith(".deb") }.forEach { spec ->
        extractDeb(files[spec["name"]]!!, debStage)
      }
      val usrLib = File(debStage, "usr/lib/x86_64-linux-gnu")
      fs.copy {
        from(usrLib) {
          include("libvlc.so*", "libvlccore.so*", "vlc/plugins/**", "vlc/libvlc_*.so")
          blocklist.forEach { exclude("vlc/plugins/$it/**") }
          includeEmptyDirs = false
        }
        into(linuxDir)
      }
      // The .deb carries libvlc.so.5 as a symlink; materialize real files so they jar-package.
      mapOf("libvlc.so.5" to "libvlc.so.5.6.1", "libvlccore.so.9" to "libvlccore.so.9.0.1")
        .forEach { (link, real) ->
          val linkFile = File(linuxDir, link)
          val realFile = File(linuxDir, real)
          if (realFile.exists() && (!linkFile.exists() || linkFile.length() == 0L)) {
            linkFile.delete()
            linkFile.writeBytes(realFile.readBytes())
          }
        }
    }
  }

  private fun extractMacOS(version: String, files: Map<String, File>, outRoot: File, dlDir: File) {
    val macDir = File(outRoot, "macos-universal")
    if (File(macDir, "lib/libvlc.dylib").exists()) return
    val dmg = files["vlc-$version-universal.dmg"]!!
    val sevenZip = listOf("7z", "7za").firstOrNull { tool ->
      try { ProcessBuilder(tool).start().waitFor(); true } catch (_: Exception) { false }
    }
    if (sevenZip != null) {
      logger.lifecycle("Extracting macOS natives with 7z...")
      val dmgStage = File(dlDir, "dmg-stage")
      fs.delete { delete(dmgStage) }
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
        fs.copy {
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
      ProcessBuilder(
        "hdiutil", "attach", dmg.absolutePath,
        "-nobrowse", "-readonly", "-mountpoint", mountPoint.absolutePath
      ).start().waitFor()
      try {
        fs.copy {
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
}
