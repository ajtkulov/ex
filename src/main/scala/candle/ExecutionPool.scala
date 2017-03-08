package candle

import java.util.concurrent._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}
import scala.language.implicitConversions

/**
  * Execution pool, to limit parallel/concurrent operations.
  */
object ExecutionPool {
  val defaultThreadPoolSize: Int = 10

  val threadFactory = new ThreadFactory {
    override def newThread(r: Runnable): Thread = {
      val thread = new Thread(r)
      thread.setDaemon(true)
      thread
    }
  }

  def createExecutionContext(threadPoolSize: Int = defaultThreadPoolSize): ExecutionContextExecutorService = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(threadPoolSize, threadFactory))
}
