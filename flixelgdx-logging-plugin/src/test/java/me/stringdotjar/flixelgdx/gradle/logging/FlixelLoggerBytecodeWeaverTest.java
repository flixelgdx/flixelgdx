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
package me.stringdotjar.flixelgdx.gradle.logging;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelLoggerBytecodeWeaverTest {

  @Test
  void weaveRewritesInfoCall() throws Exception {
    Path tmp = Files.createTempDirectory("flixel-log-weave");
    Path srcDir = tmp.resolve("flixel/weavetest");
    Files.createDirectories(srcDir);
    Path source = srcDir.resolve("Caller.java");
    Files.writeString(
      source,
      """
        package flixel.weavetest;
        import me.stringdotjar.flixelgdx.logging.*;
        public class Caller {
          public static void run(FlixelLogger log) {
            log.info("hello");
          }
        }
        """);

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    assertNotNull(compiler);
    Path out = tmp.resolve("classes");
    Files.createDirectories(out);
    String classpath = System.getProperty("java.class.path");
    try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
      Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjects(source.toFile());
      boolean ok = compiler
        .getTask(
          null,
          fileManager,
          null,
          List.of("-classpath", classpath, "-d", out.toString()),
          null,
          units)
        .call();
      assertTrue(ok, "JavaCompiler failed; check test runtime classpath includes flixelgdx-core");
    }

    Path classFile = out.resolve("flixel/weavetest/Caller.class");
    assertTrue(Files.exists(classFile));
    byte[] bytes = Files.readAllBytes(classFile);
    ClassReader reader = new ClassReader(bytes);
    ClassNode classNode = new ClassNode();
    reader.accept(classNode, ClassReader.SKIP_FRAMES);
    assertTrue(FlixelLoggerBytecodeWeaver.weave(classNode));

    boolean foundWithSite = false;
    for (MethodNode method : classNode.methods) {
      for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
        if (insn instanceof MethodInsnNode min && "infoWithSite".equals(min.name)) {
          foundWithSite = true;
        }
      }
    }
    assertTrue(foundWithSite);
  }

  @Test
  void weaveRewritesFlixelStaticInfoCall() throws Exception {
    Path tmp = Files.createTempDirectory("flixel-log-weave-facade");
    Path srcDir = tmp.resolve("flixel/weavetestfacade");
    Files.createDirectories(srcDir);
    Path source = srcDir.resolve("FacadeCaller.java");
    Files.writeString(
      source,
      """
        package flixel.weavetestfacade;
        import me.stringdotjar.flixelgdx.Flixel;
        public class FacadeCaller {
          public static void run() {
            Flixel.info("hello");
          }
        }
        """);

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    assertNotNull(compiler);
    Path out = tmp.resolve("classes");
    Files.createDirectories(out);
    String classpath = System.getProperty("java.class.path");
    try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
      Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjects(source.toFile());
      boolean ok = compiler
        .getTask(
          null,
          fileManager,
          null,
          List.of("-classpath", classpath, "-parameters", "-g", "-d", out.toString()),
          null,
          units)
        .call();
      assertTrue(ok, "JavaCompiler failed; check test runtime classpath includes flixelgdx-core");
    }

    Path classFile = out.resolve("flixel/weavetestfacade/FacadeCaller.class");
    assertTrue(Files.exists(classFile));
    byte[] bytes = Files.readAllBytes(classFile);
    ClassReader reader = new ClassReader(bytes);
    ClassNode classNode = new ClassNode();
    reader.accept(classNode, ClassReader.SKIP_FRAMES);
    assertTrue(FlixelLoggerBytecodeWeaver.weave(classNode));

    boolean foundBcHook = false;
    for (MethodNode method : classNode.methods) {
      for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
        if (insn instanceof MethodInsnNode min && "bcInfo0".equals(min.name)) {
          assertTrue(min.owner.contains("FlixelLoggingBytecodeHooks"));
          foundBcHook = true;
        }
      }
    }
    assertTrue(foundBcHook);
  }

  @Test
  void weaveSkipsFlixelStaticFacadeClass() throws Exception {
    byte[] bytes;
    try (InputStream in =
           FlixelLoggerBytecodeWeaverTest.class.getClassLoader().getResourceAsStream("me/stringdotjar/flixelgdx/Flixel.class")) {
      assertNotNull(in, "Flixel.class must be on the test classpath via flixelgdx-core");
      bytes = in.readAllBytes();
    }
    ClassReader reader = new ClassReader(bytes);
    ClassNode classNode = new ClassNode();
    reader.accept(classNode, ClassReader.SKIP_FRAMES);
    assertFalse(FlixelLoggerBytecodeWeaver.weave(classNode));

    boolean foundWithSite = false;
    for (MethodNode method : classNode.methods) {
      for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
        if (insn instanceof MethodInsnNode min && min.name != null && min.name.endsWith("WithSite")) {
          foundWithSite = true;
        }
      }
    }
    assertFalse(foundWithSite);
  }
}
