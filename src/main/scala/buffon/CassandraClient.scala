package buffon

import com.datastax.driver.core._
import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import org.mindrot.jbcrypt.BCrypt

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future, Promise}
import scala.language.implicitConversions


object CassandraUtils {
  // Convert Java ListenableFuture to Scala Future
  // Source: https://www.beyondthelines.net/databases/querying-cassandra-from-scala/
  implicit def listenableFutureToFuture[T](listenableFuture: ListenableFuture[T]): Future[T] = {
    val promise = Promise[T]()
    Futures.addCallback(listenableFuture, new FutureCallback[T] {
      def onFailure(error: Throwable): Unit = {
        promise.failure(error)
        ()
      }

      def onSuccess(result: T): Unit = {
        promise.success(result)
        ()
      }
    })
    promise.future
  }

  implicit class CqlStrings(val context: StringContext) extends AnyVal {
    def cql(args: Any*)(implicit session: Session): Future[PreparedStatement] = {
      val statement = new SimpleStatement(context.raw(args: _*))
      session.prepareAsync(statement)
    }
  }

  def execute(statement: Future[BoundStatement], params: Any*)(
    implicit executor: ExecutionContext, session: Session
  ): Future[ResultSet] = statement
    .flatMap(session.executeAsync(_))
//    .map(_.bind(params.map(_.asInstanceOf[Object]))) // Cant figure out how to map array buffer to cql data type varchar (needs a custom codec)

  def parseOne(resultSet: ResultSet): Option[Row] = Option(resultSet.one())
}

class CassandraClient(config: Config)(implicit ec: ExecutionContextExecutor) extends LazyLogging {
  import CassandraUtils._

  private val keyspace = config.getString("cassandra.keyspace")
  private val cluster = Cluster.builder
    .addContactPoint(config.getString("cassandra.hostname"))
    .withPort(config.getInt("cassandra.port"))
    .build()
  private implicit val session = cluster.connect()

  def seed(): Unit = {
    logger.info(s"CREATE KEYSPACE IF NOT EXISTS $keyspace")
    session.execute(s"CREATE KEYSPACE IF NOT EXISTS $keyspace WITH replication = {'class': 'NetworkTopologyStrategy', 'datacenter1': 1} AND durable_writes = true;")
    logger.info(s"CREATE TABLE IF NOT EXISTS $keyspace.users")
    session.execute(s"CREATE TABLE IF NOT EXISTS $keyspace.users (email varchar PRIMARY KEY, password_hash varchar);")
    logger.info(s"CREATE TABLE IF NOT EXISTS $keyspace.refresh_tokens")
    session.execute(s"CREATE TABLE IF NOT EXISTS $keyspace.refresh_tokens (selector varchar PRIMARY KEY, hash varchar, expiration time, sessionData varchar);")
  }

  def close(): Unit = {
    session.close()
    cluster.close()
  }

  // WARNING: DANGER
  def flush(yes: Boolean = false): Unit = {
    val tables = Seq("users", "refresh_tokens")
    tables.foreach { table =>
      logger.info(s"TRUNCATE TABLE $keyspace.$table ($yes)")
      if (yes) {
        session.execute(s"TRUNCATE TABLE $keyspace.$table")
      }
    }
  }

  // WARNING: DANGER
  def cleanUp(yes: Boolean = false): Unit = {
    logger.info(s"DROP KEYSPACE IF EXISTS $keyspace ($yes)")
    if (yes) {
      session.execute(s"DROP KEYSPACE IF EXISTS $keyspace")
    }
  }

  // ExecuteAsync throws an Exception when it should report a failed future...
  // https://www.datastax.com/dev/blog/cassandra-error-handling-done-right
  // https://datastax-oss.atlassian.net/browse/JAVA-1020
  def insertUser(email: String, password: String): Future[Boolean] = {
    val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(10))
    for {
      rs <- session.executeAsync(s"INSERT INTO $keyspace.users(email, password_hash) VALUES ('$email', '$passwordHash') IF NOT EXISTS")
    } yield {
      rs.wasApplied()
    }
  }

  def selectUser(query: String, column: String): Future[Option[User]] = for {
    rs <- execute(cql"SELECT * FROM $keyspace.users WHERE $column = ? LIMIT 1".map(_.bind(query)))
    row = parseOne(rs)
  } yield {
    row.map(buildUser)
  }

  def deleteUser(query: String, column: String): Future[Boolean] = for {
    rs <- execute(cql"DELETE FROM $keyspace.users WHERE $column = ? IF EXISTS"
      .map(_.bind(query)))
  } yield {
    rs.wasApplied()
  }

  def insertRefreshToken(selector: String, hash: String, expiration: Long, sessionData: String): Future[Boolean] = for {
    rs <- session.executeAsync(s"INSERT INTO $keyspace.refresh_tokens(selector, hash, expiration, sessionData) VALUES " +
      s"('$selector', '$hash', '$expiration', '$sessionData') IF NOT EXISTS")
  } yield {
    rs.wasApplied()
  }

  def selectRefreshToken(selector: String): Future[Option[RefreshToken]] = for {
    rs <- session.executeAsync(s"SELECT * FROM $keyspace.refresh_tokens WHERE selector = '$selector' LIMIT 1")
    row = parseOne(rs)
  } yield {
    row.map(buildRefreshToken)
  }

  def deleteRefreshToken(selector: String): Future[Boolean] = for {
    rs <- session.executeAsync(s"DELETE FROM $keyspace.refresh_tokens WHERE selector = '$selector' IF EXISTS")
  } yield {
    rs.wasApplied()
  }

//  def selectUser(email: String, password: String): Future[Option[User]] = {
//    val passwordHashF = selectUserHash(email)
//    val query = cql"SELECT email, password_hash FROM $keyspace.users WHERE email = ? AND password_hash = ? LIMIT 1"
//    passwordHashF.flatMap({
//      case Some(passwordHash) => for {
//          resultSet <- execute(query.map(_.bind(email, passwordHash)))
//          row = parseOne(resultSet)
//        } yield {
//          row.map(buildUser)
//        }
//      case None => Future(None)
//    })
//  }

  private def buildUser(r: Row): User = {
    val email = r.getString("email")
    val passwordHash = r.getString("password_hash")
    User(email, passwordHash)
  }

  private def buildRefreshToken(r: Row): RefreshToken = {
    val selector = r.getString("selector")
    val hash = r.getString("hash")
    val expiration = r.getLong("expiration")
    val sessionData = r.getString("sessionData")
    RefreshToken(selector, hash, expiration, sessionData)
  }
}
