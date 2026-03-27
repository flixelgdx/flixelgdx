/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.functional;

/**
 * Represents a supplier of {@code byte}-valued results.
 */
@FunctionalInterface
public interface ByteSupplier {

  /**
   * Gets a byte result.
   *
   * @return a result.
   */
  byte getAsByte();
}

