import com.diffplug.gradle.spotless.JavaExtension
import java.io.File

/**
 * Applies the one and only set of FlixelGDX Spotless Java rules to the given Spotless
 * Java block.
 *
 * Every Spotless Java configuration in the build calls this function instead of declaring
 * its own formatter steps, so the rules cannot drift between modules.
 *
 * @param rootDir The root project directory, used to locate the Eclipse formatter config.
 */
fun JavaExtension.flixelRules(rootDir: File) {
  eclipse("4.33").configFile(File(rootDir, "gradle/spotless/eclipse-formatter.xml"))
  importOrder("com", "org", "io", "java", "javax", "jdk", "", "\\#")
  removeUnusedImports()
  endWithNewline()
  trimTrailingWhitespace()
}
