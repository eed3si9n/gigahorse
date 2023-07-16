load("@rules_jvm_external//:defs.bzl", "artifact", "maven_install")

SCALA_SUFFIX = "_2.12"
FULL_SCALA_SUFFIX = "_2.12.18"

_install = tag_class(
  attrs = {
    "artifacts": attr.string_list(
      doc = "Maven artifact tuples, in `artifactId:groupId:version` format",
      allow_empty = True,
    ),
  },
)

def _modify_artifact(coordinates_string):
  coord = _parse_maven_coordinates(coordinates_string)
  if coord["full_scala"]:
    return "{}:{}:{}".format(
      coord["group_id"],
      coord["artifact_id"] + FULL_SCALA_SUFFIX,
      coord["version"],
    )
  elif coord["is_scala"]:
    return "{}:{}:{}".format(
      coord["group_id"],
      coord["artifact_id"] + SCALA_SUFFIX,
      coord["version"],
    )
  else:
    return coordinates_string

def _local_ext_impl(mctx):
  artifacts = []
  for mod in mctx.modules:
    for install in mod.tags.install:
      artifacts += [_modify_artifact(artifact) for artifact in install.artifacts]
  maven_install(
    artifacts=artifacts,
    repositories=[
      "https://repo1.maven.org/maven2",
    ],
  )

maven = module_extension(
  implementation=_local_ext_impl,
  tag_classes={"install": _install},
)

def _parse_maven_coordinates(coordinates_string):
    """
    Given a string containing a standard Maven coordinate (g:a:[p:[c:]]v),
    returns a Maven artifact map (see above).
    See also https://github.com/bazelbuild/rules_jvm_external/blob/4.3/specs.bzl
    """
    if ":::" in coordinates_string:
      idx = coordinates_string.find(":::")
      group_id = coordinates_string[:idx]
      rest = coordinates_string[idx + 3:]
      is_scala = True
      full_scala = True
    elif "::" in coordinates_string:
      idx = coordinates_string.find("::")
      group_id = coordinates_string[:idx]
      rest = coordinates_string[idx + 2:]
      is_scala = True
      full_scala = False
    elif ":" in coordinates_string:
      idx = coordinates_string.find(":")
      group_id = coordinates_string[:idx]
      rest = coordinates_string[idx + 1:]
      is_scala = False
      full_scala = False
    else:
      fail("failed to parse '{}'".format(coordinates_string))
    parts = rest.split(":")
    artifact_id = parts[0]
    if (len(parts)) == 1:
      result = dict(group_id=group_id, artifact_id=artifact_id, is_scala=is_scala, full_scala=full_scala)
    elif len(parts) == 2:
      version = parts[1]
      result = dict(group_id=group_id, artifact_id=artifact_id, version=version, is_scala=is_scala, full_scala=full_scala)
    elif len(parts) == 3:
      packaging = parts[1]
      version = parts[2]
      result = dict(group_id=group_id, artifact_id=artifact_id, packaging=packaging, version=version, is_scala=is_scala, full_scala=full_scala)
    elif len(parts) == 4:
      packaging = parts[1]
      classifier = parts[2]
      version = parts[3]
      result = dict(group_id=group_id, artifact_id=artifact_id, packaging=packaging, classifier=classifier, version=version, is_scala=is_scala, full_scala=full_scala)
    else:
      fail("failed to parse '{}'".format(coordinates_string))
    return result
