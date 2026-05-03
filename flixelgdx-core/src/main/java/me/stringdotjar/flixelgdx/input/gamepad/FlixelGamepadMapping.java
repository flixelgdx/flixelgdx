/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.input.gamepad;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerMapping;

import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;

/**
 * Translates framework logical buttons and axes to native indices for {@link Controller#getButton(int)}
 * and {@link Controller#getAxis(int)}, using the active {@link ControllerMapping} from the device.
 *
 * <p>Concrete mappings are cached per {@link FlixelGamepadModel}; obtain them with {@link #forModel(FlixelGamepadModel)}.
 */
public abstract class FlixelGamepadMapping {

  private static final EnumMap<FlixelGamepadModel, FlixelGamepadMapping> BY_MODEL = new EnumMap<>(FlixelGamepadModel.class);

  static {
    for (FlixelGamepadModel model : FlixelGamepadModel.values()) {
      if (model == FlixelGamepadModel.WII) {
        BY_MODEL.put(model, FlixelWiiGamepadMapping.INSTANCE);
      } else {
        BY_MODEL.put(model, new FlixelStandardGamepadMapping(model));
      }
    }
  }

  /**
   * Returns the shared mapping instance for a model. Never allocates after class initialization.
   *
   * @param model Controller family.
   * @return Non-null mapping.
   */
  @NotNull
  public static FlixelGamepadMapping forModel(@NotNull FlixelGamepadModel model) {
    FlixelGamepadMapping m = BY_MODEL.get(model);
    return m != null ? m : BY_MODEL.get(FlixelGamepadModel.UNKNOWN);
  }

  /**
   * Model tag carried by this mapping instance. Slot-level detection may still expose a more
   * specific model through {@link FlixelGamepadManager#getDetectedModel(int)}.
   *
   * @return Model constant.
   */
  @NotNull
  public abstract FlixelGamepadModel getModel();

  /**
   * Resolves a logical button to a native button index for the given controller.
   *
   * @param controller Controller providing {@link Controller#getMapping()}.
   * @param logicalButton Code from {@link FlixelGamepadInput}.
   * @return Native index, or {@link ControllerMapping#UNDEFINED} when unsupported for this device.
   */
  public abstract int toNativeButton(@NotNull Controller controller, int logicalButton);

  /**
   * Resolves a logical axis constant to a native axis index.
   *
   * @param controller Controller providing {@link Controller#getMapping()}.
   * @param logicalAxis Value from {@link FlixelGamepadInput} {@code AXIS_*} constants.
   * @return Native axis index, or {@link ControllerMapping#UNDEFINED} when unsupported.
   */
  public abstract int toNativeAxis(@NotNull Controller controller, int logicalAxis);
}
