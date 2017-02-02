package pipeline.outoforder;

import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.Instruction;
import generic.OperationType;
import generic.PinPointsProcessing;
import generic.PortType;
import generic.RequestType;
import generic.SimulationElement;
import generic.Statistics;

import java.io.FileWriter;
import java.io.IOException;

import main.CustomObjectPool;
import config.EnergyConfig;
import config.SimulationConfig;

public class ReorderBuffer extends SimulationElement{
	
	private Core core;	
	OutOrderExecutionEngine execEngine;
	int retireWidth;
	
	ReorderBufferEntry[] ROB;
	int MaxROBSize;	
	int head;
	int tail;
	
	int stall1Count;
	int stall2Count;
	int stall3Count;
	int stall4Count;
	int stall5Count;
	long branchCount;
	long mispredCount;
	long lastValidIPSeen;
	
	long numAccesses;

	public ReorderBuffer(Core _core, OutOrderExecutionEngine execEngine)
	{
		super(PortType.Unlimited, -1, -1, -1, -1);
		
		core = _core;
		this.execEngine = execEngine;
		retireWidth = core.getRetireWidth();
		
		MaxROBSize = core.getReorderBufferSize();
		head = -1;
		tail = -1;
		ROB = new ReorderBufferEntry[MaxROBSize];		
		for(int i = 0; i < MaxROBSize; i++)
		{
			ROB[i] = new ReorderBufferEntry(i, execEngine);
		}
		
		stall1Count = 0;
		stall2Count = 0;
		stall3Count = 0;
		stall4Count = 0;
		stall5Count = 0;
		mispredCount = 0;
		branchCount = 0;
		lastValidIPSeen = -1;		
	}
	
	//creates a  new ROB entry, initialises it, and returns it
	//check if there is space in ROB before calling this function
	public ReorderBufferEntry addInstructionToROB(Instruction newInstruction, int threadID)
	{
		if(!isFull())
		{
			tail = (tail + 1)%MaxROBSize;
			if(head == -1)
			{
				head = 0;
			}
			ReorderBufferEntry newReorderBufferEntry = ROB[tail];
			
			if(newReorderBufferEntry.isValid() == true)
			{
				System.out.println("new rob entry is alread valid");
			}
			
			newReorderBufferEntry.setInstruction(newInstruction);
			newReorderBufferEntry.setThreadID(threadID);
			newReorderBufferEntry.setOperand1PhyReg1(-1);
			newReorderBufferEntry.setOperand1PhyReg2(-1);
			newReorderBufferEntry.setOperand2PhyReg1(-1);
			newReorderBufferEntry.setOperand2PhyReg2(-1);
			newReorderBufferEntry.setPhysicalDestinationRegister(-1);
			newReorderBufferEntry.setRenameDone(false);
			newReorderBufferEntry.setOperand11Available(false);
			newReorderBufferEntry.setOperand12Available(false);
			newReorderBufferEntry.setOperand1Available(false);
			newReorderBufferEntry.setOperand21Available(false);
			newReorderBufferEntry.setOperand22Available(false);
			newReorderBufferEntry.setOperand2Available(false);
			newReorderBufferEntry.setIssued(false);
			newReorderBufferEntry.setFUInstance(-1);
			newReorderBufferEntry.setExecuted(false);
			newReorderBufferEntry.setWriteBackDone1(false);
			newReorderBufferEntry.setWriteBackDone2(false);
			newReorderBufferEntry.setAssociatedIWEntry(null);
			
			newReorderBufferEntry.setValid(true);
			
			incrementNumAccesses(1);
			
			return newReorderBufferEntry;
		}
		
		return null;
	}
	
