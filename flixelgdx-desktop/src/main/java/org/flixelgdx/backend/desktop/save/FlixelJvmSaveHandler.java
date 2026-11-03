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
package org.flixelgdx.backend.desktop.save;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelConfig;
import org.flixelgdx.logging.FlixelLogger;
import org.flixelgdx.save.FlixelSaveHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;

/**
 * The desktop save storage: one JSON file per save inside a folder on disk.
 *
 * <p>By default the folder is the OS-specific application data directory for the company name and
 * game title set in {@link FlixelConfig}, plus a {@code saves} sub-folder:
 * <ul>
 *   <li><b>Windows</b>: {@code %APPDATA%\Company\Title\saves\}</li>
 *   <li><b>macOS</b>: {@code ~/Library/Application Support/Company/Title/saves/}</li>
 *   <li><b>Linux / Unix</b>: {@code $XDG_DATA_HOME/Company/Title/saves/} (defaults to
 *       {@code ~/.local/share} when {@code XDG_DATA_HOME} is not set)</li>
 * </ul>
 *
 * <p>The default folder is worked out each time it is needed, so it always follows the current
 * {@link Flixel#config}. Pass a folder to {@link #FlixelJvmSaveHandler(Path)} to keep saves
 * somewhere else, such as a cloud-synced folder or a temporary test directory.
 *
 * <p>Writes go to a temporary file first and are then moved over the real one, so a crash or power
 * loss halfway through a save never leaves a half-written file behind.
 *
 * <p>Example:
 *
 * <pre>{@code
 * Flixel.save.bind("MyGame", "slot1", new FlixelJvmSaveHandler(Path.of("D:/Saves/MyGame")));
 * }</pre>
 */
public class FlixelJvmSaveHandler implements FlixelSaveHandler {

  private static final FlixelLogger LOG = Flixel.log.tagged("Save");

  /** The folder saves live in, or {@code null} to use the default app data folder. */
  @Nullable
  private final Path directory;

  /** Creates a handler that keeps saves in the default app data folder. */
  public FlixelJvmSaveHandler() {
    this.directory = null;
  }

  /**
   * Creates a handler that keeps saves in a specific folder. The folder is created on the first
   * write if it does not exist yet.
   *
   * @param directory The folder to keep save files in.
   */
  public FlixelJvmSaveHandler(@NotNull Path directory) {
    this.directory = Objects.requireNonNull(directory, "directory cannot be null");
  }

  /**
   * Returns the default save folder for an organization and application name, as described in the
   * class documentation.
   *
   * @param org The organization or company name (for example {@code "My Studio"}).
   * @param app The application name (for example {@code "My Game"}).
   * @return The default save folder; it may not exist yet.
   */
  @NotNull
  public static Path defaultDirectory(@NotNull String org, @NotNull String app) {
    String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    String home = System.getProperty("user.home", ".");
    Path root;
    if (os.contains("win")) {
      String appData = System.getenv("APPDATA");
      root = appData == null || appData.isEmpty() ? Path.of(home, "AppData", "Roaming") : Path.of(appData);
    } else if (os.contains("mac")) {
      root = Path.of(home, "Library", "Application Support");
    } else {
      // Linux and other Unix systems follow the XDG base directory spec.
      String xdgDataHome = System.getenv("XDG_DATA_HOME");
      root = xdgDataHome == null || xdgDataHome.isEmpty() ? Path.of(home, ".local", "share") : Path.of(xdgDataHome);
    }
    return root.resolve(org).resolve(app).resolve("saves");
  }

  @Override
  @Nullable
  public String read(@NotNull String name) {
    Path file = fileFor(name);
    if (!Files.isRegularFile(file)) {
      return null;
    }
    try {
      return Files.readString(file, StandardCharsets.UTF_8);
    } catch (IOException | RuntimeException e) {
      LOG.error("Could not read save file '{}'.", file, e);
      return null;
    }
  }

  @Override
  public boolean write(@NotNull String name, @NotNull String data) {
    Path file = fileFor(name);
    Path temp = file.resolveSibling(file.getFileName() + ".tmp");
    try {
      Path parent = file.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.writeString(temp, data, StandardCharsets.UTF_8);
      try {
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
      }
      return true;
    } catch (IOException | RuntimeException e) {
      LOG.error("Could not write save file '{}'.", file, e);
      return false;
    }
  }

  @Override
  public boolean delete(@NotNull String name) {
    try {
      Files.deleteIfExists(fileFor(name));
      return true;
    } catch (IOException | RuntimeException e) {
      LOG.error("Could not delete save file for '{}'.", name, e);
      return false;
    }
  }

  @Override
  public boolean exists(@NotNull String name) {
    return Files.isRegularFile(fileFor(name));
  }

  /**
   * Returns the file a save name is stored in.
   *
   * @param name The save name.
   * @return The save file inside {@link #getDirectory()}.
   */
  @NotNull
  public Path fileFor(@NotNull String name) {
    return getDirectory().resolve(name + ".json");
  }

  /**
   * Returns the folder saves are kept in: the folder given to the constructor, or the default app
   * data folder for the current {@link Flixel#config}.
   *
   * @return The save folder; it may not exist yet.
   */
  @NotNull
  public Path getDirectory() {
    if (directory != null) {
      return directory;
    }
    FlixelConfig config = Flixel.config;
    return defaultDirectory(config.getCompany(), config.getTitle());
  }
}
