package com.mle.cmd

/**
 * @author Michael
 */
object CommandEvents {

  sealed trait CommandEvent

  case class ExitValue(i: Int) extends CommandEvent

  case class StandardOut(line: String) extends CommandEvent

  case class ErrorOut(line: String) extends CommandEvent

}
