import sbt._
import Keys._
import sbtassembly.AssemblyPlugin.autoImport._
import xml.{NodeSeq, Node => XNode, Elem}
import xml.transform.{RuleTransformer, RewriteRule}

object Shade {
  val shadePrefix = "gigahorse.shaded.ahc"
  val ShadeSandbox = config("shade").hide

  def ahcShadeSettings: Seq[Setting[_]] =
    inConfig(ShadeSandbox)(
      Defaults.configSettings ++
      baseAssemblySettings ++ Seq(
      logLevel in assembly := Level.Error,
      assemblyMergeStrategy in assembly := {
        case "META-INF/io.netty.versions.properties" =>
          MergeStrategy.first
        case "ahc-default.properties" =>
          ahcMerge
        case x =>
          val oldStrategy = (assemblyMergeStrategy in assembly).value
          oldStrategy(x)
      },
      //logLevel in assembly := Level.Debug,
      assemblyShadeRules in assembly := Seq(
        ShadeRule.rename("org.asynchttpclient.**" -> s"$shadePrefix.@0").inAll,
        ShadeRule.rename("io.netty.**"            -> s"$shadePrefix.@0").inAll,
        ShadeRule.rename("javassist.**"           -> s"$shadePrefix.@0").inAll,
        ShadeRule.rename("com.typesafe.netty.**"  -> s"$shadePrefix.@0").inAll,
        ShadeRule.zap("org.reactivestreams.**").inAll,
        ShadeRule.zap("org.slf4j.**").inAll
      ),
      assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeBin = false, includeScala = false),
      // cut ties with Runtime
      fullClasspath in assembly := fullClasspath.value,
      // cut ties with Runtime
      externalDependencyClasspath in assembly := externalDependencyClasspath.value,
      // cut ties with Runtime
      mainClass in assembly := mainClass.value,
      // cut ties with Runtime
      test in assembly := ()
    )) ++ Seq(
      packageBin in Compile := (assembly in ShadeSandbox).value,
      exportJars := true
    )

  val ahcMerge: sbtassembly.MergeStrategy = new sbtassembly.MergeStrategy {
    def apply(tempDir: File, path: String, files: Seq[File]): Either[String, Seq[(File, String)]] = {
      import scala.collection.JavaConverters._
      val file = MergeStrategy.createMergeTarget(tempDir, path)
      val lines = IO.readLines(files.head)
      lines.foreach { line =>
        // In AsyncHttpClientConfigDefaults.java, the shading renames the resource keys
        // so we have to manually tweak the resource file to match.
        val shadedline = line.replaceAllLiterally("org.asynchttpclient", s"$shadePrefix.org.asynchttpclient")
        IO.append(file, shadedline)
        IO.append(file, IO.Newline.getBytes(IO.defaultCharset))
      }
      Right(Seq(file -> path))
    }

    override val name: String = "ahcMerge"
  }

  def dependenciesFilter(n: XNode) = new RuleTransformer(new RewriteRule {
    override def transform(n: XNode): NodeSeq = n match {
      case e: Elem if e.label == "dependencies" => NodeSeq.Empty
      case other => other
    }
  }).transform(n).head
}
