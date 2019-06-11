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

package emulatorinterface.translator.arm.registers;


import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import generic.Operand;


public class Registers 
{
	private static Hashtable<String, Long> integerRegistersHashTable = null;
	private static Hashtable<String, Long> floatRegistersHashTable = null;

	// --- Number of temporary registers must be maintained by the translator ---
//	public static int noOfIntTempRegs = 0;
//	public static int noOfFloatTempRegs = 0;

	//Allocate a new temporary register	
	public static Operand getTempIntReg(TempRegisterNum tempRegister)
	{
		return Operand.getIntegerRegister(encodeRegister("temp" + tempRegister.numTempIntRegister++));
	}

	//Allocate a new temporary float register	
	public static Operand getTempFloatReg(TempRegisterNum tempRegister)
	{
		return Operand.getFloatRegister(encodeRegister("tempFloat" + tempRegister.numTempFloatRegister++));
	}
	
	public static void createRegisterHashTable()
	{
		//Create required hash-tables
		integerRegistersHashTable = new Hashtable<String, Long>();
		floatRegistersHashTable = new Hashtable<String, Long>();
		
		
		//--------------------------Machine specific registers---------------------------------
		//Segment Registers
		
		// Load register and store register
//		machineSpecificRegistersHashTable.put("load_reg", new Long(8));
//		machineSpecificRegistersHashTable.put("store_reg", new Long(9));
		
		//-------------------------Integer register-----------------------------------------------
		//Registers available to the programmer
		integerRegistersHashTable.put("r0", new Long(0));
		integerRegistersHashTable.put("r1", new Long(1));
		integerRegistersHashTable.put("r2", new Long(2));
		integerRegistersHashTable.put("r3", new Long(3));
		integerRegistersHashTable.put("r4", new Long(4));
		integerRegistersHashTable.put("r5", new Long(5));
		integerRegistersHashTable.put("r6", new Long(6));
		integerRegistersHashTable.put("r7", new Long(7));
		integerRegistersHashTable.put("r8", new Long(8));
		integerRegistersHashTable.put("r9", new Long(9));
		integerRegistersHashTable.put("r10", new Long(10));
		integerRegistersHashTable.put("r11", new Long(11));
		integerRegistersHashTable.put("r12", new Long(12));
		integerRegistersHashTable.put("r13", new Long(13));
		integerRegistersHashTable.put("r14", new Long(14));
		integerRegistersHashTable.put("r15", new Long(15));
		
		
		// Load register and store register
		integerRegistersHashTable.put("load_reg", new Long(18));
		integerRegistersHashTable.put("store_reg", new Long(19));

		// Machine Specific Registers
		integerRegistersHashTable.put("es", new Long(20));
		integerRegistersHashTable.put("cs", new Long(21));
		integerRegistersHashTable.put("ss", new Long(22));
		integerRegistersHashTable.put("ds", new Long(23));
		integerRegistersHashTable.put("fs", new Long(24));
		integerRegistersHashTable.put("gs", new Long(25));
		
		
		integerRegistersHashTable.put("CPSCR", new Long(27));
		integerRegistersHashTable.put("SPSCR", new Long(28));
		
		//Temporary registers
		integerRegistersHashTable.put("temp0", new Long(29));
		integerRegistersHashTable.put("temp1", new Long(30));
		integerRegistersHashTable.put("temp2", new Long(31));
		integerRegistersHashTable.put("temp3", new Long(32));
		integerRegistersHashTable.put("temp4", new Long(33));
		integerRegistersHashTable.put("temp5", new Long(34));
		integerRegistersHashTable.put("temp6", new Long(35));
		integerRegistersHashTable.put("temp7", new Long(36));
		
		
		//-------------------------Floating-point register-----------------------------------------
		//Double precision registers
		
		floatRegistersHashTable.put("d0", new Long(0));
		floatRegistersHashTable.put("d1", new Long(1));
		floatRegistersHashTable.put("d2", new Long(2));
		floatRegistersHashTable.put("d3", new Long(3));
		floatRegistersHashTable.put("d4", new Long(4));
		floatRegistersHashTable.put("d5", new Long(5));
		floatRegistersHashTable.put("d6", new Long(6));
		floatRegistersHashTable.put("d7", new Long(7));
		floatRegistersHashTable.put("d8", new Long(8));
		floatRegistersHashTable.put("d9", new Long(9));
		floatRegistersHashTable.put("d10", new Long(10));
		floatRegistersHashTable.put("d11", new Long(11));
		floatRegistersHashTable.put("d12", new Long(12));
		floatRegistersHashTable.put("d13", new Long(13));
		floatRegistersHashTable.put("d14", new Long(14));
		floatRegistersHashTable.put("d15", new Long(15));

		
		//temporary floating-point registers
		floatRegistersHashTable.put("tempFloat0", new Long(16));
		floatRegistersHashTable.put("tempFloat1", new Long(17));
		floatRegistersHashTable.put("tempFloat2", new Long(18));
		floatRegistersHashTable.put("tempFloat3", new Long(19));
		floatRegistersHashTable.put("FPSCR", new Long(20));
	}

	
	//assign an index to each coarse-register
	public static long encodeRegister(String regStr)
	{
		checkAndCreateRegisterHashTable();
		
		Long codeRegister = null;
		
		if((codeRegister = integerRegistersHashTable.get(regStr)) != null)
		{
			return codeRegister.longValue();
		}
		else if((codeRegister = floatRegistersHashTable.get(regStr)) != null)
		{
			return codeRegister.longValue();
		}
		else
		{
			misc.Error.showErrorAndExit("\n\tNot a valid register : " + regStr + " !!");
			return -1;
		}
	}
	
