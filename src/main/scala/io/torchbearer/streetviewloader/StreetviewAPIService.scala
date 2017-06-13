package io.torchbearer.streetviewloader

import scala.io.Source
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import java.net.URL

import io.torchbearer.ServiceCore.Utils.formatURLWithQueryParams
import io.torchbearer.ServiceCore.AWSServices.KeyStore.getKey
import javax.imageio.ImageIO

/**
  * Created by fredricvollmer on 4/14/17.
  */
object StreetviewAPIService {
  lazy val googleKey = getKey("google-key")

  def getImageStream(lat: Double, long: Double, bearing: Int): (InputStream, Int) = {
    val urlString = formatURLWithQueryParams("https://maps.googleapis.com/maps/api/streetview",
      "key"         -> googleKey,
      "location"    -> s"$lat,$long",
      "heading"     -> bearing.toString,
      "fov"         -> "90",
      "size"        -> "640x640"
    )

    val url = new URL(urlString)

    val img = ImageIO.read(url)
    val outStream = new ByteArrayOutputStream()
    ImageIO.write(img, "jpg", outStream)
    outStream.flush()
    outStream.close()

    (new ByteArrayInputStream(outStream.toByteArray), outStream.size)
  }
}
