package app.common

import java.io.FileNotFoundException
import java.nio.file.Path

object ResourceFiles {

  def read(path: String): String = {
    Option(getClass.getResourceAsStream(path))
      .map(scala.io.Source.fromInputStream)
      .map(_.mkString)
      .getOrElse(throw new FileNotFoundException(path))
  }

  def read(path: Path): String = read(path.toString)

  def readLines(path: String): List[String] = {
    Option(getClass.getResourceAsStream(path))
      .map(scala.io.Source.fromInputStream)
      .map(_.getLines.toList)
      .getOrElse(throw new FileNotFoundException(path))
  }

  def readLines(path: Path): List[String] = readLines(path.toString)

  def exists(path: String): Boolean = {
    getClass.getResourceAsStream(path) != null
  }

  def exists(path: Path): Boolean = exists(path.toString)
}
