package com.mle.cmd

import com.mle.cmd.CommandEvents.{CommandEvent, ErrorOut, ExitValue, StandardOut}
import com.mle.concurrent.ExecutionContexts.cached
import rx.lang.scala.subjects.ReplaySubject
import rx.lang.scala.{Observable, Subject}

import scala.concurrent.duration.{Duration, DurationLong}
import scala.concurrent.{Await, Future, Promise}

/**
 * @author Michael
 */
case class CommandResponse(exitValue: Future[Int], standardOut: Observable[String], errorOut: Observable[String]) {
  def output = standardOut merge errorOut

  def await(timeout: Duration = 10.seconds): CommandResult = Await.result(result, timeout)

  def result: Future[CommandResult] = exitValue.map(exit => {
    withExit(exit)
  })

  def chain(f: CommandResult => CommandResponse): CommandResponse = {
    val newExit = Promise[Future[Int]]()
    // You can interpret Subject[Observable[_]] as a Promise of Observables.
    val out = ReplaySubject[Observable[String]]()
    out onNext standardOut
    val err = ReplaySubject[Observable[String]]()
    err onNext errorOut
    exitValue.map(withExit).foreach(result => {
      if (result.isSuccess) {
        val newResponse = f(result)
        newExit trySuccess newResponse.exitValue
        out onNext newResponse.standardOut
        err onNext newResponse.errorOut
      } else {
        newExit trySuccess exitValue
      }
      out.onCompleted()
      err.onCompleted()
    })
    // flattens the future and observables
    CommandResponse(newExit.future flatMap identity, out.concat, err.concat)
  }

  def and(f: => CommandResponse): CommandResponse = chain(_ => f)

  private def withExit(exit: Int): CommandResult = {
    // If there's an exit value, both stdout and stderror have completed... right? Then it's safe to call toBlocking.
    val standard = standardOut.toBlocking.toList
    val error = errorOut.toBlocking.toList
    val out = output.toBlocking.toList
    CommandResult(exit, standard, error, out)
  }

  def materialize: Observable[CommandEvent] = {
    val exit = Observable.from(exitValue) map ExitValue
    val s = standardOut map StandardOut
    val e = errorOut map ErrorOut
    exit merge s merge e
  }
}

object CommandResponse {
  val empty = CommandResponse(Future.successful(0), Observable.empty, Observable.empty)

  def concatOnComplete[T](obs: Observable[T], other: => Observable[T]): Observable[T] = {
    val s = Subject[Observable[T]]()
    s onNext obs
    obs.doOnCompleted {
      s onNext other
    }
    obs.doOnError(t => {
      s.onCompleted()
    })
    s.concat
  }
}