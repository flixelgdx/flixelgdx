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
package org.flixelgdx.group;

import org.flixelgdx.FlixelBasic;
import org.flixelgdx.FlixelObject;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelUnsupportedBatch;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelGroupTest {

  @Test
  void addIgnoresNullsAndRespectsTheMaxSize() {
    FlixelGroup<String> g = new FlixelGroup<>(String[]::new, 2);
    g.add(null);
    g.add("a");
    g.add("b");
    g.add("c");
    assertEquals(2, g.getMembers().getSize());
    assertEquals(2, g.getMaxSize());
    g.setMaxSize(-5);
    assertEquals(0, g.getMaxSize(), "a negative max size means no limit");
    g.add("c");
    assertEquals(3, g.getMembers().getSize());
  }

  @Test
  void removeClearAndResetStorage() {
    FlixelGroup<String> g = new FlixelGroup<>(String[]::new);
    g.add("a");
    g.add("b");
    g.remove(null);
    g.remove("a");
    assertEquals(1, g.getMembers().getSize());
    g.clear();
    assertEquals(0, g.getMembers().getSize());

    g.resetStorage();
    assertNull(g.getMembers());
    g.remove("a");
    g.clear();
    g.resetStorage();
    g.add("z");
    assertEquals(1, g.getMembers().getSize(), "adding after a reset creates the storage again");
    g.resetStorage();
    g.ensureMembers();
    assertEquals(0, g.getMembers().getSize());
  }

  @Test
  void nullFactoryIsRejected() {
    assertThrows(NullPointerException.class, () -> new FlixelGroup<String>(null));
  }

  @Test
  void defaultHelpersWalkTheMembers() {
    FlixelGroup<Object> g = new FlixelGroup<>(Object[]::new);
    g.add("a");
    g.add(1);
    g.add("b");
    g.getMembers().add(null);
    assertEquals(3, g.getFirstNullIndex());

    AtomicInteger all = new AtomicInteger();
    g.forEachMember(m -> all.incrementAndGet());
    assertEquals(3, all.get(), "null slots are skipped");

    AtomicInteger strings = new AtomicInteger();
    g.forEachMemberType(String.class, s -> strings.incrementAndGet());
    assertEquals(2, strings.get());

    g.forEachMember(null);
    g.forEachMemberType(null, s -> {
    });
    g.forEachMemberType(String.class, null);

    g.detach("a");
    g.detach(null);
    assertEquals(3, g.getMembers().getSize());

    g.getMembers().removeIndex(2);
    assertEquals(-1, g.getFirstNullIndex());
  }

  @Test
  void defaultHelpersHandleMissingStorage() {
    FlixelGroup<String> g = new FlixelGroup<>(String[]::new);
    g.resetStorage();
    assertEquals(-1, g.getFirstNullIndex());
    g.forEachMember(s -> {
    });
    g.forEachMemberType(String.class, s -> {
    });
    g.detach("a");
  }

  @Test
  void basicGroupUpdatesOnlyLiveActiveMembers() {
    Group g = new Group();
    Counter live = new Counter();
    Counter dead = new Counter();
    Counter idle = new Counter();
    Counter hidden = new Counter();
    dead.kill();
    idle.setActive(false);
    hidden.setVisible(false);
    g.add(live);
    g.add(dead);
    g.add(idle);
    g.add(hidden);
    g.getMembers().add(null);

    g.update(0.1f);
    assertEquals(1, live.updates);
    assertEquals(0, dead.updates);
    assertEquals(0, idle.updates);
    assertEquals(1, hidden.updates);

    g.draw(FlixelUnsupportedBatch.INSTANCE);
    assertEquals(1, live.draws);
    assertEquals(1, idle.draws);
    assertEquals(0, hidden.draws);
    assertEquals(0, dead.draws);
  }

  @Test
  void basicGroupRemoveMemberCanDestroyIt() {
    Group g = new Group();
    Counter a = new Counter();
    Counter b = new Counter();
    g.add(a);
    g.add(b);
    g.removeMember(a, true);
    assertEquals(1, a.destroys);
    g.removeMember(b, false);
    assertEquals(0, b.destroys);
    g.removeMember(b, true);
    assertEquals(0, b.destroys, "a member that is not in the group is left alone");
    g.removeMember(null, true);
    g.remove(a);
    g.clear();
    assertEquals(0, g.getMembers().getSize());
  }

  @Test
  void basicGroupRecyclesDeadMembersFirst() {
    Group g = new Group();
    Counter a = g.recycle();
    assertTrue(a.isExists());
    a.kill();
    a.setActive(false);
    a.setVisible(false);
    assertSame(a, g.getFirstDead());
    Counter again = g.recycle();
    assertSame(a, again);
    assertTrue(again.isActive());
    assertTrue(again.isVisible());
    assertNull(g.getFirstDead());
  }

  @Test
  void basicGroupStopsRecyclingAtTheMaxSize() {
    Group g = new Group();
    g.setMaxSize(1);
    assertEquals(1, g.getMaxSize());
    g.recycle();
    assertNull(g.recycle());

    NoFactoryGroup none = new NoFactoryGroup();
    assertNull(none.recycle(), "without a factory there is nothing to recycle into");
  }

  @Test
  void basicGroupDestroyDestroysEveryMember() {
    Group g = new Group();
    Counter a = new Counter();
    g.add(a);
    g.getMembers().add(null);
    g.destroy();
    assertEquals(1, a.destroys);
    assertNull(g.getMembers());
    g.update(0.1f);
    g.draw(FlixelUnsupportedBatch.INSTANCE);
    assertNull(g.getFirstDead());
    g.ensureMembers();
    assertEquals(0, g.getMembers().getSize());
    g.destroy();
  }

  @Test
  void basicGroupWithAStartingMaxSize() {
    NoFactoryGroup g = new NoFactoryGroup();
    assertEquals(3, g.getMaxSize());
    assertSame(g.getMembers(), g.list().getMembers());
  }

  private static final class Counter extends FlixelObject {

    int updates;
    int draws;
    int destroys;

    @Override
    public void update(float elapsed) {
      updates++;
    }

    @Override
    public void draw(FlixelBatch batch) {
      draws++;
    }

    @Override
    public void destroy() {
      destroys++;
      super.destroy();
    }
  }

  private static final class Group extends FlixelBasicGroup<Counter> {

    Group() {
      super(Counter[]::new);
    }

    @Override
    protected Counter createMemberForRecycle() {
      return new Counter();
    }
  }

  private static final class NoFactoryGroup extends FlixelBasicGroup<FlixelBasic> {

    NoFactoryGroup() {
      super(FlixelBasic[]::new, 3);
    }

    FlixelGroup<FlixelBasic> list() {
      return getMemberList();
    }
  }
}
