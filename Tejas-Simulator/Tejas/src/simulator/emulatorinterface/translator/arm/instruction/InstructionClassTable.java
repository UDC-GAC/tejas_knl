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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Hashtable;

public class InstructionClassTable {
	private static Hashtable<String, InstructionClass> instructionClassTable;
	private static Hashtable<InstructionClass, StaticInstructionHandler> instructionClassHandlerTable;

	private static void createInstructionClassHandlerTable() {
		// create an empty hash-table for storing object references.
		instructionClassHandlerTable = new Hashtable<InstructionClass, StaticInstructionHandler>();

		instructionClassHandlerTable.put(
				InstructionClass.INTEGER_ALU_IMPLICIT_DESTINATION,
				new IntegerALUImplicitDestination());

		instructionClassHandlerTable.put(
				InstructionClass.INTEGER_ALU_NO_IMPLICIT_DESTINATION,
				new IntegerALUExplicitDestination());

		/*instructionClassHandlerTable.put(
				InstructionClass.SINGLE_OPERAND_INTEGER_ALU,
				new SingleOperandIntALU());

		instructionClassHandlerTable.put(
				InstructionClass.SINGLE_OPERAND_INTEGER_ALU_IMPLICIT_ACCUMULATOR,
				new SingleOperandIntALUImplicitAccumulator());
		*/
		instructionClassHandlerTable.put(
				InstructionClass.MOVE, 
				new Move());

		instructionClassHandlerTable.put(
				InstructionClass.CONDITIONAL_BRANCH,
				new ConditionalJump());
		
		instructionClassHandlerTable.put(
				InstructionClass.BRANCH_LINK,
				new BranchLink());
		instructionClassHandlerTable.put(
				InstructionClass.COMPARE_BRANCH,
				new CompareBranch());

		instructionClassHandlerTable.put(
				InstructionClass.UNCONDITIONAL_BRANCH,
				new UnconditionalJump());

		
		instructionClassHandlerTable.put(
				InstructionClass.PUSH, 
				new Store());

		instructionClassHandlerTable.put(
				InstructionClass.POP, 
				new Load());
		/*
		instructionClassHandlerTable.put(
				InstructionClass.INTERRUPT,
				new Interrupt());*/

		instructionClassHandlerTable.put(
				InstructionClass.NOP, 
				new NOP());
		
		instructionClassHandlerTable.put(
				InstructionClass.LOAD, 
				new Load());
		
		instructionClassHandlerTable.put(
				InstructionClass.LOAD_BLOCK, 
				new Load());
				
		instructionClassHandlerTable.put(
				InstructionClass.STORE, 
				new Store());
		instructionClassHandlerTable.put(
				InstructionClass.STORE_BLOCK, 
				new Store());		
		
		/*

		instructionClassHandlerTable.put(
				InstructionClass.INTEGER_MULTIPLICATION,
				new IntegerMultiplication());

		instructionClassHandlerTable.put(
				InstructionClass.INTEGER_DIVISION,
				new IntegerDivision());

		instructionClassHandlerTable.put(
				InstructionClass.FLOATING_POINT_LOAD,
				new FloatingPointLoad());

		instructionClassHandlerTable.put(
				InstructionClass.FLOATING_POINT_STORE,
				new FloatingPointStore());

		instructionClassHandlerTable.put(
				InstructionClass.FLOATING_POINT_MULTIPLICATION,
				new FloatingPointMultiplication());

		instructionClassHandlerTable.put(
				InstructionClass.FLOATING_POINT_DIVISION,
				new FloatingPointDivisiIntegerALUImplicitDestinationon());

		instructionClassHandlerTable.put(
				InstructionClass.FLOATING_POINT_EXCHANGE,
				new FloatingPointExchange());

		instructionClassHandlerTable.put(
				InstructionClass.FLOATING_POINT_ALU,
				new FloatingPointALU());

		instructionClassHandlerTable.put(
				InstructionClass.FLOATING_POINT_SINGLE_OPERAND_ALU,
				new FloatingPointSingleOperandALU());

		instructionClassHandlerTable.put(
				InstructionClass.FLOATING_POINT_COMPLEX_OPERATION,
				new FloatingPointComplexOperation());
		
	
		instructionClassHandlerTable.put(
				InstructionClass.FLOATING_POINT_COMPARE,
				new FloatingPointCompare());
				
		instructionClassHandlerTable.put(
				InstructionClass.FLOATING_POINT_CONDITIONAL_MOVE,
				new FloatingPointConditionalMove());
		
		instructionClassHandlerTable.put(
				InstructionClass.FLOATING_POINT_LOAD_CONTROL_WORD,
				new FloatingPointLoadControlWord());
		
		instructionClassHandlerTable.put(
				InstructionClass.FLOATING_POINT_STORE_CONTROL_WORD,
				new FloatingPointStoreControlWord());
		
		instructionClassHandlerTable.put(
				InstructionClass.CONDITIONAL_SET,
				new ConditionalSet());
		*/
	}

