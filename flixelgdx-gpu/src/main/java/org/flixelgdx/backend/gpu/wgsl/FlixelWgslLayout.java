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
package org.flixelgdx.backend.gpu.wgsl;

import org.flixelgdx.collections.FlixelMap;
import org.flixelgdx.graphics.FlixelUniformBuffer;
import org.flixelgdx.graphics.FlixelUniformLayout;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * The memory layout of a shader's own uniform struct, worked out from its WGSL text.
 *
 * <p>A shader keeps its settings in one struct bound at {@code @group(1) @binding(0)}. To let Java set those
 * members by name, the framework has to know where every member lives in the GPU buffer. WGSL decides that with
 * strict alignment and size rules (a {@code vec3f} is 12 bytes wide but must start on a 16 byte boundary, a
 * {@code mat3x3f} is three padded columns, and so on). This class reads the struct once, when the shader is
 * created, applies those rules and records the byte offset of every member. Think of it as a floor plan: the
 * parser draws the plan once, and {@link FlixelUniformBuffer} then drops values into the right rooms by name.
 *
 * <p>Supported member types are {@code f32}, {@code i32}, {@code u32}, {@code vec2}, {@code vec3} and
 * {@code vec4} of those (including the {@code vec4f} style shorthands), {@code mat2x2} through {@code mat4x4} of
 * {@code f32}, fixed-size arrays of any supported type, and nested structs. The {@code @align(n)} and
 * {@code @size(n)} attributes are honored. Names are flattened: a member of a nested struct is {@code outer.inner},
 * and array elements are {@code name[2]}. The bare name of an array refers to its first element.
 *
 * <p>Uniform address space adds extra validity rules that this parser does not check, such as array elements
 * needing a 16 byte stride. The GPU rejects an invalid shader when it compiles it, with a clear message.
 */
public final class FlixelWgslLayout implements FlixelUniformLayout {

  /** A layout with no members, used by shaders that declare no uniform struct. */
  public static final FlixelWgslLayout EMPTY = new FlixelWgslLayout(new String[0], new int[0], new int[0],
      new int[0], new int[0], new int[0], 0, new FlixelMap<>());

  private static final int MAX_DEPTH = 16;
  private static final int MAX_FIELDS = 4096;

  private final String[] names;
  private final int[] offsets;
  private final int[] scalars;
  private final int[] columns;
  private final int[] rows;
  private final int[] columnStrides;
  private final int size;
  private final FlixelMap<String, Integer> index;

  private FlixelWgslLayout(String[] names, int[] offsets, int[] scalars, int[] columns, int[] rows,
      int[] columnStrides, int size, FlixelMap<String, Integer> index) {
    this.names = names;
    this.offsets = offsets;
    this.scalars = scalars;
    this.columns = columns;
    this.rows = rows;
    this.columnStrides = columnStrides;
    this.size = size;
    this.index = index;
  }

  /**
   * Finds the shader's uniform in a WGSL source and works out its layout.
   *
   * <p>The uniform is the {@code var<uniform>} declared at {@code @group(1) @binding(0)}. When it has a struct
   * type, the struct's members become the uniforms. When it has a plain type (for example {@code f32}), the
   * variable itself is the single uniform.
   *
   * @param source The WGSL text of the shader file.
   * @return The layout, or {@link #EMPTY} when the shader declares no such uniform.
   * @throws IllegalArgumentException if the declaration uses a type this parser cannot lay out.
   */
  @NotNull
  public static FlixelWgslLayout parse(@NotNull String source) {
    FlixelWgslTokens t = FlixelWgslTokens.of(source);
    int n = t.size();
    for (int i = 0; i + 3 < n; i++) {
      if (!t.is(i, "var") || !t.is(i + 1, "<") || !t.is(i + 2, "uniform") || !t.is(i + 3, ">")) {
        continue;
      }
      if (!hasBinding(t, i, FlixelWgsl.SHADER_GROUP, FlixelWgsl.SHADER_BINDING)) {
        continue;
      }
      int at = i + 4;
      String varName = t.get(at);
      if (!t.is(at + 1, ":")) {
        throw new IllegalArgumentException("Expected ': Type' after the uniform variable " + varName + ".");
      }
      Type type = new TypeParser(t).parse(at + 2);
      Builder builder = new Builder();
      builder.emit(type.kind == Type.STRUCT ? "" : varName, type, 0);
      return builder.build(type.size);
    }
    return EMPTY;
  }

