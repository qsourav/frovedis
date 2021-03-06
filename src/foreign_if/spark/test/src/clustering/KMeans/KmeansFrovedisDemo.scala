package test.clustering.KMeans;

import com.nec.frovedis.Jexrpc.FrovedisServer
import com.nec.frovedis.mllib.clustering.KMeans
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.linalg.Vectors

object KmeansFrovedisDemo {
  def main(args: Array[String]): Unit = {

    var command: String = ""
    var k: Int = -1
    var numiter: Int = 100
    var epsilon: Double =0.01
    var seed: Long = 0
    var input: String = "../../../input/kmeans_data.txt"

    args.sliding(2, 2).toList.collect {
      case Array("--cmd", cmd: String)           => command = cmd
      case Array("--num_iter", iter: String)     => numiter = iter.toInt
      case Array("--epsilon", ep: String)        => epsilon = ep.toDouble
      case Array("--k", kk: String)              => k = kk.toInt
      case Array("--seed", s: String)            => seed = s.toLong
      case Array("--input", data: String)        => input = data
    }

    if(k == -1) throw new IllegalArgumentException("k value is not provided");

    // -------- configurations --------
    val conf = new SparkConf().setAppName("KmeansFrovedis") 
    val sc = new SparkContext(conf)

    // initializing Frovedis server with "personalized command", if provided in command line
    if(command != "") FrovedisServer.initialize(command)

    // -------- data loading from sample kmeans data file at Spark side--------
    val data = sc.textFile(input)
    val parsedData = data.map(s => Vectors.dense(s.split(' ').map(_.toDouble)))
    val splits = parsedData.randomSplit(Array(0.6, 0.4), seed = 11L)
    val training = splits(0)
    val test = splits(1)

    // Build the cluster using KMeans
    val model = KMeans.train(training,k,numiter,seed,epsilon)

    // Evaluate the model on test data
    model.predict(test).foreach(println)
    
    FrovedisServer.shut_down();
    sc.stop()
  }
}
