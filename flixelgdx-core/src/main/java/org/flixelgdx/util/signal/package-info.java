/**
 * Lightweight signal and typed payload types for framework events (state switches, update hooks, etc.).
 *
 * <p>{@link org.flixelgdx.util.signal.FlixelSignal} is a multicast listener list with snapshot iteration
 * so listeners can add or remove during dispatch. Payload structs live under {@link org.flixelgdx.util.signal.FlixelSignalData}.
 *
 * <p>Global hooks are on {@link org.flixelgdx.Flixel.Signals}. Prefer reusing pre-allocated signal data objects
 * where the framework already does so (for example update signals) to avoid per-frame garbage.
 *
 * @see org.flixelgdx.util.signal.FlixelSignal
 * @see org.flixelgdx.Flixel.Signals
 */
package org.flixelgdx.util.signal;
