{% highlight scala %}
//main artifact
libraryDependencies += "com.github.jodersky" %% "flow" % "{{site.data.releases.current.version}}"

//(optional) "fat" jar containing native libraries
libraryDependencies += "com.github.jodersky" % "flow-native" % "{{site.data.releases.current.version}}" % "runtime"
{% endhighlight %}
