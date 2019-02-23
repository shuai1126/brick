package com.neo.sk.brick.core

/**
  * User: easego
  * Date: 2018/11/1
  * Time: 16:03
  */
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.concurrent.atomic.AtomicInteger
import java.awt.event.KeyEvent

import com.neo.sk.brick.core.UserActor._
import com.neo.sk.brick.Boot.roomManager
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.collection.mutable.Map
import com.neo.sk.brick.snake.GridOnServer
import com.neo.sk.brick.common.Constants._
import com.neo.sk.brick.core.GameRecorder.RoomClose
import com.neo.sk.brick.core.RoomManager.UserLeftRoom
import com.neo.sk.brick.ptcl.PDGameEvent._
import com.neo.sk.brick.ptcl._
import com.neo.sk.brick.ptcl.brick.Protocol._
import com.neo.sk.brick.ptcl.Protocol.{frameRate, rate}
import com.neo.sk.brick.models.DAO.RecordDAO._
import com.neo.sk.brick.models.DAO.RecordDAO._
import com.neo.sk.brick.Boot.esheepSyncClient
import com.neo.sk.brick.Boot.userManager

import scala.collection.mutable

object RoomActor {
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
  private final case object GameUpdateKey

  final case object GameLoop extends Command

  final case class CreateRoom(user1: RoomUserInfo, user2: RoomUserInfo) extends Command

  final case class UpdateStateFromFront(state: UserGameState) extends Command

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
      log.info(s"roomActor is starting...")
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        Behaviors.receiveMessage {
          case CreateRoom(user1, user2) =>
            val roomClient = new BreakerServerClient(user1, user2)
            user1.actor ! UserJoinRoomSuccess(ctx.self, RoomGameState(List(roomClient.getUserState(user1.name), roomClient.getUserState(user2.name))))
            user2.actor ! UserJoinRoomSuccess(ctx.self, RoomGameState(List(roomClient.getUserState(user1.name), roomClient.getUserState(user2.name))))
            timer.startSingleTimer(GameUpdateKey, GameLoop, 1.seconds)
            switchBehavior(ctx, "idle", idle(roomClient, user1, user2))

          case _ =>
            Behaviors.same
        }
      }
    }

  private def idle(
    roomClient: BreakerServerClient,
    user1: RoomUserInfo,
    user2: RoomUserInfo
  )(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] ={
    Behaviors.receive[Command]{(ctx, msg) =>
      msg match {
        case GameLoop =>
          user1.actor ! UserActor.UserAccord(RoomGameState(List(roomClient.getUserState(user1.name), roomClient.getUserState(user2.name))))
          user2.actor ! UserActor.UserAccord(RoomGameState(List(roomClient.getUserState(user1.name), roomClient.getUserState(user2.name))))
          timer.startSingleTimer(GameUpdateKey, GameLoop, 120.millis)
          Behaviors.same

        case UpdateStateFromFront(state) =>
          roomClient.changeState(state)
          Behaviors.same

        case _ =>
          Behaviors.same
      }
    }
  }

}


