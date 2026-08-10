plugins {
  id("flixelgdx.java-base")
  java
}

// The JSON annotation processor generates reflection-free (de)serializers for @JsonSerializable
// classes at compile time. It needs no runtime dependencies: it emits source that references the
// core JSON types (FlixelJsonValue, FlixelJsonWriter), which are on the consumer's classpath, and
// it matches the annotation by name so it does not depend on the core module itself.
java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(17)
  }
}