	public static boolean isFloatRegister(String regStr)
	{
		checkAndCreateRegisterHashTable();
		
		return (floatRegistersHashTable.get(regStr)!=null);
	}
	
	public static boolean isIntegerRegister(String regStr)
	{
		checkAndCreateRegisterHashTable();
		
		return (integerRegistersHashTable.get(regStr)!=null);
	}
			
	//public static void releaseTempRegister(Operand tempRegister)
	//{
	//	//Must be called only from the simplify location method only
	//	noOfIntTempRegs--;
	//}
	
	
	private static Matcher d0,d1,d2,d3,d4,d5,d6,d7,d8,d9,d10,d11,d12,d13,d14,d15,r0,r1,r2,r3,r4,r5,r6,r7,r8,r9,r10,r11,r12,r13,r14,r15;
	private static void createMatchers()
	{
		Pattern p;
		p = Pattern.compile("r0|a1");
		r0 = p.matcher("");
		p = Pattern.compile("r1|a2");
		r1 = p.matcher("");
		p = Pattern.compile("r2|a3");
		r2 = p.matcher("");
		p = Pattern.compile("r3|a4");
		r3 = p.matcher("");
		p = Pattern.compile("r4|v1");
		r4 = p.matcher("");
		p = Pattern.compile("r5|v2");
		r5 = p.matcher("");
		p = Pattern.compile("r6|v3|wr");
		r6 = p.matcher("");
		p = Pattern.compile("r7|v4");
		r7 = p.matcher("");
		p = Pattern.compile("r8|v5");
		r8 = p.matcher("");
		p = Pattern.compile("r9|sb|v6");
		r9 = p.matcher("");
		p = Pattern.compile("r10|sl|v7");
		r10 = p.matcher("");
		p = Pattern.compile("r11|fp|v8");
		r11 = p.matcher("");
		p = Pattern.compile("r12|ip");
		r12 = p.matcher("");
		p = Pattern.compile("r13|sp");
		r13 = p.matcher("");
		p = Pattern.compile("r14|lr");
		r14 = p.matcher("");
		p = Pattern.compile("r15|pc");
		r15 = p.matcher("");
		
		p = Pattern.compile("d0|s0|s1");
		d0 = p.matcher("");
		p = Pattern.compile("d1|s2|s3");
		d1 = p.matcher("");
		p = Pattern.compile("d2|s4|s5");
		d2 = p.matcher("");
		p = Pattern.compile("d3|s6|s7");
		d3 = p.matcher("");
		p = Pattern.compile("d4|s8|s9");
		d4 = p.matcher("");
		p = Pattern.compile("d5|s10|s11");
		d5 = p.matcher("");
		p = Pattern.compile("d6|s12|s13");
		d6 = p.matcher("");
		p = Pattern.compile("d7|s14|s15");
		d7 = p.matcher("");
		p = Pattern.compile("d8|s16|s17");
		d8 = p.matcher("");
		p = Pattern.compile("d9|s18|s19");
		d9 = p.matcher("");
		p = Pattern.compile("d10|s20|s21");
		d10 = p.matcher("");
		p = Pattern.compile("d11|s22|s23");
		d11 = p.matcher("");
		p = Pattern.compile("d12|s24|s25");
		d12 = p.matcher("");
		p = Pattern.compile("d13|s26|s27");
		d13 = p.matcher("");
		p = Pattern.compile("d14|s28|s29");
		d14 = p.matcher("");
		p = Pattern.compile("d15|s30|s31");
		d15 = p.matcher("");
		
	}

