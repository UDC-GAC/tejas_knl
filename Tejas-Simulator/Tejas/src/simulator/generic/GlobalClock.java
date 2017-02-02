package generic;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import memorysystem.CoreMemorySystem;

import memorysystem.Cache;
import memorysystem.MemorySystem;
import config.EnergyConfig;
import config.SystemConfig;

public class GlobalClock {
	
	static long currentTime;
	static int stepSize;

	public static void systemTimingSetUp(Core[] cores)
	{
		currentTime = 0;
		stepSize = 1;
		
		//TODO setting up of a heterogeneous clock environment
		
		//populate time_periods[]
		int[] time_periods = new int[SystemConfig.NoOfCores];
		int i = 0;
		int seed = Integer.MAX_VALUE;
		String cacheName;
		Cache cache;
		
		/*
		for(i = 0; i < SystemConfig.NoOfCores; i++)
		{
			time_periods[i] = Math.round(100000000/cores[i].getFrequency());
			if(time_periods[i] < seed)
			{
				seed = time_periods[i];
			}
		}
		for (Enumeration<String> cacheNameSet = cacheList.keys(); cacheNameSet.hasMoreElements(); )
		{
			cacheName = cacheNameSet.nextElement();
			cache = cacheList.get(cacheName);
			time_periods[i] = Math.round(100000000/cache.getFrequency());
			if(time_periods[i] < seed)
			{
				seed = time_periods[i];
			}
			i++;
		}
		time_periods[i] = Math.round(100000000/MemorySystem.mainMemory.getFrequency());
		if(time_periods[i] < seed)
		{
			seed = time_periods[i];
		}
				
		//compute HCF
		//TODO look for a better algorithm
		int j;
		boolean flag;
		int HCF = 1;
		for(i = 1; seed/i > 1; i++)
		{
			if(seed%i == 0)
			{
				flag = true;
				for(j = 0; j < SystemConfig.NoOfCores + SystemConfig.declaredCaches.size() + 1; j++)
				{
					if(time_periods[j]%(seed/i) != 0)
					{
						flag = false;
						break;
					}
				}
				if(flag == true)
				{
					HCF = (seed/i);
					break;
				}
			}
		}
		
		//set step sizes of components
		for(i = 0; i < SystemConfig.NoOfCores; i++)
		{
			cores[i].setStepSize(time_periods[i]/HCF);
			CoreMemorySystem coreMemSys;
			
			if (cores[i].isPipelineStatistical)
				coreMemSys = cores[i].getStatisticalPipeline().coreMemSys;
			else if(cores[i].isPipelineInorder)
				coreMemSys = cores[i].getExecutionEngineIn().coreMemorySystem;
			else
				coreMemSys = cores[i].getExecEngine().coreMemSys;
			
			coreMemSys.getL1Cache().setStepSize(cores[i].getStepSize());
			coreMemSys.getLsqueue().setStepSize(cores[i].getStepSize());
			coreMemSys.getTLBuffer().setStepSize(cores[i].getStepSize());
			//System.out.println(cores[i].getStepSize());
		}
		for (Enumeration<String> cacheNameSet = cacheList.keys(); cacheNameSet.hasMoreElements();)
		{
			cacheName = cacheNameSet.nextElement();
			cache = cacheList.get(cacheName);
			cache.setStepSize(time_periods[i++]/HCF);
			//System.out.println(cache.getStepSize());
		}
		MemorySystem.mainMemory.setStepSize(time_periods[i]/HCF);
		//System.out.println(MemorySystem.mainMemStepSize);
		
		*/
		
		int[] freq_list = new int[SystemConfig.NoOfCores];
		
		for(i = 0; i < SystemConfig.NoOfCores; i++)
		{
			freq_list[i] = Math.round(cores[i].getFrequency()/100);
		}
		
		//freq_list[i] = Math.round(MemorySystem.mainMemory.getFrequency()/100);
		
		//compute HCF
		//TODO look for a better algorithm
		int j;
		boolean flag = false;
		boolean HCFFound = false;
		int HCF = 1;
		for(i = 1; ; i++)
		{
			flag = true;
			for(j = 0; j < SystemConfig.NoOfCores; j++)
			{
				if(freq_list[j]%i != 0)
				{
					flag = false;
					break;
				}
				
				if(freq_list[j] == i)
				{
					HCFFound = true;
				}					
			}
			
			if(flag == true)
			{
				HCF = i;
			}
			
			if(HCFFound == true)
				break;
		}
		
		//System.out.println("HCF = " + HCF);
		
		for(i = 0; i < SystemConfig.NoOfCores; i++)
		{
			freq_list[i] = freq_list[i]/HCF;
		}
		
		int LCM, cur = freq_list[0];
		
		while(true)
		{
			flag = true;
			for(i = 0; i < SystemConfig.NoOfCores ; i++)
			{
				if(cur%freq_list[i] != 0)
				{
					flag = false;
					break;
				}
			}
			if(flag == true)
			{
				LCM = cur;
				break;
			}
			cur = cur + freq_list[0];
		}
		
		//System.out.println("LCM = " + LCM);
		
		//set step sizes of components
		for(i = 0; i < SystemConfig.NoOfCores; i++)
		{
			cores[i].setStepSize(LCM/freq_list[i]);
			CoreMemorySystem coreMemSys;
			
			coreMemSys = cores[i].getExecEngine().getCoreMemorySystem();
			
			//coreMemSys.getL1Cache().setStepSize(cores[i].getStepSize());
			//coreMemSys.getLsqueue().setStepSize(cores[i].getStepSize());
			//coreMemSys.getTLBuffer().setStepSize(cores[i].getStepSize());
			//System.out.println(cores[i].getStepSize());
		}
		/*for (Enumeration<String> cacheNameSet = cacheList.keys(); cacheNameSet.hasMoreElements(); Nothing)
		{
			cacheName = cacheNameSet.nextElement();
			cache = cacheList.get(cacheName);
			cache.setStepSize(LCM/freq_list[i++]);
			//System.out.println(cache.getStepSize());
		}
		MemorySystem.mainMemory.setStepSize(LCM/freq_list[i]);*/
		//System.out.println(MemorySystem.mainMemStepSize);
		
		for(i = 0; i < SystemConfig.NoOfCores; i++)
		{
			// System.out.println("step size of core " + i + " is " + cores[i].getStepSize());
		}
		/*for (Enumeration<String> cacheNameSet = cacheList.keys(); cacheNameSet.hasMoreElements(); )
		{
			cacheName = cacheNameSet.nextElement();
			cache = cacheList.get(cacheName);
			System.out.println("step size of cache " + (i - SystemConfig.NoOfCores) + " is " + cache.getStepSize());
			//System.out.println(cache.getStepSize());
		}
		System.out.println("step size of main memory is " + MemorySystem.mainMemory.getStepSize());
		*/
	}

	public static long getCurrentTime() {
		return GlobalClock.currentTime;
	}

	public static void setCurrentTime(long currentTime) {
		GlobalClock.currentTime = currentTime;
	}
	
	public static void incrementClock()
	{
		GlobalClock.currentTime += GlobalClock.stepSize;
	}

	public static int getStepSize() {
		return GlobalClock.stepSize;
	}

	public static void setStepSize(int stepSize) {
		GlobalClock.stepSize = stepSize;
	}
	
	public static EnergyConfig calculateAndPrintEnergy(FileWriter outputFileWriter, String componentName) throws IOException
	{
		double leakagePower = SystemConfig.globalClockPower.leakageEnergy;
		double dynamicPower = SystemConfig.globalClockPower.dynamicEnergy;
		
		EnergyConfig power = new EnergyConfig(leakagePower, dynamicPower);
		
		power.printEnergyStats(outputFileWriter, componentName);
		
		return power;
	}
}
