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
package org.flixelgdx.debug.remote;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class FlixelDebugEndpointTest {

  private static void check(String spec, String host, int port) {
    FlixelDebugEndpoint e = FlixelDebugEndpoint.tryParse(spec);
    assertEquals(host, e.getHost(), spec);
    assertEquals(port, e.getPort(), spec);
  }

  @Test
  void aHostAloneUsesTheDefaultPort() {
    check("192.168.1.20", "192.168.1.20", 6510);
    check("my-pc.local", "my-pc.local", 6510);
    check("  localhost  ", "localhost", 6510);
  }

  @Test
  void aHostAndPortAreSplitAtTheColon() {
    check("my-pc.local:7000", "my-pc.local", 7000);
    check("10.0.2.2:1", "10.0.2.2", 1);
    check("host:65535", "host", 65535);
  }

  @Test
  void aMissingHostUsesTheDefaultHost() {
    check(":7000", "127.0.0.1", 7000);
  }

  @Test
  void ipv6NeedsBracketsForAPort() {
    check("[::1]", "::1", 6510);
    check("[::1]:7000", "::1", 7000);
    check("fe80::1", "fe80::1", 6510);
    assertEquals("[::1]:7000", FlixelDebugEndpoint.tryParse("[::1]:7000").toString());
  }

  @Test
  void malformedTextIsRejected() {
    assertNull(FlixelDebugEndpoint.tryParse(null));
    assertNull(FlixelDebugEndpoint.tryParse(""));
    assertNull(FlixelDebugEndpoint.tryParse("   "));
    assertNull(FlixelDebugEndpoint.tryParse("host:"));
    assertNull(FlixelDebugEndpoint.tryParse("host:abc"));
    assertNull(FlixelDebugEndpoint.tryParse("host:0"));
    assertNull(FlixelDebugEndpoint.tryParse("host:65536"));
    assertNull(FlixelDebugEndpoint.tryParse("host:-1"));
    assertNull(FlixelDebugEndpoint.tryParse("host:123456"));
    assertNull(FlixelDebugEndpoint.tryParse("[::1"));
    assertNull(FlixelDebugEndpoint.tryParse("[]:7000"));
    assertNull(FlixelDebugEndpoint.tryParse("[::1]x"));
    assertNull(FlixelDebugEndpoint.tryParse("two words"));
  }

  @Test
  void parseFallsBackToTheDefault() {
    assertSame(FlixelDebugEndpoint.DEFAULT, FlixelDebugEndpoint.parse("host:abc"));
    assertSame(FlixelDebugEndpoint.DEFAULT, FlixelDebugEndpoint.parse(null));
    assertEquals(new FlixelDebugEndpoint("a", 5), FlixelDebugEndpoint.parse("a:5"));
    assertEquals("127.0.0.1:6510", FlixelDebugEndpoint.DEFAULT.toString());
  }
}
