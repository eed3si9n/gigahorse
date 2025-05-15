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
import java.nio.ByteBuffer
import java.security.MessageDigest
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

  def sha1(in: File): String = {
    val digest = MessageDigest.getInstance("SHA-1")
    val fis = new FileInputStream(in)
    try {
      val baos = new ByteArrayOutputStream()
      transfer(fis, baos)
      digest.update(baos.toByteArray())
      toHex(digest.digest())
    } finally fis.close()
  }
  def toHex(bytes: Array[Byte]): String = {
    val buffer = new StringBuilder(bytes.length * 2)
    for (i <- bytes.indices) {
      val b = bytes(i)
      val bi: Int = if (b < 0) b + 256 else b.toInt
      buffer append toHex((bi >>> 4).asInstanceOf[Byte])
      buffer append toHex((bi & 0x0f).asInstanceOf[Byte])
    }
    buffer.toString
  }
  private def toHex(b: Byte): Char = {
    require(b >= 0 && b <= 15, "Byte " + b + " was not between 0 and 15")
    if (b < 10)
      ('0'.asInstanceOf[Int] + b).asInstanceOf[Char]
    else
      ('a'.asInstanceOf[Int] + (b - 10)).asInstanceOf[Char]
  }
  def write(out: File, content: String): Unit = {
    val fos = new FileOutputStream(out)
    val bais = new ByteArrayInputStream(content.getBytes("UTF-8"))
    try
      transfer(bais, fos)
    finally fos.close()
  }
  def write(out: File, content: ByteBuffer): Unit = {
    val fos = new FileOutputStream(out)
    val bais = new ByteBufferBackedInputStream(content)
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

  class ByteBufferBackedInputStream(buf: ByteBuffer) extends InputStream {
    override def available: Int = buf.remaining()
    override def read: Int =
      if (buf.hasRemaining()) buf.get() & 0xff
      else -1
    override def read(bytes: Array[Byte], off: Int, len: Int): Int =
      if (!buf.hasRemaining()) -1
      else {
        val len1 = Math.min(len, buf.remaining())
        buf.get(bytes, off, len1)
        len1
      }
  }
}
