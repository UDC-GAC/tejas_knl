package emulatorinterface.translator.x86.instruction;

import emulatorinterface.translator.InvalidInstructionException;
import emulatorinterface.translator.x86.registers.Registers;
import emulatorinterface.translator.x86.registers.TempRegisterNum;
import generic.Instruction;
import generic.InstructionList;
import generic.Operand;

public class SSEComparePackedData implements X86StaticInstructionHandler {
	
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			InstructionList instructionArrayList,
			TempRegisterNum tempRegisterNum)
					throws InvalidInstructionException
	{
		if(operand1!=null && operand2!=null && operand3==null) {
			Operand srcOperand, dstOperand;
			
			// src operand into temporary
			if(operand2.isMemoryOperand()) {
				Operand tmp = Registers.getTempFloatReg(tempRegisterNum);
				instructionArrayList.appendInstruction(
					Instruction.getLoadInstruction(operand2, tmp));
				srcOperand = tmp;
			} else {
				srcOperand = operand2;
			}
			
			// dst operand into temporary
			if(operand1.isMemoryOperand()) {
				Operand tmp = Registers.getTempFloatReg(tempRegisterNum);
				instructionArrayList.appendInstruction(
					Instruction.getLoadInstruction(operand1, tmp));
				dstOperand = tmp;
			} else {
				dstOperand = operand1;
			}
			
			// perform the floating point compare
			instructionArrayList.appendInstruction(
				Instruction.getFloatingPointALU(srcOperand, dstOperand, dstOperand));
			
			// if the original destination operand was memory, then store the result back
			if(operand1.isMemoryOperand()) {
				instructionArrayList.appendInstruction(
						Instruction.getStoreInstruction(operand1, dstOperand));
			}
		}
	}
}
