package test.scala;

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import com.nec.frovedis.Jexrpc.FrovedisServer
import org.apache.log4j.{Level, Logger}

object FrovedisBFS {
  def main(args: Array[String]) {
    Logger.getLogger("org").setLevel(Level.ERROR)
    val conf = new SparkConf().setAppName("BFS").setMaster("local[1]")
    val sc = new SparkContext(conf)

    // Frovedis Demo
    if(args.length != 0) FrovedisServer.initialize(args(0))

    val f1 = "input/urldata_sssp.dat"
    val source_vertex = 1
    val frov_graph = com.nec.frovedis.graphx.GraphLoader.edgeListFile(sc, f1)
    val res = frov_graph.bfs(source_vertex)

    println("...............Frovedis BFS QUERY................")
    val q = res.bfs_query(4)
    println("Dist: " + q._1)
    println("Path: " + q._2)

    val arr: Array[Long] = Array(1,2,3,4,5)
    val qs: Array[(Long, String)] = res.bfs_query(arr)
    qs.foreach{ println }

    FrovedisServer.shut_down()
  }
}
