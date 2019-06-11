package emulatorinterface.communication.filePacket;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import main.Main;
import misc.Numbers;
import emulatorinterface.translator.x86.objparser.*;
import emulatorinterface.translator.x86.operand.OperandTranslator;
import emulatorinterface.translator.x86.registers.TempRegisterNum;
import generic.InstructionList;
import generic.Operand;
import emulatorinterface.translator.InvalidInstructionException;


public class FileWrite {

	/**
	 * @param args
	 * @throws IOException 
	 */
	/*public static void main(String[] args) throws IOException {*/
	// TODO Auto-generated method stub
	String basefilename;
	int tid;
	BufferedReader input;
	Hashtable<Long, String> hashtable;
	String line;
	long instructionPointer;
	String operation = null;
	String operand1 = null, operand2 = null, operand3 = null;
	
	StringBuilder sb = new StringBuilder();
	File f = null; 
	GZIPOutputStream out = null;
	int count=0;

	public FileWrite(int tid, String baseFileName){
		tid = tid;
		basefilename=new String(baseFileName);
	 	f = new File(basefilename+"_"+tid+".gz");

		try {
			out = new GZIPOutputStream(new FileOutputStream(f));
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String exec=Main.getEmulatorFile();
		input = ObjParser.readObjDumpOutput(exec);
		hashtable=new Hashtable<Long, String>();
		while ((line = ObjParser.readNextLineOfObjDump(input)) != null) 
		{
			if (!(ObjParser.isContainingObjDumpAssemblyCode(line))) {
				continue;
			}

			String assemblyCodeTokens[] = ObjParser.tokenizeObjDumpAssemblyCode(line);

			// read the assembly code tokens
			instructionPointer = Numbers.hexToLong(assemblyCodeTokens[0]);
			
			//instructionPrefix = assemblyCodeTokens[1];
			operation = assemblyCodeTokens[2];
			operand1 = assemblyCodeTokens[3];
			operand2 = assemblyCodeTokens[4];
			operand3 = assemblyCodeTokens[5];
			StringBuilder asm=new StringBuilder();
			
			if(operand1!=null&& operand1.contains("<")) {
				String[] temp=operand1.split(" ");
				operand1=temp[0];
			}
			
			if(operand1==null)
				asm.append(operation);
			else if(operand2==null)
			{  asm.append(operation+" "+operand1);
				
			}else if(operand3==null)
				asm.append(operation+" "+operand1+","+operand2);
			else {
				asm.append(operation+" "+operand1+","+operand2+","+operand3);
			}
			
			hashtable.put(instructionPointer, asm.toString());
			
		}		
	}
	
	public void closeFile(){
		try{
			if(count>0){
				byte[] data = sb.toString().getBytes();
				out.write(data);
				sb.setLength(0);
			}
			out.close();
			// System.out.println("File has been closed");
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	void printToFileIpValAddr(Long ip, Long val, String addr_asm) throws IOException{
		if(count<1000){
			sb.append(ip+" "+ val+" "+ addr_asm+"\n");
			count++;
		}
		else{
			count=0;
			byte[] data = sb.toString().getBytes();
			out.write(data);
			sb.setLength(0);
		}
	}

	public void analysisFn (Long ip, Long val, Long addr) throws IOException, InvalidInstructionException
	{   	
		if(val==8) {
			//timer instructions
		}	
		else {
			if(val!=28) {
				printToFileIpValAddr(ip, val, addr.toString());
			}
			else {
				String asmb;
				if(( asmb = hashtable.get(ip)) != null){
					printToFileIpValAddr(ip, 27L, asmb);
				}
			}
		}
	}
}
