package cn.piflow.flow

;

import java.util.Date

import cn.piflow._
import cn.piflow.io.{ConsoleSink, SeqAsSource}
import cn.piflow.processor.{DoSleep, Processor020}
import cn.piflow.processor.ds.DoMap
import cn.piflow.processor.io.{DoLoad, DoWrite}
import org.apache.spark.sql.SparkSession
import org.junit.{Assert, Test}

class JobManagerTest {
	val spark = SparkSession.builder.master("local[4]")
		.getOrCreate();
	spark.conf.set("spark.sql.streaming.checkpointLocation", "/tmp/");

	import spark.implicits._

	@Test
	def test() = {
		val fg = new FlowGraph();
		val node1 = fg.createNode(DoLoad(SeqAsSource(0L to 10000L)));
		val node2 = fg.createNode(DoMap[Long, Long](_ + 1));
		val node3 = fg.createNode(DoMap[Long, Long](_ * 2));
		val node4 = fg.createNode(DoWrite(ConsoleSink()));

		fg.link(node1, node2, ("_1", "_1"));
		fg.link(node2, node3, ("_1", "_1"));
		fg.link(node3, node4, ("_1", "_1"));
		fg.show();

		val runner = Runner.sparkRunner(spark);
		val man = runner.getJobManager();

		Assert.assertEquals(0, man.getScheduledJobs().size);
		Assert.assertEquals(0, man.getRunningJobs().size);

		runner.run(fg); //await termination
		runner.run(fg, 2000); //await termination, timeout=2s

		val sj1 = runner.schedule(fg);
		val sj2 = runner.schedule(fg, Start.now);
		val sj3 = runner.schedule(fg, Start.later(1000));
		val sj4 = runner.schedule(fg, Start.at(new Date(System.currentTimeMillis() + 2000)));
		val sj5 = runner.schedule(fg, Start.later(1000), Repeat.periodically(1000));
		val sj6 = runner.schedule(fg, Start.at(new Date(System.currentTimeMillis() + 2000)), Repeat.periodically(1000));
		val sj7 = runner.schedule(fg, Start.now, Repeat.daily(13, 0));
		val sj8 = runner.schedule(fg, Start.now, Repeat.cronedly("* * * * * ?"));

		Thread.sleep(2200); //1s

		val stat = runner.getStatManager();
		man.getHistoricExecutions().map(_.getId()).union(man.getRunningJobs().map(_.getId())).foreach {
			stat.getStat(_).show();
		}

		runner.stop();
	}
}

