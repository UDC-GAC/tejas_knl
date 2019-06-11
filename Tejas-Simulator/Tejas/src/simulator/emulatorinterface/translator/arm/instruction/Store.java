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
import emulatorinterface.translator.arm.registers.TempRegisterNum;
import generic.Instruction;
import generic.Operand;
import generic.InstructionList;


public class Store implements StaticInstructionHandler 
{
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			InstructionList instructionArrayList,
			TempRegisterNum tempRegisterNum) 
					throws InvalidInstructionException
	{
			
		if((operand1.isIntegerRegisterOperand()) &&
				(operand2.isMemoryOperand()) &&
				(operand3==null))
		{
			instructionArrayList.appendInstruction(Instruction.getStoreInstruction(operand2, operand1));
		}
		//if operand3 is not null so post indexing -->immediate or shifted reg by const
			//TODO: Have to add one more instruction for shifting post indexing
		else if((operand1.isIntegerRegisterOperand()) &&
				 operand2.isMemoryOperand() && 
				 operand3.isImmediateOperand())
		{
			instructionArrayList.appendInstruction(Instruction.getStoreInstruction(operand2, operand1));
		}
		
		else
		{
			misc.Error.invalidOperation("Store", operand1, operand2, operand3);
		}
	}
}
