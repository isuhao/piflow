package cn.bigdataflow.processor.ds

import scala.reflect.ClassTag
import org.apache.commons.lang3.ArrayUtils
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.Encoder
import org.apache.spark.sql.RelationalGroupedDataset
import org.apache.spark.sql.Row
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.OutputMode
import cn.bigdataflow.RunnerContext
import cn.bigdataflow.io.BatchSink
import cn.bigdataflow.io.BatchSource
import org.apache.spark.sql.Row
import scala.reflect.ManifestFactory.classType
import org.apache.spark.sql.DataFrame
import cn.bigdataflow.processor.Processor121
import cn.bigdataflow.processor.Processor021
import cn.bigdataflow.processor.Processor12N
import cn.bigdataflow.processor.ProcessorN21
import cn.bigdataflow.processor.Processor120
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author bluejoe2008@gmail.com
 */
case class DoMap[X: Encoder, Y: Encoder](fn: X ⇒ Y) extends Processor121 {
	override def toString = this.getClass.getSimpleName;
	def perform(input: Any, ctx: RunnerContext): Dataset[Y] = {
		input.asInstanceOf[Dataset[X]].map(fn);
	}
}

case class AsDataSet[Y: Encoder]() extends Processor121 {
	override def toString = this.getClass.getSimpleName;
	def perform(input: Any, ctx: RunnerContext): Dataset[Y] = {
		input.asInstanceOf[Dataset[_]].as[Y];
	}
}

case class AsDataFrame() extends Processor121 {
	override def toString = this.getClass.getSimpleName;
	def perform(input: Any, ctx: RunnerContext): DataFrame = {
		input.asInstanceOf[Dataset[_]].toDF;
	}
}

case class DoTransform[X, Y](fn: X ⇒ Y) extends Processor121 {
	override def toString = this.getClass.getSimpleName;
	def perform(input: Any, ctx: RunnerContext): Y = {
		fn(input.asInstanceOf[X]);
	}
}

case class DoFlatMap[X: Encoder, Y: Encoder](fn: X ⇒ TraversableOnce[Y]) extends Processor121 {
	def perform(input: Any, ctx: RunnerContext): Dataset[Y] = {
		input.asInstanceOf[Dataset[X]].flatMap(fn);
	}
}

case class DoFilter[X: Encoder](fn: X ⇒ Boolean) extends Processor121 {
	override def toString = this.getClass.getSimpleName;
	def perform(input: Any, ctx: RunnerContext): Dataset[X] = {
		input.asInstanceOf[Dataset[X]].filter(fn);
	}
}

case class DoGroupBy[X: Encoder](key: String) extends Processor121 {
	override def toString = this.getClass.getSimpleName;
	def perform(input: Any, ctx: RunnerContext): RelationalGroupedDataset = {
		input.asInstanceOf[Dataset[X]].groupBy(key);
	}
}

case class DoFork[X](conditions: X ⇒ Boolean*) extends Processor12N {
	/**
	 * creates N copies
	 */
	def this(ncopy: Int) = { this((1 to ncopy).map(n ⇒ (m: X) ⇒ true): _*) }
	override def toString = this.getClass.getSimpleName;
	def getOutPortNames(): Seq[String] = DEFAULT_OUT_PORT_NAMES(conditions.size);
	def perform(input: Any, ctx: RunnerContext): Map[String, _] = {
		val map = collection.mutable.Map[String, Dataset[Any]]();
		val conditionsMap = getOutPortNames.zip(conditions);
		conditionsMap.foreach { x ⇒
			map(x._1) = input.asInstanceOf[Dataset[X]].filter(x._2).asInstanceOf[Dataset[Any]];
		}

		map.toMap
	}
}

case class DoMerge[X: Encoder]() extends ProcessorN21 {
	def getInPortNames(): Seq[String] = DEFAULT_IN_PORT_NAMES(2);
	def perform(inputs: Map[String, _], ctx: RunnerContext): Dataset[X] = {
		inputs(getInPortNames()(0)).asInstanceOf[Dataset[X]]
			.union(inputs(getInPortNames()(1)).asInstanceOf[Dataset[X]]);
	}
}
