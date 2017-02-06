/*****************************************************************************
				Tejas Simulator
------------------------------------------------------------------------------------------------------------

   Copyright [2010] [Indian Institute of Technology, Delhi]
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
------------------------------------------------------------------------------------------------------------

	Contributors:  Moksh Upadhyay
*****************************************************************************/
package config;

import java.util.Hashtable;
import java.util.Vector;

import memorysystem.coherence.Coherence;

import generic.PortType;

public class SystemConfig 
{
	public static enum Interconnect {
		Bus, Noc
	}

        public static enum ClusterMode {
	        SNC,Hemi,A2A
	}

	public static int NoOfCores;
	
	public static int maxNumJavaThreads;
	public static int numEmuThreadsPerJavaThread;	
	
	public static CoreConfig[] core; 
	public static Vector<CacheConfig> sharedCacheConfigs=new Vector<CacheConfig>();	

	//added later kush
	public static boolean memControllerToUse;

        // added by markos
        public static boolean knl;

	public static int mainMemoryLatency;
	public static long mainMemoryFrequency;
	public static PortType mainMemPortType;
	public static int mainMemoryAccessPorts;
	public static int mainMemoryPortOccupancy;
	public static int cacheBusLatency;
	public static String coherenceEnforcingCache;
	public static BusConfig busConfig;
	public static NocConfig nocConfig;
	public static EnergyConfig busEnergy;

       	public static int mcdramLatency;
	public static long mcdramFrequency;
	public static PortType mcdramPortType;
	public static int mcdramAccessPorts;
	public static int mcdramPortOccupancy;
    //public static int cacheBusLatency;
    //public static String coherenceEnforcingCache;
    //public static BusConfig busConfig;
    //public static NocConfig nocConfig;
    //public static EnergyConfig busEnergy;

	
	public static Interconnect interconnect;
        public static ClusterMode clusterMode;
	public static EnergyConfig  mainMemoryControllerPower;
        public static EnergyConfig  mcdramControllerPower;
	public static EnergyConfig  globalClockPower;

        // added by markos
        public static long mcdramAddr = -1;
        public static long mcdramSize = -1; //todo
        public static long ddrAddr = -1;
        public static long ddrSize = -1; //todo 

        public static Vector<Coherence> chaList = new Vector<Coherence>();
        public static int[] mappingSNC = {0,1,2,6,7,8,12,13,14,
					  3,4,5,9,10,11,15,16,17,
					  18,19,20,24,25,26,30,31,32,
					  21,22,23,27,28,29,33,34,35};
        public static int[] mappingHemi = {0,1,2,6,7,8,12,13,14,18,19,20,24,25,26,30,31,32,
					   3,4,5,9,10,11,15,16,17,21,22,23,27,28,29,33,34,35};
        public static int[] mappingA2A = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,
					  18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35};

        public static String addrFilePath = "/home/marcos.horro/tejas_installation_kit/Tejas-Simulator/Tejas/addr.txt";
 	
	//FIXME
	//TODO
	//have to do it here since the object is not being created in xml parser yet
	public static MainMemoryConfig mainMemoryConfig;
        public static MainMemoryConfig mcdramConfig; 

        public static void setMCDRAMaddr(long addr) { SystemConfig.mcdramAddr = addr; }
        public static void setMCDRAMsize(long size) { SystemConfig.mcdramSize = size; }
        public static void setDDRaddr(long addr) { SystemConfig.ddrAddr = addr; }
        public static void setDDRsize(long size) { SystemConfig.ddrSize = size; }
}
