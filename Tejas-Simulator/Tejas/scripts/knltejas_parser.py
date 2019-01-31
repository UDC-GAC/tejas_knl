#!/env/usr/python

# USE: python3 knltejas_parser.py <poly|parboil>

import sys
import argparse
import os
import numpy as np
import pandas as pd
import tejas
import seaborn as sns
import matplotlib.pyplot as plt
from pandas import DataFrame
from pandas import MultiIndex

# this should not be hardcoded like this...
affinity = [0,1,20,21,22,23,52,53,\
            28,29,50,51,2,3,30,31,\
            36,37,56,57,8,9,38,39,\
            44,45,62,63,14,15,46,47,\
            4,5,24,25,48,49,6,7,26,27,\
            10,11,32,33,12,13,34,35,54,55,\
            16,17,40,41,58,59,18,19,42,43,60,61]

quadrant =[[0,1,20,21,28,29,50,51,36,37,56,57,44,45,62,63],
           [22,23,52,53,2,3,30,31,8,9,38,39,14,15,46,47],
           [4,5,24,25,48,49,10,11,32,33,16,17,40,41,58,59],
           [6,7,26,27,12,13,34,35,54,55,18,19,42,43,60,61]]

# get near or far interfaces given a core number
def MC_ifaces(type, core_monitor):
    ifaces = list(range(8))
    quad = 0
    for q in quadrant:
        if core_monitor in q:
            break
        quad += 1
    if type=="near":
        ifaces = [quad*2,quad*2+1]  
    elif type=="far":
        ifaces.remove(quad*2)
        ifaces.remove(quad*2+1)
    else:
        print("nor near or far type, exiting...")
        exit(0)
    return ifaces

cores = range(1)
halted_tiles = [4,8,27,30,33,37]
map_core_to_tile = list(filter(lambda a: a not in halted_tiles,[i for i in range(38) for _ in range(2)]))
 
# parse KNL output
papi_counters = ['cycles','inst','L1a','L1m','L2a','L2m','l2hits','instl2miss','MC',\
                 'MCfar','MCnear','l2tlbmisses','l1tlbmisses']

parallel = False
if "poly"==sys.argv[1]:
    #sizes = ['MINI', 'SMALL', 'MEDIUM', 'LARGE', 'EXTRALARGE']
    #sizes = ['MINI','SMALL','MEDIUM']
    benchmarks = ['correlation','covariance','gemm','gemver','gesummv','symm','syr2k','syrk','trmm',\
                  '2mm','3mm','atax','bicg','doitgen','mvt','cholesky','gramschmidt',\
                  'trisolv','adi','jacobi-1d','jacobi-2d','heat-3d','fdtd-2d','seidel-2d']
    sizes = ['MEDIUM']
    idx = MultiIndex.from_product([benchmarks,sizes])
elif "parboil"==sys.argv[1]:
    parallel = True
    #sizes = ['UT','small','default','short','medium','medium','small','small']
    #benchmarks = ['bfs','cutcp','histo','lbm','sgemm','spmv','stencil','tpacf']
    sizes = ['medium']
    benchmarks = ['spmv','lbm']
    cores = range(64)
    idx = MultiIndex.from_product([benchmarks,sizes,cores])
else:
    print("Benchmark not known!")
    exit(0)

knl_df = pd.DataFrame(0,columns=papi_counters,index=idx)
tejas_df = pd.DataFrame(0,columns=papi_counters,index=idx)

list_df = {}
path = "./outputs/"

#affinity = range(64)

