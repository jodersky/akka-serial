import sbt._
import Keys._
import java.io.File
import java.util.jar.Manifest

object NativeKeys {

    val nativeBuildDirectory = settingKey[File]("Directory containing native build scripts.")
    val nativeTargetDirectory = settingKey[File]("Base directory to store native products.")
    val nativeOutputDirectory = settingKey[File]("Actual directory where native products are stored.")
    val nativePackageUnmanagedDirectory = settingKey[File]("Directory containing external products that will be copied to the native jar.")
    val nativePackageArtifact = settingKey[Artifact]("Native artifact.")

    
    val nativeBuild = taskKey[File]("Invoke native build.")
    val nativePackage = taskKey[File]("Package native products into a jar.")
    
}

object NativeDefaults {
    import NativeKeys._

    val autoLib = Def.task {
        val log = streams.value.log
        val build = nativeBuildDirectory.value
        val out = nativeOutputDirectory.value

        val configure = Process(
            "./configure " +
            "--prefix=" + out.getAbsolutePath + " " +
            "--libdir=" + out.getAbsolutePath + " " +
            "--disable-versioned-lib",
            build)

        val make = Process("make", build)

        val makeInstall = Process("make install", build)

        val ev = configure #&& make #&& makeInstall ! log
        if (ev != 0)
            throw new RuntimeException(s"Building native library failed.")

        (out ** ("*.la")).get.foreach(_.delete())

        out
    }

    val nativePackageImpl = Def.task {
        val managedDir = nativeTargetDirectory.value
        val unmanagedDir = nativePackageUnmanagedDirectory.value

        val managed = (nativeBuild.value ** "*").get
        val unmanaged = (unmanagedDir ** "*").get

        val jarFile = nativeTargetDirectory.value / (name.value + "-" + version.value + "-native.jar")

        val managedMappings: Seq[(File, String)] = for (file <- managed; if file.isFile) yield {
            file -> ("native/" + (file relativeTo managedDir).get.getPath)
        }

        val unmanagedMappings: Seq[(File, String)] = for (file <- unmanaged; if file.isFile) yield {
            file -> ("native/" + (file relativeTo unmanagedDir).get.getPath)
        }

        IO.jar(managedMappings ++ unmanagedMappings, jarFile, new Manifest())
        jarFile
    }

    def os = System.getProperty("os.name").toLowerCase.filter(_ != ' ')
    def arch = System.getProperty("os.arch").toLowerCase

    val settings: Seq[Setting[_]] = Seq(
        nativeTargetDirectory := target.value / "native",
        nativeOutputDirectory := nativeTargetDirectory.value / (os + "-" + arch),
        nativeBuild := autoLib.value,
        nativePackage := nativePackageImpl.value,
        nativePackageArtifact := Artifact(name.value, "native"),
        nativePackageUnmanagedDirectory := baseDirectory.value / "lib_native"
    ) ++ addArtifact(nativePackageArtifact, nativePackage).settings

}

