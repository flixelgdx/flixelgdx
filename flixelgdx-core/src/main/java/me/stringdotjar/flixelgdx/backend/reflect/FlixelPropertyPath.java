/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.reflect;

/**
 * Result of resolving a dotted property path (e.g. {@code "weapon.rotation"}) to a leaf instance
 * and the final segment name used for field/property access.
 *
 * @param leafObject The object that owns the leaf property (after walking all but the last segment).
 * @param leafName The final segment (e.g. {@code "rotation"}), comparable to VarTween goal keys.
 */
public record FlixelPropertyPath(Object leafObject, String leafName) {}
