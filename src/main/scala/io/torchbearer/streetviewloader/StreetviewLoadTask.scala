package io.torchbearer.streetviewloader

import com.javadocmd.simplelatlng.util.LengthUnit
import io.torchbearer.ServiceCore.Orchestration.Task
import io.torchbearer.ServiceCore.DataModel.ExecutionPoint
import io.torchbearer.ServiceCore.AWSServices.S3
import com.javadocmd.simplelatlng.{LatLng, LatLngTool}
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}
import io.torchbearer.ServiceCore.Constants

/**
  * Created by fredricvollmer on 4/13/17.
  */
class StreetviewLoadTask(epId: Int, hitId: Int, taskToken: String)
  extends Task(epId = epId, hitId = hitId, taskToken = taskToken) {

  override def run(): Unit = {
    // Load ExecutionPoint
    val ep = ExecutionPoint.getExecutionPoint(epId) getOrElse { return }
    val point = new LatLng(ep.lat, ep.long)
    val inverseBearing = ep.bearing - 180

    val imagePointAt = LatLngTool.travel(point, inverseBearing, Constants.IMAGE_DISTANCE_AT, LengthUnit.MILE)
    val imagePointBefore = LatLngTool.travel(point, inverseBearing, Constants.IMAGE_DISTANCE_BEFORE, LengthUnit.MILE)
    val imagePointJustBefore = LatLngTool.travel(point, inverseBearing, Constants.IMAGE_DISTANCE_JUST_BEFORE, LengthUnit.MILE)

    try {
      // For maneuvers, take streetview images at 3 distances starting at 20 feet back
      if (ep.executionPointType == Constants.EXECUTION_POINT_TYPE_MANEUVER) {
        putStreetviewImage(ep, Constants.POSITION_AT, imagePointAt)

        if (ep.closestIntersectionDistance > Constants.IMAGE_DISTANCE_JUST_BEFORE) {
          putStreetviewImage(ep, Constants.POSITION_JUST_BEFORE, imagePointJustBefore)
        }

        if (ep.closestIntersectionDistance > Constants.IMAGE_DISTANCE_BEFORE) {
          putStreetviewImage(ep, Constants.POSITION_BEFORE, imagePointBefore)
        }
      }
      else {
        // For destinations, take single streetview image right at point
        putStreetviewImage(ep, Constants.POSITION_AT, point)
      }

      this.sendSuccess()
    }
    catch {
      case e: Throwable => sendFailure("Streetview Loader Error", e.getMessage)
    }
  }

  private def putStreetviewImage(executionPoint: ExecutionPoint, position: String, imagePoint: LatLng): Unit = {
    val client = S3.getClient
    val key = s"${executionPoint.executionPointId}_$position.jpg"

    val bearing = executionPoint.executionPointType match {
      case Constants.EXECUTION_POINT_TYPE_MANEUVER => executionPoint.bearing
      case Constants.EXECUTION_POINT_TYPE_DESTINATION_LEFT => (360 + executionPoint.bearing - 90) % 360
      case Constants.EXECUTION_POINT_TYPE_DESTINATION_RIGHT => (executionPoint.bearing + 90) % 360
    }

    val (imgStream, contentLength) = StreetviewAPIService.getImageStream(imagePoint.getLatitude, imagePoint.getLongitude, bearing)

    val metadata = new ObjectMetadata()
    metadata.setContentLength(contentLength)
    metadata.setContentType("image/jpeg")

    val req = new PutObjectRequest(Constants.S3_SV_IMAGE_BUCKET, key, imgStream, metadata)

    client.putObject(req)
  }
}
