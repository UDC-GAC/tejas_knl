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
import emulatorinterface.translator.RiscV.operand.OperandTranslator;
import emulatorinterface.translator.RiscV.registers.TempRegisterNum;
import generic.Instruction;
import generic.InstructionList;
import generic.Operand;

public class ConditionalBranch implements RiscVStaticInstructionHandler 
{
	public void handle(long instructionPointer, Operand operand1,
			Operand operand2, Operand operand3, InstructionList instructionArrayList,
			TempRegisterNum tempRegisterNum)
					throws InvalidInstructionException
	{
		Operand jumpLocation = null;
		

		
		if((operand1.isIntegerRegisterOperand()) &&
				  (operand2.isIntegerRegisterOperand()) &&
				  (operand3.isMemoryOperand()))
		{
			jumpLocation = OperandTranslator.getLocationToStoreValue(operand3, tempRegisterNum);
			instructionArrayList.appendInstruction(Instruction.getLoadInstruction(operand3, jumpLocation));
			instructionArrayList.appendInstruction(Instruction.getBranchInstruction(jumpLocation));
		}
		else if((operand1.isIntegerRegisterOperand()) &&
				  (operand2.isMemoryOperand()) &&
				  (operand3==null))
		{
			     
			if(operand2.isMemoryOperand())
			{
				jumpLocation = OperandTranslator.getLocationToStoreValue(operand2, tempRegisterNum);
				instructionArrayList.appendInstruction(Instruction.getLoadInstruction(operand2, jumpLocation));
			}
			else
			{
				jumpLocation = operand2;
			}
			instructionArrayList.appendInstruction(Instruction.getBranchInstruction(jumpLocation));
		}
		
		else
		{
			misc.Error.invalidOperation("Conditional Jump", operand1, operand2, operand3);
		}
	}
}
