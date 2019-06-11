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

import java.util.Hashtable;

public class InstructionClassTable {
	private static Hashtable<String, InstructionClass> instructionClassTable;
	private static Hashtable<InstructionClass, RiscVStaticInstructionHandler> instructionClassHandlerTable;

	private static void createInstructionClassHandlerTable() {
		// create an empty hash-table for storing object references.
		instructionClassHandlerTable = new Hashtable<InstructionClass, RiscVStaticInstructionHandler>();

		instructionClassHandlerTable.put(
				InstructionClass.INTEGER_ALU_IMPLICIT_DESTINATION,
				new IntegerALUImplicitDestination());

       instructionClassHandlerTable.put(
				InstructionClass.INTEGER_MULTIPLICATION,
				new IntegerMultiplication());

		instructionClassHandlerTable.put(
				InstructionClass.INTEGER_DIVISION,
				new IntegerDivision());

        instructionClassHandlerTable.put(
				InstructionClass.LOAD_EFFECTIVE_ADDRESS,
				new LoadEffectiveAddress());
         
       instructionClassHandlerTable.put(
				InstructionClass.STORE, 
				new Store());
        
        instructionClassHandlerTable.put(
				InstructionClass.MOVE, 
				new Move());
        
        instructionClassHandlerTable.put(
				InstructionClass.CONDITIONAL_BRANCH,
				new ConditionalBranch());
        
        instructionClassHandlerTable.put(
				InstructionClass.RETURN,
				new ReturnOp());
        
        instructionClassHandlerTable.put(
				InstructionClass.UNCONDITIONAL_JUMP,
				new UnconditionalJump());
        
        instructionClassHandlerTable.put(
				InstructionClass.JUMP_AND_LINK,
				new UnconditionalJump());
        
        instructionClassHandlerTable.put(
				InstructionClass.NOP, 
				new NOP());
        
        instructionClassHandlerTable.put(
				InstructionClass.INTERRUPT,
				new Interrupt());
        
        instructionClassHandlerTable.put(
				InstructionClass.SINGLE_OPERAND_INTEGER_ALU,
				new SingleOperandIntALU());
        
        instructionClassHandlerTable.put(
				InstructionClass.AUIPC,
				new SingleOperandIntALU());
        
        instructionClassHandlerTable.put(
				InstructionClass.CONDITIONAL_SET,
				new ConditionalSet());
        
        instructionClassHandlerTable.put(
				InstructionClass.ATOMIC_INSTRUCTIONS,
				new AtomicInstructions());
        
        instructionClassHandlerTable.put(
				InstructionClass.FLOATING_POINT_ALU,
				new FloatingPointALU());
        

		instructionClassHandlerTable.put(
				InstructionClass.FLOATING_POINT_MULTIPLICATION,
				new FloatingPointMultiplication());

		instructionClassHandlerTable.put(
				InstructionClass.FLOATING_POINT_DIVISION,
				new FloatingPointDivision());
		
		instructionClassHandlerTable.put(
				InstructionClass.CONVERT_FLOAT_TO_INTEGER,
				new ConvertFloatToInteger());
		
		instructionClassHandlerTable.put(
				InstructionClass.CONVERT_INTEGER_TO_FLOAT,
				new ConvertIntegerToFloat());
		
		instructionClassHandlerTable.put(
				InstructionClass.CHANGE_PRECISION,
				new ChangePrecision());
		
		instructionClassHandlerTable.put(
				InstructionClass.FLOATING_POINT_COMPLEX_OPERATION,
				new FloatingPointComplexOperation());
		
		instructionClassHandlerTable.put(
				InstructionClass.FLOATING_POINT_COMPARE,
				new FloatingPointCompare());
		
		instructionClassHandlerTable.put(
				InstructionClass.FLOATING_POINT_MOVE,
				new FloatingPointMove());
		
		instructionClassHandlerTable.put(
				InstructionClass.FLOATING_POINT_SINGLE_OPERAND_ALU,
				new FloatingPointSingleOperandALU());
        
		instructionClassHandlerTable.put(
				InstructionClass.FLOATING_POINT_LOAD,
				new FloatingPointLoad());

		instructionClassHandlerTable.put(
				InstructionClass.FLOATING_POINT_STORE,
				new FloatingPointStore());
		
		instructionClassHandlerTable.put(
				InstructionClass.FLOATING_POINT_CLASS,
				new FloatingPointLoad());
	}

