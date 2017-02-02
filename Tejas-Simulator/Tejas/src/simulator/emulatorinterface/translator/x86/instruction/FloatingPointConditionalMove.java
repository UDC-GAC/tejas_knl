package emulatorinterface.translator.x86.instruction;

import emulatorinterface.translator.InvalidInstructionException;
import emulatorinterface.translator.x86.registers.TempRegisterNum;
import generic.Operand;
import generic.InstructionList;

public class FloatingPointConditionalMove implements X86StaticInstructionHandler 
{
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			InstructionList instructionArrayList,
			TempRegisterNum tempRegisterNum)
					throws InvalidInstructionException
	{
		//TODO Must do something !!
		
		if(operand1.isFloatRegisterOperand() && operand2==null
				&& operand3==null)
		{
		
		}
		
		else if(operand1.isFloatRegisterOperand() && operand2.isFloatRegisterOperand()
				&& operand3==null)
		{
			
		}
		
		else
		{
			misc.Error.invalidOperation("Floating Point Conditional Move", operand1, operand2, operand3);
		}
	}
}
