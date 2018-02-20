package io.torchbearer.streetviewloader.services

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import java.net.URL
import javax.imageio.ImageIO
import scalaj.http._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import com.javadocmd.simplelatlng.LatLng
import io.torchbearer.ServiceCore.AWSServices.KeyStore.getKey
import io.torchbearer.ServiceCore.Utils.formatURLWithQueryParams

/**
  * Created by fredricvollmer on 4/14/17.
  */
object StreetviewAPIService {
  private lazy val googleKey = getKey("google-key")
  implicit val formats = DefaultFormats

  def getImageLocation(lat: Double, long: Double): LatLng = {
    val urlString = formatURLWithQueryParams("https://maps.googleapis.com/maps/api/streetview/metadata",
      "key"         -> googleKey,
      "location"    -> s"$lat,$long"
    )
    val response = parse(Http(urlString).asString.body)
    new LatLng((response \ "location" \ "lat").extract[Double], (response \ "location" \ "lng").extract[Double])
  }

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
