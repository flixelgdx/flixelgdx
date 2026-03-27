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
 * Implementation of {@link FlixelStackTraceProvider} for TeaVM.
 * Since TeaVM does not support stack traces, this implementation returns null.
 */
public class TeaVMStackTraceProvider implements FlixelStackTraceProvider {

  @Override
  public FlixelStackFrame getCaller() {
    return null;
  }
}
