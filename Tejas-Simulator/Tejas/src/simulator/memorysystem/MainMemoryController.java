package memorysystem;

import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import generic.SimulationElement;

import java.io.FileWriter;
import java.io.IOException;

import config.EnergyConfig;
import config.SystemConfig;

import main.ArchitecturalComponent;

public class MainMemoryController extends SimulationElement
{
	long numAccesses;
	int id = 0;
        boolean mcdram = false;
        boolean count = false;
        public int[] accesses = new int[SystemConfig.NoOfCores];

	public MainMemoryController() {
		super(SystemConfig.mainMemPortType,
                      SystemConfig.mainMemoryAccessPorts,
                      SystemConfig.mainMemoryPortOccupancy,
                      SystemConfig.mainMemoryLatency,
                      SystemConfig.mainMemoryFrequency
                      );
		for (int i=0;i<SystemConfig.NoOfCores;i++) {
		    accesses[i] = 0;
		}

	}

        public MainMemoryController(boolean knl, int id) {
		super(SystemConfig.mcdramPortType,
				SystemConfig.mcdramAccessPorts,
				SystemConfig.mcdramPortOccupancy,
				SystemConfig.mcdramLatency,
				SystemConfig.mcdramFrequency
				);
		this.mcdram = knl;
		this.id = id;
		for (int i=0;i<SystemConfig.NoOfCores;i++) {
		    accesses[i] = 0;
		}
	}
	
	public void handleEvent(EventQueue eventQ, Event event)
	{
		if (event.getRequestType() == RequestType.Cache_Read)
		{
			AddressCarryingEvent e = new AddressCarryingEvent(eventQ, 0,
					this, event.getRequestingElement(),	RequestType.Mem_Response,
					((AddressCarryingEvent)event).getAddress());
			
			getComInterface().sendMessage(e);
		}
		else if (event.getRequestType() == RequestType.Cache_Write)
		{
			//Just to tell the requesting things that the write is completed
		}
		//System.out.println("coreId = " + event.coreId + "\tmcdramId = " + this.id);
		if ((event.coreId>=0)) {
		    accesses[event.coreId]++;
		}

		incrementNumAccesses();
	}
	
	void incrementNumAccesses()
	{
		numAccesses += 1;
	}
	
	public EnergyConfig calculateAndPrintEnergy(FileWriter outputFileWriter, String componentName) throws IOException
	{
		EnergyConfig power = new EnergyConfig(SystemConfig.mainMemoryControllerPower, numAccesses);
		power.printEnergyStats(outputFileWriter, componentName);
		return power;
	}
	
	public EnergyConfig calculateEnergy(FileWriter outputFileWriter) throws IOException
	{
		EnergyConfig power = new EnergyConfig(SystemConfig.mainMemoryControllerPower, numAccesses);
		return power;
	}
}
