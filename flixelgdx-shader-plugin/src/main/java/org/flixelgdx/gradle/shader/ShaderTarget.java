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
package org.flixelgdx.gradle.shader;

/**
 * One backend variant the plugin compiles a shader into, mapping a runtime renderer to the bgfx
 * {@code shaderc} flags that produce it.
 *
 * <p>Each constant names the output directory the desktop backend reads at runtime (see
 * {@code FlixelBgfxGraphics.shaderDirFromApi}), together with the {@code shaderc} {@code --platform}
 * and {@code -p} (profile) arguments. The platforms are pinned rather than taken from the build host
 * so the produced bytecode is deterministic no matter which operating system runs the build.
 *
 * <p>The Direct3D target is marked {@link #hostLimited} because compiling HLSL to DXBC needs
 * Microsoft's FXC compiler. FXC runs natively on Windows and, on other hosts, through the
 * {@code d3d4linux} Wine shim; when neither is available the variant is skipped with a warning,
 * exactly as the framework's own {@code build_shaders.sh} does, and the bytecode can be produced on
 * a Windows machine (for example a CI runner) instead. Every other variant, including the OpenGL,
 * Vulkan, and Metal ones, compiles on any host.
 */
public enum ShaderTarget {

  /** Desktop OpenGL (and the runtime fallback), emitted as GLSL 1.20 source. */
  GLSL("glsl", "linux", "120", false),

  /** Vulkan, emitted as SPIR-V. */
  SPIRV("spirv", "linux", "spirv", false),

  /** Metal on macOS and iOS, emitted as Metal Shading Language. */
  METAL("metal", "osx", "metal", false),

  /** Direct3D 11 and 12, emitted as DXBC. Needs Microsoft's FXC compiler (native on Windows, or via the {@code d3d4linux} Wine shim elsewhere). */
  DX11("dx11", "windows", "s_5_0", true);

  private final String dir;
  private final String platform;
  private final String profile;
  private final boolean hostLimited;

  ShaderTarget(String dir, String platform, String profile, boolean hostLimited) {
    this.dir = dir;
    this.platform = platform;
    this.profile = profile;
    this.hostLimited = hostLimited;
  }

  /**
   * Returns the resource sub-directory name the runtime reads this variant from.
   *
   * @return The variant directory name, such as {@code glsl} or {@code spirv}.
   */
  public String dir() {
    return dir;
  }

  /**
   * Returns the value passed to {@code shaderc}'s {@code --platform} flag.
   *
   * @return The target platform name.
   */
  public String platform() {
    return platform;
  }

  /**
   * Returns the value passed to {@code shaderc}'s {@code -p} (profile) flag.
   *
   * @return The shader profile name.
   */
  public String profile() {
    return profile;
  }

  /**
   * Returns whether this variant can only be compiled on a specific host.
   *
   * <p>When {@code true}, a compilation failure is treated as a skippable warning instead of a
   * build error, because the required FXC compiler is not present on every host.
   *
   * @return {@code true} if the variant is host-restricted.
   */
  public boolean hostLimited() {
    return hostLimited;
  }
}
