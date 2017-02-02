package pipeline;

import java.io.FileWriter;
import java.io.IOException;

import config.CoreConfig;
import config.EnergyConfig;
import config.SystemConfig;
import generic.Core;
import generic.GlobalClock;

public class ExecutionCore {
	
	Core core;
	FunctionalUnit[][] FUs;
	boolean[] portUsedThisCycle;
	int numPorts;
	
	long numIntALUAccesses;
	long numFloatALUAccesses;
	long numComplexALUAccesses;
	
	public ExecutionCore(Core core)
	{
		this.core = core;
		
		CoreConfig coreConfig = SystemConfig.core[core.getCore_number()];
		
		FUs = new FunctionalUnit[FunctionalUnitType.values().length][];
		
		//int ALUs
		FUs[FunctionalUnitType.integerALU.ordinal()] = new FunctionalUnit[coreConfig.IntALUNum];
		for(int i = 0; i < coreConfig.IntALUNum; i++)
		{
			FunctionalUnit FU = new FunctionalUnit(FunctionalUnitType.integerALU, coreConfig.IntALULatency,
					coreConfig.IntALUReciprocalOfThroughput, coreConfig.IntALUPortNumbers[i]);
			FUs[FunctionalUnitType.integerALU.ordinal()][i] = FU;
		}
		
		//int Muls
		FUs[FunctionalUnitType.integerMul.ordinal()] = new FunctionalUnit[coreConfig.IntMulNum];
		for(int i = 0; i < coreConfig.IntMulNum; i++)
		{
			FunctionalUnit FU = new FunctionalUnit(FunctionalUnitType.integerMul, coreConfig.IntMulLatency,
					coreConfig.IntMulReciprocalOfThroughput, coreConfig.IntMulPortNumbers[i]);
			FUs[FunctionalUnitType.integerMul.ordinal()][i] = FU;
		}
		
		//int Divs
		FUs[FunctionalUnitType.integerDiv.ordinal()] = new FunctionalUnit[coreConfig.IntDivNum];
		for(int i = 0; i < coreConfig.IntDivNum; i++)
		{
			FunctionalUnit FU = new FunctionalUnit(FunctionalUnitType.integerDiv, coreConfig.IntDivLatency,
					coreConfig.IntDivReciprocalOfThroughput, coreConfig.IntDivPortNumbers[i]);
			FUs[FunctionalUnitType.integerDiv.ordinal()][i] = FU;
		}
		
		//float ALUs
		FUs[FunctionalUnitType.floatALU.ordinal()] = new FunctionalUnit[coreConfig.FloatALUNum];
		for(int i = 0; i < coreConfig.FloatALUNum; i++)
		{
			FunctionalUnit FU = new FunctionalUnit(FunctionalUnitType.floatALU, coreConfig.FloatALULatency,
					coreConfig.FloatALUReciprocalOfThroughput, coreConfig.FloatALUPortNumbers[i]);
			FUs[FunctionalUnitType.floatALU.ordinal()][i] = FU;
		}
		
		//float Muls
		FUs[FunctionalUnitType.floatMul.ordinal()] = new FunctionalUnit[coreConfig.FloatMulNum];
		for(int i = 0; i < coreConfig.FloatMulNum; i++)
		{
			FunctionalUnit FU = new FunctionalUnit(FunctionalUnitType.floatMul, coreConfig.FloatMulLatency,
					coreConfig.FloatMulReciprocalOfThroughput, coreConfig.FloatMulPortNumbers[i]);
			FUs[FunctionalUnitType.floatMul.ordinal()][i] = FU;
		}
		
		//float Divs
		FUs[FunctionalUnitType.floatDiv.ordinal()] = new FunctionalUnit[coreConfig.FloatDivNum];
		for(int i = 0; i < coreConfig.FloatDivNum; i++)
		{
			FunctionalUnit FU = new FunctionalUnit(FunctionalUnitType.floatDiv, coreConfig.FloatDivLatency,
					coreConfig.FloatDivReciprocalOfThroughput, coreConfig.FloatDivPortNumbers[i]);
			FUs[FunctionalUnitType.floatDiv.ordinal()][i] = FU;
		}
		
		//jump
		FUs[FunctionalUnitType.jump.ordinal()] = new FunctionalUnit[coreConfig.JumpNum];
		for(int i = 0; i < coreConfig.JumpNum; i++)
		{
			FunctionalUnit FU = new FunctionalUnit(FunctionalUnitType.jump, coreConfig.JumpLatency,
					coreConfig.JumpReciprocalOfThroughput, coreConfig.JumpPortNumbers[i]);
			FUs[FunctionalUnitType.jump.ordinal()][i] = FU;
		}
		
		//memory
		FUs[FunctionalUnitType.memory.ordinal()] = new FunctionalUnit[coreConfig.MemoryNum];
		for(int i = 0; i < coreConfig.MemoryNum; i++)
		{
			FunctionalUnit FU = new FunctionalUnit(FunctionalUnitType.memory, coreConfig.MemoryLatency,
					coreConfig.MemoryReciprocalOfThroughput, coreConfig.MemoryPortNumbers[i]);
			FUs[FunctionalUnitType.memory.ordinal()][i] = FU;
		}
		
		this.numPorts = coreConfig.ExecutionCoreNumPorts;
		portUsedThisCycle = new boolean[numPorts];
	}
	
