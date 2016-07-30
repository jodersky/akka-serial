#Testing

`flow-samples` directory contains fully functional application examples of flow. To run an example, change to the base directory of flow and run sbt samples<SampleName>/run.
All projects, including samples, can be listed by running sbt projects.

To be able connect You can use real device (arduino) burned with sample-echo (`dev/arduino-terminal`) code, or create Virtual Serial Port pair

[socat (SOcket CAT)](http://www.dest-unreach.org/socat/) – multipurpose relay – is a command line based utility that establishes two bidirectional byte streams and transfers data between them.
socat is #4 on the Top 100 Network Security Tools list, available in most distro repositories (on Debian/Ubuntu sudo apt-get install socat does the trick), really light on resources, and very efficient.

To create a pair of VSP’s
```socat -d -d pty,raw,echo=0 pty,raw,echo=0```

you will get something like
```
socat[5894] N PTY is /dev/ttys002
socat[5894] N PTY is /dev/ttys003
socat[5894] N starting data transfer loop with FDs [5,5] and [7,7]
```
and that’s it! As long as the socat is running, you have a pair of VSP’s open (their names are printed by socat on initialization). See [socat man page](http://www.dest-unreach.org/socat/doc/socat.html) for more details on what the above command does.
Now You can connect to first socket ( in this case `/dev/ttys002`) using some sample (or Your code), and use second for monitoring or/and sending messages 
To send - use command 
```
echo 'Hello World' > /dev/ttys003
```
To listen - use command
```
cat < /dev/ttys003
```

Connecting executable and VSP
```
socat -d -d pty,raw,echo=0 "exec:myprog ...,pty,raw,echo=0"
```
where the executable `myprog` will be connected with the VSP through stdio.

For example Echo-server would look like 
```
socat -d -d pty,raw,echo=0 "exec:/bin/cat,pty,raw,echo=0"
```