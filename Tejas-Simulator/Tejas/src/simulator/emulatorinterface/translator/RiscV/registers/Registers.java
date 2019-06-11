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

package emulatorinterface.translator.RiscV.registers;


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
		return Operand.getIntegerRegister(encodeRegister("t" + tempRegister.numTempIntRegister++));
	}

	//Allocate a new temporary float register	
	public static Operand getTempFloatReg(TempRegisterNum tempRegister)
	{
		return Operand.getFloatRegister(encodeRegister("ft" + tempRegister.numTempFloatRegister++));
	}
	
	public static void createRegisterHashTable()
	{
		//Create required hash-tables
		integerRegistersHashTable = new Hashtable<String, Long>();
		floatRegistersHashTable = new Hashtable<String, Long>();
		
		
		
		//-------------------------Integer register-----------------------------------------------
		//The constant value register zero
		integerRegistersHashTable.put("zero", new Long(0));
		integerRegistersHashTable.put("ra", new Long(1));
		integerRegistersHashTable.put("sp", new Long(2));
		integerRegistersHashTable.put("gp", new Long(3));
		integerRegistersHashTable.put("tp", new Long(4));
		integerRegistersHashTable.put("pc", new Long(5));
		integerRegistersHashTable.put("load_reg", new Long(6));
		integerRegistersHashTable.put("store_reg", new Long(7));
		
		//registers storing function arguments and return values
		integerRegistersHashTable.put("a0", new Long(8));
		integerRegistersHashTable.put("a1", new Long(9));
		integerRegistersHashTable.put("a2", new Long(10));
		integerRegistersHashTable.put("a3", new Long(11));
		integerRegistersHashTable.put("a4", new Long(12));
		integerRegistersHashTable.put("a5", new Long(13));
		integerRegistersHashTable.put("a6", new Long(14));
		integerRegistersHashTable.put("a7", new Long(15));
		
		//Saved Registers
		integerRegistersHashTable.put("s0", new Long(17));
		integerRegistersHashTable.put("s1", new Long(18));
		integerRegistersHashTable.put("s2", new Long(19));
		integerRegistersHashTable.put("s3", new Long(20));
		integerRegistersHashTable.put("s4", new Long(21));
		integerRegistersHashTable.put("s5", new Long(22));
		integerRegistersHashTable.put("s6", new Long(23));
		integerRegistersHashTable.put("s7", new Long(24));
		integerRegistersHashTable.put("s8", new Long(25));
		integerRegistersHashTable.put("s9", new Long(26));
		integerRegistersHashTable.put("s10", new Long(27));
		integerRegistersHashTable.put("s11", new Long(28));
		
		//Temporary registers
		integerRegistersHashTable.put("t0", new Long(29));
		integerRegistersHashTable.put("t1", new Long(30));
		integerRegistersHashTable.put("t2", new Long(31));
		integerRegistersHashTable.put("t3", new Long(32));
		integerRegistersHashTable.put("t4", new Long(33));
		integerRegistersHashTable.put("t5", new Long(34));
		integerRegistersHashTable.put("t6", new Long(35));
		integerRegistersHashTable.put("t7", new Long(36));
		integerRegistersHashTable.put("branch_reg", new Long(37));
		
		
		//--------------------------Floating Point registers---------------------------------
		
		//floating point saved registers
		floatRegistersHashTable.put("fs0",  new Long(0));
		floatRegistersHashTable.put("fs1",  new Long(1));
		floatRegistersHashTable.put("fs2",  new Long(2));		
		floatRegistersHashTable.put("fs3",  new Long(3));
		floatRegistersHashTable.put("fs4",  new Long(4));
		floatRegistersHashTable.put("fs5",  new Long(5));
		floatRegistersHashTable.put("fs6",  new Long(6));
		floatRegistersHashTable.put("fs7",  new Long(7));
		floatRegistersHashTable.put("fs8",  new Long(8));
		floatRegistersHashTable.put("fs9",  new Long(9));
		floatRegistersHashTable.put("fs10",  new Long(10));
		floatRegistersHashTable.put("fs11",  new Long(11));	
		
		//Floating point function arguments and return values
		floatRegistersHashTable.put("fa0",  new Long(12));
		floatRegistersHashTable.put("fa1",  new Long(13));
		floatRegistersHashTable.put("fa2",  new Long(14));
		floatRegistersHashTable.put("fa3",  new Long(15));
		floatRegistersHashTable.put("fa4",  new Long(16));
		floatRegistersHashTable.put("fa5",  new Long(17));	
		floatRegistersHashTable.put("fa6",  new Long(18));
		floatRegistersHashTable.put("fa7",  new Long(19));		

		//temporary floating-point registers
		floatRegistersHashTable.put("ft0",  new Long(20));
		floatRegistersHashTable.put("ft1",  new Long(21));
		floatRegistersHashTable.put("ft2",  new Long(22));
		floatRegistersHashTable.put("ft3",  new Long(23));
		floatRegistersHashTable.put("ft4",  new Long(24));
		floatRegistersHashTable.put("ft5",  new Long(25));
		floatRegistersHashTable.put("ft6",  new Long(26));
		floatRegistersHashTable.put("ft7",  new Long(27));
		floatRegistersHashTable.put("ft8",  new Long(28));
		floatRegistersHashTable.put("ft9",  new Long(29));
		floatRegistersHashTable.put("ft10",  new Long(30));
		floatRegistersHashTable.put("ft11",  new Long(31));

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
			
	
 	public static Operand getStackPointer()
 	{
 		return Operand.getIntegerRegister(encodeRegister("sp"));
 	}
        public static Operand getReturnAddress()
 	{
 		return Operand.getIntegerRegister(encodeRegister("ra"));
 	}
 	
        public static Operand getGlobalPointer()
 	{
 		return Operand.getIntegerRegister(encodeRegister("gp"));
 	}
        public static Operand getThreadPointer()
 	{
 		return Operand.getIntegerRegister(encodeRegister("tp"));
 	}
 	public static Operand getTopFPRegister()
 	{
 		return Operand.getFloatRegister(encodeRegister("ft0"));
 	}
 	
 	public static Operand getSecondTopFPRegister()
 	{
 		return Operand.getFloatRegister(encodeRegister("ft1"));
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
 		&& operand.getValue() >=encodeRegister("t0") && operand.getValue() <=encodeRegister("t6"));
 	}
 	
 	private static void checkAndCreateRegisterHashTable()
 	{
 		if(integerRegistersHashTable==null || floatRegistersHashTable==null)
 			createRegisterHashTable();
 	}
 	
 	public static Operand getCounterRegister()
 	{
 		return Operand.getIntegerRegister(encodeRegister("pc"));
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
