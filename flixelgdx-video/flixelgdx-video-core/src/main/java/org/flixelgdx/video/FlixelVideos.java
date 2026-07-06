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
package org.flixelgdx.video;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.files.FileHandle;

import org.flixelgdx.Flixel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Static helper that creates {@link FlixelVideo} instances from the platform backend
 * registered by your launcher.
 *
 * <p>Each platform module ships an installer that wires itself in here once, before
 * the game starts:
 *
 * <pre>{@code
 * public static void main(String[] args) {
 *   FlixelVlcVideoHandler.install();
 *   FlixelLwjgl3Launcher.launch(new MyGame());
 * }
 * }</pre>
 *
 * <p>After that, creating a video anywhere in the game needs no further setup:
 *
 * <pre>{@code
 * FlixelVideo cutscene = FlixelVideos.create("videos/intro.mp4");
 * add(cutscene);
 * cutscene.play();
 * }</pre>
 *
 * @see FlixelVideo
 * @see FlixelVideoBackend.Factory
 */
public final class FlixelVideos {

  @Nullable
  private static FlixelVideoBackend.Factory backendFactory;

  private FlixelVideos() {}

  /**
   * Creates a new video for an internal asset path using the current platform backend.
   *
   * @param path The path to the video file inside your assets, e.g. {@code "videos/intro.mp4"}.
   * @return A new video instance.
   * @throws IllegalStateException If no platform backend factory has been registered.
   */
  @NotNull
  public static FlixelVideo create(@NotNull String path) {
    return new FlixelBaseVideo(createBackend(path, false));
  }

  /**
   * Creates a new video from a libGDX file handle using the current platform backend.
   *
   * <p>Internal and classpath handles resolve through the asset manager (so packaged
   * JAR assets work); absolute, external, and local handles are opened directly.
   *
   * @param file The video file handle.
   * @return A new video instance.
   * @throws IllegalStateException If no platform backend factory has been registered.
   */
  @NotNull
  public static FlixelVideo create(@NotNull FileHandle file) {
    boolean internal = file.type() == Files.FileType.Internal
        || file.type() == Files.FileType.Classpath;
    if (internal) {
      return create(file.path());
    }
    return new FlixelBaseVideo(createBackend(file.file().getAbsolutePath(), true));
  }

  /**
   * Registers the platform video backend factory.
   *
   * <p>Called once by the platform installer (for example
   * {@code FlixelVlcVideoHandler.install()} on desktop or
   * {@code FlixelTeaVMVideoHandler.install()} on the web) before any video is created.
   *
   * @param factory The backend factory to use (must not be {@code null}).
   * @throws IllegalArgumentException If {@code factory} is {@code null}.
   */
  public static void setBackendFactory(@NotNull FlixelVideoBackend.Factory factory) {
    if (factory == null) {
      throw new IllegalArgumentException("Video backend factory cannot be null.");
    }
    backendFactory = factory;
  }

  /**
   * Returns the registered platform video backend factory.
   *
   * @return The factory, or {@code null} if no platform installer has run yet.
   */
  @Nullable
  public static FlixelVideoBackend.Factory getBackendFactory() {
    return backendFactory;
  }

  @NotNull
  private static FlixelVideoBackend createBackend(@NotNull String path, boolean external) {
    FlixelVideoBackend.Factory factory = backendFactory;
    if (factory == null) {
      throw new IllegalStateException(
          "No video backend factory registered. Call the platform installer first, e.g. "
              + "FlixelVlcVideoHandler.install() in your desktop launcher or "
              + "FlixelTeaVMVideoHandler.install() in your web launcher.");
    }
    String resolved = external ? path : Flixel.ensureAssets().extractAssetPath(path);
    return factory.createVideo(resolved, external);
  }
}
