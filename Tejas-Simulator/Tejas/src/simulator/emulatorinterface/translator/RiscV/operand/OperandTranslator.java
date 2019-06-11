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

package emulatorinterface.translator.RiscV.operand;

import emulatorinterface.translator.InvalidInstructionException;
import emulatorinterface.translator.RiscV.registers.Registers;
import emulatorinterface.translator.RiscV.registers.TempRegisterNum;
import generic.Instruction;
import generic.InstructionList;
import generic.Operand;
import generic.OperandType;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import main.CustomObjectPool;
import misc.Numbers;

public class OperandTranslator 
{
	private static Matcher memLocationMatcher, memAddressRefMatcher;
	
	private static void createMatchers() {
		Pattern p;
		
		p = Pattern.compile("[-]*\\d{0,}\\(\\w{1,2}\\)");
		memLocationMatcher = p.matcher("");
		
		p = Pattern.compile("[a-pA-P]+[ ]*[+-][ ]*[0-9a-fA-FxX]+");
		memAddressRefMatcher = p.matcher("");	
		
	}
	
	public static Operand simplifyOperand(String operandStr,
			InstructionList instructionList, TempRegisterNum tempRegisterNum)
					throws InvalidInstructionException
	{
		if(memLocationMatcher==null) {
			createMatchers();
		}
		
		//If there is no operand, then just don't process it. 
		if(operandStr == null) {
			return null;
		}
		
		
		// Remove spaces from both ends. Helps in making patterns for coming code.
		operandStr = operandStr.trim();
		
		//If operand is a valid number, then it is an immediate
		if((Numbers.isValidNumber(operandStr)&&(operandStr.charAt(0)!='a')&&(!(operandStr.equals("fa5")||operandStr.equals("fa4"))))||(operandStr.charAt(0)=='-'&&Numbers.isValidNumber(operandStr.substring(1,operandStr.length())))) 
		{
			//FIXME : We do not care about the actual value of the immediate operand 
			return Operand.getImmediateOperand();
		}
		else if(Registers.isIntegerRegister(operandStr))
		{
			return Operand.getIntegerRegister(Registers.encodeRegister(operandStr));
		}

		else if(Registers.isFloatRegister(operandStr))
		{
			return Operand.getFloatRegister(Registers.encodeRegister(operandStr));
		}
		//Simplify memory locations specified by [...]
		else if(memLocationMatcher.reset(operandStr).matches())
		{
//			System.out.println(operandStr+":");

			//contains a memory location specified by the memory address
			//Strip the string enclosed in square brackets
			String memLocation = operandStr.replaceAll("[()]"," ");
			
			return simplifyMemoryLocation(memLocation, instructionList, tempRegisterNum);
		}
		
		else if(memAddressRefMatcher.reset(operandStr).matches())
		{
//			System.out.println(operandStr);
			//this handles the memory locations which are relative to pc
			String memLocation=operandStr; 
			return simplifyMemoryLocation(memLocation, instructionList, tempRegisterNum);
		}
		
		else
		{
//			System.out.println(operandStr);
			misc.Error.invalidOperand(operandStr);
			return null;
		}
	}
	

	static Operand simplifyMemoryLocation(String operandStr,
			InstructionList instructionArrayList, TempRegisterNum tempRegisterNum) 
					throws InvalidInstructionException
	{
	    String memoryAddressTokens[] = operandStr.split("\\+|-| ");
		if(!areValidMemoryAddressTokens(memoryAddressTokens))
		{
			misc.Error.showErrorAndExit("\n\tIllegal arguments to a memory address : " 
					+ operandStr + " !!");
		}
		
		
		Operand base, offset;
		base=offset=null;
				
		
		//Determine all the parameters of the string 
		for(int i=0; i<memoryAddressTokens.length; i++)
		{
			//base register
			memoryAddressTokens[i]=memoryAddressTokens[i].trim();
			if(Registers.isIntegerRegister(memoryAddressTokens[i]))
			{
				if(base==null) 
					base = Operand.getIntegerRegister(Registers.encodeRegister(memoryAddressTokens[i]));
			}
			
			//offset
			else if(Numbers.isValidNumber(memoryAddressTokens[i]))
			{
				//if offset is zero, then this won't be considered as an offset in actual address
				if(Numbers.hexToLong(memoryAddressTokens[i])==0) {
					continue;
				}
				
				offset = Operand.getImmediateOperand();
			}
			
			else if(memoryAddressTokens[i].equals("")) {
				
			}
			else
			{
				misc.Error.invalidOperand(operandStr);
				return null;
			}
		}
		//TODO : Once xml file is ready, we have to read this boolean from the configuration parameters
		//Default value is true.
		boolean pureRisc;
		pureRisc=false;
		
		//determine the type of addressing used
		Operand memoryLocationFirstOperand = null;
		Operand memoryLocationSecondOperand = null;
		
		if(base==null && offset==null)
		{}
		
		else if(base==null && offset!=null)
		{
			memoryLocationFirstOperand = offset;
		}
		
		else if(base!=null && offset==null)
		{
			memoryLocationFirstOperand = base;
		}
		
		else if(base!=null  && offset!=null)
		{
			memoryLocationFirstOperand = base;
			memoryLocationSecondOperand = offset;
		}
		else
		{}
		
		//pure risc -> pass a single operand
//		if(pureRisc && memoryLocationSecondOperand!=null)
//		{
//			Operand tempRegister = Registers.getTempIntReg(tempRegisterNum);
//			instructionArrayList.appendInstruction(Instruction.getIntALUInstruction(memoryLocationFirstOperand, memoryLocationFirstOperand, tempRegister));
//			memoryLocationFirstOperand = tempRegister;
//			memoryLocationSecondOperand = null;
//		}
		
		return Operand.getMemoryOperand(memoryLocationFirstOperand, memoryLocationSecondOperand);
	}	

 // to be changed
	private static boolean areValidMemoryAddressTokens(String memoryAddressTokens[])
	{
		return true;
	}
	
	public static Operand getLocationToStoreValue(Operand operand, TempRegisterNum tempRegisterNum)
	{
		if(!operand.isMemoryOperand())
		{
			misc.Error.showErrorAndExit("\n\tTrying to obtain value from a " +	"non-memory operand !!");
		}
		
		Operand tempMemoryRegister;
		
		tempMemoryRegister = Registers.getTemporaryMemoryRegister(operand);
		
		//If we don't have the luxury of an additional temporary register,
		//then we must allocate a new one
		if(tempMemoryRegister == null)
		{
			//If we don't have any disposable register available, then use a new register
			tempMemoryRegister = Registers.getTempIntReg(tempRegisterNum);
		}
		
		return tempMemoryRegister;
	}
}
