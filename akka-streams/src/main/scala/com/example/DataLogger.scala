package com.example

import akka.stream.scaladsl.{RunnableGraph, Sink}
import cloudflow.akkastream.scaladsl.RunnableGraphStreamletLogic
import cloudflow.akkastream.{AkkaStreamlet, AkkaStreamletLogic}
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro.AvroInlet
import com.example.data.DataThing

class DataLogger extends AkkaStreamlet {

  val in = AvroInlet[DataThing]("in")

  override def shape() = StreamletShape(in)

  override protected def createLogic(): AkkaStreamletLogic =
    new RunnableGraphStreamletLogic() {
      override def runnableGraph(): RunnableGraph[_] =
        plainSource(in)
        .map { data =>
          println(s"akka sink => $data")
          data
        }.to(Sink.ignore)
    }
}
