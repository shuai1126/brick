package com.neo.sk.brick.front

import com.neo.sk.brick.front.utils.Performance
import com.neo.sk.brick.ptcl.paradise.Protocol._
import com.neo.sk.brick.ptcl._
import org.scalajs.dom
import org.scalajs.dom.ext.Color
import org.scalajs.dom.html
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw.HTMLElement

import scala.collection.mutable
import scalatags.JsDom.short._
import scala.math._
import scala.util.Random

class Draw(ctx: dom.CanvasRenderingContext2D) {

  class BreakerPlay(
    ctx: dom.CanvasRenderingContext2D
  ) extends BreakerSchemaImplClient {

    val shield = new ShieldClient(Point(0, dom.window.innerHeight.toInt-30))
    val pearl = new PearlClient(Point(0, dom.window.innerHeight.toInt-100))
    var bricks = (0 to 120).map{ cnt =>
      val cntFinal = Point(cnt % 20, (cnt - 1) / 20)
      new BrickClient(Point(75 * cntFinal.x, 35 * cntFinal.y + 20))
    }

    override def drawGameByTime(offsetTime:Long): Unit ={
      ctx.save()
      //    ctx.fillStyle="#0000ff"
      ctx.beginPath()
      ctx.clearRect(0, 0, dom.window.innerWidth, dom.window.innerHeight)
      ctx.fillRect(shield.position.x, shield.position.y, shield.width, shield.height)

      //offsetTime偏移
      val offsetPoint = pearl.speed * offsetTime / 120.0f
      ctx.arc(pearl.position.x + offsetPoint.x, pearl.position.y + offsetPoint.y, pearl.radius, 0, math.Pi * 2)
      ctx.stroke()
      ctx.fill()

      bricks.foreach(brick => ctx.fillRect(brick.position.x, brick.position.y, brick.width, brick.height))
      ctx.restore()
    }

    override def logicUpdate(): Unit ={
      pearl.position = pearl.position + pearl.speed
      if(pearl.isCollided(shield)) pearl.speed = Point(pearl.speed.x, -pearl.speed.y)
      bricks.foreach{ brick =>
        if(pearl.isCollided(brick)) {
          bricks = bricks.filter(t => t != brick)
          pearl.speed = Point(pearl.speed.x, -pearl.speed.y)
        }
      }
      if(pearl.position.x <= 0) pearl.speed = Point(-pearl.speed.x, pearl.speed.y)
      if(pearl.position.x >= dom.window.innerWidth) pearl.speed = Point(-pearl.speed.x, pearl.speed.y)
      if(pearl.position.y <= 0) pearl.speed = Point(pearl.speed.x, -pearl.speed.y)
      if(pearl.position.y >= dom.window.innerHeight) pearl.speed = Point(pearl.speed.x, -pearl.speed.y)

    }

    override def preExecuteUserEvent(event: GamePlayEvent): Unit = {}

    override def instantExecuteUserEvent(event: GamePlayEvent): Int = {
      event match {
        case ShieldMove(moveX) =>
          if(moveX > 0) shield.changePosition(1)
          else if(moveX == 30)shield.changePosition(3)
          else if(moveX == 40)shield.changePosition(4)
          else shield.changePosition(2)
          GameState.soloPlay
        case _ =>
          GameState.soloPlay
      }
    }

  }

}
