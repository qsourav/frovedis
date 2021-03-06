package com.nec.frovedis.Jexrpc;

import com.nec.frovedis.Jmatrix.DummyMatrix;
import com.nec.frovedis.Jmatrix.DummyGetrfResult;
import com.nec.frovedis.Jmatrix.DummyGesvdResult;
import com.nec.frovedis.Jmatrix.DummyPCAResult;
import com.nec.frovedis.Jmatrix.DummyFreqItemset;
import com.nec.frovedis.Jgraph.DummyEdge;
import com.nec.frovedis.Jgraph.DummyGraph;
import com.nec.frovedis.Jmllib.DummyGLM;
import com.nec.frovedis.Jmllib.IntDoublePair;
import com.nec.frovedis.Jmllib.DummyLDAResult;
import com.nec.frovedis.Jmllib.DummyLDAModel;
import com.nec.frovedis.Jmllib.DummyKNNResult;
import com.nec.frovedis.graphx.bfs_result;
import com.nec.frovedis.graphx.sssp_result;
import com.nec.frovedis.Jmllib.DummyTSNEResult;

public class JNISupport {
 
  static {
    // Load native library libfrovedis_client.so at runtime
    System.loadLibrary("frovedis_client_spark");

    // to ensure server will be shut_down,
    // even if user program aborts abnormally
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
        public void run() {
          try {  
            FrovedisServer.shut_down();
          }
          catch(java.rmi.ServerException e) {
            System.out.println(e.getMessage());
          }
        }
    }, "Shutdown-thread"));
  }

  public static native String checkServerException();
 
  // ---
  public static native MemPair loadFrovedisWorkerGLMData(Node t_node, 
                                                       long nrows, long ncols,
                                                       double lbl[],
                                                       int off[], 
                                                       int idx[], 
                                                       double val[]);
  public static native MemPair createFrovedisLabeledPoint(Node master_node,
                                                          MemPair eps[],
                                                          long nrows, long ncols);
  public static native void releaseFrovedisLabeledPoint(Node master_node, 
                                                        MemPair fdata, 
                                                        boolean isDense,
                                                        short mtype);
  public static native void showFrovedisLabeledPoint(Node master_node, 
                                                     MemPair fdata, 
                                                     boolean isDense,
                                                     short mtype);
  // ---
  public static native long loadFrovedisWorkerVectorStringData(Node t_node, 
                                                             String val[], 
                                                             int size);
  public static native DummyMatrix crsToFrovedisRowmajorMatrix(Node master_node,
                                                               long fdata);

  public static native long loadFrovedisWorkerData(Node t_node, 
                                                 long nrows, long ncols,
                                                 int off[], 
                                                 int idx[], 
                                                 double val[]);
  public static native DummyMatrix createFrovedisSparseMatrix(Node master_node,
                                                            long eps[],
                                                            short mtype);
  public static native long createFrovedisSparseData(Node master_node,
                                                   long eps[],
                                                   long nrows, long ncols);
  public static native void releaseFrovedisSparseData(Node master_node, 
                                                    long fdata); 
  public static native void showFrovedisSparseData(Node master_node, 
                                                 long fdata);
  // ---
  public static native long loadFrovedisWorkerRmajorData(Node t_node, 
                                                       long nrows, long ncols,
                                                       double val[]);
  public static native long loadFrovedisWorkerRmajorMatData(Node t_node, 
                                                       long nrows, long ncols,
                                                       double val[][]);
  public static native DummyMatrix getFrovedisRowmajorMatrix(Node master_node,
                                                           long fdata, 
                                                           short mtype);
  public static native long[] getAllLocalPointers(Node master_node, 
                                                  long dptr, short mtype);
  public static native double[] getLocalArray(Node t_node, 
                                              long l_dptr, short mtype);
  public static native long createFrovedisDenseData(Node master_node,
                                                  long eps[],
                                                  long nrows, long ncols,
                                                  short mtype);
  public static native void releaseFrovedisDenseData(Node master_node, 
                                                   long fdata, short mtype); 
  public static native void showFrovedisDenseData(Node master_node, 
                                                long fdata, short mtype);
  public static native void saveFrovedisDenseData(Node master_node, 
                                                long fdata, short mtype,
                                                String path, boolean isbinary);
  public static native DummyMatrix loadFrovedisDenseData(Node master_node, 
                                                       short mtype,
                                                       String path, 
                                                       boolean isbinary);
  // --- TODO ---
  public static native DummyMatrix transposeFrovedisDenseData(Node master_node,
                                                            long fdata, 
                                                            short mtype);
  public static native double[] getRowmajorArray(Node master_node, 
                                                 long fdata, short mtype);
  public static native double[] getColmajorArray(Node master_node, 
                                                 long fdata, short mtype);
  // ---
  public static native Node getMasterInfo(String cmd);
  public static native int getWorkerSize(Node master_node);
  public static native Node[] getWorkerInfo(Node master_node);
  public static native void finalizeFrovedisServer(Node master_node);
  public static native void cleanUPFrovedisServer(Node master_node);

  // -------- Logistic Regression --------
  public static native void callFrovedisLRSGD(Node master_node,
                                              MemPair fdata,
                                              int numIter, 
                                              double stepSize,
                                              double miniBatchFraction,
                                              int regType,
                                              double regParam, 
                                              boolean isMult, 
                                              boolean icpt,
                                              double tol,
                                              int mid, boolean movable,
                                              boolean isDense,
                                              boolean use_shrink);

  public static native void callFrovedisLRLBFGS(Node master_node,
                                                MemPair fdata,
                                                int numIter, 
                                                double stepSize,
                                                int histSize,
                                                int regType,
                                                double regParam, 
                                                boolean isMult, 
                                                boolean icpt,
                                                double tol,
                                                int mid, boolean movable,
                                                boolean isDense);

