import sbt._
import Keys._
import NativeKeys._
import java.io.File
import scala.collection.mutable.HashSet

object NativeFatKeys {
  val packageFat = taskKey[File]("Create a fat jar containing native binaries.")
  val packageFatSuffix = settingKey[String]("Suffix to add to name of fat jar.")
  val packageFatUnmanaged = settingKey[File]("Directory containing any pre-compiled native binaries.")
}

object NativeFatDefaults {
  import NativeFatKeys._

  val mappingsImpl = Def.task {
    val links = nativeLink.value //nativeLink produces native shared libraries for different platforms
    val unamanagedDir = packageFatUnmanaged.value

    val managed: Seq[(File, String)] = for ( (build, binary) <-  links.toSeq) yield {
      binary -> ("native/" + build.name + "/" + binary.name)
    }

    val unmanaged: Seq[(File, String)] = for (file <- (unamanagedDir ** "*").get; if file.isFile) yield {
      file -> ("native/" + (file relativeTo unamanagedDir).get.getPath)
    }
    
    managed ++ unmanaged
  }

  def settings = sbt.Defaults.packageTaskSettings(packageFat,  sbt.Defaults.packageBinMappings) ++ 
    Seq(
      packageFatSuffix := "-fat",
      packageFatUnmanaged := baseDirectory.value / "lib_native",
      products in packageFat := (products in Compile).value,
      artifact in packageFat := {
        val prev = (artifact in packageBin).value 
        prev.copy(name = prev.name + packageFatSuffix.value)
      },
      mappings in packageFat ++= mappingsImpl.value,
      publishArtifact in packageFat := true
    ) ++ addArtifact(artifact in packageFat, packageFat)

}