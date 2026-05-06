/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.gradle.logging;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

/**
 * Gradle DSL for {@code flixelLogging} when using {@link FlixelLoggingPlugin}.
 */
public class FlixelLoggingExtension {

  private final Property<Boolean> enabled;
  private final Property<Boolean> verbose;

  @Inject
  public FlixelLoggingExtension(ObjectFactory objects) {
    enabled = objects.property(Boolean.class).convention(true);
    verbose = objects.property(Boolean.class).convention(false);
  }

  public Property<Boolean> getEnabled() {
    return enabled;
  }

  public Property<Boolean> getVerbose() {
    return verbose;
  }
}
