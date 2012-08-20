package com.davezhu.blah.web

import collection.mutable.ArrayBuffer
import org.joda.time.{Duration, DateTime}
import org.slf4j.LoggerFactory
import com.davezhu.blah.core.Logging

case class Sequenced[+T](seq: Long, t: T)

class Quotes(val quotes: ArrayBuffer[(Long, Quote)] = ArrayBuffer(), val delay: Int = ONE_MINUTE,
             val dateTimeService: DateTimeService = new DateTimeService {}) {

  val LOG = LoggerFactory.getLogger(classOf[Quotes])

  for (i <- 0 until quotes.size) {
    assert(quotes(i)._1 == i.toLong)
  }

  var seq = quotes.size

  var discarded = 0L

  def +=(q: Quote) {
    quotes += ((seq, q))
    seq += 1
    Logging.debug(LOG, "Quotes size=" + quotes.size)
  }

  // from is exclusive
  def since(from: Long): Seq[Sequenced[Quote]] = {
    val now = dateTimeService.now
    quotes.slice(seqToIndex(from) + 1, quotes.size).filter {
      case (quoteSeq, quote) => isMature(now, quote)
    }.map {
      case (seq, quote) => Sequenced(seq, quote)
    }
  }

  def latest: Option[Sequenced[Quote]] = {

    val s = quotes.filter {
      case (quoteSeq, quote) => isMature(dateTimeService.now, quote)
    }

    if (s.size > 0) {
      Some(Sequenced(s.last._1, s.last._2))
    }  else {
      None
    }

  }

  def discardOld {
    val now = dateTimeService.now
    val toRemove = quotes.takeWhile {
      case (quoteSeq, quote) => (now.getMillis - quote.dts.getMillis) > FIVE_MINUTES
    }
    discarded += toRemove.size
    quotes --= toRemove
    Logging.debug(LOG, "Discarded " + toRemove.size + " old quotes")
    Logging.debug(LOG, "Quotes size=" + quotes.size)
  }

  def size = quotes.size

  def contains(quote: Quote) = quotes.exists(_._2 == quote)

  override def clone = new Quotes(quotes.clone, delay, dateTimeService)

  private def isMature(now: DateTime, quote: Quote) = now.getMillis - quote.dts.getMillis >= delay

  private def seqToIndex(seq: Long) = math.max(0, (seq - discarded).toInt - 1)

}
