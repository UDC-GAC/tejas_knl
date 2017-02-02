package pipeline.outoforder;

import pipeline.FunctionalUnitType;
import pipeline.OpTypeToFUTypeMapping;
import config.SimulationConfig;
import generic.Core;
import generic.ExecCompleteEvent;
import generic.GlobalClock;
import generic.Instruction;
import generic.OperationType;
import generic.RequestType;

/**
 * represents an entry in the instruction window
 */
public class IWEntry {
	
	Core core;
	OutOrderExecutionEngine execEngine;
	InstructionWindow instructionWindow;
	
	Instruction instruction;
	ReorderBufferEntry associatedROBEntry;
	OperationType opType;
	boolean isValid;
	int pos;

	public IWEntry(Core core, int pos,
			OutOrderExecutionEngine execEngine, InstructionWindow instructionWindow)
	{
		this.core = core;		
		this.execEngine = execEngine;
		this.instructionWindow = instructionWindow;
		
		this.pos = pos;
		isValid = false;
	}
	
	
	public boolean issueInstruction()
	{
		if(associatedROBEntry.isRenameDone() == false ||
				associatedROBEntry.getExecuted() == true)
		{
			misc.Error.showErrorAndExit("cannot issue this instruction");
		}
		
		if(associatedROBEntry.getIssued() == true)
		{
			misc.Error.showErrorAndExit("already issued!");
		}
		
		if(associatedROBEntry.isOperand1Available() && associatedROBEntry.isOperand2Available())
		{
			boolean issued = issueOthers();
			
			if(issued == true &&
					(opType == OperationType.load || opType == OperationType.store))
			{
				issueLoadStore();
			}
			
			return issued;
		}		

		return false;
	}
	
	void issueLoadStore()
	{
		//assertions
		if(associatedROBEntry.getLsqEntry().isValid() == true)
		{
			misc.Error.showErrorAndExit("attempting to issue a load/store.. address is already valid");
		}		
		if(associatedROBEntry.getLsqEntry().isForwarded() == true)
		{
			misc.Error.showErrorAndExit("attempting to issue a load/store.. value forwarded is already valid");
		}
		
		associatedROBEntry.setIssued(true);
		if(opType == OperationType.store)
		{
			//stores are issued at commit stage
			
			associatedROBEntry.setExecuted(true);
			associatedROBEntry.setWriteBackDone1(true);
			associatedROBEntry.setWriteBackDone2(true);
		}
		
		//remove IW entry
		instructionWindow.removeFromWindow(this);
		
		//tell LSQ that address is available
		execEngine.getCoreMemorySystem().issueRequestToLSQ(
				null, 
				associatedROBEntry);

		if(SimulationConfig.debugMode)
		{
			System.out.println("issue : " + GlobalClock.getCurrentTime()/core.getStepSize() + " : "  + associatedROBEntry.getInstruction());
		}
	}
	
	boolean issueOthers()
	{
		FunctionalUnitType FUType = OpTypeToFUTypeMapping.getFUType(opType);
		if(FUType == FunctionalUnitType.inValid)
		{
			associatedROBEntry.setIssued(true);
			associatedROBEntry.setFUInstance(0);
			
			//remove IW entry
			instructionWindow.removeFromWindow(this);
			
			return true;
		}
		
		long FURequest = 0;	//will be <= 0 if an FU was obtained
		//will be > 0 otherwise, indicating how long before
		//	an FU of the type will be available

		FURequest = execEngine.getExecutionCore().requestFU(FUType);
		
		if(FURequest <= 0)
		{
			if(opType != OperationType.load && opType != OperationType.store)
			{
				associatedROBEntry.setIssued(true);
				associatedROBEntry.setFUInstance((int) ((-1) * FURequest));
				
				//remove IW entry
				instructionWindow.removeFromWindow(this);			
			
				core.getEventQueue().addEvent(
						new BroadCastEvent(
								GlobalClock.getCurrentTime() + (execEngine.getExecutionCore().getFULatency(
										OpTypeToFUTypeMapping.getFUType(opType)) - 1) * core.getStepSize(),
								null, 
								execEngine.getExecuter(),
								RequestType.BROADCAST,
								associatedROBEntry));
				
				core.getEventQueue().addEvent(
						new ExecCompleteEvent(
								null,
								GlobalClock.getCurrentTime() + execEngine.getExecutionCore().getFULatency(
										OpTypeToFUTypeMapping.getFUType(opType)) * core.getStepSize(),
								null, 
								execEngine.getExecuter(),
								RequestType.EXEC_COMPLETE,
								associatedROBEntry));
			}

			if(SimulationConfig.debugMode)
			{
				System.out.println("issue : " + GlobalClock.getCurrentTime()/core.getStepSize() + " : "  + associatedROBEntry.getInstruction());
			}
			
			return true;
		}
		
		return false;
	}

	
	public ReorderBufferEntry getAssociatedROBEntry() {
		return associatedROBEntry;
	}
	public void setAssociatedROBEntry(ReorderBufferEntry associatedROBEntry) {
		this.associatedROBEntry = associatedROBEntry;
	}
	
	public boolean isValid() {
		return isValid;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

	public Instruction getInstruction() {
		return instruction;
	}

	public void setInstruction(Instruction instruction) {
		this.instruction = instruction;
		opType = instruction.getOperationType();
	}

}