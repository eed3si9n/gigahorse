load("@io_bazel_rules_scala//scala:scala.bzl", "scala_repositories")

def cross_scala_repositories():
  scala_repositories(overriden_artifacts =
    {
        "scala_compiler": "0000",
        "scala_library": "0000",
        "scala_reflect": "0000",
    }
  )
