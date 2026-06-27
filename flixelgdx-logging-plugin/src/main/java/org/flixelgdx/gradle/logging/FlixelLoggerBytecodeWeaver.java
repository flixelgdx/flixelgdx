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

import java.util.HashMap;
import java.util.Map;

/**
 * Rewrites {@code FlixelLogger} {@code info}, {@code warn}, and {@code error} calls to {@code *WithSite} overloads,
 * and {@code Flixel} static logging helpers to {@code FlixelLoggingBytecodeHooks}, using the enclosing class
 * {@code SourceFile} attribute and {@link LineNumberNode} data.
 */
public final class FlixelLoggerBytecodeWeaver {

  private static final String LOGGER_OWNER = "org/flixelgdx/logging/FlixelLogger";

  /**
   * Flixel static {@code info}, {@code warn}, and {@code error} helpers delegate to {@code FlixelLogger}. Rewriting
   * {@code Flixel} itself would only capture {@code Flixel.java} line numbers, so {@link #weave(ClassNode)} skips that
   * class. Call sites in game bytecode use {@code INVOKESTATIC Flixel...}; those are rewritten to
   * {@code FlixelLoggingBytecodeHooks} so line metadata comes from the caller class (critical for TeaVM
   * where stack walking is unavailable).
   */
  private static final String FLIXEL_STATIC_FACADE_INTERNAL = "org/flixelgdx/Flixel";

  private static final String HOOKS_OWNER = "org/flixelgdx/logging/FlixelLoggingBytecodeHooks";

  private static final Map<String, Replacement> REPLACEMENTS = new HashMap<>();

  private static final Map<String, Replacement> FLIXEL_STATIC_REPLACEMENTS = new HashMap<>();

  static {
    REPLACEMENTS.put(
        "info(Ljava/lang/Object;)V",
        new Replacement("infoWithSite",
            "(Ljava/lang/Object;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V"));
    REPLACEMENTS.put(
        "info(Ljava/lang/String;Ljava/lang/Object;)V",
        new Replacement(
            "infoWithSite",
            "(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V"));
    REPLACEMENTS.put(
        "warn(Ljava/lang/Object;)V",
        new Replacement("warnWithSite",
            "(Ljava/lang/Object;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V"));
    REPLACEMENTS.put(
        "warn(Ljava/lang/String;Ljava/lang/Object;)V",
        new Replacement(
            "warnWithSite",
            "(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V"));
    REPLACEMENTS.put(
        "error(Ljava/lang/Object;)V",
        new Replacement("errorWithSite",
            "(Ljava/lang/Object;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V"));
    REPLACEMENTS.put(
        "error(Ljava/lang/Object;Ljava/lang/Throwable;)V",
        new Replacement(
            "errorWithSite",
            "(Ljava/lang/Object;Ljava/lang/Throwable;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V"));
    REPLACEMENTS.put(
        "error(Ljava/lang/String;Ljava/lang/Object;)V",
        new Replacement(
            "errorWithSite",
            "(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V"));
    REPLACEMENTS.put(
        "error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Throwable;)V",
        new Replacement(
            "errorWithSite",
            "(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Throwable;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V"));

    FLIXEL_STATIC_REPLACEMENTS.put(
        "info(Ljava/lang/Object;)V",
        new Replacement(
            "bcInfo0", "(Ljava/lang/Object;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V"));
    FLIXEL_STATIC_REPLACEMENTS.put(
        "info(Ljava/lang/String;Ljava/lang/Object;)V",
        new Replacement(
            "bcInfo1",
            "(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V"));
    FLIXEL_STATIC_REPLACEMENTS.put(
        "warn(Ljava/lang/Object;)V",
        new Replacement(
            "bcWarn0", "(Ljava/lang/Object;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V"));
    FLIXEL_STATIC_REPLACEMENTS.put(
        "warn(Ljava/lang/String;Ljava/lang/Object;)V",
        new Replacement(
            "bcWarn1",
            "(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V"));
    FLIXEL_STATIC_REPLACEMENTS.put(
        "error(Ljava/lang/String;)V",
        new Replacement(
            "bcErr0", "(Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V"));
    FLIXEL_STATIC_REPLACEMENTS.put(
        "error(Ljava/lang/String;Ljava/lang/Object;)V",
        new Replacement(
            "bcErr1",
            "(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V"));
    FLIXEL_STATIC_REPLACEMENTS.put(
        "error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Throwable;)V",
        new Replacement(
            "bcErr2",
            "(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Throwable;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V"));
  }

  private record Replacement(String newName, String newDescriptor) {
  }

  private FlixelLoggerBytecodeWeaver() {}

  /**
   * @return {@code true} if at least one invocation was rewritten.
   */
  public static boolean weave(ClassNode classNode) {
    if (FLIXEL_STATIC_FACADE_INTERNAL.equals(classNode.name)) {
      return false;
    }
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
   * Inserts {@code sourceFile, line, classNameDots, methodName} immediately before {@code invoke}, after the original args.
   * Instructions must execute in that order so the operand stack ends as {@code ...; sourceFile; line; class; method} with
   * {@code method} on top, matching JVM parameter popping for {@code *WithSite} and hook signatures (rightmost parameter first).
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

  public static ClassWriter newClassWriter(ClassReader reader) {
    return new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES) {
      @Override
      protected String getCommonSuperClass(String type1, String type2) {
        try {
          return super.getCommonSuperClass(type1, type2);
        } catch (Exception e) {
          return "java/lang/Object";
        }
      }
    };
  }
}
