#!/usr/bin/env python

import sys
import numpy as np
from frovedis.exrpc.server import FrovedisServer
from frovedis.mllib.manifold import TSNE
from scipy.sparse import csr_matrix

desc = "Testing fit for csr_matrix : "

# initializing the frovedis server
argvs = sys.argv
argc = len(argvs)
if (argc < 2):
    print ('Please give frovedis_server calling command as the first argument\n\
           (e.g. "mpirun -np 2 -x /opt/nec/nosupport/frovedis/ve/bin/frovedis_server")')
    quit()
FrovedisServer.initialize(argvs[1])

# sample numpy dense data (3x3)
mat = np.matrix([[0, 0, 0, 0], 
                 [0, 1, 1, 1], 
                 [1, 0, 1, 0],
                 [1, 1, 1, 0],
                 [1, 1, 1, 1]], 
                dtype=np.float64)
mat = csr_matrix(mat)
tsne = TSNE(n_components=2)

try:
    tsne.fit(mat)
    print(desc, "Passed")
except:
    print(desc, "Failed")

FrovedisServer.shut_down()

