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
package org.flixelgdx.json.processor;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Generates reflection-free JSON serializers for classes annotated with {@code @JsonSerializable}.
 *
 * <p>For each annotated class {@code Foo} in package {@code p}, this emits {@code p.FooJsonSerializer}
 * with {@code String toJson(Foo)} and {@code Foo fromJson(FlixelJsonValue)}. The generated code reads
 * and writes each mapped field directly (no reflection), so it is both allocation-light and safe on
 * platforms without reflection metadata.
 *
 * <p>The annotation is matched by name, so this processor has no compile dependency on the framework;
 * the source it produces references the core JSON types, which are on the annotated project's
 * classpath.
 */
@SupportedAnnotationTypes("org.flixelgdx.json.JsonSerializable")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public final class FlixelJsonSerializableProcessor extends AbstractProcessor {

  private static final String ANNOTATION = "org.flixelgdx.json.JsonSerializable";
  private static final String SERIALIZER_SUFFIX = "JsonSerializer";

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (TypeElement annotation : annotations) {
      if (!annotation.getQualifiedName().contentEquals(ANNOTATION)) {
        continue;
      }
      for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
        if (element.getKind() == ElementKind.CLASS) {
          generate((TypeElement) element);
        } else {
          error(element, "@JsonSerializable can only be applied to a class.");
        }
      }
    }
    return true;
  }

  /** Writes the serializer source file for one annotated class. */
  private void generate(TypeElement type) {
    if (type.getModifiers().contains(Modifier.ABSTRACT)) {
      error(type, "@JsonSerializable class '" + type.getSimpleName() + "' must not be abstract.");
      return;
    }

    String packageName = packageOf(type);
    String simpleName = type.getSimpleName().toString();
    String serializerName = simpleName + SERIALIZER_SUFFIX;
    List<VariableElement> fields = mappableFields(type);

    StringBuilder src = new StringBuilder(1024);
    if (!packageName.isEmpty()) {
      src.append("package ").append(packageName).append(";\n\n");
    }
    src.append("import org.flixelgdx.json.FlixelJsonValue;\n");
    src.append("import org.flixelgdx.json.FlixelJsonWriter;\n\n");
    src.append("/** Generated JSON serializer for {@link ").append(simpleName).append("}. Do not edit. */\n");
    src.append("public final class ").append(serializerName).append(" {\n\n");
    src.append("  private ").append(serializerName).append("() {}\n\n");

    appendToJson(src, simpleName, fields);
    src.append('\n');
    appendFromJson(src, simpleName, fields);

    src.append("}\n");

    write(type, packageName, serializerName, src.toString());
  }

  private void appendToJson(StringBuilder src, String simpleName, List<VariableElement> fields) {
    src.append("  /** Serializes {@code value} to a JSON string. */\n");
    src.append("  public static String toJson(").append(simpleName).append(" value) {\n");
    src.append("    if (value == null) {\n      return \"null\";\n    }\n");
    src.append("    FlixelJsonWriter w = new FlixelJsonWriter();\n");
    src.append("    w.beginObject();\n");
    for (VariableElement field : fields) {
      String name = field.getSimpleName().toString();
      TypeMirror t = field.asType();
      src.append("    w.name(\"").append(name).append("\");\n");
      if (isArray(t)) {
        appendArrayWrite(src, "value." + name, componentType(t));
      } else {
        src.append("    ").append(writeSingle("value." + name, t)).append(";\n");
      }
    }
    src.append("    w.endObject();\n");
    src.append("    return w.toString();\n");
    src.append("  }\n");
  }

  /** Emits the statements that write one array field as a JSON array. */
  private void appendArrayWrite(StringBuilder src, String accessor, TypeMirror element) {
    src.append("    if (").append(accessor).append(" == null) {\n");
    src.append("      w.value((String) null);\n");
    src.append("    } else {\n");
    src.append("      w.beginArray();\n");
    src.append("      for (int i = 0; i < ").append(accessor).append(".length; i++) {\n");
    src.append("        ").append(writeSingle(accessor + "[i]", element)).append(";\n");
    src.append("      }\n");
    src.append("      w.endArray();\n");
    src.append("    }\n");
  }

  /** Returns the writer call (without a trailing semicolon) that writes one non-array value. */
  private String writeSingle(String accessor, TypeMirror t) {
    if (isString(t) || t.getKind().isPrimitive()) {
      return "w.value(" + accessor + ")";
    }
    if (isEnum(t)) {
      return "w.value(" + accessor + " == null ? null : " + accessor + ".name())";
    }
    return "w.raw(" + serializerFqn(t) + ".toJson(" + accessor + "))";
  }

  private void appendFromJson(StringBuilder src, String simpleName, List<VariableElement> fields) {
    src.append("  /** Fills a fresh {@code ").append(simpleName).append("} from parsed JSON. */\n");
    src.append("  public static ").append(simpleName).append(" fromJson(FlixelJsonValue json) {\n");
    src.append("    ").append(simpleName).append(" value = new ").append(simpleName).append("();\n");
    for (VariableElement field : fields) {
      String name = field.getSimpleName().toString();
      TypeMirror t = field.asType();
      if (isArray(t)) {
        appendArrayRead(src, name, componentType(t));
      } else if (isEnum(t)) {
        appendEnumRead(src, name, t);
      } else {
        src.append("    value.").append(name).append(" = ").append(readExpression(t, name)).append(";\n");
      }
    }
    src.append("    return value;\n");
    src.append("  }\n");
  }

  /** Emits a block that reads one enum field, tolerating a missing or null value. */
  private void appendEnumRead(StringBuilder src, String name, TypeMirror t) {
    String key = "\"" + name + "\"";
    String typeName = typeName(t);
    src.append("    {\n");
    src.append("      FlixelJsonValue n = json.has(").append(key).append(") ? json.get(").append(key)
        .append(") : null;\n");
    src.append("      String s = n == null ? null : n.asString();\n");
    src.append("      value.").append(name).append(" = s == null ? null : ").append(typeName).append(".valueOf(s);\n");
    src.append("    }\n");
  }

  /** Emits a block that reads one array field, defaulting to an empty array when absent. */
  private void appendArrayRead(StringBuilder src, String name, TypeMirror element) {
    String key = "\"" + name + "\"";
    String elementType = typeName(element);
    src.append("    {\n");
    src.append("      FlixelJsonValue a = json.has(").append(key).append(") ? json.get(").append(key)
        .append(") : null;\n");
    src.append("      if (a != null && a.isArray()) {\n");
    src.append("        int count = a.getSize();\n");
    src.append("        ").append(elementType).append("[] out = new ").append(elementType).append("[count];\n");
    src.append("        for (int i = 0; i < count; i++) {\n");
    if (isEnum(element)) {
      src.append("          String s = a.get(i) == null ? null : a.get(i).asString();\n");
      src.append("          out[i] = s == null ? null : ").append(elementType).append(".valueOf(s);\n");
    } else {
      src.append("          out[i] = ").append(readFromNode(element)).append(";\n");
    }
    src.append("        }\n");
    src.append("        value.").append(name).append(" = out;\n");
    src.append("      } else {\n");
    src.append("        value.").append(name).append(" = new ").append(elementType).append("[0];\n");
    src.append("      }\n");
    src.append("    }\n");
  }

  /** Builds the expression that reads one non-array, non-enum field by name from {@code json}. */
  private String readExpression(TypeMirror t, String name) {
    String key = "\"" + name + "\"";
    return switch (t.getKind()) {
      case INT -> "json.getInt(" + key + ", 0)";
      case SHORT -> "(short) json.getInt(" + key + ", 0)";
      case BYTE -> "(byte) json.getInt(" + key + ", 0)";
      case CHAR -> "(char) json.getInt(" + key + ", 0)";
      case LONG -> "json.has(" + key + ") ? (long) json.get(" + key + ").asDouble() : 0L";
      case FLOAT -> "json.getFloat(" + key + ", 0f)";
      case DOUBLE -> "json.has(" + key + ") ? json.get(" + key + ").asDouble() : 0.0";
      case BOOLEAN -> "json.getBool(" + key + ", false)";
      default -> {
        if (isString(t)) {
          yield "json.getString(" + key + ", null)";
        }
        String serializer = serializerFqn(t);
        yield "json.has(" + key + ") ? " + serializer + ".fromJson(json.get(" + key + ")) : null";
      }
    };
  }

  /** Builds the expression that reads one value (primitive, String, or nested type) from a node. */
  private String readFromNode(TypeMirror t) {
    return switch (t.getKind()) {
      case INT -> "a.get(i)" + ".asInt()";
      case SHORT -> "(short) " + "a.get(i)" + ".asInt()";
      case BYTE -> "(byte) " + "a.get(i)" + ".asInt()";
      case CHAR -> "(char) " + "a.get(i)" + ".asInt()";
      case LONG -> "(long) " + "a.get(i)" + ".asDouble()";
      case FLOAT -> "a.get(i)" + ".asFloat()";
      case DOUBLE -> "a.get(i)" + ".asDouble()";
      case BOOLEAN -> "a.get(i)" + ".asBool()";
      default -> {
        if (isString(t)) {
          yield "a.get(i)" + ".asString()";
        }
        yield serializerFqn(t) + ".fromJson(" + "a.get(i)" + ")";
      }
    };
  }

  /** Returns the class's non-static, non-transient, non-final, non-private fields, validating types. */
  private List<VariableElement> mappableFields(TypeElement type) {
    List<VariableElement> result = new java.util.ArrayList<>();
    for (Element member : type.getEnclosedElements()) {
      if (member.getKind() != ElementKind.FIELD) {
        continue;
      }
      Set<Modifier> mods = member.getModifiers();
      if (mods.contains(Modifier.STATIC) || mods.contains(Modifier.TRANSIENT) || mods.contains(Modifier.FINAL)) {
        continue;
      }
      VariableElement field = (VariableElement) member;
      if (mods.contains(Modifier.PRIVATE)) {
        error(field, "@JsonSerializable field '" + field.getSimpleName()
            + "' must not be private (the generated serializer accesses it directly).");
        continue;
      }
      if (!isSupported(field.asType())) {
        error(field, "@JsonSerializable does not support the type of field '" + field.getSimpleName()
            + "'. Supported types are primitives, String, enums, other @JsonSerializable types, "
            + "and one-dimensional arrays of those.");
        continue;
      }
      result.add(field);
    }
    return result;
  }

  private boolean isSupported(TypeMirror t) {
    return isElementSupported(t) || (isArray(t) && isElementSupported(componentType(t)));
  }

  /** Whether a type is supported as a field value or as an array element (arrays are not nested). */
  private boolean isElementSupported(TypeMirror t) {
    return t.getKind().isPrimitive() || isString(t) || isEnum(t) || isSerializable(t);
  }

  private boolean isSerializable(TypeMirror t) {
    if (t.getKind() != TypeKind.DECLARED) {
      return false;
    }
    Element element = ((DeclaredType) t).asElement();
    return element.getAnnotationMirrors().stream()
        .anyMatch(a -> a.getAnnotationType().toString().equals(ANNOTATION));
  }

  private boolean isEnum(TypeMirror t) {
    return t.getKind() == TypeKind.DECLARED
        && ((DeclaredType) t).asElement().getKind() == ElementKind.ENUM;
  }

  private boolean isArray(TypeMirror t) {
    return t.getKind() == TypeKind.ARRAY;
  }

  private TypeMirror componentType(TypeMirror t) {
    return ((ArrayType) t).getComponentType();
  }

  private boolean isString(TypeMirror t) {
    return t.getKind() == TypeKind.DECLARED
        && ((DeclaredType) t).asElement().toString().equals("java.lang.String");
  }

  /**
   * Returns a name usable to declare a variable or array of the given element type in generated
   * source: the keyword for a primitive (for example {@code int}), or the fully-qualified name for a
   * String, enum, or {@code @JsonSerializable} type.
   */
  private String typeName(TypeMirror t) {
    if (t.getKind().isPrimitive()) {
      return t.toString();
    }
    return ((TypeElement) ((DeclaredType) t).asElement()).getQualifiedName().toString();
  }

  /** Returns the fully-qualified name of the generated serializer for a nested annotated type. */
  private String serializerFqn(TypeMirror t) {
    TypeElement element = (TypeElement) ((DeclaredType) t).asElement();
    String pkg = packageOf(element);
    String name = element.getSimpleName() + SERIALIZER_SUFFIX;
    return pkg.isEmpty() ? name : pkg + "." + name;
  }

  private String packageOf(TypeElement type) {
    return processingEnv.getElementUtils().getPackageOf(type).getQualifiedName().toString();
  }

  private void write(TypeElement origin, String packageName, String serializerName, String source) {
    String fqn = packageName.isEmpty() ? serializerName : packageName + "." + serializerName;
    try {
      JavaFileObject file = processingEnv.getFiler().createSourceFile(fqn, origin);
      try (Writer writer = file.openWriter()) {
        writer.write(source);
      }
    } catch (IOException e) {
      error(origin, "Could not write " + fqn + ": " + e.getMessage());
    }
  }

  private void error(Element element, String message) {
    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
  }
}