  /**
   * Lays out one named struct from a WGSL source.
   *
   * @param source The WGSL text containing the struct declaration.
   * @param structName The struct to lay out.
   * @return The layout of the struct's members.
   * @throws IllegalArgumentException if the struct is missing or uses an unsupported type.
   */
  @NotNull
  public static FlixelWgslLayout parseStruct(@NotNull String source, @NotNull String structName) {
    FlixelWgslTokens t = FlixelWgslTokens.of(source);
    Type type = new TypeParser(t).resolveStruct(structName, 0);
    Builder builder = new Builder();
    builder.emit("", type, 0);
    return builder.build(type.size);
  }

  /**
   * Returns the index of a uniform by name.
   *
   * @param name The member name, such as {@code tint}, {@code light.color} or {@code weights[2]}.
   * @return The member index, or {@code -1} when the shader has no such uniform.
   */
  @Override
  public int find(@NotNull String name) {
    Integer found = index.get(name);
    return found != null ? found : -1;
  }

  /**
   * Returns the number of settable members, counting every array element separately.
   *
   * @return The member count.
   */
  @Override
  public int getFieldCount() {
    return names.length;
  }

  /**
   * Returns the name of a member.
   *
   * @param field The member index.
   * @return The flattened name.
   */
  @Override
  @NotNull
  public String getName(int field) {
    return names[field];
  }

  /**
   * Returns the byte offset of a member in the uniform buffer.
   *
   * @param field The member index.
   * @return The offset in bytes.
   */
  @Override
  public int getOffset(int field) {
    return offsets[field];
  }

  /**
   * Returns the scalar kind of a member: {@link #F32}, {@link #I32} or {@link #U32}.
   *
   * @param field The member index.
   * @return The scalar kind.
   */
  @Override
  public int getScalar(int field) {
    return scalars[field];
  }

  /**
   * Returns the number of columns: {@code 1} for scalars and vectors, {@code 2} to {@code 4} for matrices.
   *
   * @param field The member index.
   * @return The column count.
   */
  @Override
  public int getColumns(int field) {
    return columns[field];
  }

  /**
   * Returns the number of rows, which is the vector width: {@code 1} for scalars, {@code 2} to {@code 4} for
   * vectors, and the matrix height for matrices.
   *
   * @param field The member index.
   * @return The row count.
   */
  @Override
  public int getRows(int field) {
    return rows[field];
  }

  /**
   * Returns the distance in bytes between two columns of a matrix member.
   *
   * @param field The member index.
   * @return The column stride, or {@code 0} for scalars and vectors.
   */
  @Override
  public int getColumnStride(int field) {
    return columnStrides[field];
  }

  /**
   * Returns the size of the whole uniform buffer in bytes, rounded up to the struct's alignment.
   *
   * @return The byte size, or {@code 0} when there are no uniforms.
   */
  @Override
  public int getSize() {
    return size;
  }

  /**
   * Returns whether the shader has no uniforms at all.
   *
   * @return {@code true} when {@link #getSize()} is zero.
   */
  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  private static boolean hasBinding(FlixelWgslTokens t, int varIndex, int group, int binding) {
    boolean groupOk = false;
    boolean bindingOk = false;
    for (int j = varIndex - 1; j >= 0; j--) {
      String s = t.get(j);
      if (s.equals(";") || s.equals("}")) {
        break;
      }
      if (j >= 1 && t.is(j - 1, "@") && t.is(j + 1, "(")) {
        if (s.equals("group")) {
          groupOk = t.get(j + 2).equals(String.valueOf(group));
        } else if (s.equals("binding")) {
          bindingOk = t.get(j + 2).equals(String.valueOf(binding));
        }
      }
    }
    return groupOk && bindingOk;
  }

  private static int roundUp(int align, int value) {
    return (value + align - 1) / align * align;
  }

  private static final class Type {

    static final int SCALAR = 0;
    static final int VECTOR = 1;
    static final int MATRIX = 2;
    static final int ARRAY = 3;
    static final int STRUCT = 4;

    int kind;
    int scalar;
    int columns = 1;
    int rows = 1;
    int count;
    int stride;
    int size;
    int align;
    Type element;
    String[] memberNames;
    int[] memberOffsets;
    Type[] memberTypes;
  }

  private static final class TypeParser {

    private final FlixelWgslTokens t;

    private int cursor;

    private TypeParser(FlixelWgslTokens t) {
      this.t = t;
    }

    Type parse(int start) {
      cursor = start;
      return parseType(0);
    }

