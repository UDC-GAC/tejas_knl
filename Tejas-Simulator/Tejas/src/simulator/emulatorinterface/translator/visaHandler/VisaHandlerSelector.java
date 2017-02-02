package emulatorinterface.translator.visaHandler;

import generic.OperationType;


public class VisaHandlerSelector
{
	private static DynamicInstructionHandler inValid;
	private static DynamicInstructionHandler integerALU;
	private static DynamicInstructionHandler integerMul;
	private static DynamicInstructionHandler integerDiv;
	private static DynamicInstructionHandler floatALU;
	private static DynamicInstructionHandler floatMul;
	private static DynamicInstructionHandler floatDiv;
	private static DynamicInstructionHandler load;
	private static DynamicInstructionHandler store;
	private static DynamicInstructionHandler jump;
	private static DynamicInstructionHandler branch;
	private static DynamicInstructionHandler mov;
	private static DynamicInstructionHandler xchg;
	private static DynamicInstructionHandler acceleratedOp;
	private static DynamicInstructionHandler nop;
	private static DynamicInstructionHandler interrupt;

	public static DynamicInstructionHandler selectHandler(OperationType operationType)
	{
		// if the handlers are not defined in the beginning, we
		// must initialise them.
		if(inValid==null)
		{
			createVisaHandlers();
		}
		
		switch(operationType)
		{
			case inValid:
				return inValid;

			case integerALU:
				return integerALU;
				
			case integerMul:
				return integerMul;
				
			case integerDiv:
				return integerDiv;

			case floatALU:
				return floatALU;
				
			case floatMul:
				return floatMul;
				
			case floatDiv:
				return floatDiv;
				
			case load:
				return load;
				
			case store:
				return store;
				
			case jump:
				return jump;
				
			case branch:
				return branch;
				
			case mov:
				return mov;
				
			case xchg:
				return xchg;
				
			case acceleratedOp:
				return acceleratedOp;
				
			case nop:
				return nop;
				
			case interrupt:
				return interrupt;
				
			default:
				System.out.print("Invalid operation");
				System.exit(0);
				return null;
		}
	}

	private static void createVisaHandlers() 
	{
		inValid = new Invalid();
		integerALU = new IntegerALU();
		integerMul = new IntegerMul();
		integerDiv = new IntegerDiv();
		floatALU = new FloatALU();
		floatMul = new FloatMul();
		floatDiv = new FloatDiv();
		load = new Load();
		store = new Store();
		jump = new Jump();
		branch = new Branch();
		mov = new Mov();
		xchg = new Xchg();
		acceleratedOp = new AcceleratedOp();
		nop = new NOP();
		interrupt = new Interrupt();
	}
}