for b in benchmarks:
    for s in sizes:
        print("parsing " + b + " for size " + s + " (" + str(len(cores)) + " core/s) ...")
        tejas_file = path + "knl-" + b + "-" + str(s) + ".output"
        core,tile=tejas.parse(tejas_file)
        knl_file = path + b + "-" + s + ".output"
        f = open(knl_file)
        c = 0
        for l in f:
            if len(l.strip().split(" "))<=1: continue
            # format splited only by spaces
            if len(l.strip().split(" "))<=len(papi_counters):
                tmp = l.strip().split(" ")
                knl_df.loc[b,s] = [int(i) for i in tmp]
            else:
                # format -> PAPI thread X\tCOUNTER1 COUNTER2 ... COUNTERN
                if len(l.strip().split("\t"))<=1: continue  
                tmp = l.strip().split("\t")[1].split(" ")                                                         
                knl_df.loc[b,s,affinity[c]] = [int(i) for i in tmp]
                c += 1
        if parallel:
            for c in cores:
                knl_df.loc[b,s,c]['MC'] /=2
                knl_df.loc[b,s,c]['MCfar'] /=2
                knl_df.loc[b,s,c]['MCnear'] /=2
        for core_monitor in cores:
            print("\tcore " + str(core_monitor) + "/" + str(len(cores)-1) +\
                   " (tile " + str(map_core_to_tile[core_monitor]) + ") ...")
            #print("\t\t core " + str(core_monitor) + " near ifaces " + str(MC_ifaces("near", core_monitor)))
            #print("\t\t core " + str(core_monitor) + " far  ifaces " + str(MC_ifaces("far", core_monitor)))

            list_df[b]= {'core':core,'tile':tile}
            
            if len(cores)>1:
                i = (b,s,core_monitor)
            else:
                i = (b,s)
            tejas_df.loc[i].cycles = core['timing'].cycles[core_monitor]
            tejas_df.loc[i].inst = core['timing'].instructions[core_monitor]
            #tejas_df.loc[i].inst = core['timing'].ciscinst[core_monitor]
            #tejas_df.loc[i].L1a = core['memory'].l1.data.hits[core_monitor] + core['memory'].l1.data.misses[core_monitor]
            tejas_df.loc[i].L1a = core['memory'].l1.data.acc[core_monitor]
            tejas_df.loc[i].L1m = core['memory'].l1.data.misses[core_monitor]
            tile_monitor = map_core_to_tile[core_monitor]
            tejas_df.loc[i].l2hits = tile['memory'].l2.hits[tile_monitor]
            tejas_df.loc[i].L2a = tile['memory'].l2.hits[tile_monitor] + tile['memory'].l2.misses[tile_monitor]
            tejas_df.loc[i].L2m = tile['memory'].l2.misses[tile_monitor]
            tejas_df.loc[i].instl2miss = tile['memory'].l2.misses[tile_monitor]
            if parallel:
                tejas_df.loc[i].l2hits /= 2
                tejas_df.loc[i].L2a /= 2
                tejas_df.loc[i].L2m /= 2
                tejas_df.loc[i].instl2miss /= 2
            for r in MC_ifaces("far",core_monitor):
                tejas_df.loc[i].MCfar  += core['memory'].main.mcdram[r][core_monitor]
            for r in MC_ifaces("near",core_monitor):
                tejas_df.loc[i].MCnear += core['memory'].main.mcdram[r][core_monitor]
            tejas_df.loc[i].MC = tejas_df.loc[i].MCfar + tejas_df.loc[i].MCnear
            tejas_df.loc[i].l2tlbmisses = core['memory'].tlb.data.misses[core_monitor]
            tejas_df.loc[i].l1tlbmisses = core['memory'].tlb.data.hits[core_monitor] + core['memory'].tlb.data.misses[core_monitor]
        
df=1-tejas_df/knl_df
df.drop('instl2miss',axis=1,inplace=True)
df.drop('l1tlbmisses',axis=1,inplace=True)
df.drop('l2tlbmisses',axis=1,inplace=True)
df.drop('l2hits',axis=1,inplace=True)
if not (parallel):
    df.reset_index(level=1,drop=True,inplace=True) # remove size
df.loc['MEAN'] = df.apply(abs).mean()
