package com.mle.cmd

import java.nio.file.{Files, Path}

import com.mle.concurrent.ExecutionContexts.cached
import com.mle.file.FileUtilities
import com.mle.util.Log
import rx.lang.scala.subjects.ReplaySubject

import scala.concurrent.Future
import scala.sys.process.{Process, ProcessBuilder, ProcessLogger}

/**
 * @author Michael
 */
class Shell(cwd: Path = FileUtilities.userDir) extends Log {
  Files.createDirectories(cwd)
  log info s"Working dir: $cwd"
  val workingDir = cwd.toFile

  def run(cmd: String, f: ProcessBuilder => ProcessBuilder = p => p): CommandResponse = {
    Shell.run(f(Process(cmd split ' ', workingDir)))
  }

  def sequence(commands: Seq[String], logCommand: Boolean = true) =
    commands.foldLeft(CommandResponse.empty)((resp, cmd) => resp chain (_ => {
      if (logCommand) {
        log info s"Running: $cmd"
      }
      run(cmd)
    }))

  def redirect(file: Path)(cmd: String) = {
    run(cmd, p => p #> file.toFile)
  }
}

object Shell extends Log {
  def run(pb: ProcessBuilder): CommandResponse = {
    val stdOut = ReplaySubject[String]()
    val stdErr = ReplaySubject[String]()
    val logger = ProcessLogger(std => stdOut onNext std, err => stdErr onNext err)
    val p = pb.run(logger)
    val exitFuture = Future(p.exitValue())
    exitFuture.foreach(i => {
      stdOut.onCompleted()
      stdErr.onCompleted()
    })
    CommandResponse(exitFuture, stdOut, stdErr)
  }
}
