import sbt._
import Keys._
import java.io.File

object NativeKeys {

    val nativeBuildDirectory = settingKey[File]("Directory containing native build scripts.")
    val nativeTarget = settingKey[File]("Target directory to store native artifacts.")

    val nativeBuild = taskKey[File]("Invoke native build.")

    val nativePackUnmanaged = settingKey[File]("")
}

object NativeDefaults {
    import NativeKeys._

    val nativeBuildImpl = Def.task {
        val log = streams.value.log
        val build = nativeBuildDirectory.value
        val target = nativeTarget.value

        val configure = Process(
            "./configure " +
            "--prefix=" + target.getAbsolutePath + " " +
            "--libdir=" + target.getAbsolutePath,
            Some(build))

        val make = Process("make", build)

        val makeInstall = Process("make install", build)

        val ev = configure #&& make #&& makeInstall ! log
        if (ev != 0)
            throw new RuntimeException(s"Building native library failed.")

        (target ** ("*.la")).get.foreach(_.delete())

        target
    }


    val mappingsImpl = Def.task {
        val files = (nativeBuild.value ** "*").get
        val unamanagedDir = nativePackUnmanaged.value

        val managed: Seq[(File, String)] = for (file <- files; if file.isFile) yield {
            file -> ("native/" + (file relativeTo nativeTarget.value).get.getPath)
        }

        val unmanaged: Seq[(File, String)] = for (file <- (unamanagedDir ** "*").get; if file.isFile) yield {
            file -> ("native/" + (file relativeTo unamanagedDir).get.getPath)
        }
    
        managed ++ unmanaged
    }

    def os = System.getProperty("os.name").toLowerCase.filter(_ != ' ')
    def arch = System.getProperty("os.arch").toLowerCase

    val settings: Seq[Setting[_]] = Seq(
        nativeTarget := target.value / "native" / (os + "-" + arch),
        nativeBuild := nativeBuildImpl.value,
        nativePackUnmanaged := baseDirectory.value / "lib_native",
        mappings in (Compile, packageBin) ++= mappingsImpl.value
    )

}

