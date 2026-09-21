/*
 * MIT License
 *
 * Copyright (c) 2026 stringdotjar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.flixelgdx.gradle.packagr;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

/**
 * Configuration exposed as the {@code packagr} DSL block in a game module's {@code build.gradle}.
 *
 * <p>The block describes what to package (the {@link #getMainClass() main class} and app metadata),
 * which runtime to bundle (the {@link #getJdkVersion() version} and {@link #getJlinkModules()
 * modules}), and which platforms to build for (the {@link #getTargets() targets}). Each declared
 * target becomes a {@code package<Name>} task, and {@code packageAll} builds them all.
 *
 * <h2>Example</h2>
 *
 * <pre>{@code
 * packagr {
 *   mainClass = 'com.mygame.DesktopLauncher'
 *   appName = 'MyGame'
 *
 *   jvmArg('-Xmx1G')
 *
 *   targets {
 *     linuxX64()
 *     windowsX64()
 *     macosArm64()
 *   }
 * }
 * }</pre>
 *
 * @see PackagrTargets
 * @see PackagrPlugin
 */
public abstract class PackagrExtension {

  /** Gradle extension name the DSL block is registered under. */
  public static final String NAME = "packagr";

  private final PackagrTargets targets;

  /**
   * Creates the extension and its target container. This is called by Gradle; game build scripts
   * never call it directly.
   *
   * @param objects The object factory used to build the target container.
   */
  @Inject
  public PackagrExtension(ObjectFactory objects) {
    NamedDomainObjectContainer<PackagrTarget> container =
        objects.domainObjectContainer(PackagrTarget.class);
    this.targets = new PackagrTargets(container);
  }

  /**
   * The fully qualified name of the class whose {@code main} method starts the game, for example
   * {@code com.mygame.DesktopLauncher}.
   *
   * <p>This is required; packaging fails with a clear error when it is unset.
   *
   * @return The main-class property.
   */
  public abstract Property<String> getMainClass();

  /**
   * The application name, used for the launcher executable and the output directory.
   *
   * <p>Defaults to the Gradle project name.
   *
   * @return The application-name property.
   */
  public abstract Property<String> getAppName();

  /**
   * The application version, recorded for reference in the package.
   *
   * <p>Defaults to the Gradle project version.
   *
   * @return The application-version property.
   */
  public abstract Property<String> getAppVersion();

  /**
   * Extra arguments passed to the bundled JVM every time the game starts, for example
   * {@code -Xmx1G}.
   *
   * @return The JVM-arguments property.
   */
  public abstract ListProperty<String> getJvmArgs();

  /**
   * The feature version of the Java runtime to bundle, for example {@code 17} or {@code 21}.
   *
   * <p>Defaults to {@code 17}, the version the framework targets. The runtime is always built from
   * Eclipse Temurin, the JDK the framework recommends.
   *
   * @return The runtime-version property.
   */
  public abstract Property<Integer> getJdkVersion();

  /**
   * The directory the downloaded runtimes are cached in, shared across all builds on the machine.
   *
   * <p>Defaults to {@code ~/.flixelgdx/jdks}. A runtime already present here is reused, so a given
   * version and platform is only ever downloaded once.
   *
   * @return The runtime-cache-directory property.
   */
  public abstract DirectoryProperty getJdkCacheDir();

  /**
   * The Java module names to include in the trimmed runtime.
   *
   * <p>Defaults to a set that covers a typical FlixelGDX desktop game: {@code java.base},
   * {@code java.desktop}, {@code java.logging}, {@code java.management}, and {@code jdk.unsupported}
   * (which the native bindings need for {@code sun.misc.Unsafe}). Set this to trim further or to add
   * a module a game depends on.
   *
   * @return The jlink-modules property.
   */
  public abstract ListProperty<String> getJlinkModules();

  /**
   * The platforms to package for.
   *
   * @return The targets container.
   */
  public PackagrTargets getTargets() {
    return targets;
  }

  /**
   * Configures the platforms to package for.
   *
   * <p>Lets the DSL read as a {@code targets { ... }} block.
   *
   * @param action Configures the targets container.
   */
  public void targets(Action<? super PackagrTargets> action) {
    action.execute(targets);
  }

  /**
   * Adds one extra argument passed to the bundled JVM every time the game starts.
   *
   * <p>Convenience for {@code getJvmArgs().add(arg)} so the DSL reads as {@code jvmArg('-Xmx1G')}.
   *
   * @param arg The JVM argument to add.
   */
  public void jvmArg(String arg) {
    getJvmArgs().add(arg);
  }
}
