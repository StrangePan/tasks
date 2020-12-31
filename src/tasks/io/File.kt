package tasks.io

import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.io.Reader
import java.io.StringReader
import java.io.Writer

/** An interface for interfacing with file system files.  */
interface File {
  fun openWriter(): Writer
  fun openReader(): Reader

  companion object {
    fun fromPath(path: String): File {
      return object : File {
        override fun openReader(): Reader {
          return try {
            FileReader(path)
          } catch (ex: FileNotFoundException) {
            StringReader("")
          }
        }

        override fun openWriter(): Writer {
          return try {
            FileWriter(path)
          } catch (ex: IOException) {
            throw RuntimeException(ex)
          }
        }
      }
    }
  }
}