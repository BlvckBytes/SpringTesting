package me.blvckbytes.springtesting

import me.blvckbytes.springhttptesting.HttpClient
import org.jetbrains.exposed.spring.DatabaseInitializer
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@ContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
open class TestBase {

  @Autowired
  private lateinit var databaseInitializer: DatabaseInitializer

  @LocalServerPort
  private var serverPort = 0

  companion object {
    private const val BASE_URI = "http://localhost"

    @Container
    @ServiceConnection
    private val mySQLContainer: MySQLContainer<*> = MySQLContainer("mysql:8.0.30")
      .withDatabaseName("tagnet")
      .withUsername("test")
      .withPassword("test")
  }

  @BeforeEach
  fun configureRestAssured() {
    HttpClient.baseUrl = BASE_URI
    HttpClient.port = serverPort

    resetDatabase()
  }

  private fun resetDatabase() {
    // TODO: Calling the database initializer should be replaced by making use of flyway
    transaction {
      executeRawSqlUpdate("DROP DATABASE ${mySQLContainer.databaseName}")
      executeRawSqlUpdate("CREATE DATABASE ${mySQLContainer.databaseName}")
      executeRawSqlUpdate("USE ${mySQLContainer.databaseName}")
      databaseInitializer.run(null)
    }
  }

  private fun executeRawSqlUpdate(sql: String) {
    // These raw statements are not going to get logged otherwise, it seems, but they
    // aid the understanding of how the database is modified in-between test-cases
    LoggerFactory.getLogger("Exposed")!!.debug(sql)

    TransactionManager
      .current()
      .connection
      .prepareStatement(sql, false)
      .executeUpdate()
  }
}