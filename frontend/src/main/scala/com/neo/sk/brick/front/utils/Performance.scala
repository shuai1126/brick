package com.neo.sk.brick.front.utils

import com.neo.sk.brick.ptcl.Protocol._
import com.neo.sk.brick.ptcl._
import org.scalajs.dom
import org.scalajs.dom.ext.{Color, KeyCode}
import org.scalajs.dom.html.{Document => _, _}
import org.scalajs.dom.raw._

import scala.math._
import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer
import scalatags.JsDom.short._

object Performance {
  //drawTime Fps
  private var lastUpdateTime = System.currentTimeMillis()
  private var currentDrawTime = 0l
  private var count = 0
  private var totalTime = 0l

  //data size
  private var dataSize = 0.0
  private var showDataSize = 0.0
  //wsTime
  private var sendTime = 0l
  private var sendFrame = 0l
  private var receiveTime = 0l

  private var fps = 0
  private var wsTime = 0l

  def getDrawTime(drawTime:Long): Long ={
    val time = System.currentTimeMillis()
    count += 1
    totalTime += drawTime
    if(time - lastUpdateTime > 1000){
      lastUpdateTime = time
      currentDrawTime = totalTime/count
      totalTime = 0
      fps = count
      count = 0
      showDataSize = dataSize
      dataSize = 0
      wsTime = receiveTime
    }
    currentDrawTime
  }

  def sendWs(time:Long,frame:Long) = {
    if(frame != sendFrame){
      sendTime = time
      sendFrame = frame
    }
  }

  def receiveWs(time:Long,frame:Long) = {
    if(frame == sendFrame){
      receiveTime = time - sendTime
    }
  }

  def setDataSize(size:Double) = {
    dataSize += size
  }

  def render(drawTime:Long,ctx:dom.CanvasRenderingContext2D,rankHeight:Int,pingPongResult: Long,isPlay:Boolean) = {
    val drawPer = getDrawTime(drawTime)
    val fpsValue = fps
    ctx.font = "16px 黑体"
    ctx.fillStyle = if(drawPer < 10) "white" else if(drawPer < 17) "#FFFF00" else "red"
    ctx.fillText(s"绘制时间: $drawPer ms ", 10, 2+rankHeight)
    ctx.fillStyle = if(fpsValue > 50) "white"  else if(fpsValue >30) "#FFFF00" else "red"
    ctx.fillText(s"fps: $fpsValue ", 10,20 + rankHeight)
    ctx.fillStyle = "white"
    ctx.fillText(s"数据包: $showDataSize byte ",10,40+ rankHeight)
    if(isPlay)
      ctx.fillText(s"收发时间: $pingPongResult ms ",10,60 + rankHeight)
  }

}