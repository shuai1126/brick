package com.neo.sk.brick.front.utils

import com.neo.sk.brick.front.utils.JsFunc.decodeURI
import org.scalajs.dom.window

/**
  * Created by zx0 on 2018/10/23.
  **/
object Func {
  def getParameter():(String,Map[String,String]) = {
    val url = window.location.href.split("/").last
    val paras = url.split("[?]")//前一个代表观战或者加入游戏，后一个是参数

    val pairs = paras(1).split("&").filter(s => s.length > 0)//参数对

    val list = pairs.map(_.split("=")).filter(_.length == 2)

    val map = list.map(l=>l(0) ->l(1)).toMap
    (paras(0),map)
  }

}
