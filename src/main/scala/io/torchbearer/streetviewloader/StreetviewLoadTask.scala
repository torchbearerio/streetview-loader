package io.torchbearer.streetviewloader

import com.javadocmd.simplelatlng.util.LengthUnit
import io.torchbearer.ServiceCore.Orchestration.Task
import io.torchbearer.ServiceCore.DataModel.{ExecutionPoint, Hit, StreetviewImage}
import io.torchbearer.ServiceCore.AWSServices.S3
import com.javadocmd.simplelatlng.{LatLng, LatLngTool}
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}
import io.torchbearer.ServiceCore.Constants
import io.torchbearer.streetviewloader.services.StreetviewAPIService

/**
  * Created by fredricvollmer on 4/13/17.
  */
class StreetviewLoadTask(epId: Int, hitId: Int, taskToken: String)
  extends Task(epId = epId, hitId = hitId, taskToken = taskToken) {

  override def run(): Unit = {
    // Set start time
    Hit.setStartTimeForTask(hitId, "streetview_load", System.currentTimeMillis)

    // Load ExecutionPoint
    val ep = ExecutionPoint.getExecutionPoint(epId) getOrElse { throw new Exception("Could not load execution point") }
    val hit = Hit.getHit(hitId) getOrElse { throw new Exception("Could not load hit") }
    val point = new LatLng(ep.lat, ep.long)
    val inverseBearing = ep.bearing - 180

    val imagePointAt = LatLngTool.travel(point, inverseBearing, Constants.IMAGE_DISTANCE_AT, LengthUnit.MILE)
    val imagePointBefore = LatLngTool.travel(point, inverseBearing, Constants.IMAGE_DISTANCE_BEFORE, LengthUnit.MILE)
    val imagePointJustBefore = LatLngTool.travel(point, inverseBearing, Constants.IMAGE_DISTANCE_JUST_BEFORE, LengthUnit.MILE)

    try {
      // For maneuvers, take streetview images at 3 distances starting at 20 feet back
      if (ep.executionPointType == Constants.EXECUTION_POINT_TYPE_MANEUVER) {
        putStreetviewImage(ep, Constants.POSITION_AT, imagePointAt, hit.pipeline)

        if (ep.closestIntersectionDistance > Constants.IMAGE_DISTANCE_JUST_BEFORE) {
          putStreetviewImage(ep, Constants.POSITION_JUST_BEFORE, imagePointJustBefore, hit.pipeline)
        }

        if (ep.closestIntersectionDistance > Constants.IMAGE_DISTANCE_BEFORE) {
          putStreetviewImage(ep, Constants.POSITION_BEFORE, imagePointBefore, hit.pipeline)
        }
      }
      else {
        // For destinations, take single streetview image right at point
        putStreetviewImage(ep, Constants.POSITION_AT, point, hit.pipeline)
      }

      println(s"Finished streetview load task for ep $epId and hit $hitId")

      this.sendSuccess()
    }
    catch {
      case e: Throwable =>
        println(s"Streetview loader error wit epi $epId and hit $hitId")
        e.printStackTrace()
        sendFailure("Streetview Loader Error", e.getMessage)
    }

    finally {
      // Set end time
      Hit.setEndTimeForTask(hitId, "streetview_load", System.currentTimeMillis)
    }
  }

  private def putStreetviewImage(executionPoint: ExecutionPoint, position: String, imagePoint: LatLng, pipeline: String): Unit = {
    val client = S3.getClient
    val key = s"${executionPoint.executionPointId}_${position}_$pipeline.jpg"

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

    // Save streetview image metadata to DB
    val imageLocation = StreetviewAPIService.getImageLocation(imagePoint.getLatitude, imagePoint.getLongitude)
    val si = StreetviewImage(executionPoint.executionPointId, position, imageLocation.getLatitude, imageLocation.getLongitude)
    si.add()
  }
}
