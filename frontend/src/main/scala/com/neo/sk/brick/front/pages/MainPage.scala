package com.neo.sk.brick.front.pages

import com.neo.sk.brick.front.common.{Page, PageSwitcher}
import mhtml.{Cancelable, Rx, Var, mount}
import org.scalajs.dom

object MainPage extends PageSwitcher {

  private val currentPage = currentHashVar.map {ls =>
    ls match {
      case "Register" :: Nil => Register.app
      case "play" :: name :: Nil => new CanvasRender(name).app
      case _ => Login.app
    }
  }


  def show(): Cancelable = {
    switchPageByHash()
    val page =
      <div>
        {currentPage}
      </div>
    mount(dom.document.body, page)
  }

}
