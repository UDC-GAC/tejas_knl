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

	Contributors:  Sunil Kumar :: Shraddha Gupta
*****************************************************************************/
package emulatorinterface.translator.RiscV.instruction;

import emulatorinterface.translator.InvalidInstructionException;
import emulatorinterface.translator.RiscV.registers.TempRegisterNum;
import generic.Instruction;
import generic.Operand;
import generic.InstructionList;

public class ConditionalSet implements RiscVStaticInstructionHandler 
{
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			InstructionList instructionArrayList,
			TempRegisterNum tempRegisterNum)
					throws InvalidInstructionException
	{
		if((operand1.isIntegerRegisterOperand()) && 
				(operand2.isIntegerRegisterOperand())&&
				(operand3.isIntegerRegisterOperand()))
		{
			instructionArrayList.appendInstruction(Instruction.getMoveInstruction(operand1, 
			Operand.getImmediateOperand()));
		}
		
		else if(operand1.isMemoryOperand())
		{
			instructionArrayList.appendInstruction(Instruction.getStoreInstruction(operand1, 
			Operand.getImmediateOperand()));
		}
		
		else
		{
			misc.Error.invalidOperation("Conditional Set", operand1, operand2, operand3);
		}
	}
}