	public void performCommits()
	{	
		if(execEngine.isToStall1())
		{
			stall1Count++;
		}
		if(execEngine.isToStall2())
		{
			stall2Count++;
		}
		if(execEngine.isToStall3())
		{
			stall3Count++;
		}
		if(execEngine.isToStall4())
		{
			stall4Count++;
		}
		if(execEngine.isToStall5())
		{
			stall5Count++;
		}
		
		if(execEngine.isToStall5() == true /*pipeline stalled due to branch mis-prediction*/)
		{
			return;
		}
		
		boolean anyMispredictedBranch = false;
		
		for(int no_insts = 0; no_insts < retireWidth; no_insts++)
		{
			if(head == -1)
			{
				//ROB empty .. does not mean execution has completed
				return;
			}
			
			ReorderBufferEntry first = ROB[head];
			Instruction firstInstruction = first.getInstruction();
			OperationType firstOpType = firstInstruction.getOperationType();								
			
			if(first.isWriteBackDone() == true)
			{
				//has a thread finished?
				if(firstOpType==OperationType.inValid)
				{
					this.core.currentThreads--;
					
					if(this.core.currentThreads < 0)
					{
						this.core.currentThreads=0;
						System.out.println("num threads < 0");
					}
					
					if(this.core.currentThreads == 0)
					{   //set exec complete only if there are no other thread already 
														  //assigned to this pipeline	
						execEngine.setExecutionComplete(true);
					}
					
					if(SimulationConfig.pinpointsSimulation == false)
					{
						setTimingStatistics();
						setPerCoreMemorySystemStatistics();
					}
					else
					{
						PinPointsProcessing.processEndOfSlice();
					}
				}
				
				//if store, and if store not yet validated
				if(firstOpType == OperationType.store && !first.getLsqEntry().isValid())
				{
					break;
				}
				
				//update last valid IP seen
				if(firstInstruction.getCISCProgramCounter() != -1)
				{
					lastValidIPSeen = firstInstruction.getCISCProgramCounter();
				}
				
				//branch prediction
				if(firstOpType == OperationType.branch)
				{
					//perform prediction
					boolean prediction = execEngine.getBranchPredictor().predict(
																		lastValidIPSeen,
																		first.getInstruction().isBranchTaken());
					if(prediction != first.getInstruction().isBranchTaken())
					{	
						anyMispredictedBranch = true;
						mispredCount++;
					}	
					this.execEngine.getBranchPredictor().incrementNumAccesses(1);
					
					//train predictor
					execEngine.getBranchPredictor().Train(
							lastValidIPSeen,
							firstInstruction.isBranchTaken(),
							prediction
							);	
					this.execEngine.getBranchPredictor().incrementNumAccesses(1);

					branchCount++;
				}
				
				//Signal LSQ for committing the Instruction at the queue head
				if(firstOpType == OperationType.load || firstOpType == OperationType.store)
				{
					if (!first.getLsqEntry().isValid())
					{
						misc.Error.showErrorAndExit("The committed entry is not valid");
					}
					
					execEngine.getCoreMemorySystem().issueLSQCommit(first);
				}
				
				//free ROB entry
				retireInstructionAtHead();
				
				//increment number of instructions executed
				core.incrementNoOfInstructionsExecuted();
				if(core.getNoOfInstructionsExecuted()%1000000==0)
				{
					System.out.println(core.getNoOfInstructionsExecuted()/1000000 + " million done on " + core.getCore_number());
				}

				//debug print
				if(SimulationConfig.debugMode)
				{
					System.out.println("committed : " + GlobalClock.getCurrentTime()/core.getStepSize() + " : " + firstInstruction);
//						System.out.println(first.getOperand1PhyReg1()
//								+ " : " + first.getOperand2PhyReg1()
//								+ " : " + first.getPhysicalDestinationRegister());
				}
				
				//return instruction to pool
				returnInstructionToPool(firstInstruction);
			}
			else
			{
				//commits must be in order
				break;
			}
		}
		
		if(anyMispredictedBranch)
		{
			handleBranchMisprediction();
		}
	}
	
	void retireInstructionAtHead()
	{
		ROB[head].setValid(false);
		ROB[head].setInstruction(null);
		if(head == tail)
		{
			head = -1;
			tail = -1;
		}
		else
		{
			head = (head+1)%MaxROBSize;
		}
		incrementNumAccesses(1);
	}
	
