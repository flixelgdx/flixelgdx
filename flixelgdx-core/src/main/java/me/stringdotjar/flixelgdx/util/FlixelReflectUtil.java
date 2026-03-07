package me.stringdotjar.flixelgdx.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Backend utility class for obtaining and manipulating fields on objects through the usage of Java reflection.
 */
public final class FlixelReflectUtil {

  private FlixelReflectUtil() {}

  /**
   * Checks if a field exists on a given object.
   *
   * @param target The object to verify the existence of the given field to check.
   * @param fieldName The name of the
   * @return If the field exists on the given object.
   */
  public static boolean hasField(Object target, String fieldName) {
    return getAllFields(target.getClass()).stream().anyMatch(field -> field.getName().equals(fieldName));
  }

  /**
   * Obtains all fields of a class, including the master types above it all the way to {@link
   * Object}.
   *
   * @param type A class literal to obtain the fields from.
   * @return All fields from itself and its master classes above it.
   */
  public static List<Field> getAllFields(Class<?> type) {
    List<Field> fields = new ArrayList<>();
    for (Class<?> c = type; c != null; c = c.getSuperclass()) {
      fields.addAll(Arrays.asList(c.getDeclaredFields()));
    }
    return fields;
  }

  /**
   * Obtains all fields of a class, including the master types above it all the way to {@link
   * Object}. Obvious to the name, this version returns an array instead of a list unlike its
   * similar function .
   *
   * @param type A class literal to obtain the fields from.
   * @return All fields from itself and its master classes above it.
   */
  public static Field[] getAllFieldsAsArray(Class<?> type) {
    return getAllFields(type).toArray(new Field[0]);
  }

  /**
   * Checks if a class of a certain package is final.
   *
   * @param classPath The package definition of the class to check if final. An example could be
   * {@code "me.stringdotjar.flixelgdx.Flixel"}.
   * @return If the class provided is final. If there was an exception caught, then {@code false} is
   * automatically returned.
   */
  public static boolean isClassFinal(String classPath) {
    try {
      Class<?> clazz = Class.forName(classPath);
      // Uses the java.lang.reflect.Modifier utility.
      return Modifier.isFinal(clazz.getModifiers());
    } catch (ClassNotFoundException e) {
      // Treat non-existent class as non-final for safe binding,
      // though the user will hit an error later.
      return false;
    }
  }
}
