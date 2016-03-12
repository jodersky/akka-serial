---
layout: page
title: Downloads
---

## Latest Version <small>{{site.data.current.version}}</small>

{::options parse_block_html="true" /}
<div class="row">

<div class="col-sm-8">
### Sbt Coordinates :
Add the following to your build configuration:

~~~scala
//add resolver to Bintray's jcenter
resolvers += Resolver.jcenterRepo

//main artifact
libraryDependencies += "com.github.jodersky" %% "flow-core" % "{{site.data.current.version}}"

//(optional) "fat" jar containing native libraries
libraryDependencies += "com.github.jodersky" % "flow-native" % "{{site.data.current.version}}" % "runtime"

//(optional & experimental) support for Akka streams
libraryDependencies += "com.github.jodersky" %% "flow-stream" % "{{site.data.current.version}}"
~~~

### Archives

- Native Libraries
[.tar.gz](https://bintray.com/artifact/download/jodersky/generic/flow-native-libraries-{{site.data.current.native_version.major}}.{{site.data.current.native_version.minor}}.{{site.data.current.native_version.patch}}.tar.gz)
[.asc (signature)](https://bintray.com/artifact/download/jodersky/generic/flow-native-libraries-{{site.data.current.native_version.major}}.{{site.data.current.native_version.minor}}.{{site.data.current.native_version.patch}}.tar.gz.asc)

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
		<tr><td rowspan="4">Linux</td><td>x86_64</td></tr>
		<tr><td>x86</td></tr>
		<tr><td>armv6l</td></tr>
		<tr><td>armv7l</td></tr>
		<tr><td>Darwin (Mac OSX)</td><td>x86_64</td></tr>
	</tbody>
</table>

<p class="small">*These are the platforms for which a native library is included in the release.
Flow is POSIX compatible so it can be built for a lot more platforms.</p>
</div>

</div>

## Release Notes
Consult the <a href="https://github.com/jodersky/flow/blob/master/CHANGELOG.md">release notes</a> for important changes.

## Requirements
Flow depends on Akka 2.4 and requires a Java runtime version of 1.8. It is released for Scala binary versions 2.11 and 2.12.0-M3.

## Previous Versions
Archived releases are available in <a href="https://github.com/jodersky/flow/releases">GitHub releases</a>.
