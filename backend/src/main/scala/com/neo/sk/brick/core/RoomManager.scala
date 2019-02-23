package com.neo.sk.brick.core

/**
  * User: easego
  * Date: 2018/11/1
  * Time: 16:05
  */

import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.brick.core.RoomActor.{JoinRoom, JoinRoom4Watch}
import com.neo.sk.brick.core.UserActor._
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.util.{Left, Random, Right}
import com.neo.sk.brick.ptcl.brick.Protocol._

import scala.concurrent.Future
import com.neo.sk.brick.Boot.{esheepSyncClient, executor, scheduler, timeout}
import com.neo.sk.brick.protocol.EsheepProtocol._
import com.neo.sk.brick.ptcl.brick.{Protocol, WsSourceProtocol}

import scala.collection.mutable

object RoomManager {
  private val log = LoggerFactory.getLogger(this.getClass)

  trait Command

  private case class TimeOut(msg: String) extends Command

  private final case class SwitchBehavior(
    name: String,
    behavior: Behavior[Command],
    durationOpt: Option[FiniteDuration] = None,
    timeOut: TimeOut = TimeOut("busy time error")
  ) extends Command

  private final case object BehaviorChangeKey

  final case class UserJoinRoom(
    name: String,
    userActor: ActorRef[UserActor.Command]
  ) extends Command

  final case class UserDisConnect(
    name: String,
    userActor: ActorRef[UserActor.Command]
  ) extends Command

  private final case class ChildDead(
    id: Int,
    actor: ActorRef[RoomActor.Command]
  ) extends Command

  private[this] def switchBehavior(
    ctx: ActorContext[Command],
    behaviorName: String,
    behavior: Behavior[Command],
    durationOpt: Option[FiniteDuration] = None,
    timeOut: TimeOut = TimeOut("busy time error"))
    (implicit stashBuffer: StashBuffer[Command],
      timer: TimerScheduler[Command]): Behavior[Command] = {
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey, timeOut, _))
    stashBuffer.unstashAll(ctx, behavior)
  }

  private def busy()(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case SwitchBehavior(name, b, durationOpt, timeOut) =>
          switchBehavior(ctx, name, b, durationOpt, timeOut)

        case TimeOut(m) =>
          log.debug(s"${ctx.self.path} is time out when busy, msg=$m")
          switchBehavior(ctx, "init", init())

        case x =>
          stashBuffer.stash(x)
          Behavior.same

      }
    }

  def init(): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
      log.info(s"roomManager is starting...")
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        switchBehavior(ctx, "idle", idle(mutable.HashMap.empty[String, ActorRef[UserActor.Command]], mutable.HashMap.empty[Int, ActorRef[RoomActor.Command]]))
      }
    }

  private def idle(
    userMap: mutable.HashMap[String, ActorRef[UserActor.Command]], //name -> actor
    roomMap: mutable.HashMap[Int, ActorRef[RoomActor.Command]] //id -> actor
  )(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] ={
    Behaviors.receive[Command]{(ctx, msg) =>
      msg match {
        case UserJoinRoom(name, replyToUserActor) =>
          if(userMap.contains(name)){
            Behaviors.same
          }
          else if(userMap.nonEmpty){
            val roomId = 1 //FIXME
            val roomActor = getRoomActor(ctx, roomId)
            val userActor = replyToUserActor
            val otherUser = userMap.head
            roomActor ! RoomActor.CreateRoom(RoomUserInfo(name, userActor), RoomUserInfo(otherUser._1, otherUser._2))
            userMap.remove(otherUser._1)
            roomMap.put(roomId, roomActor)
            Behaviors.same
          }
          else
            userMap.put(name, replyToUserActor)
          Behaviors.same

        case UserDisConnect(name, replyTo) =>
          userMap.remove(name)
          Behaviors.same

        case _ =>
          Behaviors.same
      }
    }
  }

  private def getRoomActor(ctx: ActorContext[Command], id: Int): ActorRef[RoomActor.Command] = {
    val childName = s"RoomActorActor-$id"
    ctx.child(childName).getOrElse {
      val actor = ctx.spawn(RoomActor.init(), childName)
      ctx.watchWith(actor, ChildDead(id, actor))
      actor
    }.upcast[RoomActor.Command]
  }

}