---
layout: page
title: Downloads
---

## Latest Version <small>{{site.data.current.version}}</small>

{::options parse_block_html="true" /}
<div class="row">

<div class="col-sm-8">
### Sbt Coordinates :
Add the following tothe build configuration:

~~~scala
//main artifact
libraryDependencies += "com.github.jodersky" %% "flow" % "{{site.data.current.version}}"

//(optional) "fat" jar containing native libraries
libraryDependencies += "com.github.jodersky" % "flow-native" % "{{site.data.current.version}}" % "runtime"
~~~

### Archives

- Main Jar
[.jar (scala 2.11)](https://bintray.com/artifact/download/jodersky/maven/com/github/jodersky/flow_2.11/2.4.0-M1/flow_2.11-2.4.0-M1.jar)
[.asc (signature)](https://bintray.com/artifact/download/jodersky/maven/com/github/jodersky/flow_2.11/2.4.0-M1/flow_2.11-2.4.0-M1.jar.asc)

- Native Jar
[.jar](https://bintray.com/artifact/download/jodersky/maven/com/github/jodersky/flow-native/2.4.0-M1/flow-native-2.4.0-M1.jar)
[.asc (signature)](https://bintray.com/artifact/download/jodersky/maven/com/github/jodersky/flow-native/2.4.0-M1/flow-native-2.4.0-M1.jar.asc)

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
		<tr><td rowspan="3">Linux</td><td>x86_64</td></tr>
		<tr><td>x86</td></tr>
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
Flow depends on Akka 2.4 and requires a Java runtime version of 1.8.

## Previous Versions

### 2.3.1

- Main Jar
[.jar (scala 2.11)](https://bintray.com/artifact/download/jodersky/maven/com/github/jodersky/flow_2.11/2.3.1/flow_2.11-2.3.1.jar)
[.asc (signature)](https://bintray.com/artifact/download/jodersky/maven/com/github/jodersky/flow_2.11/2.3.1/flow_2.11-2.3.1.jar.asc)

- Native Jar
[.jar](https://bintray.com/artifact/download/jodersky/maven/com/github/jodersky/flow-native/2.3.1/flow-native-2.3.1.jar)
[.asc (signature)](https://bintray.com/artifact/download/jodersky/maven/com/github/jodersky/flow-native/2.3.1/flow-native-2.3.1.jar.asc)


### Older Releases
Archived releases are available in <a href="https://github.com/jodersky/flow/releases">GitHub releases</a>.
