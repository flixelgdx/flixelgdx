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
package org.flixelgdx.gradle.logging;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Rewrites {@code FlixelLogger} logging calls to their {@code *WithSite} overloads, and {@code Flixel}
 * static logging helpers to {@code FlixelLoggingBytecodeHooks}, using the enclosing class {@code SourceFile}
 * attribute and {@link LineNumberNode} data so each log records the caller's file and line.
 *
 * <p>Replacement tables are built automatically at class-initialization time by scanning the target classes:
 * <ul>
 *   <li>{@code REPLACEMENTS} is derived from {@code FlixelLogger} by finding all {@code *WithSite} methods
 *       and mapping each to its non-site counterpart.</li>
 *   <li>{@code FLIXEL_STATIC_REPLACEMENTS} is derived from {@code FlixelLoggingBytecodeHooks} by parsing
 *       the {@code bc<MethodName><Index>} naming convention and stripping the site params from the
 *       descriptor. Add a new {@code bc*} hook and the weaver picks it up with no further changes.</li>
 * </ul>
 *
 * <p>{@code Flixel.java} itself is processed for {@code INVOKESTATIC Flixel.*} calls (so internal log sites
 * like the crash handler record their accurate location), but {@code INVOKEVIRTUAL FlixelLogger.*} calls within
 * {@code Flixel.java} are skipped. Rewriting those would embed {@code Flixel.java} line numbers inside the static
 * delegation helpers (e.g. {@code warn(tag, message)} calling {@code log.warn(tag, message)}), which would be
 * misleading rather than useful.
 */
public final class FlixelLoggerBytecodeWeaver {

  private static final String LOGGER_OWNER = "org/flixelgdx/logging/FlixelLogger";

  /**
   * Flixel static logging helpers delegate to {@code FlixelLogger}. Call sites in game bytecode use
   * {@code INVOKESTATIC Flixel...}; those are rewritten to {@code FlixelLoggingBytecodeHooks} so line
   * metadata comes from the caller class (critical for TeaVM where stack walking is unavailable).
   *
   * <p>{@code INVOKESTATIC Flixel.*} calls found inside {@code Flixel.java} itself (such as the crash
   * handler) are also rewritten so their correct {@code Flixel.java} site is captured. Only
   * {@code INVOKEVIRTUAL FlixelLogger.*} calls inside {@code Flixel.java} remain untouched.
   */
  private static final String FLIXEL_STATIC_FACADE_INTERNAL = "org/flixelgdx/Flixel";

  private static final String HOOKS_OWNER = "org/flixelgdx/logging/FlixelLoggingBytecodeHooks";

  /**
   * Descriptor suffix that identifies a {@code *WithSite} method and a {@code bc*} hook method.
   * Represents the 4 site parameters {@code (String sourceFile, int lineNumber, String declaringClass,
   * String declaringMethodName)} as they appear at the end of a method descriptor, including the
   * closing {@code )V}.
   */
  private static final String SITE_PARAMS_TAIL = "Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V";

  /**
   * Maps {@code methodName + descriptor} for {@code INVOKEVIRTUAL FlixelLogger.*} calls to their
   * {@code *WithSite} replacements. Derived automatically from {@code FlixelLogger.class}.
   */
  private static final Map<String, Replacement> REPLACEMENTS = buildVirtualReplacements();

  /**
   * Maps {@code methodName + descriptor} for {@code INVOKESTATIC Flixel.*} calls to their
   * {@code FlixelLoggingBytecodeHooks} hook replacements. Derived automatically from
   * {@code FlixelLoggingBytecodeHooks.class} using the {@code bc<MethodName><Index>} naming convention.
   */
  private static final Map<String, Replacement> FLIXEL_STATIC_REPLACEMENTS = buildStaticReplacements();

  private FlixelLoggerBytecodeWeaver() {}

