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
import generic.Operand;
import generic.InstructionList;


public class AtomicInstructions implements RiscVStaticInstructionHandler 
{
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			InstructionList instructionArrayList,
			TempRegisterNum tempRegisterNum) 
					throws InvalidInstructionException
	{
		if(
		(operand1.isIntegerRegisterOperand()) &&
		(operand2.isIntegerRegisterOperand() || operand2.isMemoryOperand() || operand2.isImmediateOperand()) &&
		(operand3.isImmediateOperand() || operand3.isIntegerRegisterOperand() || operand3.isMemoryOperand()))
		{
			Operand operand1ValueOperand;
			Operand operand2ValueOperand;
			Operand operand3ValueOperand;

			
			//get value-operand for operand1

			operand1ValueOperand = operand1;

			//get value-operand for operand2
			if(operand2.isMemoryOperand())
			{
				operand2ValueOperand = OperandTranslator.getLocationToStoreValue(operand2, tempRegisterNum);
				instructionArrayList.appendInstruction(Instruction.getLoadInstruction(operand2, operand2ValueOperand));
			}
			else
			{
				operand2ValueOperand = operand2;
			}

			//get value-operand for operand1
			if(operand3.isMemoryOperand())
			{
				operand3ValueOperand = OperandTranslator.getLocationToStoreValue(operand3, tempRegisterNum);
				instructionArrayList.appendInstruction(Instruction.getLoadInstruction(operand3, operand3ValueOperand));
			}
			else
			{
				operand3ValueOperand = operand3;
			}
			
			//Perform integer alu operation
			instructionArrayList.appendInstruction(Instruction.getIntALUInstruction(operand2ValueOperand, 
					operand3ValueOperand, operand1ValueOperand));

			//If operand1 is a memory operand, then perform a store operation too
			if(operand1.isMemoryOperand())
			{
				instructionArrayList.appendInstruction(Instruction.getStoreInstruction(operand1, operand1ValueOperand));
			}
		}
		else
		{
			//System.out.println("operands: "+operand1+" : "+operand2+" : "+operand3);
			misc.Error.invalidOperation("Integer operation with implicit operands", operand1, operand2, operand3);
		}
	}
}
