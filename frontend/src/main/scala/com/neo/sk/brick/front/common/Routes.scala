package com.neo.sk.brick.front.common

/**
  * User: Taoz
  * Date: 2/24/2017
  * Time: 10:59 AM
  */
object Routes {


  val baseUrl = "/breaker"

  def wsJoinGameUrl(name:String): String = baseUrl + s"/gameJoin?name=${name}"

  object Login{

    val base: String = baseUrl + "/login"

    def userLogin: String = base + "/userLogin"

  }

  object Register{
    val base : String = baseUrl + "/register"

    def userRegister : String = base + "/userRegister"
  }










}
