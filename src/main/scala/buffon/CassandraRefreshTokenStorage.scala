package buffon

import com.softwaremill.session.{RefreshTokenData, RefreshTokenLookupResult, RefreshTokenStorage}
import com.typesafe.scalalogging.Logger

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContextExecutor, Future}


class CassandraRefreshTokenStorage(
  logger: Logger
)(
  implicit ec: ExecutionContextExecutor, cassandraClient: CassandraClient
) extends RefreshTokenStorage[String] {

  override def lookup(selector: String): Future[Option[RefreshTokenLookupResult[String]]] = {
    cassandraClient.selectRefreshToken(selector).map {
      case Some(rt) =>
        logger.info(s"Looking up token for selector: $selector, found: $rt")
        Some(RefreshTokenLookupResult(rt.hash, rt.expiration, () => rt.sessionData))
      case None => None
    }
  }

  override def store(data: RefreshTokenData[String]): Future[Unit] = {
    cassandraClient.insertRefreshToken(data.selector, data.tokenHash, data.expires, data.forSession).map { boolean =>
      if (boolean) {
        logger.info(s"Storing token for selector: ${data.selector}, user: ${data.forSession}, " +
          s"expires: ${data.expires}, now: ${System.currentTimeMillis()}")
      } else {
        logger.error(s"Failed to store token for selector: ${data.selector}, user: ${data.forSession}, " +
          s"expires: ${data.expires}, now: ${System.currentTimeMillis()}")
      }
    }
  }

  override def remove(selector: String): Future[Unit] = {
    cassandraClient.deleteRefreshToken(selector).map { boolean =>
      if (boolean) {
        logger.info(s"Removing token for selector: $selector")
      } else {
        logger.error(s"Failed to remove token for selector: $selector")
      }
    }
  }

  override def schedule[S](after: Duration)(op: => Future[S]): Unit = {
    logger.info("Running scheduled operation immediately")
    op
    Future.successful(())
  }
}
