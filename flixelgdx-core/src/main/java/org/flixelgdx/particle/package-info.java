/**
 * Lightweight particle effects such as sparks, smoke, fire, rain, and explosions.
 *
 * <p>Think of an emitter like a fountain: you choose where it sits, how it sprays, and how long each
 * drop lasts, and it handles launching, moving, and cleaning up every drop for you.
 *
 * <ul>
 *   <li>{@link org.flixelgdx.particle.FlixelEmitter FlixelEmitter} - owns a fixed number of
 *       particles, launches them in bursts or continuously, and draws them all in one batch.</li>
 *   <li>{@link org.flixelgdx.particle.FlixelParticle FlixelParticle} - a single particle. Plain
 *       numbers, no graphic of its own. Subclass it for custom behavior.</li>
 *   <li>{@link org.flixelgdx.particle.FlixelBounds FlixelBounds},
 *       {@link org.flixelgdx.particle.FlixelRangeBounds FlixelRangeBounds}, and their point and color
 *       variants - the HaxeFlixel-style range objects used to configure an emitter. Bounds pick one
 *       random value per particle; range bounds pick a start and an end value and slide between them
 *       over the particle's life.</li>
 * </ul>
 *
 * <p>Particles are created once when the emitter is built and reused forever after, so a running
 * emitter never allocates. They are purely visual and do not collide; use sprites in a recycled
 * group for gameplay objects that need collision.
 *
 * @see org.flixelgdx.particle.FlixelEmitter
 */
package org.flixelgdx.particle;
