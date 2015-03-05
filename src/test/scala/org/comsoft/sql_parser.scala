package org.comsoft

/**
 * Created by yakupov on 04.03.2015.
 */
object sql_parser extends App {
  val source = scala.io.Source.fromFile("D:\\work\\firebirdToPG\\src\\test\\resources\\aisbd-initial.sql")
  val lines = source.mkString
  source.close()
  val blocks = lines.split(";")
  val (used, _) = blocks.span(p => !p.contains("COMMIT WORK"))

  def traverse(list: List[String], l1: List[String], l2: List[String], l3: List[String]): (List[String], List[String], List[String]) = {
    list match {
      case Nil => (l1.map(_ + ";"), l2.map(_ + ";"), l3.map(_ + ";"))
      case y :: ys =>
        y match {
          case x if x.matches("(?i)(?s).*CREATE *GENERATOR.*") => traverse(ys, x :: l1, l2, l3)
          case x if x.matches("(?i)(?s).*CREATE *TABLE.*") => traverse(ys, l1, x :: l2, l3)
          case x if x.matches("(?i)(?s).*CREATE *INDEX.*") => traverse(ys, l1, l2, x :: l3)
          case x if x.matches("(?i)(?s).*ALTER *TABLE.*") => traverse(ys, l1, l2, x :: l3)
          case _ =>  traverse(ys, l1, l2, l3)
        }

    }
  }
  val result = traverse(used.toList, Nil, Nil, Nil)
  println(result._1.zipWithIndex.mkString("\n"))
  println(result._2.zipWithIndex.mkString("\n"))
  println(result._3.zipWithIndex.mkString("\n"))
}
