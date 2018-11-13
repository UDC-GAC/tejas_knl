#!/env/usr/python

import sys
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import re
import copy

def main(f):
    # python 2 vs python 3
    try:
        xrange
    except NameError:
        xrange = range
    
    # translations
    translations = {'dTLB': ['tlb','data'],
                    'iTLB': ['tlb','instr'],
                    'L1': ['l1','data'],
                    'I1': ['l1','instr'],
                    'ReadMiss': ['readmiss'],
                    'WriteMiss': ['writemiss'],
                    'WriteHit': ['writehit'],
                    'EvictionFromCoherentCache': ['evictcoherent'],
                    'EvictionFromSharedCache': ['evictshared']}
    
    core_timing_stats = False
    core_memory_stats = False
    tile_memory_stats = False
    tile_router_stats = False
    
    core_n = -1
    tile_n = -1
    
    tile_map_knl = [[-1]*7,\
                    [-1,0,12,-1,-1,13,29],\
                    [-1,4,16,28,1,17,33],\
                    [-1,8,20,32,5,21,37],\
                    [-1,-1,24,36,9,25,-1],\
                    [-1,2,14,26,3,15,27],\
                    [-1,6,18,30,7,19,31],\
                    [-1,10,22,34,11,23,35]]
    
    router_access = np.zeros(9*8).reshape(9,8)
    router_collision = np.zeros(9*8).reshape(9,8)
    
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
    router_memory_df = pd.DataFrame(index=xrange(38), columns=['data','control'])
    router_memory_df = pd.concat({'router':pd.concat({'collision': pd.Series(np.nan, index=xrange(38)),\
                                                      'packets': router_memory_df,\
                                                      'access':pd.Series(np.nan, index=xrange(38))},axis=1)},\
                                                      axis=1)
    # tile memory stats
    c_tile = ['readmiss','writehit','writemiss','evictshared','evictcoherent','hits','misses']
    l2_df = pd.DataFrame(index=xrange(38), columns=['hits','misses'])
    tile_memory_df = pd.concat({'l2': l2_df,'cha':pd.DataFrame(np.nan,index=xrange(38),columns=c_tile)},axis=1)
    
    tile = {'router': router_memory_df, 'memory': tile_memory_df}
    
    leaf_col = ['read','writes','data','control'] + col_core_timing_stats + c_tile
    mcdram_n = -1
    ddr_n = -1
    
    tejas_file = open(f)
    
    # PARSING TEJAS STATS OUTPUT 
    for l in tejas_file:
        if not l.strip():
            continue
        
        if "Nothing" in l:
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
        
        if ("[Shared Caches]" in l) or\
            ("Number of hops:" in l):
            core_timing_stats = False
            core_memory_stats = False
            tile_memory_stats = True
            tile_router_stats = False
            continue 
        
        if "[Consolidated Stats For Caches]" in l:
            core_timing_stats = False
            core_memory_stats = False
            tile_memory_stats = False
            tile_router_stats = True 
            continue
        
        #print(l)
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
                core['memory'].loc[core_n,('main','mcdram',mcdram_n)] = value
                continue
        
            stat = l.split("=")[0].strip().split(" ")
            if stat[0]=="":
                continue
            value = float(l.split("=")[1].strip().split(" ")[0])
            if "core"==stat[0]:
                core_n = value
                continue
            
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
            #print("col=" + str(col) + ", core_n=" + str(core_n) + ", value=" + str(value))
            core['memory'].loc[core_n,tuple(col)] = value
            
            
        # Parse tile params
        if tile_router_stats:
            m = re.search("router\[([0-9])\]\[([0-9])\]",l)
            value = l.strip().split("\t")[-1]
            stat = ['router','access']
            r = router_access
            if not m:
                m = re.search("collisions\[([0-9])\]\[([0-9])\]",l)
                value = l.strip().split(" = ")[-1]
                stat = ['router','collision']
                r = router_collision
                if not m:
                    continue
            x = int(m.group(1))
            y = int(m.group(2))
            r[x][y] = value
            try:
                tile_n = tile_map_knl[x][y]
                if tile_n != -1:
                    tile['router'].loc[tile_n,tuple(stat)] = value
            except IndexError:
                pass
            
        # Parse tile params
        if tile_memory_stats:
            stat = l.split("=")[0].strip().split(" ")
            m = re.search("(.+?)\[",stat[0])
            if not m:
                continue
            tile_n = int(re.findall(r'\d+',l.split("=")[0])[-1])
            value = int(re.findall(r'\d+',l)[-1])
            col = [str(m.group(1)).lower()]
            trans = copy.deepcopy(translations)
            if stat[-1] in trans.keys():
                col += trans[stat[-1]]
            elif stat[-1].lower() in leaf_col:
                col += [stat[-1].lower()]
            else:
                continue
            #print("idx "+str(tile_n) + " " + str(col))
            tile['memory'].loc[tile_n,tuple(col)] = value
    return core,tile

if __name__=="__main__":
    main(sys.argv[1])
    


    
    