	//if an FU is available, it is assigned (timeTillFUAvailable is updated);
	//						negative of the FU instance is returned
	//else, the earliest time, at which an FU of the type becomes available, is returned
	
	public long requestFU(FunctionalUnitType FUType)
	{
		long currentTime = GlobalClock.getCurrentTime();
		int stepSize = core.getStepSize();
		
		long timeTillAvailable = FUs[FUType.ordinal()][0].getTimeWhenFUAvailable();
		
		for(int i = 0; i < FUs[FUType.ordinal()].length; i++)
		{
			boolean canUse = true;
			if(FUs[FUType.ordinal()][i].getTimeWhenFUAvailable() > currentTime)
			{
				canUse = false;
			}
			if(portUsedThisCycle[FUs[FUType.ordinal()][i].getPortNumber()] == true)
			{
				canUse = false;
			}
			
			if(canUse == true)
			{
				FUs[FUType.ordinal()][i].setTimeWhenFUAvailable(currentTime
						+ FUs[FUType.ordinal()][i].getReciprocalOfThroughput()*stepSize);
				
				if(FUType == FunctionalUnitType.integerALU)
				{
					//TODO this is overcounting in case of pipelined FUs
					incrementIntALUAccesses(FUs[FUType.ordinal()][i].getLatency());
				}
				else if(FUType == FunctionalUnitType.floatALU)
				{
					incrementFloatALUAccesses(FUs[FUType.ordinal()][i].getLatency());
				}
				else
				{
					incrementComplexALUAccesses(FUs[FUType.ordinal()][i].getLatency());
				}
				
				portUsedThisCycle[FUs[FUType.ordinal()][i].getPortNumber()] = true;
				
				return i * (-1);
			}
			if(FUs[FUType.ordinal()][i].getTimeWhenFUAvailable() < timeTillAvailable)
			{
				timeTillAvailable = FUs[FUType.ordinal()][i].getTimeWhenFUAvailable();
			}
		}
		
		return timeTillAvailable;
	}
	
	public int getFULatency(FunctionalUnitType FUType)
	{
		return FUs[FUType.ordinal()][0].getLatency();
	}
	
	public int getNumberOfUnits(FunctionalUnitType FUType)
	{
		return FUs[FUType.ordinal()].length;
	}
	
	public long getTimeWhenFUAvailable(FunctionalUnitType _FUType, int _FUInstance)
	{
		return FUs[_FUType.ordinal()][_FUInstance].getTimeWhenFUAvailable();
	}
	
	void incrementIntALUAccesses(int incrementBy)
	{
		numIntALUAccesses += incrementBy;
	}
	
	void incrementFloatALUAccesses(int incrementBy)
	{
		numFloatALUAccesses += incrementBy;
	}
	
	void incrementComplexALUAccesses(int incrementBy)
	{
		numComplexALUAccesses += incrementBy;
	}
	
	public EnergyConfig calculateAndPrintEnergy(FileWriter outputFileWriter, String componentName) throws IOException
	{
		EnergyConfig totalPower = new EnergyConfig(0, 0);
		EnergyConfig intALUPower = new EnergyConfig(core.getIntALUPower(), numIntALUAccesses);
		totalPower.add(totalPower, intALUPower);
		EnergyConfig floatALUPower = new EnergyConfig(core.getFloatALUPower(), numFloatALUAccesses);
		totalPower.add(totalPower, floatALUPower);
		EnergyConfig complexALUPower = new EnergyConfig(core.getComplexALUPower(), numComplexALUAccesses);
		totalPower.add(totalPower, complexALUPower);
		
		intALUPower.printEnergyStats(outputFileWriter, componentName + ".intALU");
		floatALUPower.printEnergyStats(outputFileWriter, componentName + ".floatALU");
		complexALUPower.printEnergyStats(outputFileWriter, componentName + ".complexALU");
		
		return totalPower;
	}
	
	public void clearPortUsage()
	{
		for(int i = 0; i < numPorts; i++)
		{
			portUsedThisCycle[i] = false;
		}
	}

}
