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

	Contributors:  
*****************************************************************************/
package emulatorinterface.translator.arm.instruction;

import emulatorinterface.translator.InvalidInstructionException;
import emulatorinterface.translator.arm.operand.OperandTranslator;
import emulatorinterface.translator.arm.registers.Registers;
import emulatorinterface.translator.arm.registers.TempRegisterNum;
import generic.Instruction;
import generic.InstructionList;
import generic.Operand;

public class BranchLink implements StaticInstructionHandler 
{
	public void handle(long instructionPointer, Operand operand1,
			Operand operand2, Operand operand3, InstructionList instructionArrayList,
			TempRegisterNum tempRegisterNum)
					throws InvalidInstructionException
	{
		Operand jumpLocation = null;
		//get Instruction Pointer and save its value in link register
		
		
		if((operand1.isImmediateOperand() || operand1.isIntegerRegisterOperand()) 
		   &&  operand2==null  &&  operand3==null)
		{
			/*Operand newInstructionPointer, LRPointer;
			newInstructionPointer = Registers.getInstructionPointer();
			Operand IPLocation = Operand.getMemoryOperand(newInstructionPointer, null);
			LRPointer=Registers.getLinkRegister();
			instructionArrayList.appendInstruction(Instruction.getLoadInstruction(IPLocation,LRPointer));
			*/jumpLocation = operand1;
			Instruction jumpInstruction = Instruction.getUnconditionalJumpInstruction(jumpLocation);
			jumpInstruction.setBranchTaken(true);
			instructionArrayList.appendInstruction(jumpInstruction);
		}
		
			
		else
		{
			misc.Error.invalidOperation("Branch & Link", operand1, operand2, operand3);
		}
	}
}
