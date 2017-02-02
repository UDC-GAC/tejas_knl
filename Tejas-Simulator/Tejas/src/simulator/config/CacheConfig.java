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

import memorysystem.nuca.NucaCache.Mapping;
import memorysystem.nuca.NucaCache.NucaType;
import generic.PortType;

public class CacheConfig 
{
	public long operatingFreq;
	
	public WritePolicy writePolicy;	
	public String nextLevel;
	public int blockSize;
	public int assoc;
	public int size;
	public int numEntries;
	public int latency;
	
	public PortType portType;
	public int accessPorts;
	public int portOccupancy;
	
	public int numberOfBuses;
	public int busOccupancy;
	public int mshrSize;
	public NucaType nucaType;
	public Mapping mapping;
	
	public boolean collectWorkingSetData = false;
	public long workingSetChunkSize = -1;
	
	public CacheEnergyConfig power;
	
	public String cacheName;
	public int numComponents;
	public boolean firstLevel = false;
	public CacheDataType cacheDataType = null;
	public String nextLevelId;

	public String coherenceName;

	public boolean isDirectory = false;
	
	public static enum WritePolicy{
		WRITE_BACK, WRITE_THROUGH
	}

	//Getters and setters
	
	public WritePolicy getWritePolicy() {
		return writePolicy;
	}

	public String getNextLevel() {
		return nextLevel;
	}

	public int getBlockSize() {
		return blockSize;
	}

	public int getAssoc() {
		return assoc;
	}

	public int getSize() {
		return size;
	}

	public int getLatency() {
		return latency;
	}

	public int getAccessPorts() {
		return accessPorts;
	}

	public int getPortOccupancy() {
		return portOccupancy;
	}

	protected void setWritePolicy(WritePolicy writePolicy) {
		this.writePolicy = writePolicy;
	}

	protected void setNextLevel(String nextLevel) {
		this.nextLevel = nextLevel;
	}

	protected void setBlockSize(int blockSize) {
		this.blockSize = blockSize;
	}

	protected void setAssoc(int assoc) {
		this.assoc = assoc;
	}

	protected void setSize(int size) {
		this.size = size;
	}

	protected void setLatency(int latency) {
		this.latency = latency;
	}

	protected void setAccessPorts(int accessPorts) {
		this.accessPorts = accessPorts;
	}

	protected void setPortOccupancy(int portOccupancy) {
		this.portOccupancy = portOccupancy;
	}

	public int getNumberOfBuses() {
		return numberOfBuses;
	}
	public int getBankSize()
	{
		return size/(SystemConfig.nocConfig.numberOfColumns * SystemConfig.nocConfig.numberOfRows);
	}
	public NucaType getNucaType() {
		return nucaType;
	}

	public void setNucaType(NucaType nucaType) {
		this.nucaType = nucaType;
	}

	public int getBusOccupancy() {
		return busOccupancy;
	}

	public void setBusOccupancy(int busOccupancy) {
		this.busOccupancy = busOccupancy;
	}
	

	//	public boolean isFirstLevel() {
//		return isFirstLevel;
//	}
//
//	public void setFirstLevel(boolean isFirstLevel) {
//		this.isFirstLevel = isFirstLevel;
//	}	
	
	
}
