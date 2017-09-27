package cn.bigdataflow
import cn.bigdataflow.dsl.PipedProcessorNode

package object shell {
	implicit def toRunnable(flowGraph: FlowGraph)(implicit runner: Runner) = {
		new RunnableFlowGraph(flowGraph);
	}

	implicit def toRunnable(node: PipedProcessorNode)(implicit runner: Runner) = {
		new RunnableFlowGraph(node.flowGraph);
	}
}