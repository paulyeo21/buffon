package buffon

import akka.http.scaladsl.model.headers.RawHeader
import com.softwaremill.session.{SessionConfig, SessionManager}
import com.typesafe.config.ConfigFactory
import akka.http.scaladsl.testkit.ScalatestRouteTest


trait SessionTestUtils { this: ScalatestRouteTest =>
  val config = ConfigFactory.load()
  val sessionConfig = SessionConfig.fromConfig()
  implicit val manager = new SessionManager[String](sessionConfig)

  object SessionTestUtils {
    def getSessionToken = header(sessionConfig.sessionHeaderConfig.sendToClientHeaderName).map(_.value)
    def getRefreshToken = header(sessionConfig.refreshTokenHeaderConfig.sendToClientHeaderName).map(_.value)
    def sessionIsExpired = getSessionToken.contains("")
    def refreshTokenIsExpired = getRefreshToken.contains("")
    def setSessionHeader(s: String) = RawHeader(sessionConfig.sessionHeaderConfig.getFromClientHeaderName, s)
    def setRefreshTokenHeader(s: String) = RawHeader(sessionConfig.refreshTokenHeaderConfig.getFromClientHeaderName, s)
  }
}
