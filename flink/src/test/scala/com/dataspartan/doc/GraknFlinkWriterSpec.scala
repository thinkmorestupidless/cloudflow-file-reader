package com.dataspartan.doc


import scala.collection.immutable.Seq

import org.apache.flink.streaming.api.scala._

import cloudflow.flink.testkit._
import org.scalatest._

// 1. Extend from the abstract class FlinkTestkit
class GraknFlinkWriterSpec extends FlinkTestkit
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  "GraknFlinkWriter" should {
    "process streaming data" in {
      @transient lazy val env = StreamExecutionEnvironment.getExecutionEnvironment

      // 2. Create the FlinkStreamlet to test
      val processor = new GraknFlinkWriter

      // 3. Prepare data to be pushed into inlet ports
      val data = (1 to 10).map(_ => new ParagraphData())

      // 4. Setup inlet taps that tap the inlet ports of the streamlet
      val in: FlinkInletTap[ParagraphData] = inletAsTap[ParagraphData](
        processor.in,
        env.addSource(FlinkSource.CollectionSourceFunction(data)))

//      // 5. Setup outlet taps for outlet ports
//      val out: FlinkOutletTap[ParagraphData] = outletAsTap[ParagraphData](processor.out)

      // 6. Run the streamlet using the `run` method that the testkit offers
      run(processor, Seq(in), Seq.empty, env)

//      // 7. Write assertions to ensure that the expected results match the actual ones
//      TestFlinkStreamletContext.result should contain((Data(2, "name2")).toString())
//      TestFlinkStreamletContext.result.size should equal(5)
    }
  }
}