package generic;

public class Port 
{
	private PortType portType;
	private int noOfPorts;

	//occupancy defines the number of clockCycles needed for  
	//a single transfer on the port.
	private long latencyOfConnectedElement;
	private long occupancy;
	private long[] portBusyUntil;
	
	//NOTE : Time is in terms of GlobalClock cycles
	
	public Port(PortType portType, int noOfPorts, long occupancy, long latencyOfConnectedElement)
	{
		this.portType = portType;
		this.latencyOfConnectedElement = latencyOfConnectedElement;
		
		//initialise no. of ports and the occupancy.
		if(portType==PortType.Unlimited)
		{
			return;
		}
		
		else if(portType!=PortType.Unlimited && 
				noOfPorts>0 && occupancy>0)
		{
			// For a FCFS or a priority based port, noOfPorts and
			// occupancy must be non-zero.
			this.noOfPorts = noOfPorts;
			this.occupancy = occupancy;
			
			//set busy field of all ports to 0
			portBusyUntil = new long[noOfPorts];
					
			for(int i=0; i < noOfPorts; i++)
			{
				this.portBusyUntil[i] = 0;
			}
		}
		
		else
		{
			// Display an error for invalid initialization.
			misc.Error.showErrorAndExit("Invalid initialization of port !!\n" +
					"port-type=" + portType + " no-of-ports=" + noOfPorts + 
					" occupancy=" + occupancy);
		}
	}
	
	public void put(Event event)
	{
		//overloaded method.
		this.put(event, 1);
	}
	
	public void put(Event event, int noOfSlots)
	{
		if(this.portType == PortType.Unlimited)
		{
			// For an unlimited port, set the event with current-time.
			event.addEventTime(GlobalClock.getCurrentTime() + latencyOfConnectedElement);
			event.getEventQ().addEvent(event);
			return;
		}
		
		else if(this.portType==PortType.FirstComeFirstServe)
		{
			//else return the slot that will be available earliest.
			int availablePortID = 0;
			
			// find the first available port
			for(int i=0; i<noOfPorts; i++)
			{
				if(portBusyUntil[i]< 
					portBusyUntil[availablePortID])
				{
					availablePortID = i;
				}
			}
			
			// If a port is available, set its portBusyUntil field to
			// current time
			if(portBusyUntil[availablePortID]<
				GlobalClock.getCurrentTime())
			{
				// this port will be busy for next occupancy cycles
				portBusyUntil[availablePortID] = 
					GlobalClock.getCurrentTime() + occupancy;
			}else{
				// set the port as busy for occupancy cycles
				portBusyUntil[availablePortID] += occupancy;	
			}
						
			// set the time of the event
			event.addEventTime(portBusyUntil[availablePortID] + latencyOfConnectedElement);
			
			// add event in the eventQueue
			event.getEventQ().addEvent(event);
		}
	}

	public int getNoOfPorts() {
		return noOfPorts;
	}

	public PortType getPortType() {
		return portType;
	}
}