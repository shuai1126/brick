package com.neo.sk.brick.core

/**
  * User: easego
  * Date: 2018/10/31
  * Time: 15:53
  */
package com.neo.sk.breaker.core

import org.slf4j.LoggerFactory
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.stream.scaladsl.Flow
import com.neo.sk.breaker.Boot.{executor, userManager}
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString
import akka.{Done, NotUsed, actor}
import akka.actor.ActorSystem
import akka.util.ByteString
import com.neo.sk.breaker.shared.ptcl.protocol.BreakerEvent._

import scala.concurrent.duration.FiniteDuration


object UserManager {
  private val log = LoggerFactory.getLogger(this.getClass)

  trait Command

  private case class TimeOut(msg: String) extends Command

  private final case class SwitchBehavior(
    name: String,
    behavior: Behavior[Command],
    durationOpt: Option[FiniteDuration] = None,
    timeOut: TimeOut = TimeOut("busy time error")
  ) extends Command


  final case class GameJoin(
    name: String,
    replyTo: ActorRef[Flow[Message,Message,Any]]
  ) extends Command

  private final case class ChildDead(
    name: String,
    actor: ActorRef[UserActor.Command]
  ) extends Command

  private final case object BehaviorChangeKey

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
          switchBehavior(ctx, "idle", idle())

        case x =>
          stashBuffer.stash(x)
          Behavior.same

      }
    }

  def init(): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
      log.info(s"userManager is starting...")
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        switchBehavior(ctx, "idle", idle())
      }
    }

  private def idle()(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] ={
    Behaviors.receive[Command]{(ctx, msg) =>
      msg match {
        case GameJoin(name, replyTo) =>
          val userActor = getUserActor(ctx, name)
          replyTo ! socketOk(userActor)
          Behaviors.same

        case ChildDead(name, actor) =>
          println(s"$name is dead")
          ctx.unwatch(actor)
          Behaviors.same

        case _ =>
          Behaviors.same
      }
    }
  }

  def socketOk(selfActor: ActorRef[UserActor.Command]): Flow[Message, Message, NotUsed] ={
    import scala.language.implicitConversions
    import org.seekloud.byteobject.ByteObject._
    import org.seekloud.byteobject.MiddleBufferInJvm


    Flow[Message].map{
      case BinaryMessage.Strict(bm) =>
        val buffer = new MiddleBufferInJvm(bm.asByteBuffer)
        bytesDecode[GameFrontEvent](buffer) match {
          case Right(req) =>
            UserActor.WsMessage(req)
          case Left(e) =>
            log.debug(s"BinaryMessage error: $e")
            UserActor.WsMessage(EmptyFrontEvent)
        }
      case TextMessage.Strict(tm) =>
        println(s"$tm , ws get TextMessage")
        tm match{
          case "a" =>
            UserActor.WsMessage(ShieldMove(1))
          case "d" =>
            UserActor.WsMessage(ShieldMove(2))
          case "w" =>
            UserActor.WsMessage(ShieldMove(3))
          case "s" =>
            UserActor.WsMessage(ShieldMove(4))
          case "link" =>
            UserActor.WsMessage(RoomLink)
          case _ =>
            UserActor.WsMessage(EmptyFrontEvent)
        }
    }.via(UserActor.flow(selfActor))
      .map{
        case Wrap(ws) =>
          BinaryMessage.Strict(ByteString(ws))

        case unknown =>
          TextMessage(s"$unknown")
      }

  }


  private def getUserActor(ctx: ActorContext[Command], name: String): ActorRef[UserActor.Command] = {
    val childName = s"UserActor-$name"
    ctx.child(childName).getOrElse {
      val actor = ctx.spawn(UserActor.init(name), childName)
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.upcast[UserActor.Command]
  }

}



