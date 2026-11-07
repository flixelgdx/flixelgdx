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
package org.flixelgdx.debug;

import org.flixelgdx.FlixelBasic;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.FlixelObject;
import org.flixelgdx.FlixelSprite;
import org.flixelgdx.FlixelState;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.debug.inspector.FlixelSectionBuilder;
import org.flixelgdx.debug.panels.FlixelDebugInspectorPanel;
import org.flixelgdx.group.FlixelBasicGroup;
import org.flixelgdx.group.FlixelGroupable;
import org.flixelgdx.group.FlixelSpriteGroup;
import org.flixelgdx.particle.FlixelEmitter;
import org.flixelgdx.text.FlixelText;
import org.flixelgdx.util.FlixelColor;
import org.flixelgdx.util.FlixelString;

import java.util.function.Function;

/**
 * The inspector sections that the framework registers for its own classes.
 *
 * <p>Each section lists the values a game developer usually wants to look at when a class is selected, and makes
 * a value editable wherever the class has a public setter or field. Together they are what makes a selected
 * sprite show up with its position, size, motion, appearance, and animation without any setup.
 */
final class FlixelDebugDefaultSections {

  private static final char[] HEX = "0123456789ABCDEF".toCharArray();

  private FlixelDebugDefaultSections() {}

  /**
   * Registers the built-in sections.
   *
   * @param inspector The panel to register them on.
   */
  static void register(FlixelDebugInspectorPanel inspector) {
    inspector.register(FlixelBasic.class, "Basic", b -> b
        .intProp("ID", o -> o.id).once()
        .boolProp("Exists", FlixelBasic::isExists, FlixelBasic::setExists)
        .boolProp("Alive", o -> o.alive, (o, v) -> o.alive = v)
        .boolProp("Active", FlixelBasic::isActive, FlixelBasic::setActive)
        .boolProp("Visible", FlixelBasic::isVisible, FlixelBasic::setVisible));

    inspector.register(FlixelObject.class, "Object", b -> b
        .floatProp("X", FlixelObject::getX, FlixelObject::setX)
        .floatProp("Y", FlixelObject::getY, FlixelObject::setY)
        .floatProp("Width", FlixelObject::getWidth, FlixelObject::setWidth)
        .floatProp("Height", FlixelObject::getHeight, FlixelObject::setHeight)
        .floatProp("Angle", FlixelObject::getAngle, FlixelObject::setAngle)
        .floatProp("Velocity X", FlixelObject::getVelocityX, FlixelObject::setVelocityX)
        .floatProp("Velocity Y", FlixelObject::getVelocityY, FlixelObject::setVelocityY)
        .floatProp("Acceleration X", FlixelObject::getAccelerationX, FlixelObject::setAccelerationX)
        .floatProp("Acceleration Y", FlixelObject::getAccelerationY, FlixelObject::setAccelerationY)
        .floatProp("Drag X", FlixelObject::getDragX, FlixelObject::setDragX)
        .floatProp("Drag Y", FlixelObject::getDragY, FlixelObject::setDragY)
        .floatProp("Max velocity X", FlixelObject::getMaxVelocityX, FlixelObject::setMaxVelocityX)
        .floatProp("Max velocity Y", FlixelObject::getMaxVelocityY, FlixelObject::setMaxVelocityY)
        .floatProp("Angular velocity", FlixelObject::getAngularVelocity, FlixelObject::setAngularVelocity)
        .floatProp("Scroll factor X", FlixelObject::getScrollX, (o, v) -> o.setScrollFactor(v, o.getScrollY()))
        .floatProp("Scroll factor Y", FlixelObject::getScrollY, (o, v) -> o.setScrollFactor(o.getScrollX(), v))
        .boolProp("Moves", FlixelObject::getMoves, FlixelObject::setMoves)
        .boolProp("Solid", FlixelObject::isSolid, FlixelObject::setSolid)
        .boolProp("Immovable", FlixelObject::isImmovable, FlixelObject::setImmovable)
        .intProp("Touching", FlixelObject::getTouching));

    inspector.register(FlixelSprite.class, "Sprite", b -> {
      b.floatProp("Scale X", FlixelSprite::getScaleX, FlixelSprite::setScaleX)
          .floatProp("Scale Y", FlixelSprite::getScaleY, FlixelSprite::setScaleY)
          .floatProp("Origin X", FlixelSprite::getOriginX, (s, v) -> s.setOrigin(v, s.getOriginY()))
          .floatProp("Origin Y", FlixelSprite::getOriginY, (s, v) -> s.setOrigin(s.getOriginX(), v))
          .floatProp("Offset X", FlixelSprite::getOffsetX, FlixelSprite::setOffsetX)
          .floatProp("Offset Y", FlixelSprite::getOffsetY, FlixelSprite::setOffsetY)
          .floatProp("Alpha", FlixelSprite::getAlpha, FlixelSprite::setAlpha);
      color(b, "Color", FlixelSprite::getColor);
      b.boolProp("Flip X", FlixelSprite::isFlipX, FlixelSprite::setFlipX)
          .boolProp("Flip Y", FlixelSprite::isFlipY, FlixelSprite::setFlipY)
          .boolProp("Antialiasing", FlixelSprite::isAntialiasing, FlixelSprite::setAntialiasing)
          .intProp("Facing", FlixelSprite::getFacing, FlixelSprite::setFacing)
          .enumProp("Blend mode", FlixelSprite::getBlendMode)
          .stringProp("Animation", (s, out) -> {
            if (s.animation != null) {
              out.concat(s.animation.getCurrentAnim());
            }
          })
          .intProp("Animation frame", s -> s.animation != null ? s.animation.getCurrentKeyframeIndex() : 0)
          .intProp("Region width", FlixelSprite::getRegionWidth)
          .intProp("Region height", FlixelSprite::getRegionHeight);
    });

    inspector.register(FlixelText.class, "Text", b -> b
        .stringProp("Text", (t, out) -> out.concat(t.getTextBuffer()), FlixelText::setText)
        .intProp("Size", FlixelText::getTextSize, FlixelText::setTextSize)
        .enumProp("Alignment", FlixelText::getAlignment)
        .boolProp("Word wrap", FlixelText::isWordWrap, FlixelText::setWordWrap)
        .boolProp("Auto size", FlixelText::isAutoSize, FlixelText::setAutoSize)
        .floatProp("Field width", FlixelText::getFieldWidth, FlixelText::setFieldWidth)
        .boolProp("Bold", FlixelText::isBold, FlixelText::setBold)
        .boolProp("Italic", FlixelText::isItalic, FlixelText::setItalic)
        .floatProp("Letter spacing", FlixelText::getLetterSpacing, FlixelText::setLetterSpacing)
        .stringProp("Font", (t, out) -> {
          if (t.getFont() != null) {
            out.concat(t.getFont());
          }
        })
        .enumProp("Border style", FlixelText::getBorderStyle)
        .intProp("Lines", FlixelText::getLineCount));

    inspector.register(FlixelCamera.class, "Camera", b -> {
      b.floatProp("X", c -> c.x, (c, v) -> c.setPosition(v, c.y))
          .floatProp("Y", c -> c.y, (c, v) -> c.setPosition(c.x, v))
          .intProp("Width", c -> c.width, (c, v) -> c.setSize(v, c.height))
          .intProp("Height", c -> c.height, (c, v) -> c.setSize(c.width, v))
          .floatProp("Scroll X", c -> c.scrollX, (c, v) -> c.scrollX = v)
          .floatProp("Scroll Y", c -> c.scrollY, (c, v) -> c.scrollY = v)
          .floatProp("Zoom", FlixelCamera::getZoom, FlixelCamera::setZoom)
          .floatProp("Angle", c -> c.angle, (c, v) -> c.angle = v)
          .floatProp("Alpha", c -> c.alpha, (c, v) -> c.alpha = v)
          .floatProp("Follow lerp", c -> c.followLerp, (c, v) -> c.followLerp = v)
          .enumProp("Follow style", c -> c.style)
          .boolProp("Following", c -> c.target != null)
          .boolProp("Pixel perfect", c -> c.pixelPerfectRender, (c, v) -> c.pixelPerfectRender = v)
          .boolProp("Default draw target", c -> c.defaultDrawTarget, (c, v) -> c.defaultDrawTarget = v)
          .boolProp("Flashing", FlixelCamera::isFlashActive)
          .boolProp("Fading", FlixelCamera::isFadeActive);
      color(b, "Background", c -> c.bgColor);
    });

    inspector.register(FlixelBasicGroup.class, "Group", b -> b
        .intProp("Members", FlixelDebugDefaultSections::count)
        .intProp("Max size", FlixelBasicGroup::getMaxSize, FlixelBasicGroup::setMaxSize));

    inspector.register(FlixelSpriteGroup.class, "Sprite group", b -> b
        .intProp("Members", FlixelDebugDefaultSections::count)
        .intProp("Max size", FlixelSpriteGroup::getMaxSize, FlixelSpriteGroup::setMaxSize));

    inspector.register(FlixelState.class, "State", b -> b
        .boolProp("Has sub state", s -> s.getSubState() != null)
        .stringProp("Sub state", (s, out) -> {
          if (s.getSubState() != null) {
            out.concat(s.getSubState().getClass().getSimpleName());
          }
        }));

    inspector.register(FlixelEmitter.class, "Emitter", b -> b
        .intProp("Particles", FlixelEmitter::getCount)
        .intProp("Capacity", FlixelEmitter::getCapacity));
  }

  private static <T> void color(FlixelSectionBuilder<T> b, String name, Function<T, FlixelColor> getter) {
    b.stringProp(name, (o, out) -> appendHex(out, getter.apply(o).getRgba8888()),
        (o, v) -> getter.apply(o).set(v.toString()));
  }

  private static void appendHex(FlixelString out, int rgba) {
    out.concat('#');
    for (int shift = 28; shift >= 0; shift -= 4) {
      out.concat(HEX[(rgba >>> shift) & 0xF]);
    }
  }

  private static int count(FlixelGroupable<?> group) {
    FlixelArray<?> members = group.getMembers();
    return members != null ? members.getSize() : 0;
  }
}
