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
package org.flixelgdx.gradle.project;

import java.util.Locale;

/**
 * Writes the Java source of the generated {@code Project} class from a {@link ProjectModel}.
 *
 * <p>The class holds the game's values as constants and a {@code config()} method that returns a
 * {@code FlixelConfig.Builder} filled in from them, so a game can start with
 * {@code Project.config().build()} and only spell out what is different. The output depends only on the model, so
 * the same file always produces byte-for-byte the same class.
 *
 * <p>Strings are escaped so any text from the file is safe in Java source, including line breaks and characters
 * outside ASCII. Entries of the {@code [defines]} table go into a nested {@code Defines} class, so their names can
 * never clash with the constants the class itself has.
 */
public final class ProjectClassWriter {

  private ProjectClassWriter() {}

  /**
   * Writes the source of the class.
   *
   * @param model The checked values from {@code project.toml}.
   * @return The complete source of the class, ending with a line break.
   */
  public static String write(ProjectModel model) {
    StringBuilder out = new StringBuilder();
    out.append("// Generated from project.toml. Do not edit.\n");
    out.append("package ").append(model.packageName()).append(";\n");
    out.append('\n');
    out.append("import org.flixelgdx.FlixelConfig;\n");
    if (!model.mode().isEmpty()) {
      out.append("import org.flixelgdx.backend.FlixelRuntimeMode;\n");
    }
    out.append('\n');
    out.append("/** Values from this game's project.toml, generated at build time. */\n");
    out.append("public final class Project {\n");
    constant(out, "String", "TITLE", literal(model.title()));
    constant(out, "String", "COMPANY", literal(model.company()));
    constant(out, "String", "VERSION", literal(model.version()));
    constant(out, "int", "BUILD", String.valueOf(model.build()));
    constant(out, "String", "PROFILE", literal(model.profile()));
    constant(out, "int", "WIDTH", String.valueOf(model.width()));
    constant(out, "int", "HEIGHT", String.valueOf(model.height()));
    constant(out, "int", "FRAMERATE", String.valueOf(model.framerate()));
    constant(out, "boolean", "VSYNC", String.valueOf(model.vsync()));
    constant(out, "boolean", "FULLSCREEN", String.valueOf(model.fullscreen()));
    if (model.hasRender()) {
      constant(out, "boolean", "RENDER_RESOLUTION", String.valueOf(model.renderResolution()));
      constant(out, "int", "RENDER_WIDTH", String.valueOf(model.renderWidth()));
      constant(out, "int", "RENDER_HEIGHT", String.valueOf(model.renderHeight()));
      constant(out, "float", "RENDER_SCALE", model.renderScale() + "f");
      constant(out, "boolean", "RENDER_SMOOTH", String.valueOf(model.renderSmooth()));
      constant(out, "boolean", "TRANSPARENT", String.valueOf(model.transparent()));
    }
    out.append('\n');
    out.append("  private Project() {}\n");
    out.append('\n');
    out.append("  /** Returns a builder filled in from project.toml, ready to tweak or build(). */\n");
    out.append("  public static FlixelConfig.Builder config() {\n");
    out.append("    return new FlixelConfig.Builder(TITLE)\n");
    out.append("        .company(COMPANY)\n");
    out.append("        .version(VERSION)\n");
    out.append("        .size(WIDTH, HEIGHT)\n");
    out.append("        .framerate(FRAMERATE)\n");
    out.append("        .vsync(VSYNC)\n");
    out.append("        .fullscreen(FULLSCREEN)");
    if (model.hasRender()) {
      out.append('\n');
      if (model.renderResolution()) {
        out.append("        .renderResolution(RENDER_WIDTH, RENDER_HEIGHT, RENDER_SMOOTH)\n");
      } else {
        out.append("        .disableRenderResolution()\n");
      }
      out.append("        .renderScale(RENDER_SCALE, RENDER_SMOOTH)\n");
      out.append("        .transparentFramebuffer(TRANSPARENT)");
    }
    if (!model.mode().isEmpty()) {
      out.append('\n');
      out.append("        .mode(FlixelRuntimeMode.").append(model.mode().toUpperCase(Locale.ROOT)).append(')');
    }
    out.append(";\n");
    out.append("  }\n");
    if (!model.defines().isEmpty()) {
      out.append('\n');
      out.append("  /** Values from the [defines] table of project.toml. */\n");
      out.append("  public static final class Defines {\n");
      for (ProjectDefine define : model.defines()) {
        out.append("  ");
        constant(out, define.javaType(), define.name(), literal(define.value()));
      }
      out.append('\n');
      out.append("    private Defines() {}\n");
      out.append("  }\n");
    }
    out.append("}\n");
    return out.toString();
  }

  private static void constant(StringBuilder out, String type, String name, String value) {
    out.append("  public static final ").append(type).append(' ').append(name).append(" = ").append(value)
        .append(";\n");
  }

  private static String literal(Object value) {
    return switch (value) {
      case Long number -> number + "L";
      case Float number -> number + "f";
      case String text -> literal(text);
      default -> String.valueOf(value);
    };
  }

  private static String literal(String text) {
    StringBuilder out = new StringBuilder("\"");
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      switch (c) {
        case '"' -> out.append("\\\"");
        case '\\' -> out.append("\\\\");
        case '\n' -> out.append("\\n");
        case '\r' -> out.append("\\r");
        case '\t' -> out.append("\\t");
        default -> {
          if (c < 0x20 || c == 0x7F) {
            // An octal escape, because javac turns a backslash-u escape into the real character before it reads the
            // string, and a real line break there would end the string early.
            out.append(String.format("\\%03o", (int) c));
          } else if (c > 0x7F) {
            out.append(String.format("\\u%04X", (int) c));
          } else {
            out.append(c);
          }
        }
      }
    }
    return out.append('"').toString();
  }
}
