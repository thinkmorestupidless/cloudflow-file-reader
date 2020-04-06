package com.example

import java.nio.file
import java.nio.file.Path
import java.util.UUID

import akka.NotUsed
import akka.stream.IOResult
import akka.stream.alpakka.file.DirectoryChange
import akka.stream.alpakka.file.scaladsl.{Directory, DirectoryChangesSource}
import akka.stream.scaladsl.{FileIO, Framing, RunnableGraph, Source}
import akka.util.ByteString
import cloudflow.akkastream.scaladsl.RunnableGraphStreamletLogic
import cloudflow.akkastream.{AkkaStreamlet, AkkaStreamletLogic}
import cloudflow.streamlets.{ReadWriteMany, StreamletShape, VolumeMount}
import cloudflow.streamlets.avro.AvroOutlet
import com.dataspartan.doc.ParagraphData
import com.example.data.DataThing

import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration._

class AkkaStreamsStage extends AkkaStreamlet {

  val out = AvroOutlet[ParagraphData]("out")

  override def shape() = StreamletShape(out)

  private val sourceData = VolumeMount("source-data-mount", "/mnt/data", ReadWriteMany)

  override def volumeMounts: immutable.IndexedSeq[VolumeMount] = Vector(sourceData)

  override protected def createLogic(): AkkaStreamletLogic = new RunnableGraphStreamletLogic() {

    val listFiles = {
      val fs = getMountedPath(sourceData)
      Directory
        .ls(fs)
        .map(path => {
          println("Reading file -> " + path)
          (path, DirectoryChange.Creation)
        })
        .merge(DirectoryChangesSource(fs, pollInterval = 1.second, maxBufferSize = 1000))
    }

    val delimiter = ByteString(
      "¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬¬\n"
    )

    def getFilename(path: Path): String = {
      val filename = path.getFileName.toFile.getName
      filename.substring(0, filename.lastIndexOf("."))
    }

    val readFile: Path => Source[(String, String), Future[IOResult]] = { path: Path =>
      FileIO
        .fromPath(path)
        .via(Framing.delimiter(delimiter, maximumFrameLength = 2 * 1024 * 1024, allowTruncation = true))
        .map(p => (p.utf8String, getFilename(path)))
    }

    val mock: ((String, String)) => ParagraphData = { _ =>
      new ParagraphData()
    }

    val readFromVolume =
      listFiles
        .filterNot(_._2 == DirectoryChange.Deletion)
        .map(_._1)
        .flatMapConcat(readFile)
        .map(mock)

    val tickingRepeater =
      Source.tick(30 seconds, 1 second, NotUsed)
      .map { _ =>
        val data = new ParagraphData()
        println(s"akka => $data")
        data
      }

    override def runnableGraph(): RunnableGraph[_] =
      readFromVolume
        .to(plainSink(out))
  }
}
