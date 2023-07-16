load("@io_bazel_rules_scala//scala:scala.bzl", "scala_library", "scala_binary", "scala_test")

scala_test(
  name = "test",
  srcs = glob(["scala/**/*.scala"]),
  data = glob(["resources/*"]),
  deps = [
    "//okhttp/src/main/scala/gigahorse/support/okhttp",
    "//common-test/src/main/scala/gigahorsetest:gigahorsetest",
  ],
)
