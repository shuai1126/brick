package com.neo.sk.brick.front.pages

import com.neo.sk.brick.front.common.Index
import com.neo.sk.brick.front.utils.{Http, JsFunc}
import com.neo.sk.brick.front.common.Routes
import com.neo.sk.brick.shared.ptcl.protocol.LoginProtocol.{LoginReq, LoginUserRsp}
import mhtml.{Rx, Var}
import org.scalajs.dom
import io.circe.generic.auto._
import io.circe.syntax._
import org.scalajs.dom.html.Input

import scala.xml.Elem
import scala.concurrent.ExecutionContext.Implicits.global


object Login extends Index{

  def login():Unit ={
    val account = dom.document.getElementById("username").asInstanceOf[Input].value
    val password = dom.document.getElementById("password").asInstanceOf[Input].value
    val data =  LoginReq(account, password).asJson.noSpaces
    Http.postJsonAndParse[LoginUserRsp](Routes.Login.userLogin,data).map{
      case LoginUserRsp(name, 0, "ok") =>

        dom.window.location.hash = s"/play/$name"
      case LoginUserRsp("error",100103,"password is wrong") =>
        JsFunc.alert(s"密码错误")
      case LoginUserRsp("error",100102,"userName not exist") =>
        JsFunc.alert(s"用户不存在")
    }
  }

  def app: xml.Node = {
    <div class="home-top">
      <div>
        <div class="login">欢迎登录</div>
      </div>
      <div style="display:flex">
        <div>
          <div class="login-name" >用户名：<input class="login-input"  id="username"></input></div>
          <div class="login-name" >密 码：<input style="margin-left:12px;" class="login-input" type="password"  id="password"></input></div>
          <button class="login-bu" onclick={() => login()}>登录</button>
        </div>
        <div style="padding-left: 260px;">
          <div class="login-re" onclick={() =>dom.window.location.hash = s"#/Register"}>注册</div>
          <div class="login-re">游客模式进入</div>
        </div>
      </div>

    </div>
  }
}
