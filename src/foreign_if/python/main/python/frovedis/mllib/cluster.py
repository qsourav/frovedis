"""
cluster.py: module containing wrapper for kmeans, dbscan, agglomerative
            and spectral clustering
"""
#!/usr/bin/env python

import os.path
import pickle
import numpy as np
import numbers
from .model_util import *
from ..base import *
from ..exrpc.server import FrovedisServer
from ..exrpc import rpclib
from ..matrix.ml_data import FrovedisFeatureData
from ..matrix.dense import FrovedisRowmajorMatrix
from ..matrix.dtype import TypeUtil

def clustering_score(labels_true, labels_pred):
  try:
      from sklearn.metrics.cluster import homogeneity_score
      return homogeneity_score(labels_true, labels_pred)
  except: #for system without sklearn
      raise AttributeError("score: needs scikit-learn to use this method!")

class KMeans(BaseEstimator):
    """
    A python wrapper of Frovedis kmeans
    """
    def __init__(self, n_clusters=8, init='random', n_init=10,
                 max_iter=300, tol=1e-4, precompute_distances='auto',
                 verbose=0, random_state=None, copy_x=True,
                 n_jobs=1, algorithm='auto', use_shrink=False):
        self.n_clusters = n_clusters
        self.init = init
        self.max_iter = max_iter
        self.tol = tol
        self.precompute_distances = precompute_distances
        self.n_init = n_init
        self.verbose = verbose
        self.random_state = random_state
        self.copy_x = copy_x
        self.n_jobs = n_jobs
        self.algorithm = algorithm
        #extra
        self.use_shrink = use_shrink
        self.__mid = None
        self.__mdtype = None
        self.__mkind = M_KIND.KMEANS
        self._cluster_centers = None

    def validate(self):
        """validates hyper parameters"""
        if self.n_init is None:
            self.n_init = 10
        if self.n_init < 1:
            raise ValueError("fit: n_init must be a positive integer!")

        if isinstance(self.random_state, numbers.Number):
            if sys.version_info[0] < 3:
                self.seed = long(self.random_state)
            else:
                self.seed = int(self.random_state)
        else:
            self.seed = 0

        supported_init = ['random']
        if self.init not in supported_init:
            raise ValueError("fit: frovedis currently doesn't support " +
                             "init = %s" % self.init)

        if self.algorithm == 'auto':
            self.algorithm = 'full'
        supported_algorithm = ['full']
        if self.algorithm not in supported_algorithm:
            raise ValueError("fit: frovedis currently doesn't support " +
                             "algorithm = %s" % self.algorithm)

    def check_input(self, X, F):
        """checks input X"""
        # if X is not a sparse data, it would be loaded as rowmajor matrix
        inp_data = FrovedisFeatureData(X, \
                   caller = "[" + self.__class__.__name__ + "] " + F + ": ",\
                   dense_kind='rowmajor', densify=False)
        X = inp_data.get()
        dtype = inp_data.get_dtype()
        itype = inp_data.get_itype()
        dense = inp_data.is_dense()
        nsamples = inp_data.numRows()
        nfeatures = inp_data.numCols()
        movable = inp_data.is_movable()
        if dense and self.use_shrink:
            raise ValueError(F + ": use_shrink is applicable only for " \
                             + "sparse data!")
        if self.n_clusters is None:
            self.n_clusters = min(8, nsamples)
        if self.n_clusters < 1 or self.n_clusters > nsamples:
            raise ValueError("fit: n_samples=%d must be >= n_clusters=%d." % \
                              (nsamples, self.n_clusters))
        return X, dtype, itype, dense, nsamples, nfeatures, movable

    def fit(self, X, y=None, sample_weight=None):
        """Compute k-means clustering."""
        self.release()
        self.validate()
        X, dtype, itype, dense, \
        nsamples, nfeatures, movable = self.check_input(X, "fit")
        self.n_samples = nsamples
        self.n_features = nfeatures
        self.__mdtype = dtype
        self.__mid = ModelID.get()
        (host, port) = FrovedisServer.getServerInstance()
        ret = rpclib.kmeans_fit(host, port, X.get(), self.n_clusters,\
                            self.max_iter, self.n_init, \
                            self.tol, self.seed, self.verbose, \
                            self.__mid, dtype, itype, dense, self.use_shrink)
        excpt = rpclib.check_server_exception()
        if excpt["status"]:
            raise RuntimeError(excpt["info"])
        self.labels_ = np.asarray(ret["labels"])
        self.inertia_ = ret["inertia"]
        self.n_iter_ = ret["n_iter"]
        self.n_clusters_ = ret["n_clusters"]
        return self

    def fit_predict(self, X, y=None, sample_weight=None):
        """
        computes cluster centers and predicts cluster index for each sample.
        """
        self.fit(X, y, sample_weight)
        return self.labels_

    def fit_transform(self, X, y=None, sample_weight=None):
        """
        computes clustering and transform X to cluster-distance space.
        """
        self.release()
        self.validate()
        X, dtype, itype, dense, \
        nsamples, nfeatures, movable = self.check_input(X, "fit_transform")
        self.n_samples = nsamples
        self.n_features = nfeatures
        self.__mdtype = dtype
        self.__mid = ModelID.get()
        (host, port) = FrovedisServer.getServerInstance()
        ret = rpclib.kmeans_fit_transform(host, port, X.get(), \
                            self.n_clusters, \
                            self.max_iter, self.n_init, \
                            self.tol, self.seed, self.verbose, \
                            self.__mid, dtype, itype, dense, self.use_shrink)
        excpt = rpclib.check_server_exception()
        if excpt["status"]:
            raise RuntimeError(excpt["info"])
        self.labels_ = np.asarray(ret["labels"])
        self.inertia_ = ret["inertia"]
        self.n_iter_ = ret["n_iter"]
        self.n_clusters_ = ret["n_clusters"]
        trans_mat = {'dptr': ret["mptr"],
                     'nrow': ret["n_samples"],
                     'ncol': ret["n_clusters"]}
        ret = FrovedisRowmajorMatrix(mat=trans_mat, \
                                     dtype=TypeUtil.to_numpy_dtype(dtype))
        if movable:
            return ret.to_numpy_array()
        else:
            return ret

    def transform(self, X):
        """transforms X to a cluster-distance space."""
        if self.__mid is None:
            raise ValueError( \
            "transform: is called before calling fit, or the model is released.")
        X, dtype, itype, dense, \
        nsamples, nfeatures, movable = self.check_input(X, "transform")
        if dtype != self.__mdtype:
            raise TypeError( \
            "transform: datatype of X is different than model dtype!")
        (host, port) = FrovedisServer.getServerInstance()
        trans_mat = rpclib.kmeans_transform(host, port, \
                            self.__mid, self.__mdtype, \
                            X.get(), itype, dense)
        excpt = rpclib.check_server_exception()
        if excpt["status"]:
            raise RuntimeError(excpt["info"])
        ret = FrovedisRowmajorMatrix(mat=trans_mat, \
                                     dtype=TypeUtil.to_numpy_dtype(dtype))
        if movable:
            return ret.to_numpy_array()
        else:
            return ret

    def predict(self, X, sample_weight=None):
        """Predict the closest cluster each sample in X belongs to."""
        if self.__mid is None:
            raise ValueError( \
            "predict: is called before calling fit, or the model is released.")
        X, dtype, itype, dense, \
        nsamples, nfeatures, movable = self.check_input(X, "predict")
        if dtype != self.__mdtype:
            raise TypeError( \
            "predict: datatype of X is different than model dtype!")
        (host, port) = FrovedisServer.getServerInstance()
        len_l = X.numRows()
        ret = np.zeros(len_l, dtype=np.int32)
        rpclib.parallel_kmeans_predict(host, port, self.__mid,
                                       self.__mdtype, X.get(),
                                       ret, len_l, itype, dense)
        excpt = rpclib.check_server_exception()
        if excpt["status"]:
            raise RuntimeError(excpt["info"])
        return ret

    def score(self, X, y=None, sample_weight=None):
        """Opposite of the value of X on the K-means objective."""
        if self.__mid is None:
            raise ValueError( \
            "score: is called before calling fit, or the model is released.")
        X, dtype, itype, dense, \
        nsamples, nfeatures, movable = self.check_input(X, "score")
        if dtype != self.__mdtype:
            raise TypeError( \
            "Input test data dtype is different than model dtype!")
        (host, port) = FrovedisServer.getServerInstance()
        ret = rpclib.kmeans_score(host, port, \
                                  self.__mid, self.__mdtype, \
                                  X.get(), itype, dense)
        excpt = rpclib.check_server_exception()
        if excpt["status"]:
            raise RuntimeError(excpt["info"])
        return ret

    @property
    def cluster_centers_(self):
        """returns centroid points"""
        if self.__mid is None:
            raise ValueError("cluster_centers_: is called before fit/load")

        if self._cluster_centers is None:
            (host, port) = FrovedisServer.getServerInstance()
            center = rpclib.get_kmeans_centroid(host, port, self.__mid, \
                                                self.__mdtype)
            excpt = rpclib.check_server_exception()
            if excpt["status"]:
                raise RuntimeError(excpt["info"])
            center = np.asarray(center)
            self._cluster_centers = center.reshape(self.n_clusters_, \
                                                   self.n_features)
        return self._cluster_centers

    @cluster_centers_.setter
    def cluster_centers_(self, val):
        """Setter method for cluster_centers_ """
        raise AttributeError(\
        "attribute 'cluster_centers_' of KMeans is not writable")

    def load(self, fname, dtype=None):
        """
        NAME: load
        """
        if not os.path.exists(fname):
            raise ValueError(\
                "the model with name %s does not exist!" % fname)
        self.release()
        metadata = open(fname+"/metadata", "rb")
        self.n_clusters_, self.n_features, \
        self.__mkind, self.__mdtype = pickle.load(metadata)
        metadata.close()
        if dtype is not None:
            mdt = TypeUtil.to_numpy_dtype(self.__mdtype)
            if dtype != mdt:
                raise ValueError("load: type mismatches detected!" + \
                                 "expected type: " + str(mdt) + \
                                 "; given type: " + str(dtype))
        self.__mid = ModelID.get()
        GLM.load(self.__mid, self.__mkind, self.__mdtype, fname+"/model")
        return self

    def save(self, fname):
        """
        NAME: save
        """
        if self.__mid is None:
            raise ValueError(\
            "save: is called before calling fit, or the model is released!")
        if os.path.exists(fname):
            raise ValueError(\
            "another model with %s name already exists!" % fname)
        else:
            os.makedirs(fname)
        GLM.save(self.__mid, self.__mkind, self.__mdtype, fname+"/model")
        metadata = open(fname+"/metadata", "wb")
        pickle.dump((self.n_clusters_, self.n_features, \
                     self.__mkind, self.__mdtype), metadata)
        metadata.close()

    def debug_print(self):
        """
        NAME: debug_print
        """
        if self.__mid is not None:
            GLM.debug_print(self.__mid, self.__mkind, self.__mdtype)

    def release(self):
        """
        NAME: release
        """
        if self.__mid is not None:
            GLM.release(self.__mid, self.__mkind, self.__mdtype)
            self.__mid = None
            self._cluster_centers = None
            self.labels_ = None
            self.inertia_ = None
            self.n_iter_ = None
            self.n_clusters_ = None

    def __del__(self):
        """
        NAME: __del__
        """
        if FrovedisServer.isUP():
            self.release()

