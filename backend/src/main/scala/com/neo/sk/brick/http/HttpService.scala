package com.neo.sk.paradise.http

import akka.actor.{ActorRef, ActorSystem, Scheduler}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.Timeout

import scala.concurrent.ExecutionContextExecutor

/**
  * User: Taoz
  * Date: 8/26/2016
  * Time: 10:27 PM
  */
trait HttpService extends GameService with ResourceService with EsheepApiService {

// println("test")
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
    pathPrefix("paradise") {
      playGame ~ watchGame ~ watchRecord ~ gameRoutes ~ resourceRoutes ~ roomApiRoutes
    }


}
