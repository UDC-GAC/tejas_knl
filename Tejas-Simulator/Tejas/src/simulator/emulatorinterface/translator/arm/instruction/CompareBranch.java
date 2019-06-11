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
import emulatorinterface.translator.arm.registers.TempRegisterNum;
import generic.Operand;
import generic.Instruction;
import generic.InstructionList;

public class CompareBranch implements StaticInstructionHandler 
{
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			InstructionList instructionArrayList,
			TempRegisterNum tempRegisterNum) 
					throws InvalidInstructionException
	{
		if(operand1.isIntegerRegisterOperand()
		   && operand2.isImmediateOperand() && operand3==null)
		{
						
			//cmp rn==0 or not 
			instructionArrayList.appendInstruction(Instruction.getIntALUInstruction(operand1,
					Operand.getImmediateOperand(), null));
			
			//Perform a conditional jump bne or beq
			ConditionalJump conditionalJump = new ConditionalJump();
			conditionalJump.handle(instructionPointer, operand2, null, null, instructionArrayList, tempRegisterNum);

		}
		else
		{
			misc.Error.invalidOperation("Unconditional Jump", operand1, operand2, operand3);
		}
	}
}
