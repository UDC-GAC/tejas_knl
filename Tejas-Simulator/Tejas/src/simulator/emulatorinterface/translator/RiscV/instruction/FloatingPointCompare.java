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
import emulatorinterface.translator.RiscV.registers.Registers;
import emulatorinterface.translator.RiscV.registers.TempRegisterNum;
import generic.Instruction;
import generic.Operand;
import generic.InstructionList;

public class FloatingPointCompare implements RiscVStaticInstructionHandler 
{
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			InstructionList instructionArrayList,
			TempRegisterNum tempRegisterNum)
					throws InvalidInstructionException
	{
			
		if((operand1.isFloatRegisterOperand()||operand1.isIntegerRegisterOperand()) &&
			operand2.isFloatRegisterOperand() &&
			operand3.isFloatRegisterOperand())
		{
			//Both the operands are passed as operands to the command.
			instructionArrayList.appendInstruction(Instruction.getFloatingPointALU(operand2, operand3, operand1));
		}
		
		else if((operand1.isFloatRegisterOperand()||operand1.isIntegerRegisterOperand()) &&
				operand2.isFloatRegisterOperand() &&
				operand3==null)
		{
			//First implicit operand is implicitly st0 and second operand is passed as argument.
			instructionArrayList.appendInstruction(Instruction.getFloatingPointALU(operand1, operand2, null));
		}
		
 		else
		{
			misc.Error.invalidOperation("Floating Point Compare for ip=" 
					+ instructionPointer
					, operand1, operand2, operand3);
		}
	}
}
