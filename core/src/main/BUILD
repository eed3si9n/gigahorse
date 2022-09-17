load("@io_bazel_rules_scala//scala:scala.bzl", "scala_library", "scala_binary", "scala_test")
load("@twitter_rules_jvm_export//jvm_export:jvm_export.bzl", "jvm_export")

scala_library(
    name = "main",
    srcs = [
        "//core/src/main/contraband-scala/gigahorse",
        "//core/src/main/scala/gigahorse",
    ],
    deps = [
        "@maven//:3rdparty/jvm/com/typesafe/ssl-config-core",
        "@maven//:3rdparty/jvm/org/reactivestreams/reactive-streams",
        "@maven//:3rdparty/jvm/org/slf4j/slf4j-api",
    ],
    exports = [
        "@maven//:3rdparty/jvm/com/typesafe/ssl-config-core",
        "@maven//:3rdparty/jvm/org/reactivestreams/reactive-streams",
        "@maven//:3rdparty/jvm/org/slf4j/slf4j-api",
    ],
    tags = ["maven_coordinates=com.eed3si9n:gigahorse-core_2.12:{pom_version}"],
    visibility = ["//visibility:public"],
)

jvm_export(
    name = "main.publish",
    artifacts = [":main"],
    project_name = "core",
    project_description = "An HTTP client for Scala",
    project_url = "https://eed3si9n.com/gigahorse/",
    scm_url = "https://github.com/eed3si9n/gigahorse",
    license = "Apache-2.0",
    snapshot_repo = "central",
    release_repo = "central",
    # python_path = "/usr/local/bin/python3",
    tags = ["manual"],
)