class SpectralClustering(BaseEstimator):
    """
    A python wrapper of Frovedis Spectral clustering
    """
    def __init__(self, n_clusters=8, eigen_solver=None, n_components=None, 
                 random_state=None, n_init=10, gamma=1.0, affinity='rbf', 
                 n_neighbors=10, eigen_tol=0.0, assign_labels='kmeans', 
                 degree=3, coef0=1, kernel_params=None, n_jobs=None, 
                 verbose=0, n_iter=100, eps=0.01, norm_laplacian=True, 
                 mode=1, drop_first=False):
        self.n_clusters = n_clusters
        self.eigen_solver = eigen_solver
        self.n_components = n_clusters if n_components is None else n_components
        self.random_state = random_state
        self.n_init = n_init
        self.gamma = gamma
        self.affinity = affinity
        self.n_neighbors = n_neighbors
        self.eigen_tol = eigen_tol
        self.assign_labels = assign_labels
        self.degree = degree
        self.coef0 = coef0
        self.kernel_params = kernel_params
        self.n_jobs = n_jobs
        self.verbose = verbose
        #extra
        self.__mid = None
        self.__mdtype = None
        self.__mkind = M_KIND.SCM
        self.n_iter = n_iter
        self.eps = eps
        self.norm_laplacian = norm_laplacian
        self.mode = mode
        self.drop_first = drop_first
        self.labels_ = None

    def fit(self, X, y=None):
        """
        NAME: fit
        """
        self.release()
        # if X is not a sparse data, it would be loaded as rowmajor matrix
        inp_data = FrovedisFeatureData(X, \
                     caller = "[" + self.__class__.__name__ + "] fit: ",\
                     dense_kind='rowmajor', densify=True)
        X = inp_data.get()
        dtype = inp_data.get_dtype()
        itype = inp_data.get_itype()
        dense = inp_data.is_dense()
        if self.affinity == "precomputed":
            precomputed = True
        else:
            precomputed = False
        self.__mid = ModelID.get()
        self.__mdtype = dtype
        self.__X_movable = inp_data.is_movable()
        len_l = X.numRows()
        (host, port) = FrovedisServer.getServerInstance()
        ret = np.zeros(len_l, dtype=np.int32)
        rpclib.sca_train(host, port, X.get(), self.n_clusters, self.n_components,
                         self.n_iter, self.eps, self.gamma,
                         precomputed, self.norm_laplacian, self.mode,
                         self.drop_first, ret, len_l, self.verbose, self.__mid,
                         dtype, itype, dense)
        self.labels_ = ret
        self._affinity = None
        excpt = rpclib.check_server_exception()
        if excpt["status"]:
            raise RuntimeError(excpt["info"])
        return self

    def fit_predict(self, X, y=None):
        """
        NAME: fit_predict
        """
        self.fit(X, y)
        return self.labels_

    def score(self, X, y, sample_weight=None):
        """uses scikit-learn homogeneity_score for scoring"""
        if self.__mid is not None:
            return clustering_score(y, self.fit_predict(X, y))

    def __str__(self):
        """
        NAME: __str__
        """
        return str(self.get_params())

    def load(self, fname, dtype=None):
        """
        NAME: load
        """
        if isinstance(fname, str) == False:
            raise TypeError("Expected: String, Got: " + str(type(fname)))
        if not os.path.exists(fname):
            raise ValueError(\
                "the model with name %s does not exist!" % fname)
        self.release()
        metadata = open(fname+"/metadata", "rb")
        self.n_clusters, self.n_components, self.__mkind, self.__mdtype = \
            pickle.load(metadata)
        metadata.close()
        if dtype is not None:
            mdt = TypeUtil.to_numpy_dtype(self.__mdtype)
            if dtype != mdt:
                raise ValueError("load: type mismatches detected!" + \
                                 "expected type: " + str(mdt) + \
                                 "; given type: " + str(dtype))
        self.__mid = ModelID.get()
        (host, port) = FrovedisServer.getServerInstance()
        model_file = fname + "/model"
        self.labels_ = rpclib.load_frovedis_scm(host, port, self.__mid, \
                        self.__mdtype, model_file.encode('ascii'))
        excpt = rpclib.check_server_exception()
        if excpt["status"]:
            raise RuntimeError(excpt["info"])
        return self

    @property
    def affinity_matrix_(self):
        """
        NAME: get_affinity_matrix
        """
        if self.__mid is None:
            raise ValueError("affinity_matrix_ is called before fit/load")

        if self._affinity is None:
            (host, port) = FrovedisServer.getServerInstance()
            dmat = rpclib.get_scm_affinity_matrix(host, port, self.__mid, \
                                                  self.__mdtype)
            excpt = rpclib.check_server_exception()
            if excpt["status"]:
                raise RuntimeError(excpt["info"])
            rmat = FrovedisRowmajorMatrix(mat=dmat, dtype=TypeUtil. \
                        to_numpy_dtype(self.__mdtype))
            if self.__X_movable:
                self._affinity = rmat.to_numpy_array()
            else:
                self._affinity = rmat
        return self._affinity

    @affinity_matrix_.setter
    def affinity_matrix_(self, val):
        """Setter method for affinity_matrix_ """
        raise AttributeError(\
        "attribute 'affinity_matrix_' of SpectralClustering is not writable")

    def save(self, fname):
        """
        NAME: save
        """
        if self.__mid is not None:
            if os.path.exists(fname):
                raise ValueError(\
                    "another model with %s name already exists!" % fname)
            else:
                os.makedirs(fname)
            GLM.save(self.__mid, self.__mkind, self.__mdtype, fname+"/model")
            metadata = open(fname+"/metadata", "wb")
            pickle.dump((self.n_clusters, self.n_components, \
                self.__mkind, self.__mdtype), metadata)
            metadata.close()
        else:
            raise ValueError(\
                "save: the requested model might have been released!")

    def debug_print(self):
        """
        NAME: debug_print
        """
        if self.__mid is not None:
            GLM.debug_print(self.__mid, self.__mkind, self.__mdtype)

    def release(self):
        """
        NAME: release
        """
        if self.__mid is not None:
            GLM.release(self.__mid, self.__mkind, self.__mdtype)
            self.__mid = None
            self.labels_ = None
            self._affinity = None

    def __del__(self):
        """
        NAME: __del__
        """
        if FrovedisServer.isUP():
            self.release()