// -------- Word2Vector --------
  public static native void callFrovedisW2V(Node master_node,
                                            long hash_dptr,
                                            int[] vocab_count,
                                            int vocab_size,
                                            int vectorSize,
                                            int window,
                                            float threshold,
                                            int negative,
                                            int numIterations,
                                            double learningRate,
                                            float modelSyncPeriod,
                                            int minSyncWords,
                                            int fullSyncTimes,
                                            int messageSize,
                                            int numThreads,
                                            int mid);
  public static native float[] getW2VWeight(Node master_node, int mid);
  public static native DummyMatrix getW2VWeightPointer(Node master_node, int mid);
  public static native void showW2VWeight(Node master_node, int mid);
  public static native void saveW2VModel(Node master_node, int mid,
                                         String[] vocab, int size,
                                         String path);

  // -------- Linear SVM Classification --------
  public static native void callFrovedisSVMSGD(Node master_node,
                                             MemPair fdata,
                                             int numIter,
                                             double stepSize,
                                             double miniBatchFraction,
                                             double regParam,
                                             int mid, boolean movable,
                                             boolean isDense,
                                             int ncls);

  public static native void callFrovedisSVMLBFGS(Node master_node,
                                               MemPair fdata,
                                               int numIter,
                                               double stepSize,
                                               int histSize,
                                               double regParam,
                                               int mid, boolean movable,
                                               boolean isDense,
                                               int ncls);
  // -------- Linear SVM Regression --------
  public static native void callFrovedisSVR(Node master_node,
                                            MemPair fdata,
                                            int numIter,
                                            double stepSize,
                                            double miniBatchFraction,
                                            double regParam,
                                            String regType,
                                            String loss,
                                            double eps,
                                            boolean isIntercept,
                                            double convergenceTol,
                                            int mid, boolean movable,
                                            boolean isDense);

  // -------- SVM Kernel --------
  public static native void callFrovedisKernelSVM(Node master_node,
                                                  MemPair fdata,
                                                  double C,
                                                  String kernelType,
                                                  int degree,
                                                  double gamma,
                                                  double coef0,
                                                  double tol,
                                                  int cacheSize,
                                                  int maxIter,
                                                  int mid, boolean movable,
                                                  boolean isDense,
                                                  int ncls);


  // -------- Linear Regression --------
  public static native void callFrovedisLNRSGD(Node master_node,
                                             MemPair fdata,
                                             int numIter,
                                             double stepSize,
                                             double miniBatchFraction,
                                             int mid, 
                                             boolean movable,
                                             boolean isDense);

  public static native void callFrovedisLNRLBFGS(Node master_node,
                                               MemPair fdata,
                                               int numIter,
                                               double stepSize,
                                               int histSize,
                                               int mid, 
                                               boolean movable,
                                               boolean isDense);

  // -------- Lasso Regression --------
  public static native void callFrovedisLassoSGD(Node master_node,
                                               MemPair fdata,
                                               int numIter,
                                               double stepSize,
                                               double miniBatchFraction,
                                               double regParam,
                                               int mid, 
                                               boolean movable,
                                               boolean isDense);

  public static native void callFrovedisLassoLBFGS(Node master_node,
                                                 MemPair fdata,
                                                 int numIter,
                                                 double stepSize,
                                                 int histSize,
                                                 double regParam,
                                                 int mid, 
                                                 boolean movable,
                                                 boolean isDense);
  
  // -------- Ridge Regression --------
  public static native void callFrovedisRidgeSGD(Node master_node,
                                               MemPair fdata,
                                               int numIter,
                                               double stepSize,
                                               double miniBatchFraction,
                                               double regParam,
                                               int mid, 
                                               boolean movable,
                                               boolean isDense);

  public static native void callFrovedisRidgeLBFGS(Node master_node,
                                                 MemPair fdata,
                                                 int numIter,
                                                 double stepSize,
                                                 int histSize,
                                                 double regParam,
                                                 int mid, 
                                                 boolean movable,
                                                 boolean isDense);

  // -------- Matrix Factorization Using ALS --------
  public static native void callFrovedisMFUsingALS(Node master_node,
                                                 long fdata,
                                                 int rank,
                                                 int numIter,
                                                 double alpha,
                                                 double lambda,
                                                 long seed,
                                                 int mid, boolean movable);

  // -------- Clustering --------
  public static native void callFrovedisKMeans(Node master_node,
                                             long fdata,
                                             int k,
                                             int numIter,
                                             long seed,
                                             double epsilon,
                                             int mid, 
                                             boolean movable,
                                             boolean isDense,
                                             boolean use_shrink);
 
  public static native void callFrovedisACA(Node master_node,
                                            long fdata,
                                            int mid,
                                            String linkage,
                                            boolean movable, 
                                            boolean dense);

  public static native int[] FrovedisACMPredict(Node master_node,
                                                int mid, int ncluster);

  public static native int[] loadFrovedisACM (Node master_node, 
                                             int mid, String path);

  public static native int[] callFrovedisSCA(Node master_node,
                                             long fdata,
                                             int nCluster,
                                             int iteration,
                                             int component,
                                             double eps,
                                             double gamma,
                                             boolean normlaplacian,
                                             int mid,
                                             boolean precomputed,
                                             int mode, 
                                             boolean drop_first,
                                             boolean movable, 
                                             boolean dense);
  public static native DummyMatrix getSCMAffinityMatrix (Node master_node, 
                                                         int mid);
  public static native int[] loadFrovedisSCM (Node master_node, 
                                              int mid, String path);

  public static native void callFrovedisSEA(Node master_node,
                                            long fdata,
                                            int component,
                                            double gamma,
                                            boolean normlaplacian,
                                            int mid,
                                            boolean precomputed,
                                            int mode,
                                            boolean drop_first,
                                            boolean movable,
                                            boolean dense);
  public static native DummyMatrix getSEMAffinityMatrix (Node master_node,
                                                         int mid);
  public static native DummyMatrix getSEMEmbeddingMatrix (Node master_node,
                                                         int mid);

  public static native int[] callFrovedisDBSCAN(Node master_node,
                                               long fdata,
                                               double eps,
                                               int min_samples,
                                               int mid,
                                               boolean dense,
                                               double[] sample_weight,
                                               long sample_weight_length);

  // ---------------------KNN----------------------------------  
  public static native void callFrovedisKnnFit(Node master_node,
                                            long xptr,
                                            int k,
                                            float radius,
                                            String algorithm,
                                            String metric,
                                            float chunk_size,
                                            int mid,
                                            boolean dense);

  public static native DummyKNNResult knnKneighbors(Node master_node,
                                                    long tptr,
                                                    int k,
                                                    int mid,
                                                    boolean needDistance,
                                                    boolean dense);

  public static native DummyMatrix knnKneighborsGraph(Node master_node,
                                                      long tptr,
                                                      int k,
                                                      int mid,
                                                      String mode,
                                                      boolean dense);

  public static native DummyMatrix knnRadiusNeighbors(Node master_node,
                                                      long tptr,
                                                      float radius,
                                                      int mid,
                                                      boolean dense);

  public static native DummyMatrix knnRadiusNeighborsGraph(Node master_node,
                                                          long tptr,
                                                          float radius,
                                                          int mid,
                                                          String mode,
                                                          boolean dense);
  // ---------------------KNC----------------------------------  
  public static native void callFrovedisKncFit(Node master_node,
                                            MemPair fdata,
                                            int k,
                                            String algorithm,
                                            String metric,
                                            float chunk_size,
                                            int mid,
                                            boolean dense);

  public static native DummyKNNResult kncKneighbors(Node master_node,
                                                    long tptr,
                                                    int k,
                                                    int mid,
                                                    boolean needDistance,
                                                    boolean dense);

  public static native DummyMatrix kncKneighborsGraph(Node master_node,
                                                      long tptr,
                                                      int k,
                                                      int mid,
                                                      String mode,
                                                      boolean dense);

  public static native double[] kncDoublePredict(Node master_node,
                                                 long tptr,
                                                 int mid,
                                                 boolean saveProba,
                                                 boolean dense);

  public static native DummyMatrix kncPredictProba(Node master_node,
                                                long tptr,
                                                int mid,
                                                boolean dense);

  public static native float kncModelScore(Node master_node,
                                          long xptr,
                                          long yptr,
                                          int mid,
                                          boolean dense);

  // ---------------------KNR----------------------------------  
  public static native void callFrovedisKnrFit(Node master_node,
                                            MemPair fdata,
                                            int k,
                                            String algorithm,
                                            String metric,
                                            float chunk_size,
                                            int mid,
                                            boolean dense);

  public static native DummyKNNResult knrKneighbors(Node master_node,
                                                    long tptr,
                                                    int k,
                                                    int mid,
                                                    boolean needDistance,
                                                    boolean dense);

  public static native DummyMatrix knrKneighborsGraph(Node master_node,
                                                      long tptr,
                                                      int k,
                                                      int mid,
                                                      String mode,
                                                      boolean dense);
  
  public static native double[] knrDoublePredict(Node master_node,
                                                 long tptr,
                                                 int mid,
                                                 boolean dense);

  public static native float knrModelScore(Node master_node,
                                          long xptr,
                                          long yptr,
                                          int mid,
                                          boolean dense);


  // -------- Compute PCA --------
  public static native DummyPCAResult computePCA(Node master_node,
                                                long fdata,
                                                int k,
                                                boolean movable);

  // -------- Compute TSNE -------
  public static native DummyTSNEResult computeTSNE(Node master_node,
                                                   long fdata,
                                                   double perplexity,
                                                   double early_exaggeration,
                                                   double min_grad_norm,
                                                   double learning_rate,
                                                   int ncomponents,
                                                   int niter,
                                                   int niter_without_progress,
                                                   String metric,
                                                   String method,
                                                   String init,
                                                   boolean verbose); 

  // -------- Compute SVD --------
  public static native DummyGesvdResult computeSVD(Node master_node,
                                                   long fdata,
                                                   int k,
                                                   boolean isDense,
                                                   boolean movable,
                                                   boolean use_shrink);
  public static native DummyGesvdResult getSVDResultFromFiles(Node master_node,
                                                              short mtype,
                                                              String s_file,
                                                              String u_file,
                                                              String v_file,
                                                              boolean wantU,
                                                              boolean wantV,
                                                              boolean isbinary);
  // -------- Frovedis Model Operations --------
  public static native void showFrovedisModel(Node master_node, int mid, short mkind);
  public static native void releaseFrovedisModel(Node master_node, int mid, short mkind);
  public static native long[] broadcast2AllWorkers(Node master_node, int mid, short mkind);

  // [GLM] for multiple test vectors (prediction done in parallel in Frovedis worker nodes)
  public static native double[] doParallelGLMPredict(Node t_node, long mptr, short mkind,
                                                     long nrows, long ncols,
                                                     int off[], int idx[],
                                                     double val[]);
  // [GLM] for single test vector (prediction done in master node)
  public static native double doSingleGLMPredict(Node master_node, int mid, short mkind,
                                                 long nrows, long ncols,
                                                 int off[], int idx[],
                                                 double val[]);
  // [KMM] for multiple test vectors (prediction done in parallel in Frovedis worker nodes)
  public static native int[] doParallelKMMPredict(Node t_node, long tptr, 
                                                  int mid, boolean dense);
  // [KMM] for single test vector (prediction done in master node)
  public static native int doSingleKMMPredict(Node master_node, long tptr, 
                                              int mid, boolean dense);
  // [MFM] for multiple test vectors (prediction done in parallel in Frovedis worker nodes)
  public static native double[] doParallelALSPredict(Node t_node, long mptr, short mkind,
                                                     int uids[], int pids[]);
  // [MFM] for single test vector (prediction done in master node)
  public static native double doSingleALSPredict(Node master_node, int mid, short mkind,
                                                 int uid, int pid);
  // generic predict
  public static native double[] genericPredict(Node master_node,
                                              long tptr,
                                              int mid,
                                              short mkind,
                                              boolean dense,
                                              boolean need_prob);

  public static native double genericSinglePredict(Node master_node,
                                                   long tptr,
                                                   int mid,
                                                   short mkind,
                                                   boolean dense);

  // [MFM] for recommending 'num' no. of products (with rating) for a given user
  public static native IntDoublePair[] recommendProducts(Node master_node, 
                                                         int mid, short mkind,
                                                         int user, int num);
  // [MFM] for recommending 'num' no. of users (with rating) for a given product
  public static native IntDoublePair[] recommendUsers(Node master_node, 
                                                      int mid, short mkind,
                                                      int product, int num);

  public static native void setFrovedisGLMThreshold(Node master_node, 
                                                  int mid, short mkind, double thr);

  public static native DummyGLM loadFrovedisGLM(Node master_node, 
                                              int mid, short mkind, String path);
  public static native int loadFrovedisMFM(Node master_node, 
                                         int mid, short mkind, String path);
  public static native int loadFrovedisKMM(Node master_node, 
                                         int mid, short mkind, String path);
  public static native void saveFrovedisModel(Node master_node, int mid, 
                                            short mkind, String path);
  public static native void loadFrovedisModel(Node master_node,
                                          int model_Id, 
                                          short mkind,  
                                          String path);
  
  public static native DummyFreqItemset[] toSparkFPM(Node master_node,
                                                     int mid);

  public static native int callFrovedisFPM(Node master_node,
                                           long fdata,
                                           double min_support,
                                           int depth, int c_point, 
                                           int opt_level,
                                           int model_Id, boolean movable);

  public static native void callFrovedisFPMR(Node master_node,
                                             double minConfidence,
                                             int model_Id ,  
                                             int model_Idr);
  
  public static native int loadFPGrowthModel(Node master_node,
                                             int model_Id, 
                                             String path);

  public static native void callFrovedisFM(Node master_node,
                                         MemPair fdata, double init_stdev,
					 double learning_rate,
					 int iteration,
					 String optimizer,
					 boolean is_regression,
					 int batch_size,
					 boolean global_bias,
					 boolean one_way_interaction,
					 int num_factor,
					 double intercept,
					 double reg1way,
					 double reg_pairWise,
					 int model_id,
                                         boolean movable);

  public static native void callFrovedisNBM(Node master_node, MemPair fdata,
                                            double lambda, 
                                            double threshold, 
                                            boolean fit_prior,
                                            double[] class_prior, 
                                            long class_prior_length,
                                            double[] sample_weight, 
                                            long sample_weight_length,
                                            int model_id,
                                            String modelType, boolean movable,
                                            boolean isDense);

  public static native String loadFrovedisNBM(Node master_node,
                                              int model_id,
                                              short mkind,String path);

  // [p]blas level 1 routines   
  public static native void swap(Node master_node, short mtype, 
                                 long vptr1, long vptr2);
  public static native void copy(Node master_node, short mtype,
                                 long vptr1, long vptr2);
  public static native void scal(Node master_node, short mtype,
                                 long vptr,  double alpha);
  public static native void axpy(Node master_node, short mtype,
                                 long vptr1, long vptr2, double alpha);
  public static native double dot(Node master_node, short mtype,
                                  long vptr1, long vptr2);
  public static native double nrm2(Node master_node, short mtype,
                                   long vptr);

  // [p]blas level 2 routines   
  public static native DummyMatrix gemv(Node master_node, short mtype,
                                        long mptr, long vptr,
                                        boolean isTrans, 
                                        double alpha, double beta);
  public static native DummyMatrix ger(Node master_node, short mtype,
                                       long vptr1, long vptr2,
                                       double alpha);

  // [p]blas level 3 routines   
  public static native DummyMatrix gemm(Node master_node, short mtype,
                                        long mptr1, long mptr2,
                                        boolean isTransM1, boolean isTransM2, 
                                        double alpha, double beta);
  public static native void geadd(Node master_node, short mtype,
                                  long mptr1, long mptr2,
                                  boolean isTrans, 
                                  double alpha, double beta);
  
  // [sca]lapack results
  public static native void saveAsFrovedisDiagMatrixLocal(Node master_node, 
                                                        long dptr, String path,
                                                        boolean isbinary);   //SVAL
  public static native double[] getDoubleArray(Node master_node, long dptr); //SVAL
  public static native void releaseDoubleArray(Node master_node, long dptr); //SVAL
  public static native void releaseIPIV(Node master_node, short mtype, long dptr);
 
  // [sca]lapack routines  
  public static native DummyGetrfResult getrf(Node master_node, short mtype,
                                              long mptr);
  public static native int getri(Node master_node, short mtype,
                                 long mptr, long ipiv_ptr);
  public static native int getrs(Node master_node, short mtype,
                                 long mptrA, long mptrB, 
                                 long ipiv_ptr, boolean isTrans);
  public static native int gesv(Node master_node, short mtype,
                                long mptrA, long mptrB);
  public static native int gels(Node master_node, short mtype,
                                long mptrA, long mptrB, 
                                boolean isTrans);
  public static native DummyGesvdResult gesvd(Node master_node, short mtype,
                                              long mptr, 
                                              boolean wantU, boolean wantV);
  // Dvector and DataFrame
  public static native long loadFrovedisWorkerIntVector(Node t_node, long size,
                                                      int data[]);
  public static native long loadFrovedisWorkerLongVector(Node t_node, long size,
                                                       long data[]);
  public static native long loadFrovedisWorkerFloatVector(Node t_node, long size,
                                                        float data[]);
  public static native long loadFrovedisWorkerDoubleVector(Node t_node, long size,
                                                         double data[]);
  public static native long loadFrovedisWorkerStringVector(Node t_node, long size,
                                                         String data[]);
  public static native long loadFrovedisWorkerBoolVector(Node t_node, long size,
                                                       boolean data[]);
  public static native long createFrovedisDvector(Node master_node, long proxies[],
                                                long sizes[], long size, short dtype);
  public static native double[] getUniqueDvectorElements(Node master_node,
                                                         long dptr);
  public static native long getZeroBasedEncodedDvector(Node master_node, 
                                                       long dptr);
  public static native long getEncodedDvectorAs(Node master_node, 
                                                long dptr,
                                                double uniq_lbl[],
                                                double encoded_as[],
                                                int uniq_cnt);
  public static native void releaseFrovedisDvector(Node master_node, long dptr);

  // frovedis dataframe column extraction
  public static native long[] getLocalIntColumnPointers(Node master_node,
                                                       long proxy, String cname);
  public static native int[] getLocalIntVector(Node wnode, long dptr);

  public static native long[] getLocalLongColumnPointers(Node master_node,
                                                        long proxy, String cname);
  public static native long[] getLocalLongVector(Node wnode, long dptr);

  public static native long[] getLocalFloatColumnPointers(Node master_node,
                                                         long proxy, String cname);
  public static native float[] getLocalFloatVector(Node wnode, long dptr);

  public static native long[] getLocalDoubleColumnPointers(Node master_node,
                                                          long proxy, String cname);
  public static native double[] getLocalDoubleVector(Node wnode, long dptr);

  public static native long[] getLocalStringColumnPointers(Node master_node,
                                                           long proxy, String cname);
  public static native String[] getLocalStringVector(Node wnode, long dptr);

  public static native long createFrovedisDataframe(Node master_node,
                                                  short dtypes[],
                                                  String cols_names[],
                                                  long dvecs[], long size);
  public static native void releaseFrovedisDataframe(Node master_node, long data);

  public static native void showFrovedisDataframe(Node master_node, long data);

  public static native long getDFOperator(Node master_node, String op1, String op2,
                                          short tid, short optid, boolean isImmed);
  public static native long getDFAndOperator(Node master_node,
                                             long proxy1, long proxy2);
  public static native long getDFOrOperator(Node master_node,
                                            long proxy1, long proxy2);
  public static native long getDFNotOperator(Node master_node,
                                            long proxy);
  public static native void releaseFrovedisDFOperator(Node master_node, long proxy);
  public static native long filterFrovedisDataframe(Node master_node,
                                                  long data_proxy, long opt_proxy);
  public static native long joinFrovedisDataframes(Node master_node,
                                                 long dproxy1, long dproxy2,
                                                 long opt_proxy, String type,
                                                 String algo);
  public static native long sortFrovedisDataframe(Node master_node,
                                                long dproxy, String targets[],
                                                int[] isDesc, long size);
  public static native long selectFrovedisDataframe(Node master_node,
                                                  long dproxy,
                                                  String targets[], long size);
  public static native long groupFrovedisDataframe(Node master_node,
                                                 long dproxy, String targets[],
                                                 long size);
  public static native long renameFrovedisDataframe(Node master_node,
                                                    long dproxy,
                                                    String[] name, String[] new_name,
                                                    int size);
  public static native long getFrovedisDFSize(Node master_node, long dproxy);
  public static native String[] getFrovedisDFCounts(Node master_node,
                                                    long dproxy, String[] cname,
                                                    int size);
  public static native String[] getFrovedisDFMeans(Node master_node,
                                                   long dproxy, String[] cname,
                                                   int size);
  public static native String[] getFrovedisDFTotals(Node master_node,
                                                    long dproxy, String[] cname,
                                                    short[] tids,
                                                    int size);
  public static native String[] getFrovedisDFMins(Node master_node,
                                                  long dproxy, String[] cname,
                                                  short[] tids,
                                                  int size);
  public static native String[] getFrovedisDFMaxs(Node master_node,
                                                  long dproxy, String[] cname,
                                                  short[] tids,
                                                  int size);
  public static native String[] getFrovedisDFStds(Node master_node,
                                                  long dproxy, String[] cname,
                                                  short[] tids,
                                                  int size);
  public static native long selectFrovedisGroupedData(Node master_node,
                                                  long dproxy,
                                                  String[] cols, int sz);
  public static native long aggrFrovedisDataframe(Node master_node,
                                                  long dproxy,
                                                  String[] gCols, int sz1,
                                                  String[] aFuns, String[] aCols,
                                                  String[] aAsCols, int sz2);
  public static native void releaseFrovedisGroupedDF(Node master_node, long data);
  public static native DummyMatrix DFToRowmajorMatrix(Node master_node, long dproxy, 
                                                      String[] cname, int size); 
  public static native DummyMatrix DFToColmajorMatrix(Node master_node, long dproxy, 
                                                      String[] cname, int size); 
  public static native DummyMatrix DFToCRSMatrix(Node master_node, long dproxy, 
                                                 String[] cname, int size1,
                                                 String[] cat_name, int size2,
                                                 long info_id); 
  public static native DummyMatrix DFToCRSMatrixUsingInfo(Node master_node, long dproxy, 
                                                          long info_id);
  public static native long getCrossDfopt(Node master_node);

  // --- dftable_to_sparse_info ---
  public static native void loadSparseConversionInfo(Node master_node,long info_id,String dirname);
  public static native void saveSparseConversionInfo(Node master_node,long info_id,String dirname);
  public static native void releaseSparseConversionInfo(Node master_node,long info_id);

  // --- Graphx ---
  public static native DummyGraph loadGraphFromTextFile(Node master_node, 
                                                        String fname);
  public static native void saveGraph(Node master_node, long dptr, 
                                      String fname);
  public static native long copyGraph(Node master_node, long dptr);
  public static native void showGraph(Node master_node, long dptr);
  public static native void releaseGraph(Node master_node, long dptr);
  public static native long setGraphData(Node master_node, long dptr);
  public static native DummyEdge[] getGraphEdgeData(Node master_node, long dptr);
  public static native double[] getGraphVertexData(Node master_node, long dptr);
  public static native DummyGraph callFrovedisPageRank(Node master_node, 
                                                       long dptr,
                                                       double epsilon, 
                                                       double dfactor,
                                                       int maxIter);
  public static native sssp_result callFrovedisSSSP(Node master_node,
                                             long dptr,
                                             long source_vertex);
  public static native bfs_result callFrovedisBFS(Node master_node,
                                             long dptr,
                                             long source_vertex,
                                             int opt_level,
                                             double hyb_threshold,
                                             long depth_limit);
  public static native long[] callFrovedisCC(Node master_node,
                                             long dptr,
                                             long[] nodes_in_which_cc,
                                             long[] dist,
                                             long numVertices,
                                             int opt_level,
                                             double hyb_threshold);
  // --- LDA ---
  public static native DummyLDAModel callFrovedisLDA(Node master_node,
					    long fdata, long[] doc_id,
                                            long num_docs, int mid, 
					    int num_topics, int num_iter, 
					    double alpha, double beta,
					    int num_explore_iter, 
					    int num_eval_cycle, String algo);
  public static native DummyLDAResult callFrovedisLDATransform(Node master_node,
					    long fdata, int mid, 
					    int num_iter, 
					    double alpha, double beta,
					    int num_explore_iter, 
					    String algo);
  public static native DummyMatrix getTopicsMatrix(Node master_node, int mid);
  public static native DummyMatrix getTopicWordDistribution(Node master_node, int mid);
  public static native void extractTopWordsPerTopic(Node master_node, 
                                                    long fdata,
                                                    int num_topics,
                                                    int maxTermsPerTopic, 
                                                    int[] word_id,
                                                    double[] topic_word_dist);
  public static native DummyMatrix getTopicDocDistribution(Node master_node, int mid);
  public static native void extractTopDocsPerTopic(Node master_node, 
                                                   int mid, 
                                                   long fdata,
                                                   int num_topics,
                                                   int maxDocumentsPerTopic, 
                                                   long[] doc_id,
                                                   double[] topic_doc_dist);
  public static native DummyMatrix getDocTopicDistribution(Node master_node, int mid);
  public static native long[] getLDAModelDocIds(Node master_node, int mid);
  public static native void extractTopTopicsPerDoc(Node master_node, 
                                                   long fdata,
                                                   int num_docs,
                                                   int max_topic, 
                                                   int[] topic_id,
                                                   double[] doc_topic_dist);
  public static native void transformAndExtractTopTopicsPerDoc(Node master_node,
					                       long fdata, int mid, 
                                                               int num_iter, 
					                       double alpha, 
                                                               double beta,
					                       int num_explore_iter, 
					                       String algo,
                                                               int num_docs,
                                                               int max_topic,
                                                               int[] topic_id,
                                                               double[] doc_topic_dist);
  public static native void transformAndExtractTopDocsPerTopic(Node master_node,
					                       long fdata, 
                                                               long[] test_doc_id,
                                                               long num_docs, 
                                                               int mid, 
                                                               int num_iter, 
					                       double alpha, 
                                                               double beta,
					                       int num_explore_iter, 
					                       String algo,
                                                               int num_topics,
                                                               int maxDocumentsPerTopic,
                                                               long[] doc_id,
                                                               double[] topic_doc_dist);
  public static native DummyLDAModel loadFrovedisLDAModel(Node master_node,
                                                          int model_Id, 
                                                          String path);

   // --- to_spark_sparse_matrix() support
  public static native long[] getAllSparseMatrixLocalPointers(Node master_node,
                                                              long dptr, 
                                                              short mtype);
  public static native int[] getAllSparseMatrixLocalRows(Node master_node, 
                                                          long dptr, 
                                                          short mtype);
  public static native int[] getAllSparseMatrixLocalNNZ(Node master_node, 
                                                         long dptr, 
                                                         short mtype);
  public static native void getLocalCRSMatrixComponents(Node master_node, 
                                                          long dptr, 
                                                          double[] data, 
                                                          int[] index, 
                                                          int[] offset, 
                                                          int nrow, 
                                                          int nnz);

  // ---Random Forest ---
  public static native void callFrovedisRF(Node master_node,
                                        MemPair fdata,
                                        String Algo ,
                                        int maxDepth,
                                        double min_info_gain,
                                        int num_classes,
                                        int max_bins,
                                        double subsampling_rate,
                                        String impurityType,
                                        int num_trees,
                                        String feature_subset_strategy,
                                        long seed,
                                        int keys[],
                                        int values[], int size,
                                        int model_id,
                                        boolean movable,
                                        boolean isDense);

  public static native int rfNumTrees(Node master_node,
                                      int mid);

  public static native int rfTotalNumNodes(Node master_node,
                                           int mid);

  public static native String rfToString(Node master_node,
                                          int mid);

  // ---------------------GBT----------------------------------  
  public static native void callFrovedisGbtFit(Node master_node,
                                              MemPair fdata,
                                              String algo,
                                              String loss,
                                              String impurity,
                                              double learning_rate,
                                              int max_depth,
                                              double min_info_gain,
                                              int random_state,
                                              double tol,
                                              int max_bins,
                                              double subsampling_rate,
                                              String feature_subset_strategy,
                                              int n_estimators,
                                              int nclasses,
                                              int keys[],
                                              int values[],
                                              int size,
                                              int mid,
                                              boolean movable,
                                              boolean dense);
  
  public static native int gbtNumTrees(Node master_node,
                                      int mid);

  public static native int gbtTotalNumNodes(Node master_node,
                                           int mid);

  public static native double[] gbtTreeWeights(Node master_node,
                                              int mid);

  public static native String gbtToString(Node master_node,
                                          int mid);
  
  // Decision Tree
  public static native void callFrovedisDT(Node master_node,
                                          MemPair fdata,
                                          String Algo ,
                                          int maxDepth,
                                          int num_classes,
                                          int max_bins,
                                          String quantile_strategy,
                                          double min_info_gain,
                                          int min_instance_per_node,
                                          String impurityType,
                                          int keys[],
                                          int values[], int size,
                                          int model_id,
                                          boolean movable,
                                          boolean isDense);  

}
