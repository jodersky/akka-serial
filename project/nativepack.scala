import sbt._
import Keys._
import NativeKeys._
import java.io.File
import scala.collection.mutable.HashSet

object NativePackKeys {

  val nativePackLinkages = taskKey[Seq[(NativeBuild, File)]]("")  
  val nativePackUnmanaged = settingKey[File]("Directory containing any pre-compiled native binaries.")

}

object NativePackDefaults {
  import NativePackKeys._

  val mappingsImpl = Def.task {
    val links = nativePackLinkages.value 
    val unamanagedDir = nativePackUnmanaged.value

    val managed: Seq[(File, String)] = for ( (build, binary) <- links.toSeq) yield {
      binary -> ("native/" + build.name + "/" + binary.name)
    }

    val unmanaged: Seq[(File, String)] = for (file <- (unamanagedDir ** "*").get; if file.isFile) yield {
      file -> ("native/" + (file relativeTo unamanagedDir).get.getPath)
    }
    
    managed ++ unmanaged
  }

  def settings = Seq(
    nativePackUnmanaged := baseDirectory.value / "lib_native",
    mappings in (Compile, packageBin) ++= mappingsImpl.value
  )

}