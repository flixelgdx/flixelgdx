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

/**
 * The set of platforms a game is packaged for, configured inside the {@code targets} block.
 *
 * <p>This offers two ways to add a target so common platforms stay a one-liner while unusual ones
 * are still possible. The convenience methods, such as {@link #linuxX64()} and
 * {@link #windowsX64()}, add a preconfigured target. The {@link #register(String, Action)} method
 * adds a target you describe yourself, which is how you cover a platform the convenience methods do
 * not, for example a Linux ARM handheld:
 *
 * <pre>{@code
 * packagr {
 *   targets {
 *     linuxX64()
 *     windowsX64()
 *     register("steamDeck") {
 *       os = OperatingSystem.LINUX
 *       arch = Architecture.AARCH64
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p>Both paths add to the same underlying container, so a target added either way produces a
 * {@code package<Name>} task and is included by {@code packageAll}.
 */
public final class PackagrTargets {

  private final NamedDomainObjectContainer<PackagrTarget> container;

  /**
   * Wraps the backing container. This is created by the plugin; game build scripts never call it.
   *
   * @param container The container every declared target is added to.
   */
  public PackagrTargets(NamedDomainObjectContainer<PackagrTarget> container) {
    this.container = container;
  }

  /**
   * Registers a target you describe yourself.
   *
   * <p>Use this for a platform the convenience methods do not cover. The action must set the
   * target's {@link PackagrTarget#getOs() os} and {@link PackagrTarget#getArch() arch}.
   *
   * @param name The target name, used for its {@code package<Name>} task and output directory.
   * @param action Configures the target's operating system and architecture.
   */
  public void register(String name, Action<? super PackagrTarget> action) {
    container.register(name, action);
  }

  /** Adds a target for 64-bit x86 Linux. */
  public void linuxX64() {
    add("linux-x64", OperatingSystem.LINUX, Architecture.X64);
  }

  /** Adds a target for 64-bit ARM Linux. */
  public void linuxArm64() {
    add("linux-arm64", OperatingSystem.LINUX, Architecture.AARCH64);
  }

  /** Adds a target for 64-bit x86 Windows. */
  public void windowsX64() {
    add("windows-x64", OperatingSystem.WINDOWS, Architecture.X64);
  }

  /** Adds a target for 64-bit ARM Windows. */
  public void windowsArm64() {
    add("windows-arm64", OperatingSystem.WINDOWS, Architecture.AARCH64);
  }

  /** Adds a target for 64-bit ARM macOS (Apple Silicon). */
  public void macosArm64() {
    add("macos-arm64", OperatingSystem.MACOS, Architecture.AARCH64);
  }

  private void add(String name, OperatingSystem os, Architecture arch) {
    container.register(name, target -> {
      target.getOs().set(os);
      target.getArch().set(arch);
    });
  }

  NamedDomainObjectContainer<PackagrTarget> container() {
    return container;
  }
}
