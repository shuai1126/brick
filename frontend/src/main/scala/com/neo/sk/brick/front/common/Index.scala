package com.neo.sk.brick.front.common

import scala.xml.Node

/**
  * User: shuai
  * Date: 2019/2/16
  * Time: 15:27
  */
trait Index {
  def app: Node
  def cancel: Unit = ()
  val pageName = this.getClass.getSimpleName
  val url = "#" + pageName
}
