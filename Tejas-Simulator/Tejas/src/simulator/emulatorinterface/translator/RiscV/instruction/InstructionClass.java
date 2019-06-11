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


        //change--only one class of instructions is there..(add)
public enum InstructionClass 
{
    INTEGER_ALU_IMPLICIT_DESTINATION,
    INTEGER_MULTIPLICATION,
    INTEGER_DIVISION,
    LOAD_EFFECTIVE_ADDRESS,
    STORE,
    MOVE,
    CONDITIONAL_BRANCH,
    UNCONDITIONAL_JUMP,
    RETURN,
    JUMP_AND_LINK,
    NOP,
    INTERRUPT,
    SINGLE_OPERAND_INTEGER_ALU,
    AUIPC,
    CONDITIONAL_SET,
    CONDITIONAL_STORE,
    ATOMIC_INSTRUCTIONS,
    // floating point operations
    FLOATING_POINT_ALU,
    FLOATING_POINT_MULTIPLICATION,
	FLOATING_POINT_DIVISION,
	FLOATING_POINT_COMPLEX_OPERATION,
	FLOATING_POINT_COMPARE,
	FLOATING_POINT_MOVE,
	FLOATING_POINT_SINGLE_OPERAND_ALU,
	FLOATING_POINT_LOAD,
	FLOATING_POINT_CLASS,
	FLOATING_POINT_STORE,
	//Convert operations
	CONVERT_FLOAT_TO_INTEGER,
	CONVERT_INTEGER_TO_FLOAT,
	CHANGE_PRECISION,
	INVALID,   
}
