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

	Contributors:  Prathmesh Kallurkar, Rajshekar Kalyappam
*****************************************************************************/
package generic;

import java.io.Serializable;

import main.CustomObjectPool;

public class Instruction implements Serializable
{
	private OperationType type;
	private Operand sourceOperand1;
	
	private long sourceOperand1MemValue;
	private long sourceOperand2MemValue;
	private long destinationOperandMemValue;

	private Operand sourceOperand2;
	private Operand destinationOperand;
	
	private long ciscProgramCounter;
	
	private boolean branchTaken;
	private long branchTargetAddress;
	private long serialNo;
	private int threadID;
	
	public Instruction()
	{
		this.sourceOperand1 = null;
		this.sourceOperand2 = null;
		this.destinationOperand = null;
	}
	
	public void clear()
	{
		this.type = null;
		this.sourceOperand1 = null;
		this.sourceOperand2 = null;
		this.destinationOperand = null;
	}
	
	public Instruction(OperationType type, Operand sourceOperand1,
			Operand sourceOperand2, Operand destinationOperand)
	{
		this.type = type;
		this.sourceOperand1 = sourceOperand1;
		this.sourceOperand2 = sourceOperand2;
		this.destinationOperand = destinationOperand;
	}
	
	private void set(OperationType type, Operand sourceOperand1,
			Operand sourceOperand2, Operand destinationOperand)
	{
		this.type = type;
		this.sourceOperand1 = sourceOperand1;
		this.sourceOperand2 = sourceOperand2;
		this.destinationOperand = destinationOperand;
	}
	
//	/* our clone constructor */
//	public Instruction(Instruction oldInstruction)
//	{
//		this.type=oldInstruction.type;
//		
//		if(oldInstruction.sourceOperand1==null)
//			{this.sourceOperand1=null;}
//		else
//			{this.sourceOperand1=new Operand(oldInstruction.sourceOperand1);}
//		
//		if(oldInstruction.sourceOperand2==null)
//			{this.sourceOperand2=null;}
//		else
//			{this.sourceOperand2=new Operand(oldInstruction.sourceOperand2);}
//		
//		if(oldInstruction.destinationOperand==null)
//			{this.destinationOperand=null;}
//		else
//			{this.destinationOperand=new Operand(oldInstruction.destinationOperand);}
//		
//		this.riscProgramCounter=oldInstruction.riscProgramCounter;
//		this.ciscProgramCounter=oldInstruction.ciscProgramCounter;
//		this.branchTaken=oldInstruction.branchTaken;
//		this.branchTargetAddress=oldInstruction.branchTargetAddress;
//	}
	
	//all properties of sourceInstruction is copied to the current instruction
	public void copy(Instruction sourceInstruction)
	{
		this.type=sourceInstruction.type;

		this.ciscProgramCounter = sourceInstruction.ciscProgramCounter;
		
		this.branchTaken=sourceInstruction.branchTaken;
		this.branchTargetAddress=sourceInstruction.branchTargetAddress;

		this.sourceOperand1 = sourceInstruction.sourceOperand1;
		this.sourceOperand2 = sourceInstruction.sourceOperand2;
		this.destinationOperand = sourceInstruction.destinationOperand;
		
		this.sourceOperand1MemValue = sourceInstruction.sourceOperand1MemValue;
		this.sourceOperand2MemValue = sourceInstruction.sourceOperand2MemValue;
		this.destinationOperandMemValue = sourceInstruction.destinationOperandMemValue;
		
		this.serialNo = sourceInstruction.serialNo;
		this.threadID = sourceInstruction.threadID;
	}
	
	public static Instruction getIntALUInstruction(Operand sourceOperand1, Operand sourceOperand2, Operand destinationOperand)
	{
		Instruction ins = CustomObjectPool.getInstructionPool().borrowObject();
		ins.set(OperationType.integerALU, sourceOperand1, sourceOperand2,
			destinationOperand);
		return ins;
	}
	
	public static Instruction getInvalidInstruction()
	{
		Instruction ins = CustomObjectPool.getInstructionPool().borrowObject();
		ins.set(OperationType.inValid, null, null, null);
		return ins;
	}
	
