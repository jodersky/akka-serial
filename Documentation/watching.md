# Watching Ports
As of version 2.2.0, flow can watch directories for new files. On most unix systems this can be used for watching for new serial ports in `/dev`.
Watching happens through a message-based, publish-subscribe protocol as explained in the sections below.

## Subscribe
A client actor may watch, i.e subscribe to notifications on, a directory by sending a `Watch` command to the serial manager.

Should an error be encountered whilst trying to obtain the watch, the manager will respond with a `CommandFailed` message.
Otherwise, the client may be considered "subscribed" to the directory and the serial manager will thenceforth notify
the client on new files.

```scala
IO(Serial) ! Watch(directory = "/dev", skipInitial = true)

def receive = {
  case CommandFailed(w: Watch, reason) =>
    println(s"Cannot obtain a watch on ${w.directory}: ${reason.getMessage}")
}

```

Note the second argument `skipInitial` of the watch command. This flag specifies if the client should not be notified of files already present
during the manager's reception of the watch command.

## Notifications
Whilst subscribed to a directory, a client actor is informed of any new files in said directory by receiving
`Connected` messages from the manager.

```scala
def receive = {
  case Connected(port) if port matches "/dev/ttyUSB.*" =>
    // do something with the available port, e.g.
    // IO(Serial) ! Open(port, settings)
}
```

## Unsubscribe
Unsubscribing from events on a directory happens by sending an `Unsubscribe` message to the serial manager.

```scala
IO(Serial) ! Unwatch(directory = "/dev")
```

Note that the manager has a deathwatch on every subscribed client. Hence should a client die, underlying resources will be freed.

# Requirements
Flow uses java's `WatchService`s under the hood, therefore a java runtime of at least 1.7 is required.
