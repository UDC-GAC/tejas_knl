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

package emulatorinterface.translator.arm.operand;

import emulatorinterface.translator.InvalidInstructionException;
import emulatorinterface.translator.arm.registers.Registers;
import emulatorinterface.translator.arm.registers.TempRegisterNum;
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
	private static Matcher memLocationMatcher, indexScaleMatcher, shiftoperandMatcher;
	
	private static void createMatchers() {
		Pattern p;
		
		p = Pattern.compile(".*\\[.*\\].*");
		memLocationMatcher = p.matcher("");
				
		p = Pattern.compile("[asr|lsl|lsr|ror|rrx]+ [0-9a-zA-Z]+");
		indexScaleMatcher = p.matcher("");
		
		p = Pattern.compile("[0-9a-zA-Z]+, [asr|lsl|lsr|ror|rrx| ]*[0-9a-zA-Z]+");
		shiftoperandMatcher=p.matcher("");
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
		operandStr = operandStr.replaceAll("#|-","");
		// Replace all the occurrences of registers with the 64-bit register versions
		//operandStr = Registers.coarsifyRegisters(operandStr);
	
		
		if(memLocationMatcher.reset(operandStr).matches())
		{
			//contains a memory location specified by the memory address
			//Strip the string enclosed in square brackets
			String memLocation = operandStr = operandStr.substring(operandStr.indexOf("[") + 1, operandStr.indexOf("]"));
			
			//Mark the operand as an operand whose value is stored in the memory
			return simplifyMemoryLocation(memLocation, instructionList, tempRegisterNum);
		}
		else if(shiftoperandMatcher.reset(operandStr).matches())
		{
			
			return simplifyShiftOperand(operandStr, instructionList, tempRegisterNum);
			
		}
		else 
		{
			if(Numbers.isValidNumber(operandStr)) 
			{
				//FIXME : We do not care about the actual value of the immediate operand 
				return Operand.getImmediateOperand();
			}
			else 
			{
				operandStr = Registers.coarsifyRegisters(operandStr);
				if(Registers.isIntegerRegister(operandStr))
				{
					return Operand.getIntegerRegister(Registers.encodeRegister(operandStr));
				}

				else if(Registers.isFloatRegister(operandStr))
				{
					return Operand.getFloatRegister(Registers.encodeRegister(operandStr));
				}
				else
				{
					misc.Error.invalidOperand(operandStr);
					return null;
				}
			}
	
			
		}
	}
	
	static Operand simplifyShiftOperand(String operandStr,
			InstructionList instructionArrayList, TempRegisterNum tempRegisterNum) 
					throws InvalidInstructionException
	{
		String shiftRegTokens[] = operandStr.split("\\, [asr|lsl|lsr|ror|rrx]");
		Operand FirstOperand=null, SecondOperand=null;
		for(int i=0;i<shiftRegTokens.length;i++)
		{
			if(Registers.isIntegerRegister(shiftRegTokens[i])) {
				if(FirstOperand==null){					
					FirstOperand= Operand.getIntegerRegister(Registers.encodeRegister(shiftRegTokens[i]));
				}else{
					SecondOperand=Operand.getIntegerRegister(Registers.encodeRegister(shiftRegTokens[i]));
				}
			} 
			else if(Registers.isFloatRegister(shiftRegTokens[i])){
				if(FirstOperand==null){					
					FirstOperand= Operand.getIntegerRegister(Registers.encodeRegister(shiftRegTokens[i]));
				}else{
					SecondOperand=Operand.getIntegerRegister(Registers.encodeRegister(shiftRegTokens[i]));
				}
			}
		
			else if(Numbers.isValidNumber(shiftRegTokens[i]) )
			{
		//if scale is zero, then this won't be considered as scaling in actual address
				if(Numbers.hexToLong(shiftRegTokens[i])!=0) {
					SecondOperand = Operand.getImmediateOperand();
				}
			}
		}
		instructionArrayList.appendInstruction(Instruction.getIntALUInstruction(FirstOperand, SecondOperand, FirstOperand));
		return FirstOperand;
	}

	static Operand simplifyMemoryLocation(String operandStr,
			InstructionList instructionArrayList, TempRegisterNum tempRegisterNum) 
					throws InvalidInstructionException
	{
		String memoryAddressTokens[] = operandStr.split("\\,");
		
		if(!areValidMemoryAddressTokens(memoryAddressTokens))
		{
			misc.Error.showErrorAndExit("\n\tIllegal arguments to a memory address : " 
					+ operandStr + " !!");
		}
		
		
		Operand base, offset, index,scale;
		String indexStr = null, scaleStr = null;
		base=offset=index=scale=null;
				
		 
		//Determine all the parameters of the string 
		for(int i=0; i<memoryAddressTokens.length; i++)
		{
			memoryAddressTokens[i]=memoryAddressTokens[i].trim();
			
			///shift operand
			if(indexScaleMatcher.reset(memoryAddressTokens[i]).matches())
			{
				//indexStr = memoryAddressTokens[i].split("asr|lsl|lsr|ror|rrx")[0].trim();
				scaleStr = memoryAddressTokens[i].split(" ")[1].trim();
				if(Numbers.isValidNumber(scaleStr) )
				{
				//if scale is zero, then this won't be considered as scaling in actual address
					if(Numbers.hexToLong(scaleStr)==0) {
					scale=null;
					}
					else{
					scale = Operand.getImmediateOperand();
					}
				}
				else{
					scaleStr = Registers.coarsifyRegisters(scaleStr);							
					if(Registers.isIntegerRegister(indexStr)) {
						scale = Operand.getIntegerRegister(Registers.encodeRegister(indexStr));
					} else if(Registers.isFloatRegister(memoryAddressTokens[i])){
						scale = Operand.getFloatRegister(Registers.encodeRegister(indexStr));
					}
				} 
			}
			else
			{
				if(Numbers.isValidNumber(memoryAddressTokens[i]))
				{
					//if offset is zero, then this won't be considered as an offset in actual address
					if(Numbers.hexToLong(memoryAddressTokens[i])==0) {
						continue;
					}
				
					offset = Operand.getImmediateOperand();
				}
				else
				{
				
					memoryAddressTokens[i]=Registers.coarsifyRegisters(memoryAddressTokens[i]);
					if(Registers.isIntegerRegister(memoryAddressTokens[i]))
					{
						if(base==null) {
							base = Operand.getIntegerRegister(Registers.encodeRegister(memoryAddressTokens[i]));
						} else {
							index = Operand.getIntegerRegister(Registers.encodeRegister(memoryAddressTokens[i]));
						}
					}
					else if(Registers.isFloatRegister(memoryAddressTokens[i]))
					{
						if(base==null) {
							base = Operand.getFloatRegister(Registers.encodeRegister(memoryAddressTokens[i]));
						} else {
							index = Operand.getFloatRegister(Registers.encodeRegister(memoryAddressTokens[i]));
						}
					}
					else
					{
						misc.Error.invalidOperand(operandStr);
						return null;
					}
				}
			}
		}

		//Create scaled index
		Operand scaledIndex = null;
		if(scale!=null) {
			scaledIndex = Registers.getTempIntReg(tempRegisterNum);
			instructionArrayList.appendInstruction(Instruction.getIntALUInstruction(index, scale, scaledIndex));
		} else {
			scaledIndex = index;
		}
						
		//determine the type of addressing used
		Operand memoryLocationFirstOperand = null;
		Operand memoryLocationSecondOperand = null;

		if(base!=null && scaledIndex==null && offset==null)
		{
			memoryLocationFirstOperand = base;
			memoryLocationSecondOperand = null;
		}
		else if(base!=null && scaledIndex==null && offset!=null)
		{
			memoryLocationFirstOperand = base;
			memoryLocationSecondOperand = offset;
		}
		
		else if(base!=null && scaledIndex!=null && offset==null)
		{
			memoryLocationFirstOperand = base;
			memoryLocationSecondOperand = scaledIndex;
		}	
		else
		{}
		/*
		 * If pre indexed -->!, flag =1
		 * if(flag==1)
		 * instructionArrayList.appendInstruction(Instruction.getIntALUInstruction(memoryLocationFirstOperand, memoryLocationSecondOperand, memoryLocationFirstOperand));
		 */
		return Operand.getMemoryOperand(memoryLocationFirstOperand, memoryLocationSecondOperand);
	}	


	private static boolean areValidMemoryAddressTokens(String memoryAddressTokens[])
	{
		return ((memoryAddressTokens.length>=1 && memoryAddressTokens.length<=4));
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
	
	
	public static Operand[ ] SimplifyBlockOperand(String operandStr,
			InstructionList instructionArrayList, TempRegisterNum tempRegisterNum) 
					throws InvalidInstructionException
	{

		
		operandStr = operandStr.substring(operandStr.indexOf("{") + 1, operandStr.indexOf("}"));
		String RegTokens[] = operandStr.split("\\,");
		Operand RegList[]=new Operand[RegTokens.length];
		
		
		for(int i=0;i<RegTokens.length;i++)
		{
			RegTokens[i] = RegTokens[i].trim();
			RegTokens[i] = Registers.coarsifyRegisters(RegTokens[i]);
			if(Registers.isIntegerRegister(RegTokens[i]))
			{
				//RegList[i]= new Operand();
				RegList[i] = Operand.getIntegerRegister(Registers.encodeRegister(RegTokens[i]));
			}
			else if(Registers.isFloatRegister(RegTokens[i]))
			{
				RegList[i] = Operand.getFloatRegister(Registers.encodeRegister(RegTokens[i]));
			}
			
		}
		return RegList;
	}
	 
	
}