class AgglomerativeClustering(BaseEstimator):
    """
    A python wrapper of Frovedis Agglomerative Clustering
    """
    def __init__(self, n_clusters=2, affinity='euclidean', memory=None,
                 connectivity=None, compute_full_tree='auto',
                 linkage='average', distance_threshold=None,
                 compute_distances=False, verbose=0):
        self.n_clusters = n_clusters
        self.affinity = affinity
        self.memory = memory
        self.connectivity = connectivity
        self.compute_full_tree = compute_full_tree
        self.linkage = linkage
        self.verbose = verbose
        self.threshold = distance_threshold
        # extra
        self.__mid = None
        self.__mdtype = None
        self.__mkind = M_KIND.ACM
        self.labels_ = None
        self.n_samples = None
        self.n_features = None
        self.movable = None

    def check_input(self, X, F):
        inp_data = FrovedisFeatureData(X, \
                     caller = "[" + self.__class__.__name__ + "] "+ F +": ",\
                     dense_kind='rowmajor', densify=True)
        X = inp_data.get()        
        dtype = inp_data.get_dtype()
        itype = inp_data.get_itype()
        dense = inp_data.is_dense()
        nsamples = inp_data.numRows()
        nfeatures = inp_data.numCols()
        movable = inp_data.is_movable()
        return X, dtype, itype, dense, nsamples, nfeatures, movable    
        
    def validate(self):
        supported_linkages = {'average', 'complete', 'single'}
        if self.linkage not in supported_linkages:
            raise ValueError("linkage: Frovedis doesn't support the "\
                              + "given linkage!")
        if self.threshold is None:
            self.threshold = 0.0
        
    def fit(self, X, y=None):
        """
        NAME: fit
        """
        self.release()
        self.validate()
        X, dtype, itype, dense, nsamples, \
        nfeatures, movable = self.check_input(X, "fit")
        self.n_samples = nsamples
        self.n_features = nfeatures
        self.movable = movable
        self.__mid = ModelID.get()
        self.__mdtype = dtype
        (host, port) = FrovedisServer.getServerInstance()
        ret = np.zeros(nsamples, dtype=np.int64)
        rpclib.aca_train(host, port, X.get(), self.n_clusters,
                         self.linkage.encode('ascii'), 
                         self.threshold, ret, nsamples, 
                         self.verbose, self.__mid,
                         dtype, itype, dense)
        self.labels_ = ret#.astype(np.int64)
        self._children = None
        self._n_connected_components = None
        self._distances = None
        self._n_clusters = None
        self.n_leaves_ = self.n_samples
        excpt = rpclib.check_server_exception()
        if excpt["status"]:
            raise RuntimeError(excpt["info"])
        return self

    @property
    def children_(self):
        """
        NAME: get_children
        """
        if self.__mid is None:
            raise ValueError("children_ is called before fit/load")

        if self._children is None:
            (host, port) = FrovedisServer.getServerInstance()
            children_vector = rpclib.get_acm_children(host, port, self.__mid, \
                                                  self.__mdtype)
            excpt = rpclib.check_server_exception()
            if excpt["status"]:
                raise RuntimeError(excpt["info"])
  
            nchildren = len(children_vector) // 2
            shape = (nchildren, 2)
            # sklearn returns children as int32, 
            # where it is a candidate to be int64
            self._children = np.asarray(children_vector)\
                               .reshape(shape) #.astype(np.int32)
        return self._children

    @children_.setter
    def children_(self, val):
        """Setter method for children_ """
        raise AttributeError(\
        "attribute 'children_' of AgglomerativeClustering is not writable")

    @property
    def n_connected_components_(self):
        """
        NAME: get_n_connected_components
        """
        if self.__mid is None:
            raise ValueError("n_connected_components_ is called before fit/load")

        if self._n_connected_components is None:
            (host, port) = FrovedisServer.getServerInstance()
            ncc = rpclib.get_acm_n_components(host, port, self.__mid, \
                                                  self.__mdtype)
            excpt = rpclib.check_server_exception()
            if excpt["status"]:
                raise RuntimeError(excpt["info"])            
            self._n_connected_components = ncc            
        return self._n_connected_components

    @n_connected_components_.setter
    def n_connected_components_(self, val):
        """Setter method for n_connected_components_ """
        raise AttributeError(\
        "attribute 'n_connected_components_' of AgglomerativeClustering is not writable")

    @property
    def distances_(self):
        """
        NAME: get_distances
        """
        if self.__mid is None:
            raise ValueError("children_ is called before fit/load")

        if self._distances is None:
            (host, port) = FrovedisServer.getServerInstance()
            dist = rpclib.get_acm_distances(host, port, self.__mid, \
                                                  self.__mdtype)
            excpt = rpclib.check_server_exception()
            if excpt["status"]:
                raise RuntimeError(excpt["info"])             
            self._distances = np.asarray(dist, dtype=np.float64) #sklearn return type is float64            
        return self._distances

    @distances_.setter
    def distances_(self, val):
        """Setter method for distances_ """
        raise AttributeError(\
        "attribute 'distances_' of AgglomerativeClustering is not writable")        

    @property
    def n_clusters_(self):
        """
        NAME: get_n_clusters
        """
        if self.__mid is None:
            raise ValueError("n_clusters_ is called before fit/load")

        if self._n_clusters is None:
            (host, port) = FrovedisServer.getServerInstance()
            nclusters = rpclib.get_acm_n_clusters(host, port, self.__mid, \
                                                  self.__mdtype)
            excpt = rpclib.check_server_exception()
            if excpt["status"]:
                raise RuntimeError(excpt["info"])                
            self._n_clusters = nclusters            
        return self._n_clusters

    @n_clusters_.setter
    def n_clusters_(self, val):
        """Setter method for distances_ """
        raise AttributeError(\
        "attribute 'n_clusters_' of AgglomerativeClustering is not writable") 


    def fit_predict(self, X, y=None):
        """
        NAME: fit_predict
        """
        self.fit(X, y)
        return self.labels_

    # added for predicting with different nclusters on same model
    def reassign(self, ncluster=None):
        """
        recomputes cluster indices when input 'ncluster' is different 
        than self.n_clusters
        """
        if ncluster is not None and ncluster != self.n_clusters:
            if (ncluster <= 0): 
                raise ValueError("predict: ncluster must be a positive integer!")
            self.n_clusters = ncluster
            (host, port) = FrovedisServer.getServerInstance()
            ret = np.zeros(self.n_samples, dtype=np.int64)
            rpclib.acm_predict(host, port, self.__mid, self.__mdtype,
                               self.n_clusters, ret, self.n_samples)
            excpt = rpclib.check_server_exception()
            if excpt["status"]:
                raise RuntimeError(excpt["info"])
            self.labels_ = ret#.astype(np.int64)
        return self.labels_

    
    def score(self, X, y, sample_weight=None):
        """uses scikit-learn homogeneity_score for scoring"""
        if self.__mid is not None:
            return clustering_score(y, self.fit_predict(X, y))


    def __str__(self):
        """
        NAME: __str__
        """
        return str(self.get_params())
   
    def load(self, fname, dtype=None):
        """
        NAME: load
        """
        if isinstance(fname, str) == False:
            raise TypeError("Expected: String, Got: " + str(type(fname)))
        if not os.path.exists(fname):
            raise ValueError(\
                "the model with name %s does not exist!" % fname)
        self.release()
        metadata = open(fname+"/metadata", "rb")
        self.n_clusters, self.n_samples, self.__mkind,\
                         self.__mdtype = pickle.load(metadata)
        metadata.close()
        if dtype is not None:
            mdt = TypeUtil.to_numpy_dtype(self.__mdtype)
            if dtype != mdt:
                raise ValueError("load: type mismatches detected!" + \
                                 "expected type: " + str(mdt) + \
                                 "; given type: " + str(dtype))
        self.__mid = ModelID.get()
        (host, port) = FrovedisServer.getServerInstance()
        model_file = fname + "/model"
        # get labels
        ret = np.zeros(self.n_samples, dtype=np.int64)
        rpclib.load_frovedis_acm(host, port, self.__mid, \
                                 self.__mdtype, model_file.encode('ascii'),\
                                 ret, self.n_samples)
        excpt = rpclib.check_server_exception()
        if excpt["status"]:
            raise RuntimeError(excpt["info"])
        self.labels_ = ret#.astype(np.int64)
        return self

    def save(self, fname):
        """
        NAME: save
        """
        if self.__mid is not None:
            if os.path.exists(fname):
                raise ValueError(\
                    "another model with %s name already exists!" % fname)
            else:
                os.makedirs(fname)
            GLM.save(self.__mid, self.__mkind, self.__mdtype, fname+"/model")
            metadata = open(fname+"/metadata", "wb")
            pickle.dump((self.n_clusters, self.n_samples, self.__mkind, \
                self.__mdtype), metadata)
            metadata.close()
        else:
            raise ValueError(\
                "save: the requested model might have been released!")

    def debug_print(self):
        """
        NAME: debug_print
        """
        if self.__mid is not None:
            GLM.debug_print(self.__mid, self.__mkind, self.__mdtype)

    def release(self):
        """
        NAME: release
        """
        if self.__mid is not None:
            GLM.release(self.__mid, self.__mkind, self.__mdtype)
            self.__mid = None
            self.labels_ = None
            self.n_leaves_ = None
            self._children = None
            self._n_connected_components = None
            self._distances = None
            self._n_clusters = None            

    def __del__(self):
        """
        NAME: __del__
        """
        if FrovedisServer.isUP():
            self.release()

