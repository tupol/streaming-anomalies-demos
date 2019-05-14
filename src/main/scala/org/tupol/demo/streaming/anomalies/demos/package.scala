package org.tupol.demo.streaming.anomalies

import java.sql.Timestamp
import java.util.Date

package object demos {

  def timestamp: Timestamp = new Timestamp(new Date().getTime)

  def printLine = println("+-------+------------+------------+------------+------------+")
  def printHeader = {
    printLine
    println("| Anom? | Prob norm  |   Value    |    Mean    |   St Dev   |")
    printLine
  }

}
