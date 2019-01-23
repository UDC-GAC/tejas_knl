#!/env/usr/python

import sys
import pandas as pd
import seaborn as sns
import numpy as np
import matplotlib.pyplot as plt
import re
import copy

b = sys.argv[1]
s = sys.argv[2]

path = "./outputs/"

col_map = np.reshape(np.zeros(6*9),[9,6])
acc_map = np.reshape(np.zeros(6*9),[9,6])

tejas_file = path + "knl-" + b + "-" + str(s) + ".output"
f = open(tejas_file)
for l in f:
    if ("router" not in l) and ("collisions" not in l): continue
    if "router" in l: t = l.strip().split("\t")
    if "collisions" in l: t = l.strip().split("=")
    val = t[-1]
    x = int(re.findall(string=t[0],pattern="[0-9]")[0])
    y = int(re.findall(string=t[0],pattern="[0-9]")[1])-1
    if "router" in l: acc_map[x][y] = val
    if "collisions" in l: col_map[x][y] = val


sns.heatmap(acc_map)
plt.show()