	private static void createInstructionClassTable() 
	{
		instructionClassTable = new Hashtable<String, InstructionClass>();

		String interAluImplicitDestination[] = "add|addw|addi|addiw|sub|subw|and|andi|or|ori|xor|xori|sll|sllw|slli|slliw|sra|srai|sraw|sraiw|srl|srlw|srliw|srli"
				.split("\\|");
		for (int i = 0; i < interAluImplicitDestination.length; i++)
			instructionClassTable.put(interAluImplicitDestination[i],
					InstructionClass.INTEGER_ALU_IMPLICIT_DESTINATION);

        String integerMultiplication[] = "mul|mulw|mulh|mulhsu|mulhu".split("\\|");
		for (int i = 0; i < integerMultiplication.length; i++)
			instructionClassTable.put(integerMultiplication[i],
					InstructionClass.INTEGER_MULTIPLICATION);

		String integerDivision[] = "div|divw|divu|rem|remw|remu|remuw".split("\\|");
		for (int i = 0; i < integerDivision.length; i++)
			instructionClassTable.put(integerDivision[i],
					InstructionClass.INTEGER_DIVISION);
                
        String loadEffectiveAddress[] = "ld|lw|lwu|lh|lhu|lb|lbu|lui|li|la".split("\\|");
		for (int i = 0; i < loadEffectiveAddress.length; i++)
			instructionClassTable.put(loadEffectiveAddress[i],
					InstructionClass.LOAD_EFFECTIVE_ADDRESS);
		
		String store[] = "sb|sd|sh|sw".split("\\|");
		for (int i = 0; i < store.length; i++)
			instructionClassTable.put(store[i], InstructionClass.STORE);
		
		String move[] = "mv".split("\\|");
		for (int i = 0; i < move.length; i++)
			instructionClassTable.put(move[i], InstructionClass.MOVE);
		
		String conditionalBranch[] = "blt|bne|bge|bgeu|bltu|beqz|bnez|beq|bgez|blez|bltz|bgtz|bgt|ble|bgtu|bleu".split("\\|");
		for (int i = 0; i < conditionalBranch.length; i++)
			instructionClassTable.put(conditionalBranch[i],
					InstructionClass.CONDITIONAL_BRANCH);
		
		String unconditionalJump[] = "j|jr".split("\\|");
		for (int i = 0; i < unconditionalJump.length; i++)
			instructionClassTable.put(unconditionalJump[i],
					InstructionClass.UNCONDITIONAL_JUMP);
		
		String jumpandlink[] = "jal|jalr".split("\\|");
		for (int i = 0; i < jumpandlink.length; i++)
			instructionClassTable.put(jumpandlink[i],
					InstructionClass.JUMP_AND_LINK);
		
		String returnOp[] = "ret|sret|mret".split("\\|");
		for (int i = 0; i < returnOp.length; i++)
			instructionClassTable.put(returnOp[i], InstructionClass.RETURN);
		
		String nop[] = "nop".split("\\|");
		for (int i = 0; i < nop.length; i++)
			instructionClassTable.put(nop[i], InstructionClass.NOP);
		
		String interrupt[] = "ecall|ebreak|fence|fence.i|sfence.vm".split("\\|");
		for (int i = 0; i < interrupt.length; i++)
			instructionClassTable.put(interrupt[i], InstructionClass.INTERRUPT);
		
		String singleOperandIntegerALU[] = "neg|not|negw".split("\\|");
		for (int i = 0; i < singleOperandIntegerALU.length; i++)
			instructionClassTable.put(singleOperandIntegerALU[i],
					InstructionClass.SINGLE_OPERAND_INTEGER_ALU);
		
		String auipc[] = "auipc".split("\\|");
		for (int i = 0; i < auipc.length; i++)
			instructionClassTable.put(auipc[i], InstructionClass.AUIPC);
		
		String ConditionalSet[] = "slt|slti|sltiu|sltu|seqz|snez|sltz|sgtz"
				.split("\\|");
        for(int i=0; i<ConditionalSet.length; i++)
            instructionClassTable.put(ConditionalSet[i],InstructionClass.CONDITIONAL_SET);
		
        String ConditionalStore[] = "lr.d|sc.d"
				.split("\\|");
        for(int i=0; i<ConditionalStore.length; i++)
            instructionClassTable.put(ConditionalStore[i],InstructionClass.CONDITIONAL_STORE);
            
        String atomicInstructions[] = "amoadd.w|amoadd.d|amoand.w|amoand.d|amomax.w|amomax.d|amomaxu.w|amomaxu.d|amomin.w|amomin.d|amominu.w|amominu.d|amoor.w|amoor.d|amoswap.w|amoswap.d|amoxor.w|amoxor.d"
        		.split("\\|");
		for (int i = 0; i < atomicInstructions.length; i++)
			instructionClassTable.put(atomicInstructions[i],
					InstructionClass.ATOMIC_INSTRUCTIONS);
		
		String floatingPointALU[] = "fadd.d|fadd.d|fsub.s|fsub.d"
				.split("\\|");
		for (int i = 0; i < floatingPointALU.length; i++)
			instructionClassTable.put(floatingPointALU[i],
					InstructionClass.FLOATING_POINT_ALU);
		
		String floatingPointMultiplication[] = "fmul.s|fmul.d"
				.split("\\|");
		for (int i = 0; i < floatingPointMultiplication.length; i++)
			instructionClassTable.put(floatingPointMultiplication[i],
					InstructionClass.FLOATING_POINT_MULTIPLICATION);

		String floatingPointDivision[] = "fdiv.s|fdiv.d".split("\\|");
		for (int i = 0; i < floatingPointDivision.length; i++)
			instructionClassTable.put(floatingPointDivision[i],
					InstructionClass.FLOATING_POINT_DIVISION);
		
		String convertFloatToInteger[] = "fcvt.w.s|fcvt.w.d|fcvt.l.s|fcvt.l.d|fcvt.wu.s|fcvt.wu.d|fcvt.lu.s|fcvt.lu.d|fmv.x.s|fmv.x.d".split("\\|");
		for (int i = 0; i < convertFloatToInteger.length; i++)
			instructionClassTable.put(convertFloatToInteger[i],
					InstructionClass.CONVERT_FLOAT_TO_INTEGER);
		
		String convertIntegerToFloat[] = "fcvt.s.w|fcvt.d.w|fcvt.s.l|fcvt.d.l|fcvt.s.wu|fcvt.d.wu|fcvt.s.lu|fcvt.d.lu|fmv.s.x|fmv.d.x".split("\\|");
		for (int i = 0; i < convertIntegerToFloat.length; i++)
			instructionClassTable.put(convertIntegerToFloat[i],
					InstructionClass.CONVERT_INTEGER_TO_FLOAT);
		
		String changePrecision[] = "fcvt.s.d|fcvt.d.s"
				.split("\\|");
		for (int i = 0; i < changePrecision.length; i++)
			instructionClassTable.put(changePrecision[i],
					InstructionClass.CHANGE_PRECISION);
		
		String floatingPointComplexOperation[] = "fsqrt".split("\\|");
		for (int i = 0; i < floatingPointComplexOperation.length; i++)
			instructionClassTable.put(floatingPointComplexOperation[i],
					InstructionClass.FLOATING_POINT_COMPLEX_OPERATION);
		
		String FUCompare[] = "feq.s|feq.d|flt.s|flt.d|fle.s|fle.d|fmin.s|fmin.d|fmax.s|fmax.d".split("\\|");
		for(int i=0; i < FUCompare.length; i++)
			instructionClassTable.put(FUCompare[i], 
					InstructionClass.FLOATING_POINT_COMPARE);
		
		String FUmove[] = "fmv.s|fmv.d".split("\\|");
		for(int i=0; i < FUmove.length; i++)
			instructionClassTable.put(FUmove[i], 
					InstructionClass.FLOATING_POINT_MOVE);
		
		String floatingPointSingleOperandALU[] = "fabs.s|fabs.d|fneg.s|fneg.d".split("\\|");
		for (int i = 0; i < floatingPointSingleOperandALU.length; i++)
			instructionClassTable.put(floatingPointSingleOperandALU[i],
				InstructionClass.FLOATING_POINT_SINGLE_OPERAND_ALU);
		
		String floatingPointLoad[] = "fld|flw".split("\\|");
		for (int i = 0; i < floatingPointLoad.length; i++)
			instructionClassTable.put(floatingPointLoad[i],
				InstructionClass.FLOATING_POINT_LOAD);
		
		String floatingPointClass[] = "fclass.s|fclass.d".split("\\|");
		for (int i = 0; i < floatingPointClass.length; i++)
			instructionClassTable.put(floatingPointClass[i],
					InstructionClass.FLOATING_POINT_CLASS);

		String floatingPointStore[] = "fsd|fsw".split("\\|");
		for (int i = 0; i < floatingPointStore.length; i++)
			instructionClassTable.put(floatingPointStore[i],
					InstructionClass.FLOATING_POINT_STORE);
		
	}

	public static InstructionClass getInstructionClass(String operation) {
		if (instructionClassTable == null)
			createInstructionClassTable();

		if (operation == null)
			return InstructionClass.INVALID;

		InstructionClass instructionClass;
		instructionClass = instructionClassTable.get(operation);

		if (instructionClass == null)
			return InstructionClass.INVALID;
		else
			return instructionClass;
	}

	public static RiscVStaticInstructionHandler getInstructionClassHandler(
			InstructionClass instructionClass) {

		if (instructionClassHandlerTable == null)
			createInstructionClassHandlerTable();

		if (instructionClass == InstructionClass.INVALID)
			return null;

		RiscVStaticInstructionHandler handler;
		handler = instructionClassHandlerTable.get(instructionClass);

		return handler;
	}
}
