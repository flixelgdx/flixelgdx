/**
 * Fine-grained behavior interfaces that make up the FlixelGDX object model.
 *
 * <p>Rather than a single monolithic base class, the framework breaks its object contract into
 * small, composable interfaces. Game code and framework internals can then depend on only the
 * capability they actually need: a physics system only requires
 * {@link org.flixelgdx.functional.FlixelPhysical FlixelPhysical}; a shader pass only requires
 * {@link org.flixelgdx.functional.FlixelShaderable FlixelShaderable}; a tween only requires
 * {@link org.flixelgdx.functional.FlixelPositional FlixelPositional} or
 * {@link org.flixelgdx.functional.FlixelColorable FlixelColorable}.
 *
 * <h2>Core lifecycle interfaces</h2>
 * <ul>
 *   <li>{@link org.flixelgdx.functional.FlixelUpdatable FlixelUpdatable} - per-frame
 *       {@code update(float elapsed)} tick.</li>
 *   <li>{@link org.flixelgdx.functional.FlixelDrawable FlixelDrawable} - per-frame
 *       {@code draw(batch)} call.</li>
 *   <li>{@link org.flixelgdx.functional.FlixelDestroyable FlixelDestroyable} - one-time teardown
 *       that releases resources.</li>
 *   <li>{@link org.flixelgdx.functional.FlixelKillable FlixelKillable} - soft removal with
 *       {@code kill()} and {@code revive()}, keeping the object in memory for reuse.</li>
 *   <li>{@link org.flixelgdx.functional.FlixelExistable FlixelExistable} - the {@code exists} flag
 *       that the group update/draw loops check before ticking a member.</li>
 * </ul>
 *
 * <h2>Property interfaces</h2>
 * <ul>
 *   <li>{@link org.flixelgdx.functional.FlixelPositional FlixelPositional} - {@code x} and
 *       {@code y} world coordinates.</li>
 *   <li>{@link org.flixelgdx.functional.FlixelPhysical FlixelPhysical} - velocity and
 *       acceleration vectors.</li>
 *   <li>{@link org.flixelgdx.functional.FlixelAngleable FlixelAngleable} - rotation angle in
 *       degrees.</li>
 *   <li>{@link org.flixelgdx.functional.FlixelColorable FlixelColorable} - RGBA tint color.</li>
 *   <li>{@link org.flixelgdx.functional.FlixelVisible FlixelVisible} - visibility toggle.</li>
 *   <li>{@link org.flixelgdx.functional.FlixelShaderable FlixelShaderable} - custom shader
 *       assignment.</li>
 *   <li>{@link org.flixelgdx.functional.FlixelAntialiasable FlixelAntialiasable} - antialiasing
 *       toggle.</li>
 *   <li>{@link org.flixelgdx.functional.FlixelShakeable FlixelShakeable} - shake effect support
 *       (used by tweens).</li>
 * </ul>
 *
 * <h2>The full contract</h2>
 * <p>{@link org.flixelgdx.functional.IFlixelBasic IFlixelBasic} combines all the lifecycle interfaces
 * into the complete FlixelBasic contract. Implement it directly if you have an existing class
 * hierarchy and cannot extend {@link org.flixelgdx.FlixelBasic FlixelBasic}, but still want to
 * add instances to a {@link org.flixelgdx.FlixelState FlixelState} or
 * {@link org.flixelgdx.group.FlixelBasicGroup FlixelBasicGroup}.
 *
 * <h2>Primitive suppliers</h2>
 * <p>The {@link org.flixelgdx.functional.supplier supplier} sub-package contains functional
 * interfaces like {@link org.flixelgdx.functional.supplier.FloatSupplier FloatSupplier} that
 * return primitives without boxing, for use in tween targets and other hot-path callbacks.
 *
 * @see org.flixelgdx.functional.IFlixelBasic
 * @see org.flixelgdx.FlixelBasic
 */
package org.flixelgdx.functional;
