load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

skylib_version = "1.0.3"
http_archive(
    name = "bazel_skylib",
    sha256 = "1c531376ac7e5a180e0237938a2536de0c54d93f5c278634818e0efc952dd56c",
    type = "tar.gz",
    url = "https://mirror.bazel.build/github.com/bazelbuild/bazel-skylib/releases/download/{}/bazel-skylib-{}.tar.gz".format(skylib_version, skylib_version),
)

rules_scala_version = "58b98f2f90c3c9038d53a80b5f7901bc96ba3f60"
http_archive(
    name = "io_bazel_rules_scala",
    sha256 = "e96f0b9965b40efe0a750ce124bf468b785e0e1a1dd981772d3e043eba4bb5b3",
    strip_prefix = "rules_scala-%s" % rules_scala_version,
    type = "zip",
    url = "https://github.com/bazelbuild/rules_scala/archive/%s.zip" % rules_scala_version,
)

local_repository(
    name = "scala_multiverse",
    path = "tools/local_repos/default",
)

load("@scala_multiverse//:cross_scala_config.bzl", "cross_scala_config")
cross_scala_config()

load("@scala_multiverse//:cross_scala_repositories.bzl", "cross_scala_repositories")
cross_scala_repositories()

load("@rules_proto//proto:repositories.bzl", "rules_proto_dependencies", "rules_proto_toolchains")
rules_proto_dependencies()
rules_proto_toolchains()

register_toolchains("//toolchains:scala_toolchain")

rules_jvm_export_version = "bb586d5ecb4fd6190197dbd2a15919b7fbdc1d3d"
http_archive(
    name = "twitter_rules_jvm_export",
    sha256 = "50d419f7acbf83ada6c8a603d0afc83830358dc81e8f8200f1ccb379a5d32bfe",
    strip_prefix = "bazel-multiversion-%s/rules_jvm_export" % rules_jvm_export_version,
    type = "zip",
    url = "https://github.com/twitter/bazel-multiversion/archive/%s.zip" % rules_jvm_export_version,
)

# load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")
# git_repository(
#     name = "graknlabs_bazel_distribution",
#     remote = "https://github.com/graknlabs/bazel-distribution",
#     commit = "e181add439dc1cfb7b1c27db771ec741d5dd43e6"
# )

# optional: setup ScalaTest toolchain and dependencies
load("@io_bazel_rules_scala//testing:scalatest.bzl", "scalatest_repositories", "scalatest_toolchain")
scalatest_repositories()
scalatest_toolchain()
