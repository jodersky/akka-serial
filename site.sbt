import com.typesafe.sbt.site.JekyllSupport._
import com.typesafe.sbt.site.PamfletSupport._

site.settings

site.addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), "latest/api")

sourceDirectory in Jekyll := file("site")

site.jekyllSupport()