  /**
   * @return {@code true} if at least one invocation was rewritten.
   */
  public static boolean weave(ClassNode classNode) {
    if (HOOKS_OWNER.equals(classNode.name) || LOGGER_OWNER.equals(classNode.name)) {
      return false;
    }
    // When processing Flixel.java itself, only INVOKESTATIC Flixel.* calls are rewritten (so sites like
    // the crash handler capture the correct Flixel.java location). INVOKEVIRTUAL FlixelLogger.* calls
    // inside Flixel.java are skipped to avoid embedding Flixel.java line numbers inside the static
    // delegation helpers (e.g. warn(tag, message) -> log.warn(tag, message)).
    boolean isFacadeClass = FLIXEL_STATIC_FACADE_INTERNAL.equals(classNode.name);
    boolean changed = false;
    String sourceFile = classNode.sourceFile != null ? classNode.sourceFile : "UnknownFile";
    String classNameDots = classNode.name.replace('/', '.');
    for (MethodNode method : classNode.methods) {
      if (method.instructions == null || method.instructions.size() == 0) {
        continue;
      }
      int currentLine = -1;
      for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
        if (insn instanceof LineNumberNode lineNumberNode) {
          currentLine = lineNumberNode.line;
          continue;
        }
        if (!(insn instanceof MethodInsnNode min)) {
          continue;
        }
        int op = min.getOpcode();
        int line = Math.max(currentLine, 0);

        if (op == Opcodes.INVOKESTATIC && FLIXEL_STATIC_FACADE_INTERNAL.equals(min.owner)) {
          Replacement facadeReplacement = FLIXEL_STATIC_REPLACEMENTS.get(min.name + min.desc);
          if (facadeReplacement != null) {
            insertSiteArguments(method.instructions, min, sourceFile, line, classNameDots, method.name);
            min.owner = HOOKS_OWNER;
            min.name = facadeReplacement.newName();
            min.desc = facadeReplacement.newDescriptor();
            min.itf = false;
            changed = true;
          }
          continue;
        }

        // Skip INVOKEVIRTUAL FlixelLogger rewrites inside Flixel.java itself.
        if (isFacadeClass) {
          continue;
        }

        if (op != Opcodes.INVOKEVIRTUAL && op != Opcodes.INVOKEINTERFACE) {
          continue;
        }
        if (!LOGGER_OWNER.equals(min.owner)) {
          continue;
        }
        Replacement replacement = REPLACEMENTS.get(min.name + min.desc);
        if (replacement == null) {
          continue;
        }
        insertSiteArguments(method.instructions, min, sourceFile, line, classNameDots, method.name);
        min.name = replacement.newName();
        min.desc = replacement.newDescriptor();
        min.itf = false;
        min.setOpcode(Opcodes.INVOKEVIRTUAL);
        changed = true;
      }
    }
    return changed;
  }

  /**
   * Creates a {@link ClassWriter} that recomputes only max stack and locals ({@link ClassWriter#COMPUTE_MAXS}),
   * not full frames. Our transformations only insert {@code LDC}/{@code SIPUSH} sequences before existing
   * invoke instructions; they do not add branches or jump targets, so compiler-generated frames remain
   * valid and do not need to be recalculated. Using {@link ClassWriter#COMPUTE_FRAMES} would require
   * resolving the full class hierarchy via {@link ClassWriter#getCommonSuperClass}, which is unavailable
   * in isolated environments such as Gradle artifact transforms.
   */
  public static ClassWriter newClassWriter(ClassReader reader) {
    return new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
  }

  static AbstractInsnNode pushInt(int v) {
    return switch (v) {
      case -1 -> new InsnNode(Opcodes.ICONST_M1);
      case 0 -> new InsnNode(Opcodes.ICONST_0);
      case 1 -> new InsnNode(Opcodes.ICONST_1);
      case 2 -> new InsnNode(Opcodes.ICONST_2);
      case 3 -> new InsnNode(Opcodes.ICONST_3);
      case 4 -> new InsnNode(Opcodes.ICONST_4);
      case 5 -> new InsnNode(Opcodes.ICONST_5);
      default -> {
        if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
          yield new IntInsnNode(Opcodes.BIPUSH, v);
        }
        if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
          yield new IntInsnNode(Opcodes.SIPUSH, v);
        }
        yield new LdcInsnNode(v);
      }
    };
  }

  /**
   * Scans {@code FlixelLogger.class} for {@code *WithSite} methods and builds a map from each method's
   * original (non-site) {@code name+descriptor} to its {@code WithSite} replacement. New log methods
   * with a matching {@code *WithSite} counterpart are picked up automatically.
   */
  private static Map<String, Replacement> buildVirtualReplacements() {
    ClassNode node = loadClass(LOGGER_OWNER);
    Map<String, Replacement> map = new HashMap<>();
    if (node == null) {
      return map;
    }
    for (MethodNode method : node.methods) {
      if (!method.name.endsWith("WithSite")) {
        continue;
      }
      String origDesc = stripSiteParams(method.desc);
      if (origDesc == null) {
        continue;
      }
      String origName = method.name.substring(0, method.name.length() - "WithSite".length());
      map.put(origName + origDesc, new Replacement(method.name, method.desc));
    }
    return map;
  }

  /**
   * Scans {@code FlixelLoggingBytecodeHooks.class} for {@code bc*} methods and builds a map from each
   * corresponding {@code Flixel.*} method's {@code name+descriptor} to the hook replacement. Hook methods
   * must follow the {@code bc<MethodName><Index>} naming convention (e.g. {@code bcDebug0},
   * {@code bcError2}): the method name is derived by stripping {@code bc}, stripping the trailing digit(s),
   * and lowercasing. The original descriptor is derived by stripping the site params from the hook's
   * descriptor. Add a new {@code bc*} hook following this convention and the weaver picks it up
   * automatically.
   */
  private static Map<String, Replacement> buildStaticReplacements() {
    ClassNode node = loadClass(HOOKS_OWNER);
    Map<String, Replacement> map = new HashMap<>();
    if (node == null) {
      return map;
    }
    for (MethodNode method : node.methods) {
      if (!method.name.startsWith("bc")) {
        continue;
      }
      String flixelMethod = flixelMethodFromHookName(method.name);
      if (flixelMethod == null) {
        continue;
      }
      String origDesc = stripSiteParams(method.desc);
      if (origDesc == null) {
        continue;
      }
      map.put(flixelMethod + origDesc, new Replacement(method.name, method.desc));
    }
    return map;
  }

  /**
   * Derives the corresponding {@code Flixel} method name from a {@code bc*} hook method name by stripping
   * the {@code bc} prefix, stripping the trailing digit(s), and lowercasing the remainder using
   * {@link Locale#ROOT}.
   *
   * <p>For example: {@code bcDebug0} and {@code bcDebug1} both map to {@code debug}; {@code bcError2}
   * maps to {@code error}.
   *
   * @return the derived method name, or {@code null} if the name does not follow the convention.
   */
  private static String flixelMethodFromHookName(String hookName) {
    // Strip "bc" prefix; must have at least one letter after.
    if (hookName.length() <= 2) {
      return null;
    }
    String rest = hookName.substring(2);
    int end = rest.length();
    while (end > 0 && Character.isDigit(rest.charAt(end - 1))) {
      end--;
    }
    if (end == 0) {
      return null;
    }
    return rest.substring(0, end).toLowerCase(Locale.ROOT);
  }

  /**
   * Strips the four trailing site parameters ({@code String, int, String, String}) from a method
   * descriptor. The descriptor must end with {@link #SITE_PARAMS_TAIL} and return {@code void}.
   *
   * @return the original descriptor without site params, or {@code null} if the descriptor does not
   *     end with the expected site params tail.
   */
  private static String stripSiteParams(String desc) {
    if (!desc.endsWith(SITE_PARAMS_TAIL)) {
      return null;
    }
    // Remove the site params and the closing ")V", then re-add ")V".
    return desc.substring(0, desc.length() - SITE_PARAMS_TAIL.length()) + ")V";
  }

  /**
   * Loads the named internal class from the plugin's class loader for discovery purposes.
   *
   * @param internalName the JVM internal name (e.g. {@code org/flixelgdx/logging/FlixelLogger}).
   * @return the parsed {@link ClassNode}, or {@code null} if the class is not available.
   */
  private static ClassNode loadClass(String internalName) {
    try (InputStream in = FlixelLoggerBytecodeWeaver.class.getClassLoader()
        .getResourceAsStream(internalName + ".class")) {
      if (in == null) {
        return null;
      }
      ClassReader reader = new ClassReader(in.readAllBytes());
      ClassNode node = new ClassNode();
      reader.accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
      return node;
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Inserts {@code sourceFile, line, classNameDots, methodName} immediately before {@code invoke},
   * after the original args. Instructions must execute in that order so the operand stack ends as
   * {@code ...; sourceFile; line; class; method} with {@code method} on top, matching JVM parameter
   * popping for {@code *WithSite} and hook signatures (rightmost parameter first).
   */
  private static void insertSiteArguments(
      InsnList list,
      MethodInsnNode invoke,
      String sourceFile,
      int line,
      String classNameDots,
      String methodName) {
    list.insertBefore(invoke, new LdcInsnNode(sourceFile));
    list.insertBefore(invoke, pushInt(line));
    list.insertBefore(invoke, new LdcInsnNode(classNameDots));
    list.insertBefore(invoke, new LdcInsnNode(methodName));
  }

  private record Replacement(String newName, String newDescriptor) {
  }
}