	void handleBranchMisprediction()
	{
		if(SimulationConfig.debugMode)
		{
			System.out.println("branch mispredicted");
		}
		
		if(core.getBranchMispredictionPenalty() <= 0)
		{
			//if branch mispredictions have no penalty
			return;
		}
		
		//impose branch mis-prediction penalty
		execEngine.setToStall5(true);
		
		//set-up event that signals end of misprediction penalty period
		core.getEventQueue().addEvent(
				new MispredictionPenaltyCompleteEvent(
						GlobalClock.getCurrentTime() + core.getBranchMispredictionPenalty() * core.getStepSize(),
						null,
						this,
						RequestType.MISPRED_PENALTY_COMPLETE));
		
	}
	
	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		
		if(event.getRequestType() == RequestType.MISPRED_PENALTY_COMPLETE)
		{
			completeMispredictionPenalty();
		}
		
	}
	
	void completeMispredictionPenalty()
	{
		execEngine.setToStall5(false);
	}
	
	void returnInstructionToPool(Instruction instruction)
	{
		try {
			CustomObjectPool.getInstructionPool().returnObject(instruction);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//debug helper - print contents of ROB
	public void dump()
	{
		ReorderBufferEntry e;
		
		System.out.println();
		System.out.println();
		System.out.println("----------ROB dump---------");
		
		if(head == -1)
		{
			return;
		}
		
		int i = head;
		while(true)
		{
			e = ROB[i];
			System.out.println(e.getOperand1PhyReg1() + " ; " + e.getOperand1PhyReg2() + " ; "
					+ e.getOperand2PhyReg1() + " ; "+ e.getOperand2PhyReg2() + " ; " + 
					e.getPhysicalDestinationRegister() + " ; " + 
					e.getIssued() + " ; " + 
					e.getFUInstance() + " ; " + e.getExecuted());
			if(e.getAssociatedIWEntry() != null)
			{
				System.out.println(e.isOperand1Available()
						 + " ; " + e.isOperand2Available());
			}
			System.out.println(e.getInstruction().toString());
			
			if(i == tail)
			{
				break;
			}
			i = (i+1)%MaxROBSize;
		}
		System.out.println();
	}
	
	public void setTimingStatistics()
	{
		core.setCoreCyclesTaken(GlobalClock.getCurrentTime()/core.getStepSize());
	}
	
	public void setPerCoreMemorySystemStatistics()
	{
		if(SimulationConfig.collectInsnWorkingSetInfo==true) {
			setInsWorkingSetStats();
		}
		
		if(SimulationConfig.collectDataWorkingSetInfo==true) {
			setDataWorkingSetStats();
		}
		
	}
	
	private void setInsWorkingSetStats() {
		Statistics.setMinInsWorkingSetSize(execEngine.getCoreMemorySystem().getiCache().minWorkingSetSize, 
			core.getCore_number());
		Statistics.setMaxInsWorkingSetSize(execEngine.getCoreMemorySystem().getiCache().maxWorkingSetSize, 
			core.getCore_number());
		Statistics.setTotalInsWorkingSetSize(execEngine.getCoreMemorySystem().getiCache().totalWorkingSetSize, 
			core.getCore_number());
		Statistics.setNumInsWorkingSetNoted(execEngine.getCoreMemorySystem().getiCache().numFlushesInWorkingSet, 
			core.getCore_number());
		Statistics.setNumInsWorkingSetHits(execEngine.getCoreMemorySystem().getiCache().numWorkingSetHits, 
			core.getCore_number());
		Statistics.setNumInsWorkingSetMisses(execEngine.getCoreMemorySystem().getiCache().numWorkingSetMisses, 
			core.getCore_number());
	}
	
	private void setDataWorkingSetStats() {
		Statistics.setMinDataWorkingSetSize(execEngine.getCoreMemorySystem().getL1Cache().minWorkingSetSize, 
			core.getCore_number());
		Statistics.setMaxDataWorkingSetSize(execEngine.getCoreMemorySystem().getL1Cache().maxWorkingSetSize, 
			core.getCore_number());
		Statistics.setTotalDataWorkingSetSize(execEngine.getCoreMemorySystem().getL1Cache().totalWorkingSetSize, 
			core.getCore_number());
		Statistics.setNumDataWorkingSetNoted(execEngine.getCoreMemorySystem().getL1Cache().numFlushesInWorkingSet, 
			core.getCore_number());
		Statistics.setNumDataWorkingSetHits(execEngine.getCoreMemorySystem().getL1Cache().numWorkingSetHits, 
			core.getCore_number());
		Statistics.setNumDataWorkingSetMisses(execEngine.getCoreMemorySystem().getL1Cache().numWorkingSetMisses, 
			core.getCore_number());
	}

	public boolean isFull()
	{
		if((tail - head) == MaxROBSize - 1)
		{
			return true;
		}
		if((tail - head) == -1)
		{
			return true;
		}
		return false;
	}
	
	public ReorderBufferEntry[] getROB()
	{
		return ROB;
	}
	
	public int indexOf(ReorderBufferEntry reorderBufferEntry)
	{
		if(reorderBufferEntry.pos - head >= 0)
		{
			return (reorderBufferEntry.pos - head);
		}
		else
		{
			return (reorderBufferEntry.pos - head + MaxROBSize);
		}
	}
	
	public int getMaxROBSize()
	{
		return MaxROBSize;
	}

	public int getStall1Count() {
		return stall1Count;
	}

	public int getStall2Count() {
		return stall2Count;
	}

	public int getStall3Count() {
		return stall3Count;
	}

	public int getStall4Count() {
		return stall4Count;
	}

	public int getStall5Count() {
		return stall5Count;
	}

	public long getBranchCount() {
		return branchCount;
	}

	public long getMispredCount() {
		return mispredCount;
	}
	
	void incrementNumAccesses(int incrementBy)
	{
		numAccesses += incrementBy;
	}
	
	public EnergyConfig calculateAndPrintEnergy(FileWriter outputFileWriter, String componentName) throws IOException
	{
		EnergyConfig power = new EnergyConfig(core.getRobPower(), numAccesses);
		power.printEnergyStats(outputFileWriter, componentName);
		return power;
	}

}