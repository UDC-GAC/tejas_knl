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
import generic.Instruction;
import generic.Operand;
import generic.InstructionList;


public class IntegerALUExplicitDestination implements StaticInstructionHandler 
{
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			InstructionList instructionArrayList,
			TempRegisterNum tempRegisterNum) 
					throws InvalidInstructionException
	{
		if(
		(operand1.isIntegerRegisterOperand() ) &&
		(operand2.isImmediateOperand() || operand2.isIntegerRegisterOperand() ) &&
		(operand3==null )
		)
		{
			Operand operand1ValueOperand;
			Operand operand2ValueOperand;
			
			
			//get value-operand for operand1
				operand1ValueOperand = operand1;
				
			//get value-operand for operand2
				operand2ValueOperand = operand2;

			//Perform integer alu operation
			instructionArrayList.appendInstruction(Instruction.getIntALUInstruction( operand1ValueOperand, operand2ValueOperand, null));
		}
		else
		{
			misc.Error.invalidOperation("Integer ALU operation with no implicit destination operand", operand1, operand2, operand3);
		}
	}
}
