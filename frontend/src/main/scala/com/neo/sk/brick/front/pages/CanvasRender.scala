package com.neo.sk.brick.front.pages

import com.neo.sk.brick.front.common.Index
import com.neo.sk.brick.front.breaker.GameHolder
import com.neo.sk.brick.front.utils.Shortcut
import com.neo.sk.brick.shared.ptcl.model.Point
import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.ext.Color
import org.scalajs.dom.html.Canvas
import mhtml._
import scala.xml.Elem


class CanvasRender(name: String) extends Index{

  private val canvas = <canvas id ="GameView" tabindex="1"></canvas>

  def init(): Unit = {
    val gameHolder = new GameHolder("GameView")
    gameHolder.start(name)

  }



  def app: xml.Node ={
    Shortcut.scheduleOnce(() =>init(),0)
    <div>
      {canvas}
    </div>
  }



}
