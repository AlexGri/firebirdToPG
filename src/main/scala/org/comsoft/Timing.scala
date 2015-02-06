package org.comsoft

import akka.actor.Actor

/**
 * Created by alexgri on 06.02.15.
 */
trait Timing {
  self: Actor =>
  def time[A, B <: TimingMsg](a: => A)(msgFactory: Long => B):A = {
    val now = System.nanoTime
    val result = a
    val micros = (System.nanoTime - now) / 1000
    sender() ! msgFactory(micros)
    result
  }
}

sealed trait TimingMsg {
  def time:Long
}

case class FBTime(time:Long) extends TimingMsg
case class PGTime(time:Long) extends TimingMsg

trait FBTiming extends Timing {
  self: Actor =>
  def fbTiming[A](a: => A):A = time(a)(FBTime)
}

trait PGTiming extends Timing {
  self: Actor =>
  def pgTiming[A](a: => A):A = time(a)(PGTime)
}
