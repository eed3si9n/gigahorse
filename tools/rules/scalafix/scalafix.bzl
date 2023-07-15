def _scalafix_impl(ctx):
    out = ctx.actions.declare_file(ctx.label.name + ".sh")
    tool = ctx.executable._scalafix_bin
    tool_rf = ctx.attr._scalafix_bin[DefaultInfo]
    srcs = ctx.files.srcs
    semanticdb = ctx.attr.semanticdb[DefaultInfo]
    deps = semanticdb.files.to_list()
    script = """SANDBOX_DIR=$(pwd)
JARS=""
JARS0=({jars})
for JAR in ${{JARS0[@]}}; do
  JARS="$JARS --classpath $SANDBOX_DIR/$JAR"
done
cd "$BUILD_WORKING_DIRECTORY"
exec "$SANDBOX_DIR/{tool}" --files {srcs} $JARS --scalac-options -Xlint $@
""".format(
        tool = tool.short_path,
        srcs = " --files ".join([src.short_path for src in srcs]),
        jars = " ".join([x.short_path for x in deps])
    )
    ctx.actions.write(out, script, is_executable = True)
    files = srcs + deps
    rf = ctx.runfiles(
        transitive_files = depset(files)
    ).merge(tool_rf.default_runfiles)
    return [DefaultInfo(
        executable = out,
        runfiles = rf,
    )]

scalafix = rule(
    attrs = {
        "srcs": attr.label_list(
            allow_files = [".scala"],
        ),
        "semanticdb": attr.label(),
        "_scalafix_bin": attr.label(
            executable = True,
            cfg = "host",
            allow_files = True,
            default = Label("//tools/scalafix_app:bin"),
        ),
    },
    doc = "Runs scalafix",
    executable = True,
    implementation = _scalafix_impl,
)
