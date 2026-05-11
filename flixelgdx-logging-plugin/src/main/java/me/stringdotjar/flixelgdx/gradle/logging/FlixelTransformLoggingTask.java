/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.gradle.logging;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Post-processes compiled classes in a directory, rewriting {@code FlixelLogger} calls.
 */
public abstract class FlixelTransformLoggingTask extends DefaultTask {

  @InputDirectory
  @PathSensitive(PathSensitivity.RELATIVE)
  public abstract DirectoryProperty getClassesDirectory();

  @Input
  public abstract Property<Boolean> getVerbose();

  /**
   * Rewrites {@code FlixelLogger} call sites under {@code root}. Used by {@link #run()} and by {@link FlixelLoggingPlugin}
   * via {@code compileJava.doLast(...)} so task registration does not run inside {@code JavaCompile} configuration callbacks.
   *
   * @param root Output directory tree containing {@code .class} files.
   * @param verbose When {@code true}, log each rewritten path at lifecycle level.
   * @param logger Receiver for verbose messages.
   * @throws IOException If walking or writing fails.
   */
  public static void weaveDirectory(Path root, boolean verbose, Logger logger) throws IOException {
    if (!Files.isDirectory(root)) {
      return;
    }
    Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (!file.toString().endsWith(".class") || file.getFileName().toString().equals("module-info.class")) {
          return FileVisitResult.CONTINUE;
        }
        byte[] original = Files.readAllBytes(file);
        ClassReader reader = new ClassReader(original);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.SKIP_FRAMES);
        if (!FlixelLoggerBytecodeWeaver.weave(classNode)) {
          return FileVisitResult.CONTINUE;
        }
        ClassWriter writer = FlixelLoggerBytecodeWeaver.newClassWriter(reader);
        classNode.accept(writer);
        Files.write(file, writer.toByteArray());
        if (verbose) {
          logger.lifecycle("Flixel logging weave: {}", root.relativize(file));
        }
        return FileVisitResult.CONTINUE;
      }
    });
  }

  @TaskAction
  public void run() throws IOException {
    weaveDirectory(getClassesDirectory().get().getAsFile().toPath(), getVerbose().get(), getLogger());
  }
}
