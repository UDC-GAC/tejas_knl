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

import java.util.Hashtable;

public class InstructionClassTable {
	private static Hashtable<String, InstructionClass> instructionClassTable;
	private static Hashtable<InstructionClass, X86StaticInstructionHandler> instructionClassHandlerTable;

	private static void createInstructionClassHandlerTable() {
		// create an empty hash-table for storing object references.
		instructionClassHandlerTable = new Hashtable<InstructionClass, X86StaticInstructionHandler>();

		instructionClassHandlerTable.put(
				InstructionClass.INTEGER_ALU_IMPLICIT_DESTINATION,
				new IntegerALUImplicitDestination());

		instructionClassHandlerTable.put(
				InstructionClass.INTEGER_ALU_NO_IMPLICIT_DESTINATION,
				new IntegerALUExplicitDestination());

		instructionClassHandlerTable.put(
				InstructionClass.SINGLE_OPERAND_INTEGER_ALU,
				new SingleOperandIntALU());

		instructionClassHandlerTable.put(
				InstructionClass.SINGLE_OPERAND_INTEGER_ALU_IMPLICIT_ACCUMULATOR,
				new SingleOperandIntALUImplicitAccumulator());

		instructionClassHandlerTable.put(
				InstructionClass.SHIFT_OPERATION_THREE_OPERANDS,
				new ShiftOperationThreeOperand());

		instructionClassHandlerTable.put(
				InstructionClass.MOVE, 
				new Move());

		instructionClassHandlerTable.put(
				InstructionClass.CONDITIONAL_MOVE,
				new ConditionalMove());

		instructionClassHandlerTable.put(
				InstructionClass.EXCHANGE,
				new Exchange());

		instructionClassHandlerTable.put(
				InstructionClass.EXCHANGE_AND_ADD,
				new ExchangeAndAdd());

		instructionClassHandlerTable.put(
				InstructionClass.CONDITIONAL_JUMP,
				new ConditionalJump());

		instructionClassHandlerTable.put(
				InstructionClass.UNCONDITIONAL_JUMP,
				new UnconditionalJump());

		instructionClassHandlerTable.put(
				InstructionClass.LOOP, 
				new Loop());

		instructionClassHandlerTable.put(
				InstructionClass.PUSH, 
				new Push());

		instructionClassHandlerTable.put(
				InstructionClass.POP, 
				new Pop());

		instructionClassHandlerTable.put(
				InstructionClass.CALL, 
				new Call());

		instructionClassHandlerTable.put(
				InstructionClass.INTERRUPT,
				new Interrupt());

		instructionClassHandlerTable.put(
				InstructionClass.RETURN,
				new ReturnOp());

		instructionClassHandlerTable.put(
				InstructionClass.NOP, 
				new NOP());

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
				InstructionClass.FLOATING_POINT_LOAD_CONSTANT,
				new FloatingPointLoadConstant());

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
				new FloatingPointDivision());

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
				InstructionClass.CONVERT_FLOAT_TO_INTEGER,
				new ConvertFloatToInteger());
		
		instructionClassHandlerTable.put(
				InstructionClass.CONVERT_INTEGER_TO_FLOAT,
				new ConvertIntegerToFloat());

		instructionClassHandlerTable.put(
				InstructionClass.STRING_MOVE,
				new StringMove());

		instructionClassHandlerTable.put(
				InstructionClass.STRING_COMPARE,
				new StringCompare());

		instructionClassHandlerTable.put(
				InstructionClass.SSE_MOVE,
				new SSEMove());

		instructionClassHandlerTable.put(
				InstructionClass.SSE_ALU, 
				new SSEALU());

		instructionClassHandlerTable.put(
				InstructionClass.SSE_MULTIPLICATION,
				new SSEMultiplication());

		instructionClassHandlerTable.put(
				InstructionClass.SSE_DIVISION,
				new SSEDivision());
		
		instructionClassHandlerTable.put(
				InstructionClass.SSE_COMPARE_PACKED_DATA,
				new SSEComparePackedData());
		
		instructionClassHandlerTable.put(
				InstructionClass.FLOATING_POINT_COMPARE,
				new FloatingPointCompare());
				
		instructionClassHandlerTable.put(
				InstructionClass.FLOATING_POINT_CONDITIONAL_MOVE,
				new FloatingPointConditionalMove());
		
		instructionClassHandlerTable.put(
				InstructionClass.LEAVE,
				new Leave());
		
		instructionClassHandlerTable.put(
				InstructionClass.FLOATING_POINT_LOAD_CONTROL_WORD,
				new FloatingPointLoadControlWord());
		
		instructionClassHandlerTable.put(
				InstructionClass.FLOATING_POINT_STORE_CONTROL_WORD,
				new FloatingPointStoreControlWord());
		
		instructionClassHandlerTable.put(
				InstructionClass.CONDITIONAL_SET,
				new ConditionalSet());
	}

	private static void createInstructionClassTable() 
	{
		instructionClassTable = new Hashtable<String, InstructionClass>();

		String interAluImplicitDestination[] = "and|or|xor|add|adc|sub|sbb|bt|bts|btr|btc|shl|sal|shr|sar|rol|rcl|ror|rcr"
				.split("\\|");
		for (int i = 0; i < interAluImplicitDestination.length; i++)
			instructionClassTable.put(interAluImplicitDestination[i],
					InstructionClass.INTEGER_ALU_IMPLICIT_DESTINATION);

		String integerALUNoImplicitDestination[] = "cmp|test".split("\\|");
		for (int i = 0; i < integerALUNoImplicitDestination.length; i++)
			instructionClassTable.put(integerALUNoImplicitDestination[i],
					InstructionClass.INTEGER_ALU_NO_IMPLICIT_DESTINATION);

		String singleOperandIntegerALU[] = "neg|inc|dec|not|bswap".split("\\|");
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

		String conditionalJump[] = "jo|jno|jc|jb|jnae|jnc|jae|jnb|je|jz|jne|jnz|jbe|jna|ja|jnbe|js|jns|jp|jpe|jnp|jpo|jl|jnge|jge|jnl|jle|jng|jg|jnle|jcxz|jecxz"
				.split("\\|");
		for (int i = 0; i < conditionalJump.length; i++)
			instructionClassTable.put(conditionalJump[i],
					InstructionClass.CONDITIONAL_JUMP);

		String unconditionalJump[] = "jmp".split("\\|");
		for (int i = 0; i < unconditionalJump.length; i++)
			instructionClassTable.put(unconditionalJump[i],
					InstructionClass.UNCONDITIONAL_JUMP);

		String loop[] = "loop|loopw|loopd|loope|looped|loopew|loopne|loopned|loopnew|loopz|loopzd|loopzw|loopnz|loopnzd|loopnzw"
				.split("\\|");
		for (int i = 0; i < loop.length; i++)
			instructionClassTable.put(loop[i], InstructionClass.LOOP);

		String shiftOperationThreeOperands[] = "shld|shrd".split("\\|");
		for (int i = 0; i < shiftOperationThreeOperands.length; i++)
			instructionClassTable.put(shiftOperationThreeOperands[i],
					InstructionClass.SHIFT_OPERATION_THREE_OPERANDS);

		String returnOp[] = "ret|retn|retf|retw|retnw|retfw|retd|retnd|retfd|iret|iretw|retd"
				.split("\\|");
		for (int i = 0; i < returnOp.length; i++)
			instructionClassTable.put(returnOp[i], InstructionClass.RETURN);

		//FIXME : movsx* does a sign-extend + move. Right now, we are doing move only.
		String move[] = "mov|movsx|movsxd|movzx|movzxd|movsd|movnti|movslq|movzbl".split("\\|");
		for (int i = 0; i < move.length; i++)
			instructionClassTable.put(move[i], InstructionClass.MOVE);

		String conditionalMove[] = "cmov|cmovb|cmovae|cmovg|cmovbe|cmovne|cmove|cmova"
				.split("\\|");
		for (int i = 0; i < conditionalMove.length; i++)
			instructionClassTable.put(conditionalMove[i],
					InstructionClass.CONDITIONAL_MOVE);

		String exchange[] = "xchg".split("\\|");
		for (int i = 0; i < exchange.length; i++)
			instructionClassTable.put(exchange[i], InstructionClass.EXCHANGE);

		String exchangeAndAdd[] = "xadd".split("\\|");
		for (int i = 0; i < exchangeAndAdd.length; i++)
			instructionClassTable.put(exchangeAndAdd[i],
					InstructionClass.EXCHANGE_AND_ADD);

		String pop[] = "pop|popw|popd".split("\\|");
		for (int i = 0; i < pop.length; i++)
			instructionClassTable.put(pop[i], InstructionClass.POP);

		String push[] = "push|pushw|pushd".split("\\|");
		for (int i = 0; i < push.length; i++)
			instructionClassTable.put(push[i], InstructionClass.PUSH);

		String call[] = "call|callq".split("\\|");
		for (int i = 0; i < call.length; i++)
			instructionClassTable.put(call[i], InstructionClass.CALL);

		String nop[] = "nop|nopl|fnop|nopw".split("\\|");
		for (int i = 0; i < nop.length; i++)
			instructionClassTable.put(nop[i], InstructionClass.NOP);

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

		String loadEffectiveAddress[] = "lea".split("\\|");
		for (int i = 0; i < loadEffectiveAddress.length; i++)
			instructionClassTable.put(loadEffectiveAddress[i],
					InstructionClass.LOAD_EFFECTIVE_ADDRESS);

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
					InstructionClass.FLOATING_POINT_ALU);

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

		String floatingPointExchange[] = "fxch".split("\\|");
		for (int i = 0; i < floatingPointExchange.length; i++)
			instructionClassTable.put(floatingPointExchange[i],
					InstructionClass.FLOATING_POINT_EXCHANGE);
		
		String convertFloatToInteger[] = "cvtpd2dq|cvtpd2pi|cvtps2dq|cvtsd2si|cvtss2si|cvttpd2dq|cvttpd2pi|cvttps2dq|cvttps2pi|cvttsd2si|cvttss2si".split("\\|");
		for (int i = 0; i < convertFloatToInteger.length; i++)
			instructionClassTable.put(convertFloatToInteger[i],
					InstructionClass.CONVERT_FLOAT_TO_INTEGER);
		
		String convertIntegerToFloat[] = "cvtdq2pd|cvtdq2ps|cvtpi2pd|cvtpi2ps|cvtsi2sd|cvtsi2ss".split("\\|");
		for (int i = 0; i < convertIntegerToFloat.length; i++)
			instructionClassTable.put(convertIntegerToFloat[i],
					InstructionClass.CONVERT_INTEGER_TO_FLOAT);

		String stringMove[] = "movs|movsd".split("\\|");
		for (int i = 0; i < stringMove.length; i++)
			instructionClassTable.put(stringMove[i],
					InstructionClass.STRING_MOVE);

		String stringCompare[] = "cmpsb|cmps".split("\\|");
		for (int i = 0; i < stringCompare.length; i++)
			instructionClassTable.put(stringCompare[i],
					InstructionClass.STRING_COMPARE);

		// TODO Lock and repeat are not yet handled
		String lock[] = "lock".split("\\|");
		for (int i = 0; i < lock.length; i++)
			instructionClassTable.put(lock[i], InstructionClass.LOCK);

		String repeat[] = "rep|repe|repne|repz|repnz".split("\\|");
		for (int i = 0; i < repeat.length; i++)
			instructionClassTable.put(repeat[i], InstructionClass.REPEAT);

		// TODO SSE Instructions
		String SSEMove[] = "movaps|movapd|movups|movupd|movhps|movhpd|movhlps|movlpd|movlhps|movlhpd|movlps|movlpd|movmskps|movmskpd|movss|movsd|movdqa|movdqu|movq2dq|movdq2q|movq|movl"
				.split("\\|");
		for (int i = 0; i < SSEMove.length; i++)
			instructionClassTable.put(SSEMove[i], InstructionClass.SSE_MOVE);

		String SSEALU[] = "addps|addpd|addss|addsd|subps|subpd|subss|subsd|andps|andpd|andnps|andnpd|orps|orpd|xorps|xorpd|pand|por|pxor"
				.split("\\|");
		for (int i = 0; i < SSEALU.length; i++)
			instructionClassTable.put(SSEALU[i], InstructionClass.SSE_ALU);

		String SSEMultiplication[] = "mulps|mulss".split("\\|");
		for (int i = 0; i < SSEMultiplication.length; i++)
			instructionClassTable.put(SSEMultiplication[i],
					InstructionClass.SSE_MULTIPLICATION);

		String SSEDivision[] = "divps|divss".split("\\|");
		for (int i = 0; i < SSEDivision.length; i++)
			instructionClassTable.put(SSEDivision[i],
					InstructionClass.SSE_DIVISION);
		
		String SSEComparePackedData[] = "pcmpeqb|pcmpeqw|pcmpeqd|pcmpgtb|pcmpgtw|pcmpgtd".split("\\|");
		for (int i = 0; i < SSEComparePackedData.length; i++)
			instructionClassTable.put(SSEComparePackedData[i],
					InstructionClass.SSE_COMPARE_PACKED_DATA);
		
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
		
		String ConditionalSet[] = "seta|setae|setb|setbe|setc|sete|setg|setge|setl|setle|setna|setnae|setnb|setnbe|setnc|setne|setng|setnge|setnl|setnle|setno|setnp|setns|setnz|seto|setp|setpe|setpo|sets|setz"
									.split("\\|");
		for(int i=0; i<ConditionalSet.length; i++)
			instructionClassTable.put(ConditionalSet[i], 
					InstructionClass.CONDITIONAL_SET);
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

	public static X86StaticInstructionHandler getInstructionClassHandler(
			InstructionClass instructionClass) {

		if (instructionClassHandlerTable == null)
			createInstructionClassHandlerTable();

		if (instructionClass == InstructionClass.INVALID)
			return null;

		X86StaticInstructionHandler handler;
		handler = instructionClassHandlerTable.get(instructionClass);

		return handler;
	}
}