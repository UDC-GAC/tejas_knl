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
import emulatorinterface.translator.x86.registers.Registers;
import emulatorinterface.translator.x86.registers.TempRegisterNum;
import generic.InstructionList;
import generic.Instruction;
import generic.Operand;

public class Exchange implements X86StaticInstructionHandler 
{
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			InstructionList instructionArrayList,
			TempRegisterNum tempRegisterNum)
					throws InvalidInstructionException
	{
		//operand1 is a register and operand2 is also a register
		if((operand1.isIntegerRegisterOperand()) &&
		   (operand2.isIntegerRegisterOperand())&&
		   operand3==null)
		{
			instructionArrayList.appendInstruction(Instruction.getExchangeInstruction(operand1, operand2));
		}

		//operand1 is memory operand and operand2 is a register
		else if((operand1.isMemoryOperand() || operand1.isIntegerRegisterOperand()) &&
				(operand2.isMemoryOperand() || operand2.isIntegerRegisterOperand()) &&
				 operand3==null)
		{
			Operand memLocation = null, tempRegister = null, register = null;
			
			tempRegister = Registers.getTempIntReg(tempRegisterNum);
			
			if(operand1.isMemoryOperand() && !operand2.isMemoryOperand()) {
				memLocation = operand1;
				register = operand2;
			} else if(operand2.isMemoryOperand() && !operand1.isMemoryOperand()) {
				memLocation = operand2;
				register = operand1;
			} else {
				misc.Error.invalidOperation("Exchange", operand1, operand2, operand3);
			}
			
			//temp = mem
			instructionArrayList.appendInstruction(Instruction.getLoadInstruction(memLocation, tempRegister));
			//mem = reg
			instructionArrayList.appendInstruction(Instruction.getStoreInstruction(memLocation, register));
			// reg = temp
			instructionArrayList.appendInstruction(Instruction.getMoveInstruction(register, tempRegister));
		}

		else
		{
			misc.Error.invalidOperation("Exchange", operand1, operand2, operand3);
		}
	}
}