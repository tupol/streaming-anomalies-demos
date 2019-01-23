package org.tupol.demo.streaming.anomalies

package object demos {

  def printLine = println("+-------+------------+------------+------------+------------+")
  def printHeader = {
    printLine
    println("| Anom? | Prob norm  |   Value    |    Mean    |   St Dev   |")
    printLine
  }

}
