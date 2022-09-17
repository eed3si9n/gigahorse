```
$ jenv shell 1.8
$ sbt
release
exit
```

```
bin/multiversion import-build --output-path=3rdparty/jvm_deps.bzl

bazel run //core/src/main:main.publish -- release --publish_to=/tmp/repo
```
