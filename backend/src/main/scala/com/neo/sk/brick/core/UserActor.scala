package com.neo.sk.brick.core

/**
  * User: easego
  * Date: 2018/11/1
  * Time: 12:13
  */


import akka.NotUsed
import com.neo.sk.breaker.shared.ptcl.protocol.BreakerEvent._
import com.neo.sk.breaker.Boot.{executor, roomManager}
import org.seekloud.byteobject.ByteObject._
import org.seekloud.byteobject.MiddleBufferInJvm
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import com.neo.sk.breaker.shared.ptcl.protocol.BreakerEvent
import org.slf4j.LoggerFactory

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._


object UserActor {
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

  final case object TextGet extends Command

  final case class TextFailure(e: Throwable) extends Command

  final case class WsMessage(msg: GameFrontEvent) extends Command

  final case class UserJoin(frontActor: ActorRef[WsMsg]) extends Command

  final case class UserDisconnect(frontActor: ActorRef[WsMsg]) extends Command

  final case class UserJoinRoomSuccess(roomActor: ActorRef[RoomActor.Command], gameState: RoomGameState) extends Command

  final case class UserAccord(state: RoomGameState) extends Command

  def flow(selfActor: ActorRef[Command]): Flow[WsMessage, WsMsg, NotUsed] ={
    val in: Sink[WsMessage, NotUsed] = Flow[WsMessage].to(ActorSink.actorRef[Command](selfActor, TextGet, TextFailure))
    val out: Source[WsMsg, Unit] = ActorSource.actorRef[WsMsg](
      completionMatcher = {
        case WsComplete ⇒
          println("complete")
      },
      failureMatcher = {
        case WsFailure ⇒
          val a = new Throwable("fail")
          a
      },
      128,
      OverflowStrategy.dropHead).mapMaterializedValue(actor => selfActor ! UserActor.UserJoin(actor))
    Flow.fromSinkAndSource(in, out)
  }

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

  private def busy(name: String)(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case SwitchBehavior(name, b, durationOpt, timeOut) =>
          switchBehavior(ctx, name, b, durationOpt, timeOut)

        case TimeOut(m) =>
          log.debug(s"${ctx.self.path} is time out when busy, msg=$m")
          switchBehavior(ctx, "init", init(name))

        case x =>
          stashBuffer.stash(x)
          Behavior.same

      }
    }

  def init(name: String): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
      log.info(s"userActor($name) is starting...")
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      implicit val sendBuffer: MiddleBufferInJvm = new MiddleBufferInJvm(81920)
      Behaviors.withTimers[Command] { implicit timer =>
        Behaviors.receiveMessage[Command] {
          case UserJoin(frontActor) =>
            ctx.watchWith(frontActor, UserDisconnect(frontActor))
            switchBehavior(ctx, "idle", idle(name, frontActor))

          case unknownMsg =>
            println(s"init unknown msg : $unknownMsg")
            Behaviors.same
        }
      }
    }

  private def idle(
    userName: String,
    frontActor: ActorRef[WsMsg]
  )(
    implicit stashBuffer: StashBuffer[Command],
    sendBuffer: MiddleBufferInJvm,
    timer: TimerScheduler[Command]
  ): Behavior[Command] ={
    Behaviors.receive[Command]{(ctx, msg) =>
      msg match {
        case WsMessage(frontEvent) =>
          frontEvent match {
            case RoomLink =>
              roomManager ! RoomManager.UserJoinRoom(userName, ctx.self)
              Behaviors.same
            case _ =>
              Behaviors.same
          }

        case UserJoinRoomSuccess(roomActor, state) =>
          dispatchTo(frontActor, state)
          switchBehavior(ctx, "play", play(userName, frontActor, roomActor))

        case UserDisconnect(actor) => //frontActor中断
          ctx.unwatch(actor)
          Behaviors.stopped

        case TextGet => //前端连接中断
          frontActor ! WsComplete
          roomManager ! RoomManager.UserDisConnect(userName, ctx.self)
          ctx.unwatch(frontActor)
          Behaviors.stopped

        case unknownMsg =>
          println(s"idle unknown msg : $unknownMsg")
          Behaviors.same
      }
    }
  }

  private def play(
    userName: String,
    frontActor: ActorRef[WsMsg],
    roomActor: ActorRef[RoomActor.Command]
  )(
    implicit stashBuffer: StashBuffer[Command],
    sendBuffer: MiddleBufferInJvm,
    timer: TimerScheduler[Command]
  ): Behavior[Command] ={
    Behaviors.receive[Command]{(ctx, msg) =>
      msg match {
        case WsMessage(frontEvent) =>
          frontEvent match {
            case userState: UserGameState =>
              roomActor ! RoomActor.UpdateStateFromFront(userState)
              Behaviors.same
            case _ =>
              Behaviors.same
          }

        case UserDisconnect(actor) => //frontActor中断
          ctx.unwatch(actor)
          Behaviors.stopped

        case TextGet => //前端连接中断
          frontActor ! WsComplete
          roomManager ! RoomManager.UserDisConnect(userName, ctx.self)
          ctx.unwatch(frontActor)
          Behaviors.stopped

        case UserAccord(state) =>
          dispatchTo(frontActor, state)
          Behaviors.same

        case unknownMsg =>
          println(s"play unknown msg : $unknownMsg")
          Behaviors.same
      }
    }
  }

  private def dispatchTo(subscriber: ActorRef[WsMsg], msg: GameBackendEvent)(implicit sendBuffer: MiddleBufferInJvm) = {
    subscriber ! Wrap(msg.fillMiddleBuffer(sendBuffer).result())
  }

}