class DBSCAN(BaseEstimator):
    """
    A python wrapper of Frovedis dbcsan
    """
    def __init__(self, eps=0.5, min_samples=5, metric='euclidean', 
                 metric_params=None, algorithm='auto', leaf_size=30, 
                 p=None, n_jobs=None, verbose=0):
        self.eps = eps
        self.min_samples = min_samples
        self.metric = metric
        self.metric_params = metric_params
        self.algorithm = algorithm
        self.leaf_size = leaf_size
        self.p = p
        self.n_jobs = n_jobs
        self.verbose = verbose
        #extra
        self.__mid = None
        self.__mdtype = None
        self.__mkind = M_KIND.DBSCAN 
        self._labels = None
        self._core_sample_indices = None
        self._components = None
        self.n_samples = None
        self.n_features = None
        self.movable = None

    def validate(self):
        """
        DESC: validates hyper-parameters for dbscan
        """
        if self.eps <= 0:
            raise ValueError(\
                "Invalid parameter value passed for eps")
        if self.min_samples < 1:
            raise ValueError(\
                "Invalid parameter value passed for min_samples")
        if self.metric is not "euclidean":
            raise ValueError(\
                "Currently Frovedis DBSCAN does not support %s metric!" \
                % self.metric)
        if self.algorithm == "auto":
            self.algorithm = "brute"
        supported_algorithms = ["brute"]
        if self.algorithm not in supported_algorithms:
            raise ValueError(\
                "Currently Frovedis DBSCAN does not support %s algorithm!" \
                % self.algorithm)

    def check_input(self, X, F):
        """checks input X"""
        # Currently Frovedis DBSCAN does not support sparse data,
        # it would be loaded as rowmajor matrix
        inp_data = FrovedisFeatureData(X, \
                   caller = "[" + self.__class__.__name__ + "] " + F + ": ",\
                   dense_kind='rowmajor', densify=True)
        X = inp_data.get()
        dtype = inp_data.get_dtype()
        itype = inp_data.get_itype()
        dense = inp_data.is_dense()
        nsamples = inp_data.numRows()
        nfeatures = inp_data.numCols()
        movable = inp_data.is_movable()
        return X, dtype, itype, dense, nsamples, nfeatures, movable

    def check_sample_weight(self, sample_weight):
        if sample_weight is None:
            weight = np.array([], dtype=np.float64)
        elif isinstance(sample_weight, numbers.Number):
            weight = np.full(self.n_samples, sample_weight, dtype=np.float64)
        else:
            weight = np.ravel(sample_weight)
            if len(weight) != self.n_samples:
                 raise ValueError("sample_weight.shape == {}, expected {}!"\
                       .format(sample_weight.shape, (self.n_samples,)))
        return np.asarray(weight, dtype=np.float64)

    def fit(self, X, y=None, sample_weight=None):
        """
        DESC: fit method for dbscan
        """
        self.release()
        self.validate()
        # Currently Frovedis DBSCAN does not support sparse data, 
        # it would be loaded as rowmajor matrix
        X, dtype, itype, dense, \
        n_samples, n_features, movable = self.check_input(X, "fit")
        self.n_samples = n_samples
        self.n_features = n_features
        self.__mdtype = dtype
        self.__mid = ModelID.get()
        self.movable = movable
        
        sample_weight = self.check_sample_weight(sample_weight)

        (host, port) = FrovedisServer.getServerInstance()
        ret = np.zeros(n_samples, dtype=np.int32)
        rpclib.dbscan_train(host, port, X.get(), sample_weight, \
                            len(sample_weight), self.eps, self.min_samples, \
                            ret, n_samples, self.verbose, self.__mid, dtype, \
                            itype, dense)
        excpt = rpclib.check_server_exception()
        if excpt["status"]:
            raise RuntimeError(excpt["info"])
        self._labels = ret
        self._core_sample_indices = None
        self._components = None
        return self

    def fit_predict(self, X, sample_weight=None):
        """
        NAME: fit_predict
        """
        self.fit(X, sample_weight = sample_weight)
        return self._labels

    def score(self, X, y, sample_weight=None):
        """uses scikit-learn homogeneity_score for scoring"""
        if self.__mid is not None:
            return clustering_score(y, self.fit_predict(X))

    def release(self):
        """
        NAME: release
        """
        if self.__mid is not None:
            GLM.release(self.__mid, self.__mkind, self.__mdtype)
            #print (self.__mid, " model is released")
            self.__mid = None
            self._labels= None
            self.n_samples = None
            self.n_features = None
            self._core_sample_indices = None
            self._components = None

    @property
    def labels_(self):
        """labels_ getter"""
        if self.__mid is not None:
            if self._labels is not None:
                return self._labels
        else:
            raise AttributeError(\
            "attribute 'labels_' might have been released or called before fit")

    @labels_.setter
    def labels_(self, val):
        """labels_ setter"""
        raise AttributeError(\
            "attribute 'labels_' of DBSCAN object is not writable")

    @property
    def core_sample_indices_(self):
        """core_sample_indices_ getter"""
        if self.__mid is not None:
            if self._core_sample_indices is None:
                (host, port) = FrovedisServer.getServerInstance()
                core_sample_indices = rpclib.get_dbscan_core_sample_indices(host, port, self.__mid, \
                       self.__mkind, self.__mdtype)
                excpt = rpclib.check_server_exception()
                if excpt["status"]:
                    raise RuntimeError(excpt["info"])
                self._core_sample_indices = np.asarray(core_sample_indices)
            return self._core_sample_indices
        else:
            raise AttributeError(\
            "attribute 'core_sample_indices_' might have been released or called before fit")

    @core_sample_indices_.setter
    def core_sample_indices_(self, val):
        """core_sample_indices_ setter"""
        raise AttributeError(\
            "attribute 'core_sample_indices_' of DBSCAN object is not writable")

    @property
    def components_(self):
        """components_ getter"""
        if self.__mid is not None:
            if self._components is None:
                (host, port) = FrovedisServer.getServerInstance()
                dmat = rpclib.get_dbscan_components(host, port, self.__mid, \
                                                    self.__mkind, self.__mdtype)
                excpt = rpclib.check_server_exception()
                if excpt["status"]:
                    raise RuntimeError(excpt["info"])
                components = FrovedisRowmajorMatrix(mat=dmat, \
                             dtype=TypeUtil.to_numpy_dtype(self.__mdtype))
                if self.movable:
                    self._components = components.to_numpy_array()
                else:
                    self._components = components
            return self._components
        else:
            raise AttributeError(\
            "attribute 'components_' might have been released or called before fit")

    @components_.setter
    def components_(self, val):
        """components_ setter"""
        raise AttributeError(\
            "attribute 'components_' of DBSCAN object is not writable")

