/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.gradle.logging;

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
 * Rewrites {@code FlixelLogger} {@code info}, {@code warn}, and {@code error} calls to {@code *WithSite} overloads
 * using the enclosing class {@code SourceFile} attribute and {@link LineNumberNode} data.
 */
public final class FlixelLoggerBytecodeWeaver {

  private static final String LOGGER_OWNER = "me/stringdotjar/flixelgdx/logging/FlixelLogger";

  /**
   * Flixel static {@code info}, {@code warn}, and {@code error} helpers delegate to {@code FlixelLogger}. Weaving those
   * invokes would attribute every caller to the delegate lines in {@code Flixel.java}; skipping lets runtime caller
   * resolution see the real call site (with matching skips in {@code FlixelDefaultStackTraceProvider} on the JVM).
   */
  private static final String FLIXEL_STATIC_FACADE_INTERNAL = "me/stringdotjar/flixelgdx/Flixel";

  private static final Map<String, Replacement> REPLACEMENTS = new HashMap<>();

  static {
    REPLACEMENTS.put(
      "info(Ljava/lang/Object;)V",
      new Replacement("infoWithSite", "(Ljava/lang/Object;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V"));
    REPLACEMENTS.put(
      "info(Ljava/lang/String;Ljava/lang/Object;)V",
      new Replacement(
        "infoWithSite",
        "(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V"));
    REPLACEMENTS.put(
      "warn(Ljava/lang/Object;)V",
      new Replacement("warnWithSite", "(Ljava/lang/Object;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V"));
    REPLACEMENTS.put(
      "warn(Ljava/lang/String;Ljava/lang/Object;)V",
      new Replacement(
        "warnWithSite",
        "(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V"));
    REPLACEMENTS.put(
      "error(Ljava/lang/Object;)V",
      new Replacement("errorWithSite", "(Ljava/lang/Object;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V"));
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
  }

  private record Replacement(String newName, String newDescriptor) {}

  private FlixelLoggerBytecodeWeaver() {}

  /**
   * @return {@code true} if at least one invocation was rewritten.
   */
  public static boolean weave(ClassNode classNode) {
    if (FLIXEL_STATIC_FACADE_INTERNAL.equals(classNode.name)) {
      return false;
    }
    boolean changed = false;
    String sourceFile = classNode.sourceFile != null ? classNode.sourceFile : "UnknownFile.java";
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
        int line = currentLine >= 0 ? currentLine : 0;
        insertSiteArguments(method.instructions, min, sourceFile, line, classNameDots, method.name);
        min.name = replacement.newName;
        min.desc = replacement.newDescriptor;
        min.itf = false;
        min.setOpcode(Opcodes.INVOKEVIRTUAL);
        changed = true;
      }
    }
    return changed;
  }

  /**
   * Inserts {@code sourceFile, line, classNameDots, methodName} immediately before {@code invoke}, after the original args.
   */
  private static void insertSiteArguments(
    InsnList list,
    MethodInsnNode invoke,
    String sourceFile,
    int line,
    String classNameDots,
    String methodName) {
    list.insertBefore(invoke, new LdcInsnNode(methodName));
    list.insertBefore(invoke, new LdcInsnNode(classNameDots));
    list.insertBefore(invoke, pushInt(line));
    list.insertBefore(invoke, new LdcInsnNode(sourceFile));
  }

  static AbstractInsnNode pushInt(int v) {
    switch (v) {
      case -1:
        return new InsnNode(Opcodes.ICONST_M1);
      case 0:
        return new InsnNode(Opcodes.ICONST_0);
      case 1:
        return new InsnNode(Opcodes.ICONST_1);
      case 2:
        return new InsnNode(Opcodes.ICONST_2);
      case 3:
        return new InsnNode(Opcodes.ICONST_3);
      case 4:
        return new InsnNode(Opcodes.ICONST_4);
      case 5:
        return new InsnNode(Opcodes.ICONST_5);
      default:
        if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
          return new IntInsnNode(Opcodes.BIPUSH, v);
        }
        if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
          return new IntInsnNode(Opcodes.SIPUSH, v);
        }
        return new LdcInsnNode(v);
    }
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
