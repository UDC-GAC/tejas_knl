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

import java.util.Vector;
import generic.PortType;

public class CoreConfig 
{
	public long frequency;
	
	public int LSQSize;
	public int LSQLatency;
	public PortType LSQPortType;
	public int LSQAccessPorts;
	public int LSQPortOccupancy;
		
	public PipelineType pipelineType;
		
	public int ITLBSize;
	public int ITLBLatency;
	public int ITLBMissPenalty;
	public PortType ITLBPortType;
	public int ITLBAccessPorts;
	public int ITLBPortOccupancy;
	
	public int DTLBSize;
	public int DTLBLatency;
	public int DTLBMissPenalty;
	public PortType DTLBPortType;
	public int DTLBAccessPorts;
	public int DTLBPortOccupancy;

	public int DecodeWidth;
	public int IssueWidth;
	public int RetireWidth;
	public int ROBSize;
	public int IWSize;
	public int IntRegFileSize;
	public int FloatRegFileSize;
	public int IntArchRegNum;
	public int FloatArchRegNum;
	
	public int BranchMispredPenalty;
	
	public int ExecutionCoreNumPorts;
	
	public int IntALUNum;
	public int IntMulNum;
	public int IntDivNum;
	public int FloatALUNum;
	public int FloatMulNum;
	public int FloatDivNum;
	public int JumpNum;
	public int MemoryNum;
	
	public int IntALULatency;
	public int IntMulLatency;
	public int IntDivLatency;
	public int FloatALULatency;
	public int FloatMulLatency;
	public int FloatDivLatency;
	public int JumpLatency;
	public int MemoryLatency;
	
	public int IntALUReciprocalOfThroughput;
	public int IntMulReciprocalOfThroughput;
	public int IntDivReciprocalOfThroughput;
	public int FloatALUReciprocalOfThroughput;
	public int FloatMulReciprocalOfThroughput;
	public int FloatDivReciprocalOfThroughput;
	public int JumpReciprocalOfThroughput;
	public int MemoryReciprocalOfThroughput;
	
	public int[] IntALUPortNumbers;
	public int[] IntMulPortNumbers;
	public int[] IntDivPortNumbers;
	public int[] FloatALUPortNumbers;
	public int[] FloatMulPortNumbers;
	public int[] FloatDivPortNumbers;
	public int[] JumpPortNumbers;
	public int[] MemoryPortNumbers;
	
	public Vector<CacheConfig> coreCacheList = new Vector<CacheConfig>();

	public BranchPredictorConfig branchPredictor;
	
	public boolean TreeBarrier;

	public int barrierLatency;
	public int barrierUnit;
	
	public EnergyConfig bPredPower;
	public EnergyConfig decodePower;
	public EnergyConfig intRATPower;
	public EnergyConfig floatRATPower;
	public EnergyConfig intFreeListPower;
	public EnergyConfig floatFreeListPower;
	public EnergyConfig lsqPower;
	public EnergyConfig intRegFilePower;
	public EnergyConfig floatRegFilePower;
	public EnergyConfig iwPower;
	public EnergyConfig robPower;
	public EnergyConfig intALUPower;
	public EnergyConfig floatALUPower;
	public EnergyConfig complexALUPower;
	public EnergyConfig resultsBroadcastBusPower;
	public EnergyConfig iTLBPower;
	public EnergyConfig dTLBPower;
	
	public int getICacheLatency() {
		int latency = 0;
		
		for(CacheConfig config : coreCacheList) {
			if(config.firstLevel) {
				if(config.cacheDataType==CacheDataType.Instruction ||
					config.cacheDataType==CacheDataType.Unified) {
					return config.latency;
				}
			}
		}
		
		misc.Error.showErrorAndExit("Could not locate instruction cache config !!");
		return latency;
	}
}