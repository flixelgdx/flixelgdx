/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.teavm.logging;

import me.stringdotjar.flixelgdx.logging.FlixelStackFrame;
import me.stringdotjar.flixelgdx.logging.FlixelStackTraceProvider;

/**
 * Implementation of {@link FlixelStackTraceProvider} for TeaVM. Stack walking is not available in the same
 * way as on the JVM, so this implementation returns {@code null} for {@link #getCaller()}. The
 * {@link me.stringdotjar.flixelgdx.logging.FlixelLogger} treats a missing caller as an unknown file/line
 * and still emits logs, file output, and in-game log listeners.
 */
public class TeaVMStackTraceProvider implements FlixelStackTraceProvider {

  @Override
  public FlixelStackFrame getCaller() {
    return null;
  }
}
