package com.github.jodersky.flow.internal

import java.io.File
import java.io.FileOutputStream

/**Loads the current system's native library for flow. */
object NativeLoader {

  def load = {
    val os = System.getProperty("os.name").toLowerCase
    val arch = System.getProperty("os.arch").toLowerCase

    val in = NativeLoader.getClass().getResourceAsStream("/native/" + os + "/" + arch + "/" + "libflow.so")
    val temp = File.createTempFile("flow" + os + arch, ".so");
    temp.deleteOnExit()
    val out = new FileOutputStream(temp);

    try {
      var read: Int = 0; ;
      val buffer = new Array[Byte](4096);
      do {
        read = in.read(buffer)
        if (read != -1) {
          out.write(buffer, 0, read);
        }
      } while (read != -1)
    } finally {
      in.close()
      out.close
    }

    System.load(temp.getAbsolutePath())

  }

}