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
	long numAccesses = 0;
	long numResponses = 0;
	long numWrites = 0;

        public int[] responses = new int[SystemConfig.NoOfCores];
        public int[] accesses = new int[SystemConfig.NoOfCores];
        public int[] writes = new int[SystemConfig.NoOfCores];

	int id = 0;
        boolean mcdram = false;
        boolean count = false;
        
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

                        incrementNumResponses(event);
			AddressCarryingEvent e = new AddressCarryingEvent(eventQ, 0,
					this, event.getRequestingElement(),	RequestType.Mem_Response,
					((AddressCarryingEvent)event).getAddress());
			
			getComInterface().sendMessage(e);

		}
		else if (event.getRequestType() == RequestType.Cache_Write)
		{
			//Just to tell the requesting things that the write is completed
		        incrementNumWrites(event);

		}

		incrementNumAccesses(event);
                //System.out.println("coreId = " + event.coreId + "\tmcdramId = " + this.id);

	}
	
	void incrementNumAccesses(Event event)
	{
	        if ((event.coreId>=0)) {
	             accesses[event.coreId]++;
	        }
		numAccesses += 1;
	}
	
	void incrementNumResponses(Event event)
	{
            if ((event.coreId>=0)) {
                responses[event.coreId]++;
            }
	    numResponses += 1;
	}
	
        void incrementNumWrites(Event event)
        {
            if ((event.coreId>=0)) {
                writes[event.coreId]++;
            }
            numWrites += 1;
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
