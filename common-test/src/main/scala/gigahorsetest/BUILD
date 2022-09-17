load("@io_bazel_rules_scala//scala:scala.bzl", "scala_library", "scala_binary", "scala_test")

scala_library(
    name = "gigahorsetest",
    srcs = glob(["*.scala"]),
    deps = [
      "//core/src/main",
      "@maven//:3rdparty/jvm/org/scalatest/scalatest",
      "@maven//:3rdparty/jvm/ws/unfiltered/unfiltered-directives",
      "@maven//:3rdparty/jvm/ws/unfiltered/unfiltered-filter",
      "@maven//:3rdparty/jvm/ws/unfiltered/unfiltered-netty-websockets",
    ],
    exports = [
      "//core/src/main",
      "@maven//:3rdparty/jvm/org/scalatest/scalatest",
      "@maven//:3rdparty/jvm/ws/unfiltered/unfiltered-directives",
      "@maven//:3rdparty/jvm/ws/unfiltered/unfiltered-filter",
      "@maven//:3rdparty/jvm/ws/unfiltered/unfiltered-netty-websockets",
    ],
    visibility = ["//visibility:public"],
)
