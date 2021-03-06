package com.nec.frovedis.exrpc;

import com.nec.frovedis.Jexrpc._
import com.nec.frovedis.Jmatrix.DummyMatrix
import com.nec.frovedis.matrix.FrovedisColmajorMatrix
import com.nec.frovedis.matrix.FrovedisRowmajorMatrix
import com.nec.frovedis.matrix.MAT_KIND
import com.nec.frovedis.matrix.DoubleDvector
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.regression.LabeledPoint
import scala.collection.immutable.Map

class FrovedisLabeledPoint extends java.io.Serializable {

  protected var fdata : MemPair = null
  protected var encoded_fdata : MemPair = null
  protected var num_row: Long = 0
  protected var num_col: Long = 0
  protected var isDense: Boolean = false
  protected var mtype: Short = 0
  protected var uniqueLabels: Array[Double] = null

  def this (data: RDD[LabeledPoint],
            need_rowmajor: Boolean = false) = {
    this()
    load(data, need_rowmajor)
  }

  def load(data: RDD[LabeledPoint],
           need_rowmajor: Boolean = false) : Unit  = {
    /** releasing the old data (if any) */
    release()

    // Getting the global nrows and ncols information from the RDD 
    val nrow = data.count
    val ncol = data.first.features.size

    // extracting label and points
    val y = data.map(_.label)
    val x = data.map(_.features)

    // judging type of Vector
    this.isDense = x.first.getClass.toString() matches ".*DenseVector*."

    val yptr = DoubleDvector.get(y) // getting dvector pointer
    val fs = FrovedisServer.getServerInstance()
    this.uniqueLabels = JNISupport.getUniqueDvectorElements(fs.master_node, yptr)
    val info = JNISupport.checkServerException()
    if (info != "") throw new java.rmi.ServerException(info)

    // getting matrix pointer along with num_row, num_col info
    var xptr: Long = -1
    if (this.isDense) {
       // TODO FIX: if destructor gets activated, the below matrix would 
       //           get released, right after get() is called
       if (need_rowmajor) {
         val mat = new FrovedisRowmajorMatrix(x)
         xptr = mat.get()
         this.mtype = mat.matType()
         this.num_row = mat.numRows()
         this.num_col = mat.numCols()
       } 
       else {
         val mat = new FrovedisColmajorMatrix(x)
         xptr = mat.get()
         this.mtype = mat.matType()
         this.num_row = mat.numRows()
         this.num_col = mat.numCols()
       }
    }
    else {
       val mat = new FrovedisSparseData(x)
       xptr = mat.get()
       this.mtype = mat.matType()
       this.num_row = mat.numRows()
       this.num_col = mat.numCols()
    }
    require(nrow == this.num_row && ncol == this.num_col, 
      "Internal error occured in FrovedisLabeledPoint creation - report bug!")
    this.fdata = new MemPair(xptr, yptr)
  }
 
  def encode_labels(): (MemPair, Map[Double,Double]) = {
    val xptr = fdata.first()
    val yptr = fdata.second()
    val uniqCnt = get_distinct_label_count().intValue
    val uniq_labels = get_distinct_labels()
    val fs = FrovedisServer.getServerInstance()
    val encoded_yptr = JNISupport.getZeroBasedEncodedDvector(fs.master_node, 
                                                             yptr)
    val info = JNISupport.checkServerException()
    if (info != "") throw new java.rmi.ServerException(info)
    val encoded_as: Array[Double] = new Array(uniqCnt)
    for (i <- 0 to (uniqCnt - 1)) encoded_as(i) = i
    val labelEncodingLogic = (encoded_as zip uniq_labels).toMap
    this.encoded_fdata = new MemPair(xptr, encoded_yptr) 
    return (this.encoded_fdata, labelEncodingLogic)
  }

  def encode_labels(encoded_as: Array[Double]): 
    (MemPair, Map[Double,Double]) = {
    val xptr = fdata.first()
    val yptr = fdata.second()
    val uniqCnt = get_distinct_label_count().intValue
    require(encoded_as.size == uniqCnt, s"size of unique labels and encoded values are not matching!")
    val uniq_labels = get_distinct_labels()
    val fs = FrovedisServer.getServerInstance()
    val encoded_yptr = JNISupport.getEncodedDvectorAs(fs.master_node, yptr, 
                                                      uniq_labels, encoded_as, 
                                                      uniqCnt) 
    val info = JNISupport.checkServerException()
    if (info != "") throw new java.rmi.ServerException(info)
    val labelEncodingLogic = (encoded_as zip uniq_labels).toMap
    this.encoded_fdata = new MemPair(xptr, encoded_yptr)
    return (this.encoded_fdata, labelEncodingLogic)
  }

  def release_encoded_labels(): Unit = {
    if (encoded_fdata != null) {
      val fs = FrovedisServer.getServerInstance()
      JNISupport.releaseFrovedisDvector(fs.master_node, encoded_fdata.second())
      val info = JNISupport.checkServerException()
      if (info != "") throw new java.rmi.ServerException(info)
      encoded_fdata = null
    }
  }
 
  def release() : Unit = {
    release_encoded_labels()
    if (fdata != null) {
      val fs = FrovedisServer.getServerInstance()
      JNISupport.releaseFrovedisLabeledPoint(fs.master_node,fdata,isDense,mtype)
      val info = JNISupport.checkServerException()
      if (info != "") throw new java.rmi.ServerException(info)
      num_row = 0
      num_col = 0
      isDense = false
      mtype = 0
      fdata = null
      uniqueLabels = null
    }
  }

  def debug_print() : Unit = {
    if (fdata != null) {
      val fs = FrovedisServer.getServerInstance()
      JNISupport.showFrovedisLabeledPoint(fs.master_node,fdata,isDense,mtype)
      val info = JNISupport.checkServerException();
      if (info != "") throw new java.rmi.ServerException(info);
    }
  }

  def get() = fdata
  def feature_as_rowmajor(): FrovedisRowmajorMatrix = {
    require(this.matType() == MAT_KIND.RMJR, 
      "FrovedisLabeledPoint does not have rowmajor data!")
    val dmat = new DummyMatrix(fdata.first(),numRows(),numCols(),matType())
    return new FrovedisRowmajorMatrix(dmat)
  }
  def feature_as_colmajor(): FrovedisColmajorMatrix = {
    require(this.matType() == MAT_KIND.CMJR, 
      "FrovedisLabeledPoint does not have colmajor data!")
    val dmat = new DummyMatrix(fdata.first(),numRows(),numCols(),matType())
    return new FrovedisColmajorMatrix(dmat)
  }
  def feature_as_crs(): FrovedisSparseData = {
    require(this.matType() == MAT_KIND.SCRS, 
      "FrovedisLabeledPoint does not have CRS data!")
    val dmat = new DummyMatrix(fdata.first(),numRows(),numCols(),matType())
    return new FrovedisSparseData(dmat)
  }
  def label() = fdata.second()
  def numRows() = num_row
  def numCols() = num_col
  def is_dense() = isDense
  def matType() = mtype
  def get_distinct_labels()= uniqueLabels
  def get_distinct_label_count() = uniqueLabels.size
}
