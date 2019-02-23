package com.neo.sk.brick.http

/**
  * User: shuai
  * Date: 2019/2/18
  * Time: 12:03
  */

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import com.neo.sk.breaker.models.dao.LoginDAO
import com.neo.sk.breaker.shared.ptcl.SuccessRsp
import com.neo.sk.breaker.shared.ptcl.protocol.LoginProtocol._
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}

trait RegisterService extends ServiceUtils with BaseService {

  import io.circe._
  import io.circe.generic.auto._

  private val log = LoggerFactory.getLogger(getClass)

    private val userRegister : Route =(path("userRegister")& post){

      entity(as[Either[Error,addNewUserReq]]){
        case Right(req) =>
          val p = req.password
          val q = req.passwordAgain
          val user = req.user
          if(p == q){
            dealFutureResult(
              LoginDAO.userLogin(user).map{
                l =>
                  if(l.isEmpty){

                    dealFutureResult(
                      LoginDAO.addUser(user,p).map{
                        ls =>
                          complete(SuccessRsp(0,"ok"))
                      }
                    )
                  }
                  else{
                    complete(SuccessRsp(100100,"user exits"))
                  }
              }
            )
          }else{
            complete(SuccessRsp(100101,"register failed"))
          }


        case Left(e) =>
          complete(s"data parse error: $e")
      }
    }

  val registerRoutes: Route = pathPrefix("register") {
    userRegister
  }

}
