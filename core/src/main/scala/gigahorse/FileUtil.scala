package gigahorse

import java.io.{
  ByteArrayInputStream,
  ByteArrayOutputStream,
  File,
  FileInputStream,
  FileOutputStream,
  InputStream,
  OutputStream,
}
import scala.annotation.tailrec

object FileUtil {
  def read(in: File): String = {
    val fis = new FileInputStream(in)
    try read(fis)
    finally fis.close()
  }
  def read(in: InputStream): String = {
    val baos = new ByteArrayOutputStream()
    transfer(in, baos)
    new String(baos.toByteArray(), "UTF-8")
  }
  def write(out: File, content: String): Unit = {
    val fos = new FileOutputStream(out)
    val bais = new ByteArrayInputStream(content.getBytes("UTF-8"))
    try
      transfer(bais, fos)
    finally fos.close()
  }

  def transfer(in: InputStream, out: OutputStream): Unit =
    transfer(in, out, false)

  def transfer(in: InputStream, out: OutputStream, close: Boolean): Unit =
    try {
      val buffer = new Array[Byte](1024 * 1014)
      @tailrec def read(): Unit = {
        val byteCount = in.read(buffer)
        if (byteCount >= 0) {
          out.write(buffer, 0, byteCount)
          read()
        }
      }
      read()
    } finally {
      if (close) in.close
    }
}
