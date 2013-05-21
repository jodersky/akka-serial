name := "flow"

scalaVersion := "2.10.1"

fork := true

connectInput in run := true

javaOptions in run += "-Djava.library.path=."