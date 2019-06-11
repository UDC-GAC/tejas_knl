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

public class ReturnOp implements RiscVStaticInstructionHandler 
{
	static int k=0;
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			InstructionList instructionArrayList,
			TempRegisterNum tempRegisterNum)
					throws InvalidInstructionException
	{

		if ((operand1 == null || operand1.isImmediateOperand())
				&& operand2 == null && operand3 == null) 
		{

      // Create stack-pointer and [stack-pointer]
		Operand stackPointer = Registers.getStackPointer();
		Operand stackPointerLocation = Operand.getMemoryOperand(stackPointer, null);
		Operand newInstructionPointer;
		newInstructionPointer = Registers.getCounterRegister();

			// load the new instruction-pointer from the stack
		instructionArrayList.appendInstruction(Instruction.getLoadInstruction(stackPointerLocation,	newInstructionPointer));

			// perform an unconditional jump to the new location
		(new UnconditionalJump()).handle(instructionPointer,
					newInstructionPointer, null, null, instructionArrayList, tempRegisterNum);
      
      if(operand1==null) {
        
			  //stack-pointer = stack-pointer - 4
			  instructionArrayList.appendInstruction(Instruction.getIntALUInstruction(stackPointer, Operand.getImmediateOperand(), stackPointer));

      } else if (operand1 != null && operand1.isImmediateOperand()) {
        //stack-pointer = stack-pointer - (operand1+4)
        
        Operand tempRegister = Registers.getTempIntReg(tempRegisterNum);
        
        //temp = operand1 + 4
			  instructionArrayList.appendInstruction(Instruction.getIntALUInstruction(operand1, Operand.getImmediateOperand(), tempRegister));

        // stack-pointer = stack-pointer + temp-Register
			  instructionArrayList.appendInstruction(Instruction.getIntALUInstruction(stackPointer, tempRegister, stackPointer));
			} else {
      	
        misc.Error.invalidOperation("Return Operation", operand1, operand2,
					operand3);
      }
		}
		else 
		{
			misc.Error.invalidOperation("Return Operation", operand1, operand2,
					operand3);
		}
	}
}