    Type parseType(int depth) {
      if (depth > MAX_DEPTH) {
        throw new IllegalArgumentException("The uniform type is nested too deeply.");
      }
      String name = t.get(cursor++);
      if (name.isEmpty()) {
        throw new IllegalArgumentException("Expected a type but the source ended.");
      }
      switch (name) {
        case "f32" -> {
          return scalarType(F32);
        }
        case "i32" -> {
          return scalarType(I32);
        }
        case "u32" -> {
          return scalarType(U32);
        }
        case "f16", "bool" ->
          throw new IllegalArgumentException("The type " + name + " is not supported in shader uniforms.");
        case "atomic" -> {
          expect("<");
          Type inner = parseType(depth + 1);
          expect(">");
          return inner;
        }
        case "array" -> {
          return parseArray(depth);
        }
        default -> {
        }
      }
      if (name.startsWith("vec") && name.length() >= 4) {
        int rows = name.charAt(3) - '0';
        if (rows >= 2 && rows <= 4) {
          return vectorType(rows, scalarOfVector(name));
        }
      }
      if (name.startsWith("mat") && name.length() >= 6 && name.charAt(4) == 'x') {
        int cols = name.charAt(3) - '0';
        int rows = name.charAt(5) - '0';
        if (cols >= 2 && cols <= 4 && rows >= 2 && rows <= 4) {
          int scalar = F32;
          if (name.length() == 6) {
            expect("<");
            scalar = scalarOf(t.get(cursor++));
            expect(">");
          } else if (!name.endsWith("f")) {
            throw new IllegalArgumentException("Only f32 matrices are supported: " + name + ".");
          }
          if (scalar != F32) {
            throw new IllegalArgumentException("Only f32 matrices are supported: " + name + ".");
          }
          return matrixType(cols, rows);
        }
      }
      return resolveStruct(name, depth);
    }

    private int scalarOfVector(String name) {
      if (name.length() == 4) {
        expect("<");
        int scalar = scalarOf(t.get(cursor++));
        expect(">");
        return scalar;
      }
      char suffix = name.charAt(4);
      return switch (suffix) {
        case 'f' -> F32;
        case 'i' -> I32;
        case 'u' -> U32;
        default -> throw new IllegalArgumentException("Unsupported vector type " + name + ".");
      };
    }

    private Type parseArray(int depth) {
      expect("<");
      Type element = parseType(depth + 1);
      int count = 0;
      if (t.is(cursor, ",")) {
        cursor++;
        String number = t.get(cursor++);
        try {
          count = Integer.parseInt(number);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("Array sizes in uniforms must be integer literals, found: " + number);
        }
      } else {
        throw new IllegalArgumentException("Runtime-sized arrays cannot live in a uniform struct.");
      }
      expect(">");
      Type type = new Type();
      type.kind = Type.ARRAY;
      type.element = element;
      type.count = count;
      type.align = element.align;
      type.stride = roundUp(element.align, element.size);
      type.size = count * type.stride;
      return type;
    }

    Type resolveStruct(String name, int depth) {
      if (depth > MAX_DEPTH) {
        throw new IllegalArgumentException("The struct " + name + " is nested too deeply.");
      }
      int n = t.size();
      for (int i = 0; i + 2 < n; i++) {
        if (t.is(i, "struct") && t.is(i + 1, name) && t.is(i + 2, "{")) {
          return parseStruct(i + 3, name, depth);
        }
      }
      throw new IllegalArgumentException("Unknown type or missing struct declaration: " + name + ".");
    }

    private Type parseStruct(int start, String structName, int depth) {
      int savedCursor = cursor;
      cursor = start;
      String[] names = new String[8];
      int[] offsets = new int[8];
      Type[] types = new Type[8];
      int count = 0;
      int running = 0;
      int maxAlign = 1;
      while (!t.is(cursor, "}")) {
        if (cursor >= t.size()) {
          throw new IllegalArgumentException("The struct " + structName + " is missing its closing brace.");
        }
        int alignOverride = 0;
        int sizeOverride = 0;
        while (t.is(cursor, "@")) {
          String attr = t.get(cursor + 1);
          cursor += 2;
          if (t.is(cursor, "(")) {
            int depthParen = 0;
            int valueIndex = cursor + 1;
            do {
              if (t.is(cursor, "(")) {
                depthParen++;
              } else if (t.is(cursor, ")")) {
                depthParen--;
              }
              cursor++;
            } while (depthParen > 0 && cursor < t.size());
            if (attr.equals("align")) {
              alignOverride = parseIntOrFail(t.get(valueIndex), "@align");
            } else if (attr.equals("size")) {
              sizeOverride = parseIntOrFail(t.get(valueIndex), "@size");
            }
          }
        }
        String memberName = t.get(cursor++);
        if (!t.is(cursor, ":")) {
          throw new IllegalArgumentException("Expected ':' after the member " + memberName + " in " + structName + ".");
        }
        cursor++;
        Type memberType = parseType(depth + 1);
        if (t.is(cursor, ",")) {
          cursor++;
        }
        int align = alignOverride > 0 ? alignOverride : memberType.align;
        int size = sizeOverride > 0 ? sizeOverride : memberType.size;
        int offset = roundUp(align, running);
        running = offset + size;
        maxAlign = Math.max(maxAlign, align);
        if (count == names.length) {
          names = Arrays.copyOf(names, count * 2);
          offsets = Arrays.copyOf(offsets, count * 2);
          types = Arrays.copyOf(types, count * 2);
        }
        names[count] = memberName;
        offsets[count] = offset;
        types[count] = memberType;
        count++;
      }
      Type type = new Type();
      type.kind = Type.STRUCT;
      type.memberNames = Arrays.copyOf(names, count);
      type.memberOffsets = Arrays.copyOf(offsets, count);
      type.memberTypes = Arrays.copyOf(types, count);
      type.align = maxAlign;
      type.size = roundUp(maxAlign, running);
      cursor = savedCursor;
      return type;
    }

