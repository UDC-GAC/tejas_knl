#!/env/usr/python

import sys
import argparse
import os
import numpy as np
import pandas as pd
import tejas
from pandas import DataFrame

# usage:

#def readable_dir(prospective_dir):
#  if not os.path.isdir(prospective_dir):
#    raise Exception("readable_dir:{0} is not a valid path".format(prospective_dir))
#  if os.access(prospective_dir, os.R_OK):
#    return prospective_dir
#  else:
#    raise Exception("readable_dir:{0} is not a readable dir".format(prospective_dir))
#
#parser = argparse.ArgumentParser(description='Process output files')
#parser.add_argument('bench', metavar='B', type=str, nargs='+',
#                    help='an integer for the accumulator')
#parser.add_argument('-p','--path',type=readable_dir)
#
#args = parser.parse_args()

if "poly"==sys.argv[1]:
    #sizes = ['MINI', 'SMALL', 'MEDIUM', 'LARGE', 'EXTRALARGE']
    sizes = ['MEDIUM']
    benchmarks = ['correlation','covariance','gemm','gemver','gesummv','symm','syr2k','syrk','trmm',\
                  '2mm','3mm','atax','bicg','doitgen','mvt','cholesky','durbin','gramschmidt',\
                  'lu','ludcmp','trisolv','adi','jacobi-1d','jacobi-2d','heat-3d','fdtd-2d','seidel-2d']
    #benchmarks = ['fdtd-2d']
else:
    print("Benchmark not known!")
    exit(0)

    
# parse KNL output
papi_counters = ['cycles','l1accesses','l1misses','l2accesses','l2misses',\
                 'mcdramfar','mcdramnear','l2tlbmisses','l1tlbmisses']

knl_df = pd.DataFrame(0,columns=papi_counters,index=benchmarks)
tejas_df = pd.DataFrame(0,columns=papi_counters,index=benchmarks)

list_df = {}
path = "./outputs/"
core_monitor = 0
for b in benchmarks:
    for s in sizes:
        print("parsing " + b + " for size " + s + "...")
        tejas_file = path + "knl-" + b + "-" + str(s) + ".output"
        core,tile=tejas.main(tejas_file)
        knl_file = path + b + "-" + s + ".output"
        f = open(knl_file)
        for l in f:
            if len(l.strip().split(" "))==len(papi_counters):
                knl_df.loc[b] = [int(i) for i in l.strip().split(" ")]
        list_df[b]= {'core':core,'tile':tile}
        tejas_df.loc[b].cycles = core['timing'].cycles[core_monitor]
        tejas_df.loc[b].l1accesses = core['memory'].l1.data.hits[core_monitor] + core['memory'].l1.data.misses[core_monitor]
        tejas_df.loc[b].l1misses = core['memory'].l1.data.misses[core_monitor]
        tejas_df.loc[b].l2accesses = tile['memory'].l2.hits[core_monitor] + tile['memory'].l2.misses[core_monitor]
        tejas_df.loc[b].l2misses = tile['memory'].l2.misses[core_monitor]
        for i in range(2,8):
            tejas_df.loc[b].mcdramfar += core['memory'].main.mcdram[i][core_monitor]
        for i in range(0,2):
            tejas_df.loc[b].mcdramnear += core['memory'].main.mcdram[i][core_monitor]
        tejas_df.loc[b].l1tlbmisses = core['memory'].tlb.data.misses[core_monitor]
        tejas_df.loc[b].l2tlbmisses = core['memory'].tlb.data.hits[core_monitor] + core['memory'].tlb.data.misses[core_monitor]
        

#print("Absolute error in terms of cycles:")
#for b in benchmarks:
#    print(b + "\t\t" + str(abs((knl_df.cycles[b]-list_df[b]['core']['timing'].cycles[0])/knl_df.cycles[b])))
    
#print("Absolute error in terms of cache accesses misses:")
#for b in benchmarks:
#    m = list_df[b]['core']['memory']
#    l1acc = abs((knl_df.l1accesses[b]-(m.l1.data.hits[0]+m.l1.data.misses[0])/knl_df.l1accesses[b]))
#    print(b + "\t\t" + str(l1acc))

