# Roadmap - changes that are, will or may be applied

## Application side
- Eliminate registering to receive data from an operator. A port is now opened by a client who will be the sole actor to receive messages from the operator. (DONE)
- Use of direct buffers to increase performance when receiving and transmititng data. (DONE)
- Remove flow-pack (Probably, it may look convenient at first but it is really a kind of dirty hack)

## Build
- Add better makefile build (Mostly done, TODO: become agnostic of jni.h location)
- Add GNU Autotools build (Not sure, since autotools look kind of overly complex for a really simple native backend)
- Add debian packaging (DONE)
- Drop sbt native build (Probably, it seems like it was a mistake to call native compiler commands from sbt)
- Reimplement native compilation for other Unix-like platforms