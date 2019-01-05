#!/usr/bin/env python

import sys
import numpy as np
from frovedis.exrpc.server import FrovedisServer
from frovedis.matrix.crs import FrovedisCRSMatrix
from frovedis.matrix.dvector import FrovedisDvector
from frovedis.mllib.fm import FactorizationMachineClassifier

# initializing the Frovedis server
argvs = sys.argv
argc = len(argvs)
if (argc < 2):
    print ('Please give frovedis_server calling command as the first argument \n')
    quit()
FrovedisServer.initialize(argvs[1])

mat = FrovedisCRSMatrix(dtype=np.float64).load("./input/classification.txt")
lbl = FrovedisDvector([1,1,1,-1,-1,1,-1,1,-1,1],dtype=np.float64)
# fitting input matrix and label on Factorization Machine Classifier object

fm_obj = FactorizationMachineClassifier(iteration = 100, init_stdev = 0.1,
               init_learn_rate = 0.1, optimizer="SGD", dim = (True, True, 8),
               reg = (0, 0, 0), batch_size_pernode = 1, verbose = 0)

# prediction on created model
model = fm_obj.fit(mat, lbl)
pr_res = model.predict(mat)

# saving the model
model.save("./out/FMCModel1")

check = np.asarray([1,1,1,-1,-1,1,-1,1,-1,1], dtype=np.float64)
out = np.in1d(pr_res, check)
if False not in out:
  print("Passed") 
else:
  print("Failed") 

model.release()
FrovedisServer.shut_down()