    private void expect(String punct) {
      if (!t.is(cursor, punct)) {
        throw new IllegalArgumentException("Expected '" + punct + "' but found '" + t.get(cursor) + "'.");
      }
      cursor++;
    }

    private static int parseIntOrFail(String text, String what) {
      try {
        return Integer.parseInt(text);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(what + " must be an integer literal, found: " + text);
      }
    }

    private static int scalarOf(String name) {
      return switch (name) {
        case "f32" -> F32;
        case "i32" -> I32;
        case "u32" -> U32;
        default -> throw new IllegalArgumentException("Unsupported scalar type " + name + ".");
      };
    }

    private static Type scalarType(int scalar) {
      Type type = new Type();
      type.kind = Type.SCALAR;
      type.scalar = scalar;
      type.size = 4;
      type.align = 4;
      return type;
    }

    private static Type vectorType(int rows, int scalar) {
      Type type = new Type();
      type.kind = Type.VECTOR;
      type.scalar = scalar;
      type.rows = rows;
      type.size = rows * 4;
      type.align = rows == 2 ? 8 : 16;
      return type;
    }

    private static Type matrixType(int cols, int rows) {
      Type type = new Type();
      type.kind = Type.MATRIX;
      type.scalar = F32;
      type.columns = cols;
      type.rows = rows;
      type.align = rows == 2 ? 8 : 16;
      type.stride = roundUp(type.align, rows * 4);
      type.size = cols * type.stride;
      return type;
    }
  }

  private static final class Builder {

    private final FlixelMap<String, Integer> lookup = new FlixelMap<>();

    private String[] names = new String[16];
    private int[] offsets = new int[16];
    private int[] scalars = new int[16];
    private int[] columns = new int[16];
    private int[] rows = new int[16];
    private int[] strides = new int[16];
    private int count;

    void emit(String prefix, Type type, int base) {
      switch (type.kind) {
        case Type.STRUCT -> {
          for (int i = 0; i < type.memberNames.length; i++) {
            String child = prefix.isEmpty() ? type.memberNames[i] : prefix + "." + type.memberNames[i];
            emit(child, type.memberTypes[i], base + type.memberOffsets[i]);
          }
        }
        case Type.ARRAY -> {
          int firstIndex = count;
          for (int i = 0; i < type.count && count < MAX_FIELDS; i++) {
            emit(prefix + "[" + i + "]", type.element, base + i * type.stride);
          }
          if (count > firstIndex && !lookup.containsKey(prefix)) {
            lookup.put(prefix, firstIndex);
          }
        }
        default -> add(prefix, type, base);
      }
    }

    private void add(String name, Type type, int offset) {
      if (count == names.length) {
        int cap = count * 2;
        names = Arrays.copyOf(names, cap);
        offsets = Arrays.copyOf(offsets, cap);
        scalars = Arrays.copyOf(scalars, cap);
        columns = Arrays.copyOf(columns, cap);
        rows = Arrays.copyOf(rows, cap);
        strides = Arrays.copyOf(strides, cap);
      }
      names[count] = name;
      offsets[count] = offset;
      scalars[count] = type.scalar;
      columns[count] = type.columns;
      rows[count] = type.rows;
      strides[count] = type.kind == Type.MATRIX ? type.stride : 0;
      if (!lookup.containsKey(name)) {
        lookup.put(name, count);
      }
      count++;
    }

    FlixelWgslLayout build(int size) {
      return new FlixelWgslLayout(Arrays.copyOf(names, count), Arrays.copyOf(offsets, count),
          Arrays.copyOf(scalars, count), Arrays.copyOf(columns, count),
          Arrays.copyOf(rows, count), Arrays.copyOf(strides, count), size, lookup);
    }
  }
}
