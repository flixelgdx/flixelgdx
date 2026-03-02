# Contributing to FlixelGDX

We welcome contributions! Whether you're fixing bugs, adding new features, or improving documentation, your help is appreciated. To ensure a smooth process for everyone, please follow these guidelines.

## Workflow

To maintain a stable code base, we follow a specific branching model:

1. **The Develop Branch**: All pull requests must be made against the `develop` branch. Pull requests targeting the `master` branch will not be merged. 
2. **Build Checks**: Your PR **MUST** pass all automated build checks. If the build fails, <u>**_the PR will not be considered for merging until the issues are resolved._**</u>
3. **Commit Messages**: Keep commit messages concise and descriptive.

## Coding Standards

FlixelGDX aims to be a high-quality framework. Please follow these standards:

### Consistency

- Match the existing project style exactly (indentation, bracket placement, etc.). We will be strict about this. We want to have a codebase that
  looks like it was written by a single person. This will help us maintain a high-quality project and encourage contributions.
- Use `final` for parameters and local variables where appropriate.
- Follow the naming conventions established in the project.

### Performance & Memory

- **Avoid allocations**: Avoid frequent `new` allocations in `update()` or `draw()` loops to prevent GC pressure.
- **Use Pooling**: Use `Poolable` interfaces for frequently spawned objects (like `FlxPoint`).

### Examples

**Good Code:**

```java
public class MyObject extends FlixelSprite {
  private final Vector2 tempVector = new Vector2(); // Reusable object

  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    tempVector.set(velocity).scl(elapsed);
    changeX(tempVector.x);
    changeY(tempVector.y);
  }
}
```

**Bad Code:**

```java
public class MyObject extends FlixelSprite {
  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    Vector2 movement = new Vector2(velocity).scl(elapsed); // Bad: new allocation every frame
    changeX(movement.x);
    changeY(movement.y);
  }
}
```

## Creating a Pull Request

A good PR is easy to review and merge. 

- **Title**: Descriptive and brief (e.g., "Add FlixelSpriteGroup support").
- **Description**: Explain *what* was changed and *why*.
- **Self-Review**: Before submitting, ensure your code compiles and follows the style guide.
- **Tests**: If you add a new feature, include tests or a way to verify the functionality.
