/*****************************************************************************
				
------------------------------------------------------------------------------------------------------------

	Contributors: Ismi Abidi
*****************************************************************************/

package emulatorinterface.translator.arm.instruction;

public enum InstructionClass 
{
	INTEGER_ALU_IMPLICIT_DESTINATION,
	INTEGER_ALU_NO_IMPLICIT_DESTINATION,
	SINGLE_OPERAND_INTEGER_ALU,
	SINGLE_OPERAND_INTEGER_ALU_IMPLICIT_ACCUMULATOR,
	MOVE,
	CONDITIONAL_BRANCH,
	UNCONDITIONAL_BRANCH,
	BRANCH_LINK,
	COMPARE_BRANCH,
	
	NOP,
	INTEGER_MULTIPLICATION,
	INTEGER_DIVISION,
	INTERRUPT,
	//CONDITIONAL_SET,
	LOAD,
	STORE,
	LOAD_BLOCK,
	STORE_BLOCK,
	
	//Stack Operations
	PUSH,
	POP,
	
	
	//Floating Point operations
	FLOATING_POINT_LOAD,
	FLOATING_POINT_STORE,
	FLOATING_POINT_MULTIPLICATION,
	FLOATING_POINT_DIVISION,
	FLOATING_POINT_ALU,
	FLOATING_POINT_SINGLE_OPERAND_ALU,
	FLOATING_POINT_EXCHANGE,
	FLOATING_POINT_COMPLEX_OPERATION,
	FLOATING_POINT_COMPARE,
	FLOATING_POINT_CONDITIONAL_MOVE,
	FLOATING_POINT_LOAD_CONTROL_WORD,
	FLOATING_POINT_STORE_CONTROL_WORD,
	
	
	//Not Handled

	
	
	INVALID,   
}
