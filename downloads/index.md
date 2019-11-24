---
layout: page
title: Downloads
---

## Latest Version <small>{{site.version_latest}}</small>

{::options parse_block_html="true" /}
<div class="row">

<div class="col-sm-8">
### Sbt Coordinates
Add the following to your build configuration:

~~~scala
// main artifact
libraryDependencies += "ch.jodersky" %% "akka-serial-core" % "{{site.version_latest}}"

// "fat" jar containing native libraries
libraryDependencies += "ch.jodersky" % "akka-serial-native" % "{{site.version_latest}}" % "runtime"

// support for Akka streams
libraryDependencies += "ch.jodersky" %% "akka-serial-stream" % "{{site.version_latest}}"
~~~

</div>

<div class="col-sm-4">
### Supported Platforms*

<table class="table">
	<thead>
		<tr>
			<th>Kernel</th><th>Architecture</th>
		</tr>
	</thead>
	<tbody>
		<tr><td rowspan="4">Linux (glibc >= 2.4)</td><td>x86_64</td></tr>
		<tr><td>x86</td></tr>
		<tr><td>armv6</td></tr>
		<tr><td>armv7l</td></tr>
		<tr><td>Darwin (Mac OSX >= 10.8)</td><td>x86_64</td></tr>
	</tbody>
</table>

<p class="small">*These are the platforms for which a native library is included in the fat jar release.
akka-serial is POSIX compatible so it can be built for a lot more platforms.</p>
</div>

</div>

## Release Notes
Consult the <a href="https://github.com/jodersky/akka-serial/blob/master/CHANGELOG.md">release notes</a> for important changes.

## Requirements
akka-serial depends on Akka 2.6 and requires a Java runtime version of 1.8. It is released for Scala binary versions 2.12 and 2.13.

## Previous Versions
Archived releases are available in <a href="https://github.com/jodersky/akka-serial/releases">GitHub releases</a>.
