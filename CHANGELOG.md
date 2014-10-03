Version 2.0.6
- Fix version incoherency problem.

Version 2.0.5
-  Upgrade to Akka dependency 2.3.6

Version 2.0.4
-  Upgrade to Akka dependency 2.3.5

Version 2.0.3
-  Upgrade to Akka dependency 2.3.4

Version 2.0.2
-  Upgrade to Akka dependency 2.3.3 (merge #10)
-  Add support for Scala 2.11 (merge #10)
-  Remove Scala version from native fat jar.

Version 2.0.1
-  Use system actor for manager.

Version 2.0
-  Use of direct buffers to increase performance when receiving and transmititng data.
-  Remove need to register to receive incoming data from an operator. A port is now opened by a client who will be the sole actor to receive messages from the operator.
-  Migrate native build to Autotools (C compiler is not called through sbt anymore).
-  Add debian packaging.
-  Add mac packaging.
-  Upgrade Akka dependency to 2.3.2.

Version 1.2
-  Upgrade Akka dependency to 2.3.0. (merge #3)

Version 1.1
-  Restructure build for easier cross-compilation. (fixes #1)

Version 1.0
-  Initial release.