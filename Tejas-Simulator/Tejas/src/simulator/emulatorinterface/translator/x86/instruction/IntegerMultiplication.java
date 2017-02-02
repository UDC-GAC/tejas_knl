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
import emulatorinterface.translator.x86.operand.OperandTranslator;
import emulatorinterface.translator.x86.registers.Registers;
import emulatorinterface.translator.x86.registers.TempRegisterNum;
import generic.Instruction;
import generic.Operand;
import generic.InstructionList;

public class IntegerMultiplication implements X86StaticInstructionHandler 
{
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			InstructionList instructionArrayList,
			TempRegisterNum tempRegisterNum) 
					throws InvalidInstructionException
	{
		Operand multiplier;
		Operand multiplicand;
		
		//Single operand
		//In this case, the accumulator is the implicit operand
		if((operand1.isImmediateOperand() || operand1.isIntegerRegisterOperand() || operand1.isMemoryOperand()) &&
			operand2==null && operand3==null)
		{
			//if operand1 is a memory operand, then we must first fetch the value at its location
			if(operand1.isMemoryOperand())
			{
				multiplier = OperandTranslator.getLocationToStoreValue(operand1, tempRegisterNum);
				instructionArrayList.appendInstruction(Instruction.getLoadInstruction(operand1,	multiplier));				
			}
			else
			{
				multiplier = operand1;
			}
			
			Operand accumulatorRegister = Registers.getAccumulatorRegister();
			
			instructionArrayList.appendInstruction(Instruction.getIntegerMultiplicationInstruction
					(accumulatorRegister, multiplier, accumulatorRegister));
		}

		else if(    
			   (operand1.isIntegerRegisterOperand()) &&
			   (operand2.isImmediateOperand() || operand2.isIntegerRegisterOperand() || operand2.isMemoryOperand()) &&
				operand3==null)
		{
			multiplicand = operand1;
			
			//if operand1 is a memory operand, then we must first fetch the value at its location
			if(operand2.isMemoryOperand())
			{
				multiplier = OperandTranslator.getLocationToStoreValue(operand2, tempRegisterNum);
				instructionArrayList.appendInstruction(Instruction.getLoadInstruction(operand2, multiplier));				
			}

			else
			{
				multiplier = operand2;
			}
			
			//perform multiplication operation and store the result in operand1
			instructionArrayList.appendInstruction(Instruction.getIntegerMultiplicationInstruction
					(multiplicand, multiplier, multiplicand));
		}
		

		//If all the three operands are valid, then the operand1 is a general register, 
		//operand2 is a general register or memory-value and operand3 must be an immediate value
		else if(    
			(operand1.isIntegerRegisterOperand()) &&
			(operand2.isIntegerRegisterOperand() || operand2.isMemoryOperand()) &&
			 operand3.isImmediateOperand())
		{
			multiplier = operand3;
			
			//if operand1 is a memory operand, then we must first fetch the value at its location
			if(operand2.isMemoryOperand())
			{
				multiplicand = OperandTranslator.getLocationToStoreValue(operand2, tempRegisterNum);
				instructionArrayList.appendInstruction(Instruction.getLoadInstruction(operand2, multiplicand));				
			}
			else
			{
				multiplicand = operand2;
			}

			instructionArrayList.appendInstruction(Instruction.getIntegerMultiplicationInstruction(
					multiplicand, multiplier, operand1));
		}
		
		else
		{
			misc.Error.invalidOperation("Integer Multiplication", operand1, operand2, operand3);
		}
	}
}