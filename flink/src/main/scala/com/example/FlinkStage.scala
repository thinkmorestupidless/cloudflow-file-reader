package com.example

import cloudflow.flink.{FlinkStreamlet, FlinkStreamletLogic}
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro.AvroInlet
import com.dataspartan.doc.ParagraphData
import org.apache.flink.streaming.api.scala._

class FlinkStage extends FlinkStreamlet {

  @transient val in = AvroInlet[ParagraphData]("in")
  //@transient val out = AvroOutlet[ParagraphData]("out")

  @transient def shape() = StreamletShape(in)

  override protected def createLogic(): FlinkStreamletLogic = new FlinkStreamletLogic() {
    override def buildExecutionGraph(): Unit = {
      readStream(in).print()
//      val stream = readStream(in)
//      writeStream(out, stream)
    }
  }
}
