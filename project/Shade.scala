import sbt.*
import Keys.*
import sbtassembly.AssemblyPlugin.autoImport.*
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import xml.{ NodeSeq, Node as XNode, Elem }
import xml.transform.{ RuleTransformer, RewriteRule }

object Shade {
  val shadePrefix = "gigahorse.shaded.ahc"
  val shadePrefix2 = "gigahorse.shaded.apache"
  val ShadeSandbox = config("shade").hide

  def apacheShadeSettings: Seq[Setting[?]] =
    inConfig(ShadeSandbox)(
      Defaults.configSettings ++
        baseAssemblySettings ++ Seq(
          assembly / logLevel := Level.Error,
          // assembly / logLevel := Level.Debug,
          assembly / assemblyShadeRules := Seq(
            ShadeRule.rename("org.apache.**" -> s"$shadePrefix2.@0").inAll,
            ShadeRule.zap("org.reactivestreams.**").inAll,
            ShadeRule.zap("org.slf4j.**").inAll
          ),
          assembly / assemblyOption := (assembly / assemblyOption).value
            .withIncludeBin(false)
            .withIncludeScala(false),
          // cut ties with Runtime
          assembly / fullClasspath := fullClasspath.value,
          // cut ties with Runtime
          assembly / externalDependencyClasspath := externalDependencyClasspath.value,
          // cut ties with Runtime
          assembly / mainClass := mainClass.value,
          // cut ties with Runtime
          assembly / test := {}
        )
    ) ++ Seq(
      Compile / packageBin := (ShadeSandbox / assembly).value,
    )

  def ahcShadeSettings: Seq[Setting[?]] =
    inConfig(ShadeSandbox)(
      Defaults.configSettings ++
        baseAssemblySettings ++ Seq(
          assembly / logLevel := Level.Error,
          assembly / assemblyMergeStrategy := {
            case "META-INF/io.netty.versions.properties" =>
              MergeStrategy.first
            case "gigahorse/shaded/ahc/org/asynchttpclient/config/ahc-default.properties" =>
              ahcMerge
            case x =>
              val oldStrategy = (assembly / assemblyMergeStrategy).value
              oldStrategy(x)
          },
          // assembly / logLevel  := Level.Debug,
          assembly / assemblyShadeRules := Seq(
            ShadeRule.rename("org.asynchttpclient.**" -> s"$shadePrefix.@0").inAll,
            ShadeRule.rename("io.netty.**" -> s"$shadePrefix.@0").inAll,
            ShadeRule.rename("javassist.**" -> s"$shadePrefix.@0").inAll,
            ShadeRule.rename("com.typesafe.netty.**" -> s"$shadePrefix.@0").inAll,
            ShadeRule.zap("org.reactivestreams.**").inAll,
            ShadeRule.zap("org.slf4j.**").inAll
          ),
          assembly / assemblyOption := (assembly / assemblyOption).value
            .withIncludeBin(false)
            .withIncludeScala(false),
          // cut ties with Runtime
          assembly / fullClasspath := fullClasspath.value,
          // cut ties with Runtime
          assembly / externalDependencyClasspath := externalDependencyClasspath.value,
          // cut ties with Runtime
          assembly / mainClass := mainClass.value,
          // cut ties with Runtime
          assembly / test := {}
        )
    ) ++ Seq(
      Compile / packageBin := (ShadeSandbox / assembly).value,
    )

  val ahcMerge: sbtassembly.MergeStrategy = sbtassembly.CustomMergeStrategy("ahcMerge") {
    dependencies =>
      val Seq(resourceFile) = dependencies
      val result = () => {
        val newLines = IO
          .readStream(resourceFile.stream.apply())
          .linesIterator
          .flatMap { line =>
            // In AsyncHttpClientConfigDefaults.java, the shading renames the resource keys
            // so we have to manually tweak the resource file to match.
            val shadedline =
              line.replaceAllLiterally("org.asynchttpclient", s"$shadePrefix.org.asynchttpclient")

            Seq(
              line,
              IO.Newline,
              shadedline,
              IO.Newline
            )
          }
          .mkString
        new ByteArrayInputStream(newLines.getBytes(StandardCharsets.UTF_8))
      }
      Right(Vector(JarEntry(resourceFile.target, result)))
  }

  def dependenciesFilter(n: XNode) = new RuleTransformer(new RewriteRule {
    override def transform(n: XNode): NodeSeq = n match {
      case e: Elem if e.label == "dependencies" => NodeSeq.Empty
      case other                                => other
    }
  }).transform(n).head
}
