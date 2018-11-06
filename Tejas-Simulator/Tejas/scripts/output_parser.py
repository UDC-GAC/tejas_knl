#!/env/usr/python2

import sys
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import re
import copy

# python 2 vs python 3
try:
    xrange
except NameError:
    xrange = range

# translations
translations = {'dTLB': ['tlb','data'],
                'iTLB': ['tlb','instr'],
                'L1': ['l1','data'],
                'I1': ['l1','instr']}

core_timing_stats = False
core_memory_stats = False
tile_memory_stats = False
tile_router_stats = False

core_n = -1
tile_n = -1

###############################
# core memory stats
tmp_df = pd.DataFrame(index=xrange(64), columns=['hits','misses'])
tmp_df = pd.concat({'instr': tmp_df, 'data': tmp_df},axis=1)
tmp_df = pd.concat({'tlb': tmp_df, 'l1': tmp_df}, axis=1)
mem_tmp_df = pd.concat({'main':\
                        pd.concat({'ddr': pd.DataFrame(np.nan, index=xrange(64), columns=xrange(2)),\
                                   'mcdram': pd.DataFrame(np.nan, index=xrange(64), columns=xrange(8))},\
                                   axis=1)},\
                        axis=1)
tmp_df = tmp_df.join(mem_tmp_df)
# core timing
col_core_timing_stats = ['branch', 'cycles', 'instructions']
time_df = pd.DataFrame(index=xrange(64), columns=col_core_timing_stats)

core = {'timing': time_df, 'memory': tmp_df}
###############################
# tile router stats

# tile memory stats
#tmp_df = pd.DataFrame(index=xrange(38), columns=['read','write'])
#tmp_df = pd.concat({'cha':pd.concat({'hit': tmp_df, 'miss': tmp_df},axis=1)},axis=1)
#tmp_df['access'] = pd.Series(np.nan, index=xrange(38))
#tmp_df['evicted'] = pd.Series(np.nan, index=xrange(38))
#l2_df = pd.DataFrame(index=xrange(38), columns=['hit','miss'])

#tile = {'router': router_df, 'memory': tile_memory_df}

leaf_col = ['hits','misses','read','writes','data','control'] + col_core_timing_stats
mcdram_n = -1
ddr_n = -1

tejas_file = open(sys.argv[1])

# PARSING TEJAS STATS OUTPUT 
for l in tejas_file:
    if not l.strip():
        continue
    
    if "[Timing Statistics]" in l:
        core_timing_stats = True
        core_memory_stats = False
        tile_memory_stats = False
        tile_router_stats = False
        continue 
    
    if ("[Per core statistics]" in l) or\
     ("Accesses to each MCDRAM module by each core" in l):
        core_timing_stats = False
        core_memory_stats = True
        tile_memory_stats = False
        tile_router_stats = False
        continue
    
    if "[Shared Caches]" in l:
        core_timing_stats = False
        core_memory_stats = False
        tile_memory_stats = True
        tile_router_stats = False
        continue 
    
    if "[Consolidated Stats For Caches]" in l:
        core_timing_stats = False
        core_memory_stats = False
        tile_memory_stats = True
        tile_router_stats = True 
        continue
    
    # Parse core params
    if core_timing_stats:
        if ("core" in l) and ("frequency" not in l):
            core_n = int(l.split("=")[1].strip().split(" ")[0])
        stat = l.split("=")[0].strip().split(" ")[0]
        if stat in leaf_col:
            value = float(l.split("=")[1].strip().split(" ")[0])
            core['timing'].loc[core_n,stat] = value

    # Parse tile params
    if core_memory_stats:
        if "Accesses from each core to each MCDRAM module" in l:
            core_memory_stats = False
            continue
        if "MCDRAMController" in l:
            mcdram_n = int(re.findall(r'\d+',l)[0])
            continue
        if "CORE" in l:
            core_n = int(re.findall(r'\d+',l)[0])
            value = int(re.findall(r'\d+',l)[1])
            #print("core " + str(core_n) + " mcdram " + str(mcdram_n) + " value " + str(value))
            core['memory'].loc[core_n,('main','mcdram',mcdram_n)] = value
            continue
    
        stat = l.split("=")[0].strip().split(" ")
        #print("stat =>" + stat[0] + " value => " + l.split("=")[1].strip().split(" ")[0])
        if stat[0]=="":
            continue
        value = float(l.split("=")[1].strip().split(" ")[0])
        if "core"==stat[0]:
            core_n = value
            continue
        
        print("stat =>" + stat[0] + " value => " + l.split("=")[1].strip().split(" ")[0])
        m = re.search("(.+?)\[",stat[0])
        if not m:
            continue
        col = []
        trans = copy.deepcopy(translations)
        if m.group(1) in trans.keys():
            col = trans[m.group(1)]
        else:
            continue
        if stat[1].lower() in leaf_col:
            col += [stat[1].lower()]
        else:
            continue
        print(col)
        core['memory'].loc[core_n,tuple(col)] = value
        
        
    # Parse tile params
    if tile_router_stats:
        pass
    
    # Parse tile params
    if tile_memory_stats:
        pass