	private static void createInstructionClassTable() 
	{
		instructionClassTable = new Hashtable<String, InstructionClass>();
		
		String integerAluImplicitDestination[] = "and|orr|eor|bic|orn|add|adc|sub|sbc|rsb|lsl|lsr|asr|ror|rrx"
				.split("\\|");
		for (int i = 0; i < integerAluImplicitDestination.length; i++)
			instructionClassTable.put(integerAluImplicitDestination[i],
					InstructionClass.INTEGER_ALU_IMPLICIT_DESTINATION);

		String integerALUNoImplicitDestination[] = "cmp|cmn|tst|teq".split("\\|");
		for (int i = 0; i < integerALUNoImplicitDestination.length; i++)
			instructionClassTable.put(integerALUNoImplicitDestination[i],
					InstructionClass.INTEGER_ALU_NO_IMPLICIT_DESTINATION);

		/*String singleOperandIntegerALU[] = "neg|inc|dec|not|bswap".split("\\|");
		for (int i = 0; i < singleOperandIntegerALU.length; i++)
			instructionClassTable.put(singleOperandIntegerALU[i],
					InstructionClass.SINGLE_OPERAND_INTEGER_ALU);

		String singleOperandIntegerALUImplicitAccumulator[] = "cwd|cdq|cbw|cwde|daa|das|aaa|aas|aam|aad"
				.split("\\|");
		for (int i = 0; i < singleOperandIntegerALUImplicitAccumulator.length; i++)
			instructionClassTable
					.put(
							singleOperandIntegerALUImplicitAccumulator[i],
							InstructionClass.SINGLE_OPERAND_INTEGER_ALU_IMPLICIT_ACCUMULATOR);

		*/					
		String conditionalJump[] = "beq|bne|bhs|blo|bml|bpl|bvs|bhi|bls|bge|blt|bgt|ble|bal"
				.split("\\|");
		for (int i = 0; i < conditionalJump.length; i++)
			instructionClassTable.put(conditionalJump[i],
					InstructionClass.CONDITIONAL_BRANCH);

		String unconditionalJump[] = "b".split("\\|");
		for (int i = 0; i < unconditionalJump.length; i++)
			instructionClassTable.put(unconditionalJump[i],
					InstructionClass.UNCONDITIONAL_BRANCH);

	
		String move[] = "mov|mvn|movt".split("\\|");
		for (int i = 0; i < move.length; i++)
			instructionClassTable.put(move[i], InstructionClass.MOVE);

		String load[] = "ldr|ldrh|ldrb"
				.split("\\|");
		for (int i = 0; i < load.length; i++)
			instructionClassTable.put(load[i],
					InstructionClass.LOAD);
		
		String loadblock[] = "ldm"
				.split("\\|");
		for (int i = 0; i < loadblock.length; i++)
			instructionClassTable.put(loadblock[i],
					InstructionClass.LOAD_BLOCK);
					
		
		String store[] = "str|strh|strb"
				.split("\\|");
		for (int i = 0; i < store.length; i++)
			instructionClassTable.put(store[i],
					InstructionClass.STORE);
		
		String storeblock[] = "stm"
				.split("\\|");
		for (int i = 0; i < storeblock.length; i++)
			instructionClassTable.put(storeblock[i],
					InstructionClass.STORE_BLOCK);
	     
		String pop[] = "pop|popne|popeq".split("\\|");
		for (int i = 0; i < pop.length; i++)
			instructionClassTable.put(pop[i], InstructionClass.POP);

		String push[] = "push|pushne|pusheq".split("\\|");
		for (int i = 0; i < push.length; i++)
			instructionClassTable.put(push[i], InstructionClass.PUSH);
		
		String nop[] = "nop".split("\\|");
		for (int i = 0; i < nop.length; i++)
			instructionClassTable.put(nop[i], InstructionClass.NOP);

		/*
		String integerMultiplication[] = "mul|imul".split("\\|");
		for (int i = 0; i < integerMultiplication.length; i++)
			instructionClassTable.put(integerMultiplication[i],
					InstructionClass.INTEGER_MULTIPLICATION);

		String integerDivision[] = "div|idiv".split("\\|");
		for (int i = 0; i < integerDivision.length; i++)
			instructionClassTable.put(integerDivision[i],
					InstructionClass.INTEGER_DIVISION);

		String interrupt[] = "int".split("\\|");
		for (int i = 0; i < interrupt.length; i++)
			instructionClassTable.put(interrupt[i], InstructionClass.INTERRUPT);

		String floatingPointLoadConstant[] = "fld1|fldz|fldl2t|fldl2e|fldpi|fldlg2|fldln2"
				.split("\\|");
		for (int i = 0; i < floatingPointLoadConstant.length; i++)
			instructionClassTable.put(floatingPointLoadConstant[i],
					InstructionClass.FLOATING_POINT_LOAD_CONSTANT);

		String floatingPointLoad[] = "fld|fild".split("\\|");
		for (int i = 0; i < floatingPointLoad.length; i++)
			instructionClassTable.put(floatingPointLoad[i],
					InstructionClass.FLOATING_POINT_LOAD);

		String floatingPointStore[] = "fst|fstp|fist|fistp".split("\\|");
		for (int i = 0; i < floatingPointStore.length; i++)
			instructionClassTable.put(floatingPointStore[i],
					InstructionClass.FLOATING_POINT_STORE);

		String floatingPointMultiplication[] = "fmul|fmulp|fimul|fimulp|mulsd"
				.split("\\|");
		for (int i = 0; i < floatingPointMultiplication.length; i++)
			instructionClassTable.put(floatingPointMultiplication[i],
					InstructionClass.FLOATING_POINT_MULTIPLICATION);

		String floatingPointDivision[] = "fdiv|fdivp|fidiv|fidivp|fdivr|fdivrp|divsd".split("\\|");
		for (int i = 0; i < floatingPointDivision.length; i++)
			instructionClassTable.put(floatingPointDivision[i],
					InstructionClass.FLOATING_POINT_DIVISION);

		String floatingPointALU[] = "fadd|faddp|fiadd|fiaddp|fsub|fsubp|fsubr|fsubrp|fisub|fisubr|fisubrp|addsd|subsd|unpcklps|ucomisd"
				.split("\\|");
		for (int i = 0; i < floatingPointALU.length; i++)
			instructionClassTable.put(floatingPointALU[i],
					InstructionClass.FLOATING_POINT_ALU);String nop[] = "nop".split("\\|");
		for (int i = 0; i < nop.length; i++)
			instructionClassTable.put(nop[i], InstructionClass.NOP);

		// TODO : look out for floating point operations that require a
		// single operand which is source as well as destination
		String floatingPointSingleOperandALU[] = "fabs|fchs|frdint".split("\\|");
		for (int i = 0; i < floatingPointSingleOperandALU.length; i++)
			instructionClassTable.put(floatingPointSingleOperandALU[i],
					InstructionClass.FLOATING_POINT_SINGLE_OPERAND_ALU);

		String floatingPointComplexOperation[] = "fsqrt".split("\\|");
		for (int i = 0; i < floatingPointComplexOperation.length; i++)
			instructionClassTable.put(floatingPointComplexOperation[i],
					InstructionClass.FLOATING_POINT_COMPLEX_OPERATION);

		String stringMove[] = "movs|movsd".split("\\|");
		for (int i = 0; i < stringMove.length; i++)
			instructionClassTable.put(stringMove[i],
					InstructionClass.STRING_MOVE);

		String stringCompare[] = "cmpsb|cmps".split("\\|");
		for (int i = 0; i < stringCompare.length; i++)
			instructionClassTable.put(stringCompare[i],
					InstructionClass.STRING_COMPARE);

		
		String FUCompare[] = "fcom|fcomp|fcompp|fucom|fucomp|fucompp|fcomi|fcomip|fucomi|fucomip".split("\\|");
		for(int i=0; i < FUCompare.length; i++)
			instructionClassTable.put(FUCompare[i], 
					InstructionClass.FLOATING_POINT_COMPARE);
		
		String FloatingPointConditionalMove[] = "fcmovb|fcmove|fcmovbe|fcmovu|fcmovnb|fcmovne|fcmovnbe|fcmovnu".split("\\|");
		for(int i=0; i < FloatingPointConditionalMove.length; i++)
			instructionClassTable.put(FloatingPointConditionalMove[i], 
					InstructionClass.FLOATING_POINT_CONDITIONAL_MOVE);
		
		String Leave[]="leave".split("\\|");
		for(int i=0; i<Leave.length; i++)
			instructionClassTable.put(Leave[i],
					InstructionClass.LEAVE);
		
		String FloatingPointLoadControlWord[] = "fldcw".split("\\|");
		for(int i=0; i<FloatingPointLoadControlWord.length; i++)
			instructionClassTable.put(FloatingPointLoadControlWord[i],
					InstructionClass.FLOATING_POINT_LOAD_CONTROL_WORD);

		
		String FloatingPointStoreControlWord[] = "fstcw|fnstcw".split("\\|");
		for(int i=0; i<FloatingPointStoreControlWord.length; i++)
			instructionClassTable.put(FloatingPointStoreControlWord[i],
					InstructionClass.FLOATING_POINT_STORE_CONTROL_WORD);
		
		*/
	}
	private static Matcher ImplicitAluMatcher,ExplicitAluMatcher,ConditionBranchMatcher,UnConditionBranchMatcher,CompareBranchMatcher,BranchLinkMatcher, MoveMatcher,LoadMatcher,StoreMatcher,LoadBlockMatcher,StoreBlockMatcher,PushMatcher,PopMatcher,NopMatcher;
	
