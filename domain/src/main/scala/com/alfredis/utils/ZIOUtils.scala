package com.alfredis.utils

import com.alfredis.error.DomainError
import zio.{IO, Runtime, Task, Unsafe, ZIO}

import scala.concurrent.Future

object ZIOUtils {
  implicit class AsZIO[T](val future: Future[T]) extends AnyVal {
    def asZIO(): Task[T] =
      ZIO.fromFuture(implicit executionContext => future)

    def asZIO(errorMapper: Throwable => DomainError): IO[DomainError, T] =
      ZIO
        .fromFuture(implicit executionContext => future)
        .mapError(errorMapper)
  }

  implicit class UnsafelyRunnable(val effect: ZIO[Any, Any, Any]) extends AnyVal {
    def unsafelyRunUnit(implicit runtime: Runtime[Any]): Unit = {
      val _ = Unsafe.unsafe(implicit unsafe => runtime.unsafe.run(effect).getOrThrowFiberFailure())
    }
  }
}
