package com.neo.sk.brick.front.pages

import com.neo.sk.brick.front.common.Index
import com.neo.sk.brick.front.utils.{Http, JsFunc}
import com.neo.sk.brick.front.common.Routes
import com.neo.sk.brick.shared.ptcl.protocol.LoginProtocol.addNewUserReq
import org.scalajs.dom
import io.circe.generic.auto._
import io.circe.syntax._
import org.scalajs.dom.html.Input
import com.neo.sk.brick.shared.ptcl.{SuccessRsp,ErrorRsp}
import scala.concurrent.ExecutionContext.Implicits.global


/**
  * User: shuai
  * Date: 2019/2/21
  * Time: 15:29
  */
object Register extends  Index {

  def register() : Unit ={
    val user=dom.document.getElementById("user").asInstanceOf[Input].value
    val password=dom.document.getElementById("password").asInstanceOf[Input].value
    val passwordAgain=dom.document.getElementById("password-again").asInstanceOf[Input].value
    val data =addNewUserReq(user,password,passwordAgain).asJson.noSpaces
    Http.postJsonAndParse[SuccessRsp](Routes.Register.userRegister, data).map {
      case SuccessRsp(0,"ok") =>
        JsFunc.alert("注册成功！")
        dom.window.location.hash = s"#/login"
      case SuccessRsp(100100,"user exits") =>
        JsFunc.alert("用户已存在")
      case SuccessRsp(100101,"register failed") =>
        JsFunc.alert("注册失败")
    }
  }

  def app: xml.Node={
    <div class="home-top">
      <div class="register">
        <div class="login">请注册账号</div>
        <div class="register-name">用户名：<input style="margin-left:20px" class="login-input" id ="user"></input></div>
        <div class="register-name">密码：<input style="margin-left:36px" class="login-input" type="password" id ="password"></input></div>
        <div class="register-name">确认密码：<input class="login-input" type="password" id ="password-again"></input></div>
      </div>
      <div style="display: flex">
        <div class="login-re" style="margin-left:600px" onclick={ () => register()}> 注册</div>
        <div class="login-re" style="margin-right:550px" onclick={() =>dom.window.location.hash = s"#/login"}> 登录</div>
      </div>
    </div>
  }
}
