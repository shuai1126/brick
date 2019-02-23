package com.neo.sk.brick.front

import com.neo.sk.brick.ptcl._

import scala.util.Random

/**
  * User: Taoz
  * Date: 9/3/2016
  * Time: 10:13 PM
  */
class GridOnClient(override val boundary: Point) extends Grid {

  private var wsSetup = false

  private var websocketStreamOpt : Option[WebSocket] = None

  def getWsState = wsSetup

  def getWebSocketUri(name:String): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}${Routes.wsJoinGameUrl(name)}"
  }

  private val sendBuffer:MiddleBufferInJs = new MiddleBufferInJs(2048)

  def sendByteMsg(msg: GameFrontEvent): Unit = {
    import com.neo.sk.breaker.front.utils.byteObject.ByteObject._
    websocketStreamOpt.foreach{s =>
      s.send(msg.fillMiddleBuffer(sendBuffer).result())
    }
  }

  def sendTextMsg(msg: String): Unit ={
    websocketStreamOpt.foreach{ s =>
      s.send(msg)
    }
  }


  def setup(name:String):Unit = {
    if(wsSetup){

    }else{
      val websocketStream = new WebSocket(getWebSocketUri(name))
      websocketStreamOpt = Some(websocketStream)

      websocketStream.onopen = { (event: Event) =>
        wsSetup = true
        connectSuccessCallback(event)
      }

      websocketStream.onerror = { (event: Event) =>
        wsSetup = false
        websocketStreamOpt = None
        connectErrorCallback(event)
      }

      websocketStream.onmessage = { (event: MessageEvent) =>
        messageHandler(event)
      }

      websocketStream.onclose = { (event: Event) =>
        wsSetup = false
        websocketStreamOpt = None
        closeCallback(event)
      }
    }
  }

}
