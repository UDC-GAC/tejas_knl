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

public enum InstructionClass 
{
	INTEGER_ALU_IMPLICIT_DESTINATION,
	INTEGER_ALU_NO_IMPLICIT_DESTINATION,
	SINGLE_OPERAND_INTEGER_ALU,
	SINGLE_OPERAND_INTEGER_ALU_IMPLICIT_ACCUMULATOR,
	SHIFT_OPERATION_THREE_OPERANDS,
	MOVE,
	CONDITIONAL_MOVE,
	EXCHANGE,
	EXCHANGE_AND_ADD,
	CONDITIONAL_JUMP,
	UNCONDITIONAL_JUMP,
	LOOP,
	NOP,
	INTEGER_MULTIPLICATION,
	INTEGER_DIVISION,
	INTERRUPT,
	LOAD_EFFECTIVE_ADDRESS,
	CONDITIONAL_SET,

	
	//Stack Operations
	PUSH,
	POP,
	CALL,
	RETURN,
	LEAVE,

	
	//String Operations
	STRING_MOVE,
	STRING_COMPARE,
	
	
	//Floating Point operations
	FLOATING_POINT_LOAD_CONSTANT,
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
	
	//Convert operations
	CONVERT_FLOAT_TO_INTEGER,
	CONVERT_INTEGER_TO_FLOAT,
	
	//Not Handled
	REPEAT,
	LOCK,

	//TODO SSE Instructions
	SSE_MOVE,
	SSE_ALU,
	SSE_MULTIPLICATION,
	SSE_DIVISION,
	SSE_COMPARE_PACKED_DATA,
	
	INVALID,   
}