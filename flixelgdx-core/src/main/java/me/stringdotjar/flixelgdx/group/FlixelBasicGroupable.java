/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.group;

import me.stringdotjar.flixelgdx.FlixelBasic;

/**
 * A {@link FlixelGroupable} whose members are constrained to {@link FlixelBasic}.
 *
 * <p>Use this in engine systems that assume FlixelGDX lifecycle and fields like
 * {@code exists}, {@code active}, or {@code visible}. External libGDX projects
 * that do not want to extend {@link FlixelBasic} can implement {@link FlixelGroupable}
 * directly instead.
 *
 * @param <T> The member type.
 */
public interface FlixelBasicGroupable<T extends FlixelBasic> extends FlixelGroupable<T> {}

