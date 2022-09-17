load("@io_bazel_rules_scala//scala:scala.bzl", "scala_library", "scala_binary", "scala_test")
load("@twitter_rules_jvm_export//jvm_export:jvm_export.bzl", "jvm_export")

scala_library(
    name = "okhttp",
    srcs = glob(["*.scala"]),
    deps = [
      "//core/src/main",
      "@maven//:3rdparty/jvm/com/squareup/okhttp3/okhttp",
    ],
    exports = [
      "//core/src/main",
      "@maven//:3rdparty/jvm/com/squareup/okhttp3/okhttp",
    ],
    visibility = ["//visibility:public"],
)

jvm_export(
    name = "okhttp.publish",
    artifacts = [":okhttp"],
    project_name = "core",
    project_description = "An HTTP client for Scala",
    project_url = "https://eed3si9n.com/gigahorse/",
    scm_url = "https://github.com/eed3si9n/gigahorse",
    license = "Apache-2.0",
    snapshot_repo = "central",
    release_repo = "central",
    python_path = "/usr/local/bin/python3",
)
