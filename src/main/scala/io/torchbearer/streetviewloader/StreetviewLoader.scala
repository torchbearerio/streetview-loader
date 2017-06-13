package io.torchbearer.streetviewloader

import io.torchbearer.ServiceCore.AWSServices.SFN.getTaskForActivityArn
import io.torchbearer.ServiceCore.{Constants, TorchbearerDB}
import org.json4s.jackson.JsonMethods._
import org.json4s.DefaultFormats

import scala.concurrent.{Future, blocking}
import scala.concurrent.ExecutionContext.Implicits.global

object StreetviewLoader extends App {
  implicit val formats = DefaultFormats

  println("Welcome to streetview-loader")

  // Initialize core services
  TorchbearerDB.init()

  val loadTask = new StreetviewLoadTask(437, 56, "sdfsf")
  loadTask.run()

  while (true) {
    println("here")
    val task = getTaskForActivityArn(Constants.ActivityARNs("STREETVIEW_IMAGE_LOAD"))

    // If no tasks were returned, exit
    if (task.getTaskToken != "") {

      val input = parse(task.getInput)
      val epId = (input \ "epId").extract[Int]
      val hitId = (input \ "hitId").extract[Int]
      val taskToken = task.getTaskToken

      val loadTask = new StreetviewLoadTask(epId, hitId, taskToken)

      Future {
        blocking {
          loadTask.run()
        }
      }
    }
  }
}
