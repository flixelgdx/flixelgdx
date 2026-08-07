#!/usr/bin/env python3
#
# Author tool that generates the primitive collection classes (and their unit
# tests) in org.flixelgdx.collections from the templates under scripts/templates.
#
# The generated Java files are committed to the repository so IDEs and the
# Javadoc tool see them as normal source. Run this script manually from the
# repository root whenever a template changes:
#
#     python3 scripts/generate_primitive_collections.py
#
# This is NOT a build step. It is an authoring tool.

import os
import sys

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEMPLATE_DIR = os.path.join(REPO_ROOT, "scripts", "templates")
MAIN_DIR = os.path.join(
    REPO_ROOT, "flixelgdx-core", "src", "main", "java", "org", "flixelgdx", "collections"
)
TEST_DIR = os.path.join(
    REPO_ROOT, "flixelgdx-test", "src", "test", "java", "org", "flixelgdx", "collections"
)

# Shared per-primitive facts. Every token map below draws from these.
PRIMITIVES = {
    "int": {
        "type": "int",
        "TypeName": "Int",
        "Boxed": "Integer",
        "zero": "0",
        "article": "an",
        "hashExpr": "key * HASH_MULTIPLIER",
        "hashNote": "",
    },
    "long": {
        "type": "long",
        "TypeName": "Long",
        "Boxed": "Long",
        "zero": "0L",
        "article": "a",
        "hashExpr": "(int) (key ^ (key >>> 32)) * HASH_MULTIPLIER",
        "hashNote": (
            "\n *\n * <p>The hash folds the high 32 bits of the key with XOR before"
            " multiplying,\n * so keys that differ only in the upper word map to"
            " different buckets."
        ),
    },
    "float": {
        "type": "float",
        "TypeName": "Float",
        "Boxed": "Float",
        "zero": "0.0f",
        "article": "a",
    },
    "short": {
        "type": "short",
        "TypeName": "Short",
        "Boxed": "Short",
        "zero": "(short) 0",
        "article": "a",
    },
    "byte": {
        "type": "byte",
        "TypeName": "Byte",
        "Boxed": "Byte",
        "zero": "(byte) 0",
        "article": "a",
    },
    "boolean": {
        "type": "boolean",
        "TypeName": "Boolean",
        "Boxed": "Boolean",
        "zero": "false",
        "article": "a",
    },
}

# Sample literals used by the generated array tests. Each type needs three
# values that are valid to construct and compare with ==. Booleans only have two
# distinct values, so v0 and v2 repeat, which the array tests are designed for.
ARRAY_TEST_SAMPLES = {
    "int": ("10", "20", "99"),
    "long": ("10L", "20L", "99L"),
    "float": ("10.0f", "20.0f", "99.0f"),
    "short": ("(short) 10", "(short) 20", "(short) 99"),
    "byte": ("(byte) 10", "(byte) 20", "(byte) 99"),
    "boolean": ("true", "false", "true"),
}

# Object-keyed maps carry a separate value primitive. The assertEquals calls in
# the generated tests need a delta argument for floating-point comparisons.
VALUE_PRIMITIVES = {
    "int": {
        "valueType": "int",
        "ValueTypeName": "Int",
        "ValueBoxed": "Integer",
        "valueZero": "0",
        "valueArticle": "an",
        "valueDelta": "",
    },
    "float": {
        "valueType": "float",
        "ValueTypeName": "Float",
        "ValueBoxed": "Float",
        "valueZero": "0.0f",
        "valueArticle": "a",
        "valueDelta": ", 1e-6f",
    },
}


def render(template_text, tokens):
    """Replaces every {{token}} in the template with its configured value."""
    out = template_text
    for name, value in tokens.items():
        out = out.replace("{{" + name + "}}", value)
    if "{{" in out:
        # Fail loudly so a missing token is never silently written to disk.
        start = out.index("{{")
        raise SystemExit(
            "Unsubstituted token near: " + out[start : start + 40].replace("\n", " ")
        )
    return out


def load_template(name):
    with open(os.path.join(TEMPLATE_DIR, name), "r", encoding="utf-8") as handle:
        return handle.read()


def write_java(directory, class_name, text):
    path = os.path.join(directory, class_name + ".java")
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(text)
    print("  wrote " + os.path.relpath(path, REPO_ROOT))


def generate():
    if not os.path.isdir(TEST_DIR):
        raise SystemExit("Test directory not found: " + TEST_DIR)

    array_tpl = load_template("PrimitiveArray.template.java")
    array_test_tpl = load_template("PrimitiveArrayTest.template.java")
    map_tpl = load_template("PrimitiveKeyedMap.template.java")
    map_test_tpl = load_template("PrimitiveKeyedMapTest.template.java")
    set_tpl = load_template("PrimitiveKeyedSet.template.java")
    set_test_tpl = load_template("PrimitiveKeyedSetTest.template.java")
    obj_map_tpl = load_template("ObjectKeyedPrimitiveMap.template.java")
    obj_map_test_tpl = load_template("ObjectKeyedPrimitiveMapTest.template.java")

    print("Primitive arrays:")
    for key in ("int", "float", "long", "short", "byte", "boolean"):
        tokens = dict(PRIMITIVES[key])
        write_java(MAIN_DIR, "Flixel" + tokens["TypeName"] + "Array", render(array_tpl, tokens))
        v0, v1, v2 = ARRAY_TEST_SAMPLES[key]
        test_tokens = dict(tokens)
        test_tokens.update({"v0": v0, "v1": v1, "v2": v2})
        write_java(
            TEST_DIR,
            "Flixel" + tokens["TypeName"] + "ArrayTest",
            render(array_test_tpl, test_tokens),
        )

    print("Primitive-keyed maps:")
    for key in ("int", "long"):
        tokens = dict(PRIMITIVES[key])
        write_java(MAIN_DIR, "Flixel" + tokens["TypeName"] + "Map", render(map_tpl, tokens))
        write_java(
            TEST_DIR,
            "Flixel" + tokens["TypeName"] + "MapTest",
            render(map_test_tpl, tokens),
        )

    print("Primitive-keyed sets:")
    for key in ("int", "long"):
        tokens = dict(PRIMITIVES[key])
        write_java(MAIN_DIR, "Flixel" + tokens["TypeName"] + "Set", render(set_tpl, tokens))
        write_java(
            TEST_DIR,
            "Flixel" + tokens["TypeName"] + "SetTest",
            render(set_test_tpl, tokens),
        )

    print("Object-keyed primitive-value maps:")
    for key in ("int", "float"):
        tokens = dict(VALUE_PRIMITIVES[key])
        write_java(
            MAIN_DIR, "FlixelObject" + tokens["ValueTypeName"] + "Map", render(obj_map_tpl, tokens)
        )
        write_java(
            TEST_DIR,
            "FlixelObject" + tokens["ValueTypeName"] + "MapTest",
            render(obj_map_test_tpl, tokens),
        )

    print("Done.")


if __name__ == "__main__":
    generate()