	public static Instruction getSyncInstruction()
	{
		Instruction ins = CustomObjectPool.getInstructionPool().borrowObject();
		ins.set(OperationType.sync, null, null, null);
		return ins;
	}
	
	public static Instruction getMoveInstruction(Operand destinationOperand, Operand sourceOperand)
	{
		Instruction ins = CustomObjectPool.getInstructionPool().borrowObject();
		ins.set(OperationType.mov, sourceOperand, null, destinationOperand);
		return ins;
	}
	
	public static Instruction getNOPInstruction()
	{
		Instruction ins = CustomObjectPool.getInstructionPool().borrowObject();
		ins.set(OperationType.nop, null, null, null);
		return ins;
	}
	
	public static Instruction getIntegerDivisionInstruction(Operand sourceOperand1,
			Operand sourceOperand2, Operand destinationOperand)
	{
		Instruction ins = CustomObjectPool.getInstructionPool().borrowObject();
		ins.set(OperationType.integerDiv, sourceOperand1, sourceOperand2, 
				destinationOperand);
		return ins;
	}
	
	public static Instruction getIntegerMultiplicationInstruction(Operand sourceOperand1,
			Operand sourceOperand2, Operand destinationOperand)
	{
		Instruction ins = CustomObjectPool.getInstructionPool().borrowObject();
		ins.set(OperationType.integerMul, sourceOperand1, sourceOperand2, 
				destinationOperand);
		return ins;
	}
	
	public static Instruction getExchangeInstruction(Operand operand1, Operand operand2)
	{
		Instruction ins = CustomObjectPool.getInstructionPool().borrowObject();
		ins.set(OperationType.xchg, operand1, operand2, null);
		return ins;
	}
	
	public static Instruction getInterruptInstruction(Operand interruptNumber)
	{
		Instruction ins = CustomObjectPool.getInstructionPool().borrowObject();
		ins.set(OperationType.interrupt, interruptNumber, null, null);
		return ins;
	}
	
	public static Instruction getFloatingPointALU(Operand sourceOperand1, 
			Operand sourceOperand2, Operand destinationOperand)
	{
		Instruction ins = CustomObjectPool.getInstructionPool().borrowObject();
		ins.set(OperationType.floatALU, sourceOperand1, sourceOperand2,
				destinationOperand);
		return ins;
	}
	
	public static Instruction getFloatingPointMultiplication(Operand sourceOperand1, 
			Operand sourceOperand2, Operand destinationOperand)
	{
		Instruction ins = CustomObjectPool.getInstructionPool().borrowObject();
		ins.set(OperationType.floatMul, sourceOperand1, sourceOperand2,
				destinationOperand);
		return ins;
	}
	
	public static Instruction getFloatingPointDivision(Operand sourceOperand1, 
			Operand sourceOperand2, Operand destinationOperand)
	{
		Instruction ins = CustomObjectPool.getInstructionPool().borrowObject();
		ins.set(OperationType.floatDiv, sourceOperand1, sourceOperand2,
				destinationOperand);
		return ins;
	}
	
	
	public static Instruction getBranchInstruction(Operand newInstructionAddress)
	{
		Instruction ins = CustomObjectPool.getInstructionPool().borrowObject();
		ins.set(OperationType.branch, newInstructionAddress, null, null);
		return ins;
	}
	
	public static Instruction getUnconditionalJumpInstruction(Operand newInstructionAddress)
	{
		Instruction ins = CustomObjectPool.getInstructionPool().borrowObject();
		ins.set(OperationType.jump, newInstructionAddress, null, null);
		return ins;
	}
	
	public static Instruction getLoadInstruction(Operand memoryLocation, Operand destinationRegister)
	{
		Instruction ins = CustomObjectPool.getInstructionPool().borrowObject();
		ins.set(OperationType.load, memoryLocation,	null, destinationRegister);
		return ins;
	}
	
	public static Instruction getStoreInstruction(Operand memoryLocation, Operand sourceOperand)
	{
		Instruction ins = CustomObjectPool.getInstructionPool().borrowObject();
		ins.set(OperationType.store, memoryLocation, sourceOperand, null);
		return ins;
	}
	
