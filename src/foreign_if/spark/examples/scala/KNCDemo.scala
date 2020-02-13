package test.scala;

import com.nec.frovedis.Jexrpc.FrovedisServer
import com.nec.frovedis.mllib.clustering.SpectralEmbedding
import com.nec.frovedis.mllib.clustering.KMeans
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.mllib.regression.LabeledPoint
import com.nec.frovedis.matrix.FrovedisRowmajorMatrix
import com.nec.frovedis.mllib.neighbors.KNeighborsClassifier
import org.apache.spark.rdd.RDD
import com.nec.frovedis.exrpc.FrovedisLabeledPoint
import com.nec.frovedis.matrix.DoubleDvector
import org.apache.log4j.{Level, Logger}

object KNCDemo {
  def main(args: Array[String]): Unit = {
    Logger.getLogger("org").setLevel(Level.ERROR)

    // -------- configurations --------
    val conf = new SparkConf().setAppName("SPAExample").setMaster("local[2]")
    val sc = new SparkContext(conf)

    // initializing Frovedis server with "personalized command", if provided in command line
    if(args.length != 0) FrovedisServer.initialize(args(0))

    val data = Vector(
       Vectors.dense(-1,1),
       Vectors.dense(-2, -1),
       Vectors.dense(-3, -2),
       Vectors.dense(1,1),
       Vectors.dense(2, 1),
       Vectors.dense(3, 2)
    )
    
    val lbl = Vector(10.0, 10.0, 10.0, 20.0, 10.0, 20.0)
    val zip1 = lbl zip data
    var lpv = zip1.map( a => LabeledPoint(a._1, a._2) ) // vector of LabeledPoint
    var d_lp: RDD[LabeledPoint] = sc.parallelize(lpv)  // distributed LabeledPoint
    val f_lp = new FrovedisLabeledPoint(d_lp, true) // frovedis LabeledPoint

    //  -------- data loading from sample file at Spark side--------
    val s_data = sc.textFile("./input/knn_data.txt")
                   .map(s => Vectors.dense(s.split(' ').map(_.toDouble)))

    val fdata = new FrovedisRowmajorMatrix(s_data)
    
    val knc = new KNeighborsClassifier().setNNeighbors(3)
                                        .setAlgorithm("brute")
                                        .setMetric("euclidean")

    println("Using frovedis data")
    knc.run(f_lp)
    var (dist, ind) = knc.kneighbors(fdata, 3, true);
    println("distance matrix")
    dist.debug_print()
    println("indices matrix")
    ind.debug_print()
    
    var graph = knc.kneighbors_graph(fdata, 3, "connectivity")
    println("knn graph:")
    graph.debug_print()

    var pred: Array[Double] = knc.predict(s_data)
    println("predicted output: ")
    for(e <- pred) print(e + " ")

    var pred_proba = knc.predict_proba(fdata)
    println("predict proba output")
    pred_proba.debug_print()
    
    var d_lbl = sc.parallelize(lbl) // distributed labels
    var score = knc.score(fdata, DoubleDvector.get(d_lbl))
    println("score: " + score)

    println("Using spark data")
    knc.run(d_lp)
    var (dist2, ind2) = knc.kneighbors(s_data)
    
    println("Distance Row matrix: ")
    dist2.rows.collect.foreach(println)
    println("Indices Row matrix:")
    ind2.rows.collect.foreach(println)

    var pred2: Array[Double] = knc.predict(s_data)
    println("predicted output: ")
    for(e <- pred2) print(e + " ")

    var pred_proba2 = knc.predict_proba(s_data)
    println("predict proba output: ")
    pred_proba2.rows.collect.foreach(println)

    var score2 = knc.score(d_lp)
    println("score: " + score)

    fdata.release()
    knc.release()

    FrovedisServer.shut_down()
    sc.stop()
  }
}
