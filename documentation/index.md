---
layout: page
title: Documentation
---
## Documentation

-----

### NOTICE: Upgrading from 2.0 to 3.0

The latest release, 3.0.0, introduces some compatibility-breaking changes:

- Organization and packages have been changes from "com.github.jodersky" to "ch.jodersky"
- A new major version of the native library has been released, libflow4

It should be sufficient to change any imports that previously used "com.github.jodersky.flow" to "ch.jodersky.flow".
In case you manually istaled the native library, you will also need to install libflow4 (see the Developer Guide for more information).

-----

Start by reading the manual and checking out some examples.

- <i class="fa fa-book"></i> [Manual]({{site.version_docs}}/manual)
  Explains how to get started and how to use all features. Definitive guide to using flow.

- <i class="fa fa-book"></i> [Examples](https://github.com/jodersky/flow/tree/master/flow-samples)
  See some very simple, working demo applications.

- <i class="fa fa-code"></i> [API documentation]({{site.version_docs}}/api/index.html#ch.jodersky.flow.Serial$)
  Browse flow's API.

- <i class="fa fa-book"></i> [Developer Guide]({{site.version_docs}}/developer)
  Instructions on building and publishing flow.

## Help
Have a question or suggestion? Found a bug? There are several channels to get help.

- <i class="fa fa-bug"></i> [Issues](https://github.com/jodersky/flow/issues)
  Check known issues or file a new one. This is also the place to go for long questions or propositions.

- <i class="fa fa-comments"></i> [Chat](https://gitter.im/jodersky/flow)
  Gitter chat for simple inquiries.

## Use cases
Get inspired by some real projects that use flow.

- [Virtual Cockpit](https://github.com/project-condor/mavigator) part of [Project Condor](https://project-condor.github.io/), a do-it-yourself drone.

- [Hyperion](https://github.com/mthmulders/hyperion) back-end, part of a web-based energy dashboard for "smart meters".

- [(add yours by opening an issue)](https://github.com/jodersky/flow/issues)
