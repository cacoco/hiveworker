import com.twitter.logging.{ConsoleHandler, Logger, LoggerFactory}
import io.angstrom.hiveworker.Config
import io.angstrom.hiveworker.configuration.HiveEnvironmentConfig

new Config {
  port = 9090
  name = "hiveworker"

  loggerFactory = LoggerFactory(
    node = "",
    level = Some(Logger.INFO),
    handlers = ConsoleHandler()
  )

  hiveEnvironmentConfig = HiveEnvironmentConfig(
    hadoopVersion= "0.20.205",
    amiVersion = "2.0.4",
    hiveVersion = "0.7.1.3",
    nodeHeapSize = "2048"
  )
}