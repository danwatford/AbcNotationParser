package com.foomoo.abc.app

import java.nio.file.{Path, Paths}
import java.util.Calendar

import com.foomoo.abc.notation.parsing.AbcNotationParser
import com.foomoo.abc.notation.{AbcNotationHeader, AbcNotationHeaderInformationField, AbcNoteNotation, AbcTuneNotation}
import com.foomoo.abc.service.SubsequenceMatchService
import com.foomoo.abc.service.SubsequenceMatchService.NoteSequence

import scala.io.Source
import scala.util.parsing.input.CharSequenceReader

/**
  * An example application to parse a collection of ABC tunes and search for matching note sequences between them.
  */
object TuneMatcher extends App {

  // Arguments are paths to tune files.
  println("Args: " + args.toSeq)
  private val paths: Seq[Path] = args.map(Paths.get(_))
  println("Paths: " + paths)

  private val tuneFileStrings: Seq[String] = paths.map(p => Source.fromFile(p.toFile, "UTF-8").mkString)

  // Convert each string to tunes.
  println(s"Reading ${tuneFileStrings.length} tune files")
  val tunes: Seq[AbcTuneNotation] = tuneFileStrings.flatMap { tuneFileString =>
    val input = new CharSequenceReader(tuneFileString)
    AbcNotationParser.tunes(input) match {
      case AbcNotationParser.Success(ts, _) => ts
      case AbcNotationParser.NoSuccess(msg, next) => throw new IllegalArgumentException(
        msg + "\nNext is: " + next.pos)
    }
  }

  println(Calendar.getInstance().getTime)
  println(s"Read ${tunes.length} tunes")

  private val subsequenceTunesMap: Map[NoteSequence, Set[AbcTuneNotation]] = SubsequenceMatchService.getSubsequenceTunes(16, tunes)

  // Filter out any sequences with only one matching tune.
  private val filteredSubsequenceTunesMap: Map[NoteSequence, Set[AbcTuneNotation]] = subsequenceTunesMap.filter(_._2.size > 1)

  println(Calendar.getInstance().getTime)
  println(s"Have ${filteredSubsequenceTunesMap.size} filtered sub-sequences")

  filteredSubsequenceTunesMap.foreach {case (subSequence, tuneSet) =>

      val tuneTitles: Set[String] = tuneSet.map(tuneToTitle)

      val tuneTitlesFormatted = tuneTitles.mkString("--- ", "\n--- ", "")

      println(subSequence.map { case AbcNoteNotation(note) => note })
      println("appears in")
      println(tuneTitlesFormatted)
  }

  def tuneToTitle(tuneNotation: AbcTuneNotation): String = {
    headerValue(tuneNotation, "T").head
  }

  def headerValue(tuneNotation: AbcTuneNotation, key: String): List[String] = tuneNotation match {
    case AbcTuneNotation(AbcNotationHeader(headerList), _) =>
      headerList.filter {
        case AbcNotationHeaderInformationField(headerKey, _) => headerKey == key
        case _ => false
      } map {
        case AbcNotationHeaderInformationField(_, headerValue) => headerValue
        case _ => throw new IllegalArgumentException("Only AbcNotationHeaderInformation expected")
      }
  }

}
