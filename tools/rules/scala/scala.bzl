load(
    "@io_bazel_rules_scala//scala:scala.bzl",
    upstream_scala_library = "scala_library",
)
load(
    "@io_bazel_rules_scala//scala/unstable:defs.bzl",
    "make_scala_library",
)
load(
    "//tools/rules/scalafix:scalafix.bzl",
    "scalafix"
)

scala_semanticdb = make_scala_library()

def scala_library(
        name,
        srcs = [],
        deps = [],
        runtime_deps = [],
        plugins = [],
        data = [],
        resources = [],
        scalacopts = None,
        main_class = "",
        exports = [],
        resource_jars = [],
        visibility = None,
        javacopts = [],
        tags = []):
    upstream_scala_library(
        name = name,
        srcs = srcs,
        deps = deps,
        runtime_deps = runtime_deps,
        plugins = plugins,
        resources = resources,
        scalacopts = scalacopts,
        main_class = main_class,
        exports = exports,
        resource_jars = resource_jars,
        visibility = visibility,
        javacopts = [],
        tags = tags,
    )
    scalacopts_mod = scalacopts
    if scalacopts_mod and ("-Xfatal-warnings" in scalacopts_mod):
        scalacopts_mod.remove("-Xfatal-warnings")

    scala_semanticdb(
        name = "{}__semanticdb".format(name),
        srcs = srcs,
        deps = deps,
        runtime_deps = runtime_deps,
        plugins = plugins + ["//3rdparty/jvm:org_scalameta__semanticdb_scalac"],
        resources = resources,
        scalacopts = scalacopts_mod,
        main_class = main_class,
        exports = exports,
        resource_jars = resource_jars,
        visibility = visibility,
        javacopts = [],
        tags = tags + ["manual"],
    )
    scalafix(
        name = "{}__scalafix".format(name),
        srcs = srcs,
        semanticdb = "{}__semanticdb".format(name),
        tags = tags + ["manual"],
    )
