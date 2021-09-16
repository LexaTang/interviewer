package interviewer.room

import interviewer.common.LoggerVerticle
import io.vertx.core.impl.logging.Logger
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.kotlin.coroutines.CoroutineVerticle

class RoomVerticle: CoroutineVerticle(), LoggerVerticle {
  override val logger: Logger by lazy { LoggerFactory.getLogger(this.javaClass.name) }

}
