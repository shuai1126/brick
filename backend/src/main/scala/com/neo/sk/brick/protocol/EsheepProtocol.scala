package com.neo.sk.brick.protocol

import com.neo.sk.paradise.core.RoomManager
import com.neo.sk.paradise.ptcl.PDGameEvent.UserJoinLeft

object EsheepProtocol {

  final case class RoomUserInfo(
    name: String,
    actor: ActorRef[UserActor.Command]
  )


}
