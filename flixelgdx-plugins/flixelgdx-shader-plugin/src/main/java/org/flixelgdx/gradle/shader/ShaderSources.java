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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Assembles the sources the shader tools compile from the plain GLSL a developer writes.
 *
 * <p>A game developer never has to learn a backend's shader dialect. They write an ordinary GLSL
 * {@code void main()} that reads a small set of framework-provided names, and this class wraps that
 * body in two ways:
 * <ul>
 *   <li>{@link #glslVertex} and {@link #glslFragment} build a complete GLSL 4.50 source for
 *   {@link Glslang}. Its SPIR-V output is the source of truth that {@link SpirvCross} translates into
 *   the ESSL the web and Android backends load.</li>
 *   <li>{@link #vertex} and {@link #fragment} build the bgfx {@code .sc} sources for {@link Shaderc}:
 *   the {@code $input}/{@code $output} declarations, the {@code #include <bgfx_shader.sh>} preamble,
 *   the stage-0 sampler the batch binds, and {@code #define} aliases mapping the friendly names to
 *   bgfx's internal ones.</li>
 * </ul>
 * This mirrors how the HaxeFlixel compatibility layer in {@code FlixelShader} rewrites
 * {@code #pragma header} shaders.
 *
 * <p>The bgfx sources are built from the developer's original text rather than from translated
 * SPIR-V. bgfx compiles several variants through an HLSL front end, where {@code a * b} on a matrix is
 * a per-component multiply and only {@code mul(a, b)} is a real matrix product. Translated code
 * always writes {@code a * b}, so feeding it to bgfx would silently break every matrix transform.
 *
 * <p>A vertex body is written against the bgfx naming convention ({@code a_texcoord0},
 * {@code a_color0}, {@code u_modelViewProj}, {@code mul}). The GLSL preamble aliases those names to
 * the ones the GL backends bind ({@code a_texCoord0}, {@code a_color}, {@code u_projTrans}), so the
 * same body compiles on every backend, provided it reads {@code a_position} as a {@code vec2} (which
 * is what the framework's vertex buffer actually contains).
 *
 * <p>The names available to a fragment shader are:
 * <ul>
 *   <li>{@code u_texture} - the sampler for the sprite or camera scene texture (bound at stage 0).</li>
 *   <li>{@code v_texCoords} - the interpolated texture coordinate ({@code vec2}).</li>
 *   <li>{@code v_color} - the interpolated vertex tint ({@code vec4}).</li>
 *   <li>{@code flixel_texture(uv)} - shorthand for {@code texture2D(u_texture, uv)}.</li>
 * </ul>
 * The shader writes its result to {@code gl_FragColor}. A developer must not redeclare these names;
 * the preamble already provides them, just like {@code #pragma header} does in HaxeFlixel.
 *
 * <p>The methods here are pure string transforms with no Gradle or file-system dependencies, so
 * they are unit tested directly.
 */
public final class ShaderSources {

  /**
   * The shared varying and attribute declarations both stages compile against.
   *
   * <p>This matches the vertex layout the desktop backend registers: position (2 floats),
   * texture coordinate (2 floats), and a packed color (4 normalized bytes).
   */
  public static final String VARYING_DEF = """
      vec2 v_texcoord0 : TEXCOORD0 = vec2(0.0, 0.0);
      vec4 v_color0    : COLOR0    = vec4(1.0, 1.0, 1.0, 1.0);

      vec2 a_position  : POSITION;
      vec2 a_texcoord0 : TEXCOORD0;
      vec4 a_color0    : COLOR0;
      """;

  /**
   * The built-in pass-through vertex shader used when a shader declares no custom vertex source.
   *
   * <p>Every quad corner is already transformed on the CPU by the batch, so the vertex stage only
   * applies the combined view-projection matrix and forwards the texture coordinate and tint. This
   * covers the overwhelming majority of sprite and camera post-processing effects, which only need
   * a custom fragment stage.
   */
  public static final String DEFAULT_VERTEX = """
      void main()
      {
        gl_Position = mul(u_modelViewProj, vec4(a_position.xy, 0.0, 1.0));
        v_texCoords = a_texcoord0;
        v_color = a_color0;
      }
      """;

  private static final String HEADER =
      "// Generated by the FlixelGDX shader plugin. Do not edit.\n";

  /**
   * Matches a single scalar or vector uniform declaration, such as {@code uniform float u_time;}.
   *
   * <p>Group 1 is the indentation, group 2 is the type, and group 3 is the name. Declarations with several names or an array
   * size are not matched and pass through unchanged.
   */
  private static final Pattern UNIFORM_DECLARATION = Pattern.compile(
      "^([ \\t]*)uniform\\s+(?:(?:lowp|mediump|highp)\\s+)?(float|int|vec2|vec3|vec4)\\s+(\\w+)\\s*;",
      Pattern.MULTILINE);

  /**
   * The GLSL 4.50 preamble prepended to a vertex body before it is compiled to SPIR-V.
   *
   * <p>It declares the fixed vertex layout under the names the GL backends bind, at the same
   * attribute slots they use ({@code a_position} at 0, {@code a_texCoord0} at 1, {@code a_color} at 2),
   * along with the projection uniform and the varyings. It then aliases the bgfx-side names so one
   * vertex body compiles on every backend:
   * <ul>
   *   <li>{@code a_texcoord0} maps to {@code a_texCoord0} (bgfx uses lowercase {@code c}).</li>
   *   <li>{@code a_color0} maps to {@code a_color} (bgfx uses a numeric suffix).</li>
   *   <li>{@code u_modelViewProj} maps to {@code u_projTrans} (different uniform name).</li>
   *   <li>{@code mul(a, b)} expands to {@code ((a) * (b))} (bgfx helper, absent in plain GLSL).</li>
   * </ul>
   */
  private static final String GLSL_VERTEX_PREAMBLE = """
      layout(location = 0) in vec2 a_position;
      layout(location = 1) in vec2 a_texCoord0;
      layout(location = 2) in vec4 a_color;
      uniform mat4 u_projTrans;
      layout(location = 0) out vec2 v_texCoords;
      layout(location = 1) out vec4 v_color;
      #define a_texcoord0 a_texCoord0
      #define a_color0 a_color
      #define u_modelViewProj u_projTrans
      #define mul(a, b) ((a) * (b))
      """;

  /**
   * The GLSL 4.50 preamble prepended to a fragment body before it is compiled to SPIR-V.
   *
   * <p>It provides the friendly names ({@code u_texture}, {@code v_texCoords}, {@code v_color},
   * {@code flixel_texture}) and maps the older GLSL spellings developers write ({@code gl_FragColor},
   * {@code texture2D}) onto their GLSL 4.50 equivalents, so a fragment body written for any
   * FlixelGDX backend compiles unchanged.
   */
  private static final String GLSL_FRAGMENT_PREAMBLE = """
      layout(location = 0) in vec2 v_texCoords;
      layout(location = 1) in vec4 v_color;
      layout(location = 0) out vec4 flixel_FragColor;
      uniform sampler2D u_texture;
      #define gl_FragColor flixel_FragColor
      #define texture2D texture
      vec4 flixel_texture(vec2 uv) { return texture(u_texture, uv); }
      """;

  /**
   * Words that are ordinary identifiers in GLSL but reserved in HLSL.
   *
   * <p>bgfx compiles the SPIR-V and Direct3D variants through an HLSL front end, so a GLSL variable
   * named {@code line} or {@code sample} fails there even though the desktop GLSL, Metal, Android,
   * and web variants accept it. Words that bgfx's own header uses (such as {@code register}) are
   * left out, because renaming them would break the header's macros.
   */
  private static final String[] HLSL_ONLY_KEYWORDS = {
      "compile", "dword", "globallycoherent", "groupshared", "line", "lineadj", "linear", "matrix",
      "nointerpolation", "packoffset", "pass", "point", "precise", "sample", "snorm", "string",
      "technique", "triangle", "triangleadj", "unorm", "vector"
  };

  /** Matches the start of a {@code vec2(}, {@code vec3(}, or {@code vec4(} constructor call. */
  private static final Pattern VECTOR_CONSTRUCTOR =
      Pattern.compile("(?<![\\w.])vec([234])\\s*\\(");

  /** Matches a plain numeric literal, such as {@code 0}, {@code 1.5}, or {@code -2.0}. */
  private static final Pattern NUMBER_LITERAL = Pattern.compile("-?\\d+(\\.\\d*)?([eE][-+]?\\d+)?");

  private ShaderSources() {}

  /**
   * Builds the complete GLSL 4.50 vertex source that {@link Glslang} compiles to SPIR-V.
   *
   * <p>When {@code vertexGlsl} is {@code null} or blank, the built-in {@link #DEFAULT_VERTEX} is used,
   * which covers the overwhelming majority of sprite and camera effects. The body is placed after a
   * {@code #line} directive naming {@code sourceName}, so compile errors point at the developer's own
   * file and line numbers instead of the generated preamble.
   *
   * @param vertexGlsl The developer's vertex body, or {@code null} to use the built-in pass-through.
   * @param sourceName The file name reported in compile errors.
   * @return Complete GLSL 4.50 vertex source.
   */
  @NotNull
  public static String glslVertex(@Nullable String vertexGlsl, @NotNull String sourceName) {
    String body = (vertexGlsl == null || vertexGlsl.isBlank()) ? DEFAULT_VERTEX : vertexGlsl;
    return glsl(GLSL_VERTEX_PREAMBLE, body, sourceName);
  }

  /**
   * Builds the complete GLSL 4.50 fragment source that {@link Glslang} compiles to SPIR-V.
   *
   * @param fragmentGlsl The developer's fragment source, containing a {@code void main()}.
   * @param sourceName The file name reported in compile errors.
   * @return Complete GLSL 4.50 fragment source.
   */
  @NotNull
  public static String glslFragment(@NotNull String fragmentGlsl, @NotNull String sourceName) {
    return glsl(GLSL_FRAGMENT_PREAMBLE, fragmentGlsl, sourceName);
  }

  /**
   * Wraps a developer's fragment GLSL in the bgfx {@code .sc} fragment contract.
   *
   * @param fragmentGlsl The developer's fragment source, containing a {@code void main()}.
   * @return A complete bgfx fragment {@code .sc} source ready for {@code shaderc}.
   */
  @NotNull
  public static String fragment(@NotNull String fragmentGlsl) {
    return HEADER
        + "$input v_texcoord0, v_color0\n\n"
        + "#include <bgfx_shader.sh>\n\n"
        + "SAMPLER2D(s_texture, 0);\n\n"
        + "#define u_texture s_texture\n"
        + "#define v_texCoords v_texcoord0\n"
        + "#define v_color v_color0\n"
        + "#define flixel_texture(_uv) texture2D(s_texture, _uv)\n"
        + renameHlslKeywords(fragmentGlsl)
        + "\n"
        + bgfxBody(fragmentGlsl)
        + "\n";
  }

  /**
   * Wraps a vertex stage in the bgfx {@code .sc} vertex contract, using the built-in pass-through
   * source when no custom one is supplied.
   *
   * <p>A custom vertex shader may read {@code a_position} ({@code vec2}), {@code a_texcoord0}
   * ({@code vec2}), and {@code a_color0} ({@code vec4}), transform with {@code u_modelViewProj},
   * and must write {@code v_texCoords} and {@code v_color} for the fragment stage.
   *
   * @param vertexGlsl The developer's vertex source, or {@code null} to use {@link #DEFAULT_VERTEX}.
   * @return A complete bgfx vertex {@code .sc} source ready for {@code shaderc}.
   */
  @NotNull
  public static String vertex(@Nullable String vertexGlsl) {
    String body = (vertexGlsl == null || vertexGlsl.isBlank()) ? DEFAULT_VERTEX : vertexGlsl;
    return HEADER
        + "$input a_position, a_texcoord0, a_color0\n"
        + "$output v_texcoord0, v_color0\n\n"
        + "#include <bgfx_shader.sh>\n\n"
        + "#define v_texCoords v_texcoord0\n"
        + "#define v_color v_color0\n"
        + renameHlslKeywords(body)
        + "\n"
        + bgfxBody(body)
        + "\n";
  }

  /**
   * Joins a preamble and a developer's body into one GLSL 4.50 source.
   *
   * <p>The {@code GL_GOOGLE_cpp_style_line_directive} extension lets the {@code #line} directive carry
   * a file name, so glslang reports errors as {@code crt.frag.glsl:3} rather than as a line in the
   * generated file.
   *
   * @param preamble The stage's declarations and aliases.
   * @param body The developer's source.
   * @param sourceName The file name reported in compile errors.
   * @return The complete source.
   */
  @NotNull
  private static String glsl(@NotNull String preamble, @NotNull String body, @NotNull String sourceName) {
    String name = sourceName.replace('\\', '/').replace("\"", "");
    return "#version 450\n"
        + "#extension GL_GOOGLE_cpp_style_line_directive : require\n"
        + HEADER
        + preamble
        + "#line 1 \"" + name + "\"\n"
        + body.strip()
        + "\n";
  }

  /**
   * Rewrites scalar and vector uniforms into the {@code vec4} form bgfx requires.
   *
   * <p>bgfx only understands {@code vec4}, {@code mat3}, {@code mat4}, and sampler uniforms. A plain
   * {@code uniform float u_time;} is silently dropped from the desktop GLSL variant and fails to
   * compile to SPIR-V, so every {@code float}, {@code int}, {@code vec2}, {@code vec3}, and
   * {@code vec4} uniform is declared as a {@code vec4} under the same name, followed by a macro
   * that reads back the original type:
   *
   * <pre>{@code
   * uniform float u_time;   // becomes: uniform vec4 u_time;
   *                         //          #define u_time u_time.x
   * }</pre>
   *
   * <p>A macro that names itself is only expanded once, so every later use of {@code u_time} reads
   * {@code u_time.x} while the declaration above it keeps the real name. The desktop backend
   * therefore finds the uniform under the exact name game code passes to {@code setUniform(...)}.
   * Precision qualifiers are dropped because bgfx declares its own.
   *
   * @param glsl The developer's shader source.
   * @return The source with each matching uniform promoted to {@code vec4}.
   */
  @NotNull
  static String promoteUniforms(@NotNull String glsl) {
    Matcher matcher = UNIFORM_DECLARATION.matcher(glsl);
    StringBuilder out = new StringBuilder(glsl.length() + 64);
    while (matcher.find()) {
      String indent = matcher.group(1);
      String type = matcher.group(2);
      String name = matcher.group(3);
      String read = switch (type) {
        case "float" -> name + ".x";
        case "int" -> "int(" + name + ".x)";
        case "vec2" -> name + ".xy";
        case "vec3" -> name + ".xyz";
        default -> null;
      };
      String replacement = indent + "uniform vec4 " + name + ";";
      if (read != null) {
        replacement += "\n" + indent + "#define " + name + " " + read;
      }
      matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(out);
    return out.toString();
  }

  /**
   * Returns macros that rename every HLSL-only reserved word the source uses as an identifier.
   *
   * <p>For example, a source that declares {@code float line;} gets {@code #define line flx_line}, so
   * the HLSL front end behind the SPIR-V and Direct3D variants sees {@code flx_line} instead of the
   * reserved {@code line}. Other backends compile the renamed code just the same.
   *
   * @param glsl The developer's shader source.
   * @return One {@code #define} line per reserved word found, or an empty string.
   */
  @NotNull
  static String renameHlslKeywords(@NotNull String glsl) {
    StringBuilder out = new StringBuilder();
    for (String word : HLSL_ONLY_KEYWORDS) {
      if (Pattern.compile("\\b" + word + "\\b").matcher(glsl).find()) {
        out.append("#define ").append(word).append(" flx_").append(word).append('\n');
      }
    }
    return out.toString();
  }

  /**
   * Prepares a developer's shader body for {@code shaderc}: promotes uniforms and replaces
   * single-argument vector constructors, adding the helper functions the replacements call.
   *
   * @param glsl The developer's shader source.
   * @return The rewritten body, preceded by any helper functions it needs.
   */
  @NotNull
  private static String bgfxBody(@NotNull String glsl) {
    String body = expandVectorConstructors(promoteUniforms(glsl.strip()));
    StringBuilder helpers = new StringBuilder();
    if (body.contains("flx_vec2(")) {
      helpers.append("vec2 flx_vec2(float x) { return vec2(x, x); }\n")
          .append("vec2 flx_vec2(vec2 v) { return v; }\n")
          .append("vec2 flx_vec2(vec3 v) { return v.xy; }\n")
          .append("vec2 flx_vec2(vec4 v) { return v.xy; }\n");
    }
    if (body.contains("flx_vec3(")) {
      helpers.append("vec3 flx_vec3(float x) { return vec3(x, x, x); }\n")
          .append("vec3 flx_vec3(vec3 v) { return v; }\n")
          .append("vec3 flx_vec3(vec4 v) { return v.xyz; }\n");
    }
    if (body.contains("flx_vec4(")) {
      helpers.append("vec4 flx_vec4(float x) { return vec4(x, x, x, x); }\n")
          .append("vec4 flx_vec4(vec4 v) { return v; }\n");
    }
    return helpers.length() == 0 ? body : helpers + "\n" + body;
  }

  /**
   * Rewrites every single-argument vector constructor into a form HLSL accepts.
   *
   * <p>GLSL lets {@code vec3(x)} fill every component with one value, or narrow a larger vector.
   * HLSL, which bgfx uses for the SPIR-V and Direct3D variants, rejects both with "incorrect number
   * of arguments". The argument's type cannot be known from the text alone, so the call becomes a
   * call to an overloaded helper ({@code flx_vec3(x)}) that the compiler resolves by type:
   *
   * <pre>{@code
   * vec3(gray)        // becomes: flx_vec3(gray)
   * vec2(0.0)         // becomes: vec2(0.0, 0.0), since a number is always a scalar
   * }</pre>
   *
   * <p>A {@code const} declaration cannot call a function, so there the argument is repeated for
   * each component instead, which is correct for the scalar values constants are built from.
   *
   * @param glsl The shader source.
   * @return The source with every single-argument vector constructor rewritten.
   */
  @NotNull
  static String expandVectorConstructors(@NotNull String glsl) {
    StringBuilder out = new StringBuilder(glsl.length() + 64);
    Matcher matcher = VECTOR_CONSTRUCTOR.matcher(glsl);
    int copied = 0;
    int searchFrom = 0;
    while (matcher.find(searchFrom)) {
      int open = matcher.end() - 1;
      int close = findClosingParen(glsl, open);
      if (close < 0) {
        break;
      }
      String argument = glsl.substring(open + 1, close);
      int size = matcher.group(1).charAt(0) - '0';
      out.append(glsl, copied, matcher.start());
      if (hasTopLevelComma(argument) || argument.isBlank()) {
        out.append(glsl, matcher.start(), open + 1).append(expandVectorConstructors(argument)).append(')');
      } else {
        String inner = expandVectorConstructors(argument).strip();
        if (NUMBER_LITERAL.matcher(inner).matches() || isInConstDeclaration(glsl, matcher.start())) {
          out.append("vec").append(size).append('(');
          for (int i = 0; i < size; i++) {
            out.append(i == 0 ? "" : ", ").append(inner);
          }
          out.append(')');
        } else {
          out.append("flx_vec").append(size).append('(').append(inner).append(')');
        }
      }
      copied = close + 1;
      searchFrom = close + 1;
    }
    out.append(glsl, copied, glsl.length());
    return out.toString();
  }

  /**
   * Returns the index of the parenthesis that closes the one at {@code open}, or {@code -1}.
   *
   * @param text The source text.
   * @param open The index of an opening parenthesis.
   * @return The index of the matching closing parenthesis, or {@code -1} when there is none.
   */
  private static int findClosingParen(@NotNull String text, int open) {
    int depth = 0;
    for (int i = open; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '(') {
        depth++;
      } else if (c == ')' && --depth == 0) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Returns whether an argument list has a comma outside any nested parentheses or brackets.
   *
   * @param argument The text between a call's parentheses.
   * @return {@code true} when the call has more than one argument.
   */
  private static boolean hasTopLevelComma(@NotNull String argument) {
    int depth = 0;
    for (int i = 0; i < argument.length(); i++) {
      char c = argument.charAt(i);
      if (c == '(' || c == '[') {
        depth++;
      } else if (c == ')' || c == ']') {
        depth--;
      } else if (c == ',' && depth == 0) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns whether the statement containing {@code index} starts with {@code const}.
   *
   * @param text The source text.
   * @param index A position inside the statement.
   * @return {@code true} when the statement is a {@code const} declaration.
   */
  private static boolean isInConstDeclaration(@NotNull String text, int index) {
    int start = index;
    while (start > 0) {
      char c = text.charAt(start - 1);
      if (c == ';' || c == '{' || c == '}') {
        break;
      }
      start--;
    }
    return text.substring(start, index).strip().startsWith("const ");
  }

  /**
   * Returns the shared {@code varying.def.sc} contents both stages are compiled against.
   *
   * @return The varying definition source.
   */
  @NotNull
  public static String varyingDef() {
    return HEADER + VARYING_DEF;
  }
}