	/**
	 * This method converts the smaller parts of register to the complete register
	 * @param operandStr Operand string 
	 */
 	public static String coarsifyRegisters(String operandStr)
	{
 		if(r0==null) {
 			createMatchers();
 		}
 		operandStr = r0.reset(operandStr).replaceAll("r0");
 		operandStr = r1.reset(operandStr).replaceAll("r1");
 		operandStr = r2.reset(operandStr).replaceAll("r2");
 		operandStr = r3.reset(operandStr).replaceAll("r3");
 		operandStr = r4.reset(operandStr).replaceAll("r4");
 		operandStr = r5.reset(operandStr).replaceAll("r5");
 		operandStr = r6.reset(operandStr).replaceAll("r6");
 		operandStr = r7.reset(operandStr).replaceAll("r7");
 		operandStr = r8.reset(operandStr).replaceAll("r8");
 		operandStr = r9.reset(operandStr).replaceAll("r9");
 		operandStr = r10.reset(operandStr).replaceAll("r10");
 		operandStr = r11.reset(operandStr).replaceAll("r11");
 		operandStr = r12.reset(operandStr).replaceAll("r12");
 		operandStr = r13.reset(operandStr).replaceAll("r13");
 		operandStr = r14.reset(operandStr).replaceAll("r14");
 		operandStr = r15.reset(operandStr).replaceAll("r15");
 		
 		operandStr = d0.reset(operandStr).replaceAll("d0");
 		operandStr = d1.reset(operandStr).replaceAll("d1");
 		operandStr = d2.reset(operandStr).replaceAll("d2");
 		operandStr = d3.reset(operandStr).replaceAll("d3");
 		operandStr = d4.reset(operandStr).replaceAll("d4");
 		operandStr = d5.reset(operandStr).replaceAll("d5");
 		operandStr = d6.reset(operandStr).replaceAll("d6");
 		operandStr = d7.reset(operandStr).replaceAll("d7");
 		operandStr = d8.reset(operandStr).replaceAll("d8");
 		operandStr = d9.reset(operandStr).replaceAll("d9");
 		operandStr = d10.reset(operandStr).replaceAll("d10");
 		operandStr = d11.reset(operandStr).replaceAll("d11");
 		operandStr = d12.reset(operandStr).replaceAll("d12");
 		operandStr = d13.reset(operandStr).replaceAll("d13");
 		operandStr = d14.reset(operandStr).replaceAll("d14");
 		operandStr = d15.reset(operandStr).replaceAll("d15");
 		 		 		
		return operandStr;
	}

 	
 	public static Operand getStackPointer()
 	{
 		return Operand.getIntegerRegister(encodeRegister("r13"));
 	}
 	
 	/*
 	public static Operand getSecondTopFPRegister()
 	{
 		return Operand.getFloatRegister(encodeRegister("st(1)"));
 	}
 	*/
 	public static Operand getInstructionPointer()
 	{
 		return Operand.getIntegerRegister(encodeRegister("r15"));
 	}
 	

 	public static Operand getTemporaryMemoryRegister(Operand operand)
	{
 		Operand firstOperand;
 		Operand secondOperand;
 		
 		firstOperand = operand.getMemoryLocationFirstOperand();
 		secondOperand = operand.getMemoryLocationSecondOperand();
 		
		if(	operand.isMemoryOperand() && isTempIntRegister(firstOperand))
		{
			return firstOperand;
		}
		
		else if(operand.isMemoryOperand() && isTempIntRegister(secondOperand))
		{
			return secondOperand;
		}
		
		else
		{
			return null;
		}
	}

 	private static boolean isTempIntRegister(Operand operand)
 	{
 		return( 
 		operand!=null && operand.isIntegerRegisterOperand()
 		&& operand.getValue() >=encodeRegister("temp0") && operand.getValue() <=encodeRegister("temp7"));
 	}
 	
 	private static void checkAndCreateRegisterHashTable()
 	{
 		if(integerRegistersHashTable==null || floatRegistersHashTable==null)
 			createRegisterHashTable();
 	}
 	
 	public static Operand getLinkRegister()
 	{
 		return Operand.getIntegerRegister(encodeRegister("r14"));
 	}

 	
	public static Operand getFloatingPointControlRegister() 
	{
		return Operand.getIntegerRegister(encodeRegister("FPSCR"));
	}

	public static int getMaxIntegerRegisters() {
		checkAndCreateRegisterHashTable();
		return integerRegistersHashTable.size();
	}

	public static int getMaxFloatRegisters() {
		checkAndCreateRegisterHashTable();
		return floatRegistersHashTable.size();
	}
 }