	private static void createMatchers() {
		Pattern p;
		
		p = Pattern.compile("and.*|orr.*|eor.*|bic.*|orn.*|add.*|adc.*|sub.*|sbc.*|rsb.*|lsl.*|lsr.*|asr.*|ror.*|rrx.*");
		ImplicitAluMatcher = p.matcher("");
				
		p = Pattern.compile("cmp.*|cmn.*|tst.*|teq.*|clz*");
		ExplicitAluMatcher = p.matcher("");
		
		p = Pattern.compile("beq|bne|bhs|blo|bml|bpl|bvs|bhi|bls|bge|blt|bgt|ble|bal");
		ConditionBranchMatcher=p.matcher("");
		
		p = Pattern.compile("b|bx*");
		UnConditionBranchMatcher=p.matcher("");
		
		p = Pattern.compile("cbz|cbnz");
		CompareBranchMatcher=p.matcher("");
		
		p = Pattern.compile("bl");
		BranchLinkMatcher=p.matcher("");
		
		p = Pattern.compile("mov.*|mvn.*|movt.*");
		MoveMatcher=p.matcher("");
		
		p = Pattern.compile("ldr.*");//include ldrh, ldrb FIX: for ldrd
		LoadMatcher=p.matcher("");
		
		p = Pattern.compile("str.*");
		StoreMatcher=p.matcher("");
		
		p = Pattern.compile("ldm.*");
		LoadBlockMatcher=p.matcher("");
		
		p = Pattern.compile("stm.*");
		StoreBlockMatcher=p.matcher("");
		
		p = Pattern.compile("push.*");
		PushMatcher=p.matcher("");
		
		p = Pattern.compile("pop.*");
		PopMatcher=p.matcher("");
	
		p = Pattern.compile("nop.*");
		NopMatcher=p.matcher("");
	}
	public static InstructionClass getInstructionClass(String operation) {
		
		if(ImplicitAluMatcher==null){
		 createMatchers();
		 }
		 
		if (operation == null)
			return InstructionClass.INVALID;

		if(ImplicitAluMatcher.reset(operation).matches())
		{
		    return InstructionClass.INTEGER_ALU_IMPLICIT_DESTINATION;
		}
		else if(ExplicitAluMatcher.reset(operation).matches())
		{
		    return InstructionClass.INTEGER_ALU_NO_IMPLICIT_DESTINATION;
		}
		else if(MoveMatcher.reset(operation).matches())
		{
		    return InstructionClass.MOVE;
		}
		else if(ConditionBranchMatcher.reset(operation).matches())
		{
		    return InstructionClass.CONDITIONAL_BRANCH;
		}
		else if(UnConditionBranchMatcher.reset(operation).matches())
		{
		    return InstructionClass.UNCONDITIONAL_BRANCH;
		}
		else if(CompareBranchMatcher.reset(operation).matches())
		{
		    return InstructionClass.COMPARE_BRANCH;
		}
		else if(BranchLinkMatcher.reset(operation).matches())
		{
		    return InstructionClass.BRANCH_LINK;
		}
		else if(LoadMatcher.reset(operation).matches())
		{
		    return InstructionClass.LOAD;
		}
		else if(StoreMatcher.reset(operation).matches())
		{
		    return InstructionClass.STORE;
		}
		else if(LoadBlockMatcher.reset(operation).matches())
		{
		    return InstructionClass.LOAD_BLOCK;
		}
		else if(StoreBlockMatcher.reset(operation).matches())
		{
		    return InstructionClass.STORE_BLOCK;
		}
		else if(StoreMatcher.reset(operation).matches())
		{
		    return InstructionClass.STORE;
		}
		else if(PushMatcher.reset(operation).matches())
		{
		    return InstructionClass.PUSH;
		}
		else if(PopMatcher.reset(operation).matches())
		{
		    return InstructionClass.POP;
		}
		else if(NopMatcher.reset(operation).matches())
		{
		    return InstructionClass.NOP;
		}
		else {//if (instructionClass == null)
			return InstructionClass.INVALID;
		}/*if (instructionClassTable == null)
			createInstructionClassTable();
			*/

		  /*InstructionClass instructionClass;
		instructionClass = instructionClassTable.get(operation);

		if (instructionClass == null)
			return InstructionClass.INVALID;
		else
			return instructionClass;*/
	}

	public static StaticInstructionHandler getInstructionClassHandler(
			InstructionClass instructionClass) {

		if (instructionClassHandlerTable == null)
			createInstructionClassHandlerTable();

		if (instructionClass == InstructionClass.INVALID)
			return null;

		StaticInstructionHandler handler;
		handler = instructionClassHandlerTable.get(instructionClass);

		return handler;
	}
}

