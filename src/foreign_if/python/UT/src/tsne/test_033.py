#!/usr/bin/env python

import sys
import numpy as np
from frovedis.exrpc.server import FrovedisServer
from frovedis.mllib.manifold import TSNE

desc = "Testing fit for metric='precomputed': "

# initializing the frovedis server
argvs = sys.argv
argc = len(argvs)
if (argc < 2):
    print ('Please give frovedis_server calling command as the first argument\n\
           (e.g. "mpirun -np 2 -x /opt/nec/nosupport/frovedis/ve/bin/frovedis_server")')
    quit()
FrovedisServer.initialize(argvs[1])

# sample numpy dense data (3x3)
mat = np.matrix([[0, 4, 9, 16], 
                 [9, 0, 4, 9], 
                 [1, 4, 0, 9],
                 [9, 16, 25, 0]], 
                dtype=np.float64)
tsne = TSNE(n_components=2, metric='precomputed')

try:
    tsne.fit(mat)
    print(desc, "Passed")
except:
    print(desc, "Failed")

FrovedisServer.shut_down()

