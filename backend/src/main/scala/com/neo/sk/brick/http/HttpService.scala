package com.neo.sk.brick.http

import akka.actor.{ActorSystem, Scheduler}
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import akka.util.Timeout

import scala.concurrent.ExecutionContextExecutor

/**
  * User: Taoz
  * Date: 8/26/2016
  * Time: 10:27 PM
  */
trait HttpService  {

  implicit val system: ActorSystem

  implicit val executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  implicit val timeout: Timeout

  implicit val scheduler: Scheduler

  val playGame = (pathPrefix("playGame") & get){
    parameter(
      'playerId.as[String],
      'playerName,
      'accessCode.as[String],
      'roomId.as[Long].?) { (playerId,name, acCode,roomId) =>
//      authPlatUser(acCode){user=>
        getFromResource("html/netSnake.html")
//      }
    }
  }

  val watchGame = (pathPrefix("watchGame") & get){
    parameter(
      'playerId.as[String].?,
      'accessCode.as[String],
      'roomId.as[Long]) { (playerId, acCode,roomId) =>
//      authPlatUser(acCode){user=>
        getFromResource("html/netSnake.html")
//      }
    }
  }

  val watchRecord = (pathPrefix("watchRecord") & get){
    parameter(
      'recordId.as[Long],
      'playerId.as[String],
      'frame.as[Int],
      'accessCode.as[String]) { (recordId, playerId, frame, accessCode) =>
      //      authPlatUser(acCode){user=>
      getFromResource("html/netSnake.html")
      //      }
    }
  }

  val routes =
    pathPrefix("brick") {
      playGame ~ watchGame ~ watchRecord
    }


}