	public long getCISCProgramCounter()
	{
		return ciscProgramCounter;
	}
	
	public void setCISCProgramCounter(long programCounter) 
	{
		this.ciscProgramCounter = programCounter;
	}
	
	public OperationType getOperationType()
	{
		return type;
	}
	
	public void setOperationType(OperationType operationType)
	{
		this.type = operationType;
	}
	
	public boolean isBranchTaken() {
		return branchTaken;
	}

	public void setBranchTaken(boolean branchTaken) {
		this.branchTaken = branchTaken;
	}


	public long getBranchTargetAddress() {
		return branchTargetAddress;
	}


	public void setBranchTargetAddress(long branchTargetAddress) {
		this.branchTargetAddress = branchTargetAddress;
	}

	public Operand getSourceOperand1()
	{
		return sourceOperand1;
	}
	
	public void setSourceOperand1(Operand sourceOperand1) {
		this.sourceOperand1 = sourceOperand1;
	}	
	
	public Operand getSourceOperand2()
	{
		return sourceOperand2;
	}

	public void setSourceOperand2(Operand sourceOperand2) {
		this.sourceOperand2 = sourceOperand2;
	}
	
	public Operand getDestinationOperand()
	{
		return destinationOperand;
	}

	public void setDestinationOperand(Operand destinationOperand) {
		this.destinationOperand = destinationOperand;
	}

	public long getSerialNo() {
		return serialNo;
	}

	public void setSerialNo(long serialNo) {
		this.serialNo = serialNo;
	}

	public int getThreadID() {
		return threadID;
	}

	public long getSourceOperand1MemValue() {
		return sourceOperand1MemValue;
	}

	public void setSourceOperand1MemValue(long sourceOperand1MemValue) {
		this.sourceOperand1MemValue = sourceOperand1MemValue;
	}

	public long getSourceOperand2MemValue() {
		return sourceOperand2MemValue;
	}

	public void setSourceOperand2MemValue(long sourceOperand2MemValue) {
		this.sourceOperand2MemValue = sourceOperand2MemValue;
	}

	public long getDestinationOperandMemValue() {
		return destinationOperandMemValue;
	}

	public void setDestinationOperandMemValue(long destinationOperandMemValue) {
		this.destinationOperandMemValue = destinationOperandMemValue;
	}
	
	/**
	 * strInstruction method returns the instruction information in a string.
	 * @return String describing the instruction
	 */
	public String toString()
	{
		return 
		(
			String.format("%-20s", "IP = " + Long.toHexString(ciscProgramCounter)) +
			String.format("%-20s", "Op = " + type) +
			String.format("%-60s", "srcOp1 = " + sourceOperand1) +
			String.format("%-60s", "srcOp2 = " + sourceOperand2) +
			String.format("%-60s", "dstOp = " + destinationOperand) 
		);
	}
	
	public static long createAddressForBM(long address, int bm)
	{
		// In order to tag an address with the benchmark ID, 
		// we replace the 8 MSB bits with the benchmark ID bits
		// Address is represented using a long which is 64 bits long
		// Changing the 8 MSB bits will not affect the 
		// correctness since current 64 bit processors use only the 48 LSB bits of the addresses. 
		// They ignore the top 16 bits. So changing top 8 bits should always work.
		long newAddress = bm;
		newAddress = newAddress << 56;
		newAddress = newAddress | address;
		return newAddress;
	}

	public void changeAddressesForBenchmark(int bm)
	{
		// This function modifies all the addresses in an instruction object
		
		// CISC program counter = -1 is a special value
		if(ciscProgramCounter!=-1) {
			ciscProgramCounter = createAddressForBM(ciscProgramCounter, bm);
		}
		
		sourceOperand1MemValue = createAddressForBM(sourceOperand1MemValue, bm);
		sourceOperand2MemValue = createAddressForBM(sourceOperand2MemValue, bm);
		destinationOperandMemValue = createAddressForBM(destinationOperandMemValue, bm);
		branchTargetAddress = createAddressForBM(branchTargetAddress, bm);
	}
}