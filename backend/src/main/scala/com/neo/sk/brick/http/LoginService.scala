package com.neo.sk.brick.http

/**
  * User: shuai
  * Date: 2019/2/18
  * Time: 15:29
  */

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import com.neo.sk.breaker.models.dao.LoginDAO
import com.neo.sk.breaker.shared.ptcl.protocol.LoginProtocol._
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}


trait LoginService extends ServiceUtils with BaseService {
  import io.circe._
  import io.circe.generic.auto._

  private val log = LoggerFactory.getLogger(getClass)


  private val userLogin: Route = (path("userLogin") & post){

    entity(as[Either[Error, LoginReq]]) {
      case Right(req) =>
        dealFutureResult(
          LoginDAO.userLogin(req.name).map{
            user =>
              if(user.isEmpty){
                complete(LoginUserRsp("error",100102,"userName not exist"))
              }else{
                if(user.get.password==req.password){
                  log.info(s"user ${req.name} login success")
                  complete(LoginUserRsp(user.get.name))
                }else{
                  complete(LoginUserRsp("error",100103,"password is wrong"))
                }
              }
            }
        )

      case Left(e) =>
        complete(s"data parse error: $e")
    }
  }

  val loginRoutes: Route = pathPrefix("login") {
    userLogin
  }

}
