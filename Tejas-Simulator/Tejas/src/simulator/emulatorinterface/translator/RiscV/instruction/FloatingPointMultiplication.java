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

	Contributors:   Sunil Kumar :: Shraddha Gupta
*****************************************************************************/

package emulatorinterface.translator.RiscV.instruction;

import emulatorinterface.translator.InvalidInstructionException;
import emulatorinterface.translator.RiscV.registers.Registers;
import emulatorinterface.translator.RiscV.registers.TempRegisterNum;
import generic.Instruction;
import generic.Operand;
import generic.InstructionList;

public class FloatingPointMultiplication implements RiscVStaticInstructionHandler 
{
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			InstructionList instructionArrayList,
			TempRegisterNum tempRegisterNum) 
					throws InvalidInstructionException
	{
		//If no operand is provided to the function, then st(0)
		//and st(1) are the implicit operands
		if(operand1==null && operand2==null	&& operand3==null)
		{
			Operand st0 = Registers.getTopFPRegister();
			Operand st1 = Registers.getSecondTopFPRegister();
			
			instructionArrayList.appendInstruction(
					Instruction.getFloatingPointMultiplication(st1, st0, st0));
		}
		
		
		else if((operand1.isFloatRegisterOperand()|| operand1.isMemoryOperand()) &&
				(operand2.isFloatRegisterOperand()|| operand2.isImmediateOperand())&&
				(operand3.isFloatRegisterOperand()|| operand3.isImmediateOperand())
				)
		{
			if(operand1.isMemoryOperand())
			{
				Operand tempFloatRegister1;
				tempFloatRegister1=Registers.getTempFloatReg(tempRegisterNum);
				instructionArrayList.appendInstruction(Instruction.getLoadInstruction(operand1, tempFloatRegister1));
				instructionArrayList.appendInstruction(Instruction.getFloatingPointMultiplication(tempFloatRegister1, 
						operand3, operand1));
			}
			else
			{
				instructionArrayList.appendInstruction(Instruction.getFloatingPointMultiplication(
					operand2, operand3, operand1));
			}
		}
		
		else
		{
			misc.Error.invalidOperation("Floating Point Multiplication", operand1, operand2, operand3);
		}
	}
}