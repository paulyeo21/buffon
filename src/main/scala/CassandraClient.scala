import com.datastax.driver.core.Cluster
import com.typesafe.config.Config


class CassandraClient(config: Config) {
  private val cluster = Cluster.builder
    .addContactPoint(config.getString("cassandra.hostname"))
    .withPort(config.getInt("cassandra.port"))
    .build()
  private val session = cluster.connect()

  def apply(statement: String) = {
    session.execute(statement)
  }

//  https://github.com/magro/play2-scala-cassandra-sample/blob/master/app/models/Cassandra.scala
  def seed(): Unit = {
    val keyspace = config.getString("cassandra.keyspace")
    session.execute(s"CREATE KEYSPACE IF NOT EXISTS $keyspace WITH replication = {'class': 'NetworkTopologyStrategy', 'DC1': 3} AND durable_writes = true;")
    session.execute(s"CREATE TABLE IF NOT EXISTS $keyspace.users (" +
      s"id uuid PRIMARY KEY," +
      s"email text," +
      s"password_salt text," +
      s"password_hash text);")
  }

  def close(): Unit = {
    cluster.close()
    session.close()
  }
}
