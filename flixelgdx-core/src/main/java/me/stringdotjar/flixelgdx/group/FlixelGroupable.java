package me.stringdotjar.flixelgdx.group;

/**
 * Interface for creating new groups with members inside of them.
 */
public interface FlixelGroupable<T> {
  void add(T member);
  void remove(T member);
  void clear();
}
