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
package org.flixelgdx.backend.html5.logging;

import org.jetbrains.annotations.NotNull;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.StringConstantInstruction;

import java.util.Set;

/**
 * A TeaVM class transformer that inserts a call-site marker before every FlixelGDX log call.
 *
 * <p>For each call to {@code debug}, {@code info}, {@code warn}, {@code error}, or {@code log} on
 * {@code Flixel}, {@code FlixelLogger}, or {@code FlixelDefaultLogger}, this adds the equivalent of
 * the following line right before the call:
 *
 * <pre>{@code
 * FlixelLogSiteMarker.mark("PlayState.java", 42, "org.game.PlayState", "update");
 * }</pre>
 *
 * <p>The file name and line number come from the debug information that TeaVM keeps for each
 * instruction. The class and method names come from the class and method being transformed. The
 * marker only holds one pending site at a time, and the logger reads and clears it when it builds
 * the entry, so a call that logs nothing never leaves a stale note behind.
 *
 * <p>Some details worth knowing:
 *
 * <ul>
 *   <li>Classes in the {@code org.flixelgdx.logging} package are never touched, so the logger's own
 *       internal calls cannot overwrite the game's site.
 *   <li>Inside {@code Flixel}, the static facade methods {@code debug}, {@code info}, {@code warn},
 *       and {@code error} are skipped for the same reason, because they forward to the logger.
 *   <li>The file name is only the last part of the path (for example {@code PlayState.java}),
 *       because the package is printed from the class name.
 *   <li>If a call has no location information (the class was compiled without debug info), the
 *       marker is still inserted with the file {@code Unknown} and line {@code 0}, so the class and
 *       method are still known.
 *   <li>The body of a lambda is reported under the name of the method that contains the lambda
 *       instead of the compiler generated name such as {@code lambda$update$0}.
 *   <li>Only calls whose declared owner is exactly one of the three types above are marked. A call
 *       through a custom subclass type is not.
 * </ul>
 *
 * <p>This code runs inside the TeaVM compiler at build time, never in the browser, so it is free to
 * use the standard Java collections.
 *
 * @see FlixelLogSiteTeaVMPlugin
 */
public final class FlixelLogSiteTransformer implements ClassHolderTransformer {

  private static final String LOG_PACKAGE = "org.flixelgdx.logging.";
  private static final String FACADE_CLASS = "org.flixelgdx.Flixel";
  private static final String UNKNOWN_FILE = "Unknown";
  private static final String LAMBDA_PREFIX = "lambda$";

  private static final Set<String> LOG_OWNERS = Set.of(
      FACADE_CLASS,
      "org.flixelgdx.logging.FlixelLogger",
      "org.flixelgdx.logging.FlixelDefaultLogger");

  private static final Set<String> LOG_METHODS = Set.of("debug", "info", "warn", "error", "log");
  private static final Set<String> FACADE_METHODS = Set.of("debug", "info", "warn", "error");

  private static final MethodReference MARK = new MethodReference(
      "org.flixelgdx.logging.FlixelLogSiteMarker",
      "mark",
      ValueType.object("java.lang.String"),
      ValueType.INTEGER,
      ValueType.object("java.lang.String"),
      ValueType.object("java.lang.String"),
      ValueType.VOID);

  @Override
  public void transformClass(@NotNull ClassHolder cls, @NotNull ClassHolderTransformerContext context) {
    String className = cls.getName();
    if (className.startsWith(LOG_PACKAGE)) {
      return;
    }
    boolean isFacade = className.equals(FACADE_CLASS);
    for (MethodHolder method : cls.getMethods()) {
      if (isFacade && FACADE_METHODS.contains(method.getName())) {
        continue;
      }
      // Abstract and native methods have no body to transform.
      if (!method.hasProgram()) {
        continue;
      }
      Program program = method.getProgram();
      if (program == null) {
        continue;
      }
      transformProgram(program, className, displayName(method.getName()));
    }
  }

  private static void transformProgram(Program program, String className, String methodName) {
    for (int i = 0; i < program.basicBlockCount(); i++) {
      BasicBlock block = program.basicBlockAt(i);
      Instruction insn = block.getFirstInstruction();
      while (insn != null) {
        // Read the next instruction first, because inserting before insn must not make the loop
        // visit the instructions that were just added.
        Instruction next = insn.getNext();
        if (insn instanceof InvokeInstruction invoke && isLogCall(invoke)) {
          insertMarker(program, invoke, className, methodName);
        }
        insn = next;
      }
    }
  }

  private static boolean isLogCall(InvokeInstruction invoke) {
    MethodReference ref = invoke.getMethod();
    return LOG_OWNERS.contains(ref.getClassName()) && LOG_METHODS.contains(ref.getName());
  }

  private static void insertMarker(Program program, InvokeInstruction invoke, String className, String methodName) {
    TextLocation location = invoke.getLocation();
    String file = UNKNOWN_FILE;
    int line = 0;
    if (location != null && !location.isEmpty()) {
      line = Math.max(location.getLine(), 0);
      String name = location.getFileName();
      if (name != null && !name.isEmpty()) {
        file = lastSegment(name);
      }
    }

    Variable fileVar = program.createVariable();
    Variable lineVar = program.createVariable();
    Variable classVar = program.createVariable();
    Variable methodVar = program.createVariable();

    invoke.insertPrevious(stringConstant(fileVar, file, location));
    invoke.insertPrevious(intConstant(lineVar, line, location));
    invoke.insertPrevious(stringConstant(classVar, className, location));
    invoke.insertPrevious(stringConstant(methodVar, methodName, location));

    InvokeInstruction mark = new InvokeInstruction();
    mark.setType(InvocationType.SPECIAL);
    mark.setMethod(MARK);
    mark.setArguments(fileVar, lineVar, classVar, methodVar);
    mark.setLocation(location);
    invoke.insertPrevious(mark);
  }

  private static StringConstantInstruction stringConstant(Variable receiver, String value, TextLocation location) {
    StringConstantInstruction insn = new StringConstantInstruction();
    insn.setReceiver(receiver);
    insn.setConstant(value);
    insn.setLocation(location);
    return insn;
  }

  private static IntegerConstantInstruction intConstant(Variable receiver, int value, TextLocation location) {
    IntegerConstantInstruction insn = new IntegerConstantInstruction();
    insn.setReceiver(receiver);
    insn.setConstant(value);
    insn.setLocation(location);
    return insn;
  }

  private static String lastSegment(String path) {
    int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
    return slash >= 0 ? path.substring(slash + 1) : path;
  }

  /**
   * Turns a compiler generated lambda method name such as {@code lambda$update$0} back into the name
   * of the method that contains the lambda ({@code update}). Other names are returned unchanged.
   */
  private static String displayName(String methodName) {
    if (methodName.startsWith(LAMBDA_PREFIX)) {
      int end = methodName.lastIndexOf('$');
      if (end > LAMBDA_PREFIX.length()) {
        return methodName.substring(LAMBDA_PREFIX.length(), end);
      }
    }
    return methodName;
  }
}
