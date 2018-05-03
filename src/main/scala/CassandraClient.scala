import com.datastax.driver.core._
import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import org.mindrot.jbcrypt.BCrypt

import scala.concurrent.{ExecutionContext, Future, Promise}
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

class CassandraClient(config: Config) {
  import CassandraUtils._

  private val keyspace = config.getString("cassandra.keyspace")
  private val cluster = Cluster.builder
    .addContactPoint(config.getString("cassandra.hostname"))
    .withPort(config.getInt("cassandra.port"))
    .build()
  private implicit val executionContext = ExecutionContext.global
  private implicit val session = cluster.connect()

  def seed(): Unit = {
//    logger.info(s"CREATE KEYSPACE IF NOT EXISTS $keyspace")
    session.execute(s"CREATE KEYSPACE IF NOT EXISTS $keyspace WITH replication = {'class': 'NetworkTopologyStrategy', 'datacenter1': 1} AND durable_writes = true;")
//    logger.info(s"CREATE TABLE IF NOT EXISTS $keyspace.users")
    session.execute(s"CREATE TABLE IF NOT EXISTS $keyspace.users (email text PRIMARY KEY, password_hash text);")
  }

  def close(): Unit = {
    session.close()
    cluster.close()
  }

  // WARNING: DANGER
  def flush(yes: Boolean = false): Unit = {
    val tables = Seq("users")
    tables.foreach { table =>
//      logger.info(s"TRUNCATE TABLE $keyspace.$table ($yes)")
      if (yes) {
        session.execute(s"TRUNCATE TABLE $keyspace.$table")
      }
    }
  }

  // WARNING: DANGER
  def cleanUp(yes: Boolean = false): Unit = {
//    logger.info(s"DROP KEYSPACE IF EXISTS $keyspace ($yes)")
    if (yes) {
      session.execute(s"DROP KEYSPACE IF EXISTS $keyspace")
    }
  }

  def insertUser(email: String, password: String): Future[ResultSet] = {
    val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(10))
    execute(cql"INSERT INTO $keyspace.users(email, password_hash) VALUES (?, ?) IF NOT EXISTS"
      .map(_.bind(email, passwordHash)))
  }

  def selectUser(email: String): Future[Option[User]] = {
    for {
      resultSet <- execute(cql"SELECT * FROM $keyspace.users WHERE email = ? LIMIT 1".map(_.bind(email)))
      row = parseOne(resultSet)
    } yield {
      row.map(buildUser)
    }
  }

//  def selectUserHash(email: String): Future[Option[String]] = {
//    for {
//      resultSet <- execute(cql"SELECT password_hash FROM $keyspace.users WHERE email = ? LIMIT 1".map(_.bind(email)))

//      row = parseOne(resultSet)
//    } yield {
//      row.map(_.getString("password_hash"))
//    }
//  }
//
//  def selectUser(email: String, password: String): Future[Option[User]] = {
//    val passwordHashF = selectUserHash(email)
//    val query = cql"SELECT email, password_hash FROM $keyspace.users WHERE email = ? AND password_hash = ? LIMIT 1 ALLOW FILTERING"
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
}
