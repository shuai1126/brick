package com.neo.sk.brick

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.neo.sk.brick.core.{EsheepSyncClient, RoomManager, UserManager}
import com.neo.sk.brick.http.HttpService
import akka.actor.typed.ActorRef

import scala.language.postfixOps
import akka.actor.typed.scaladsl.adapter._
import com.neo.sk.utils.ESSFSupport

import scala.language.postfixOps

/**
  * User: Taoz
  * Date: 8/26/2016
  * Time: 10:25 PM
  */
object Boot extends HttpService {

  import concurrent.duration._
  import com.neo.sk.paradise.common.AppSettings._


  override implicit val system = ActorSystem("paradise", config)
  // the executor should not be the default dispatcher.
  override implicit val executor = system.dispatchers.lookup("akka.actor.my-blocking-dispatcher")
  override implicit val materializer = ActorMaterializer()
  override implicit val scheduler = system.scheduler

  override val timeout = Timeout(20 seconds) // for actor asks

  val log: LoggingAdapter = Logging(system, getClass)

  val userManager: ActorRef[UserManager.Command] = system.spawn(UserManager.create(),"userManager")
  val roomManager: ActorRef[RoomManager.Command] = system.spawn(RoomManager.create(),"roomManager")

  val esheepSyncClient:ActorRef[EsheepSyncClient.Command] = system.spawn(EsheepSyncClient.create,"esheepSyncClient")

  def main(args: Array[String]) {
    log.info("Starting.")
    Http().bindAndHandle(routes, httpInterface, httpPort)
//    ESSFSupport.main(new Array[String](1))
    log.info(s"Listen to the $httpInterface:$httpPort")
    log.info("Done.")
  }






}
