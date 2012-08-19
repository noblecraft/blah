package com.davezhu.blah.web

import collection.mutable.ArrayBuffer
import org.joda.time.{Duration, DateTime}

class Quotes(val quotes: ArrayBuffer[(Long, Quote)] = ArrayBuffer(), val delay: Int = ONE_MINUTE,
             val dateTimeService: DateTimeService = new DateTimeService {}) {

  for (i <- 0 until quotes.size) {
    assert(quotes(i)._1 == i.toLong)
  }

  var seq = quotes.size

  var discarded = 0L

  def +=(q: Quote) {
    quotes += ((seq, q))
    seq += 1
  }

  // from is exclusive
  def since(from: Long): Seq[Quote] = {
    val now = dateTimeService.now
    val qs: ArrayBuffer[(Long, Quote)] = quotes.slice(seqToIndex(from) + 1, quotes.size).filter {
      case (quoteSeq, quote) => now.getMillis - quote.dts.getMillis >= delay
    }
    qs.map(_._2)
  }

  private def seqToIndex(seq: Long) = math.max(0, (seq - discarded).toInt - 1)

  def latest = quotes.filter {
    case (quoteSeq, quote) => (dateTimeService.now.getMillis - quote.dts.getMillis) >= delay
  }.last._2

  def discardOld {
    val now = dateTimeService.now
    val toRemove = quotes.takeWhile {
      case (quoteSeq, quote) => (now.getMillis - quote.dts.getMillis) > FIVE_MINUTES
    }
    discarded += toRemove.size
    quotes --= toRemove
  }

  def size = quotes.size

  def contains(quote: Book) = quotes.contains(quote)

  override def clone = new Quotes(quotes.clone, delay, dateTimeService)

}
