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
@file:JvmName("FlixelSignals")

package org.flixelgdx.ktx.signal

import org.flixelgdx.util.signal.FlixelSignal
import org.flixelgdx.util.signal.FlixelSignal.SignalHandler

/**
 * Idiomatic Kotlin operators for [FlixelSignal].
 *
 * Adding a listener with a lambda already works, since [SignalHandler] has a single method. This
 * file adds `+=`/`-=` for registering listeners and the `invoke` operator so a signal can be fired
 * by calling it directly, as in `onDeath(player)` or `onTick()`.
 */

/** Registers [handler] as a permanent listener, so `signal += { ... }` reads naturally. */
operator fun <T> FlixelSignal<T>.plusAssign(handler: SignalHandler<T>) = add(handler)

/** Removes a previously registered [handler], mirroring [plusAssign]. */
operator fun <T> FlixelSignal<T>.minusAssign(handler: SignalHandler<T>) = remove(handler)

/** Fires the signal with no data, so `signal()` reads like a function call. */
operator fun <T> FlixelSignal<T>.invoke() = dispatch()

/** Fires the signal with [data], so `signal(value)` reads like a function call. */
operator fun <T> FlixelSignal<T>.invoke(data: T) = dispatch(data)
