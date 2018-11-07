#!/env/usr/python

import sys
import argparse
import os
import pandas as pd
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
    sizes = ['MINI', 'SMALL', 'MEDIUM', 'LARGE', 'EXTRALARGE']
else:
    print("Benchmark not known!")
    exit(0)

list_df = []
for s in sizes:
    file_name = "knl-" + sys.argv[1] + "-" + str(s) + ".output"
    os.system("python output_parser.py " + file_name)
    list_df += [[core,tile]]
    
