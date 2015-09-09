# Watching Ports
As of version 2.2.0, flow can watch directories for new files. On most unix systems this can be used for watching for new serial ports in `/dev/`.
Watching happens through a message-based, publish-subscribe protocol as explained in the sections below.

## Subscribing
A client actor may watch -- i.e subscribe to notifications on -- a directory by sending a `Watch` command to the serial manager.

Should an error be encountered whilst trying to obtain the watch, the manager will respond with a `CommandFailed` message.
Otherwise, the client may be considered "subscribed" to the directory and the serial manager will thenceforth notify
the client on new files.

```scala
IO(Serial) ! Serial.Watch("/dev/")

def receive = {
  case Serial.CommandFailed(w: Watch, reason) =>
    println(s"Cannot obtain a watch on ${w.directory}: ${reason.getMessage}")
}

```

## Notifications
Whilst subscribed to a directory, a client actor is informed of any new files in said directory by receiving
`Connected` messages from the manager.

```scala
def receive = {
  case Serial.Connected(port) if port matches "/dev/ttyUSB\\d+" =>
    // do something with the available port, e.g.
    // IO(Serial) ! Open(port, settings)
}
```

## Unsubscribing
Unsubscribing from events on a directory is done by sending an `Unsubscribe` message to the serial manager.

```scala
IO(Serial) ! Unwatch("/dev/")
```

## Resource Handling
Note that the manager has a deathwatch on every subscribed client. Hence, should a client die, any underlying resources will be freed.

## Requirements
Flow uses Java's `WatchService`s under the hood, therefore a Java runtime of a version of at least 1.7 is required.
