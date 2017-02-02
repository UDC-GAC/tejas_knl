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

	Contributors:  Prathmesh Kallurkar
*****************************************************************************/

package emulatorinterface.translator.x86.instruction;

import emulatorinterface.translator.InvalidInstructionException;
import emulatorinterface.translator.x86.registers.TempRegisterNum;
import generic.Instruction;
import generic.InstructionLinkedList;
import generic.Operand;
import generic.InstructionList;

public class SSEMove implements X86StaticInstructionHandler 
{
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			InstructionList instructionArrayList,
			TempRegisterNum tempRegisterNum) 
					throws InvalidInstructionException
	{
		if(operand1.isFloatRegisterOperand() && operand2.isFloatRegisterOperand()
				&& operand3==null)
		{
			//If both operands are registers use a simple move operation.
			instructionArrayList.appendInstruction(Instruction.getMoveInstruction(operand1,
					operand2));
		}
		
		else if(operand1.isFloatRegisterOperand() && operand2.isMemoryOperand() 
				&& operand3==null)
		{
			//If the source operand is a memory location then use a load operation
			instructionArrayList.appendInstruction(Instruction.getLoadInstruction(operand2,
					operand1));
		}
		
		else if(operand1.isMemoryOperand() && operand2.isFloatRegisterOperand() 
				&& operand3==null)
		{
			//If the destination operand is a memory location, its a store operation
			instructionArrayList.appendInstruction(Instruction.getStoreInstruction(operand1,
					operand2));
		}
		
		else
		{
			misc.Error.invalidOperation("SSE Move", operand1, operand2, operand3);
		}
	}
}