package groupcache

import java.net.URL

object Implicits {
  /**
   * Implicitly converts a string to a URL.  Convenient when
   * constructing peers and groups.
   * @param string
   * @return
   */
  implicit def string2Url(string: String): URL = new URL(string)

  /**
   * Implicitly converts a string to a byte view.
   * @param string
   * @return
   */
  implicit def string2ByteView(string: String): ByteView = ByteView(string)

  /**
   * Implicitly converts a byte view to a string.
   * @param byteView
   * @return
   */
  implicit def byteView2String(byteView: ByteView): String = byteView.toString

  /**
   * Implicitly converts a byte array to a byte view.
   * @param byteArray
   * @return
   */
  implicit def byteArray2ByteView(byteArray: Array[Byte]): ByteView = ByteView(byteArray)

  /**
   * Implicitly converts a byte view to a byte array.
   * @param byteView
   * @return
   */
  implicit def byteView2ByteArray(byteView: ByteView): Array[Byte] = byteView.byteSlice
}

