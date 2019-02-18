/*
 * This represents a reader thread in the simulator which keeps on reading from
 * EMUTHREADS.
 */

package emulatorinterface;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Vector;

import main.ArchitecturalComponent;
import main.Main;
import memorysystem.Cache;
import memorysystem.MemorySystem;
import pipeline.PipelineInterface;
import config.MainMemoryConfig;
import config.SystemConfig;
import emulatorinterface.ThreadBlockState.blockState;
import emulatorinterface.communication.Encoding;
import emulatorinterface.communication.IpcBase;
import emulatorinterface.communication.Packet;
import emulatorinterface.communication.shm.SharedMem;
import emulatorinterface.translator.x86.objparser.ObjParser;
import generic.BarrierTable;
import generic.CircularPacketQueue;
import generic.Core;
import generic.GenericCircularQueue;
import generic.GlobalClock;
import generic.Instruction;
import generic.Statistics;

/*
 * MaxNumThreads threads are created from this class. Each thread
 * continuously keeps reading from the shared memory segment according
 * to its index(taken care in the jni C file).
 */
public class RunnableThread implements Encoding, Runnable {
    
    public static final int             INSTRUCTION_THRESHOLD = 2000;
    
    boolean                             oNotProcess           = false;
    
    int                                 javaTid;
    long                                sum                   = 0; // checksum
    int                                 EMUTHREADS;
    int                                 currentEMUTHREADS     = 0;  // total
                                                                    // number of
                                                                    // livethreads
    int                                 maxCoreAssign         = 0;      // the
                                                                        // maximum
                                                                        // core
                                                                        // id
                                                                        // assigned
    
    static EmulatorThreadState[]        emulatorThreadState;// = new
                                                            // EmulatorThreadState[EMUTHREADS];
    static ThreadBlockState[]           threadBlockState;// =new
                                                         // ThreadBlockState[EMUTHREADS];
    GenericCircularQueue<Instruction>[] inputToPipeline;
    // static long ignoredInstructions = 0;
    
    // QQQ re-arrange packets for use by translate instruction.
    // DynamicInstructionBuffer[] dynamicInstructionBuffer;
    
    static long[]                       noOfMicroOps;
    // long[] numInstructions;
    // FIXME PipelineInterface should be in IpcBase and not here as pipelines
    // from other RunnableThreads
    // will need to interact.
    PipelineInterface[]                 pipelineInterfaces;
    long                                prevTotalInstructions,
            currentTotalInstructions;
    long[]                              prevCycles;
    
    public IpcBase                      ipcBase;
    
    private static int                  liveJavaThreads;
    
    static boolean                      printIPTrace          = false;
    static long                         numShmWrites[];
    
    // aded by harveenk kushagra
    // to synch RAM and core clock
    long                                counter1              = 0;
    long                                counter2              = 0;
    
    // changed by kush, only declare here, initialize later
    long                                RAMclock;
    long                                CoreClock;
    
    /*
     * This keeps on reading from the appropriate index in the shared memory
     * till it gets a -1 after which it stops. NOTE this depends on each thread
     * calling threadFini() which might not be the case. This function will
     * break if the threads which started do not call threadfini in the PIN (in
     * case of unclean termination). Although the problem is easily fixable.
     */
    public void run() {
        
        // create pool for emulator packets
        ArrayList<CircularPacketQueue> fromEmulatorAll = new ArrayList<CircularPacketQueue>(
                EMUTHREADS);
        for (int i = 0; i < EMUTHREADS; i++) {
            CircularPacketQueue fromEmulator = new CircularPacketQueue(
                    SharedMem.COUNT);
            fromEmulatorAll.add(fromEmulator);
        }
        
        if (printIPTrace == true) {
            numShmWrites = new long[SystemConfig.maxNumJavaThreads
                    * SystemConfig.numEmuThreadsPerJavaThread];
        }
        
        Packet pnew = new Packet();
        
        boolean allover = false;
        // boolean emulatorStarted = false;
        
        // start gets reinitialized when the program actually starts
        // Main.setStartTime(System.currentTimeMillis());
        EmulatorThreadState threadParam;
        // keep on looping till there is something to read. iterates on the
        // emulator threads from which it has to read.
        // tid is java thread id
        // tidEmu is the local notion of pin threads for the current java thread
        // tidApp is the actual tid of a pin thread
        
        while (true) {
            
            for (int tidEmulator = 0; tidEmulator < EMUTHREADS; tidEmulator++) {
                
                CircularPacketQueue fromEmulator = fromEmulatorAll
                        .get(tidEmulator);
                
                threadParam = emulatorThreadState[tidEmulator];
                
                // Thread is halted on a barrier or a sleep
                if (threadParam.halted /* || thread.finished */) {
                    continue;        // one bug need to be fixed to remove this
                                     // comment
                }
                
                int tidApplication = javaTid * EMUTHREADS + tidEmulator;
                int numReads = 0;
                long v = 0;
                
                // add outstanding micro-operations to input to pipeline
                if (threadParam.outstandingMicroOps.isEmpty() == false) {
                    if (threadParam.outstandingMicroOps
                            .size() < inputToPipeline[tidEmulator]
                                    .spaceLeft()) {
                        while (threadParam.outstandingMicroOps
                                .isEmpty() == false) {
                            inputToPipeline[tidEmulator]
                                    .enqueue(threadParam.outstandingMicroOps
                                            .pollFirst());
                        }
                    } else {
                        // there is no space in pipelineBuffer. So don't fetch
                        // any more instructions
                        continue;
                    }
                }
                
                // get the number of packets to read. 'continue' and read from
                // some other thread if there is nothing.
                numReads = ipcBase.fetchManyPackets(tidApplication,
                        fromEmulator);
                // System.out.println("numReads = " + numReads);
                if (fromEmulator.size() == 0) {
                    continue;
                }
                // update the number of read packets
                threadParam.totalRead += numReads;
                
                // If java thread itself is terminated then break out from this
                // for loop. also update the variable all-over so that I can
                // break from the outer while loop also.
                if (ipcBase.javaThreadTermination[javaTid] == true) {
                    allover = true;
                    break;
                }
                
                // need to do this only the first time
                if (ipcBase.javaThreadStarted[javaTid] == false) {
                    // emulatorStarted = true;
                    // Main.setStartTime(System.currentTimeMillis());
                    ipcBase.javaThreadStarted[javaTid] = true;
                    
                }
                
                threadParam.checkStarted();
                
                // Process all the packets read from the communication channel
                while (fromEmulator.isEmpty() == false) {
                    pnew = fromEmulator.dequeue();
                    v = pnew.value;
                    
                    if (printIPTrace == true) {
                        System.out.println("pinTrace[" + tidApplication + "] "
                                + (++numShmWrites[tidApplication]) + " : "
                                + pnew.ip);
                    }
                    
                    // if we read -1, this means this emulator thread finished.
                    if (v == Encoding.THREADCOMPLETE) {
                        System.out.println(
                                "runnableshm : last packetList received for application-thread "
                                        + tidApplication + " numCISC="
                                        + pnew.ip);
                        // Statistics.setNumPINCISCInsn(pnew.ip, 0,
                        // tidEmulator);
                        threadParam.isFirstPacket = true;  // preparing the
                                                           // thread for next
                                                           // packetList in same
                                                           // pipeline
                        signalFinish(tidApplication);
                    }
                    
                    if (v == Encoding.SUBSETSIMCOMPLETE) {
                        System.out.println("within SUBSETSIMCOMPLETE ");
                        ipcBase.javaThreadTermination[javaTid] = true;
                        liveJavaThreads--;
                        allover = true;
                        break;
                    }
                    
                    boolean ret = processPacket(threadParam, pnew, tidEmulator);
                    if (ret == false) {
                        // There is not enough space in pipeline buffer.
                        // So don't process any more packets.
                        break;
                    }
                }
                
                if (printIPTrace == true) {
                    System.out.flush();
                }
                
                // perform error check.
                ipcBase.errorCheck(tidApplication, threadParam.totalRead);
                
                if (ipcBase.javaThreadTermination[javaTid] == true) {  // check
                                                                       // if
                                                                       // java
                                                                       // thread
                                                                       // is
                                                                       // finished
                    allover = true;
                    break;
                }
                // System.out.println("here");
                if (liveJavaThreads == 1) {// System.out.println("size
                                           // :"+inputToPipeline[tidEmulator].size());
                    if (inputToPipeline[tidEmulator].size() <= 0) {
                        // System.out.println("******************************continued*******************");
                        continue;
                    }
                } else if (liveJavaThreads > 1) {// System.out.println("live
                                                 // threads :"+liveJavaThreads);
                    if (inputToPipeline[tidEmulator].size() <= 0
                            || liveJavaThreads > 1 && statusOfOtherThreads()) {
                        // System.out.println("******************************continued2*******************");
                        continue;
                    }
                }
                
            }
            
            runPipelines();
            // System.out.println("after execution n= "+n+" Thread finished ?
            // "+threadParams[1].finished);
            
            // this runnable thread can be stopped in two ways. Either the
            // emulator threads from which it was supposed to read never
            // started(none of them) so it has to be signalled by the main
            // thread. When this happens 'all over' becomes 'true' and it
            // breaks out from the loop. The second situation is that all the
            // emulator threads which started have now finished, so probably
            // this thread should now terminate.
            // The second condition handles this situation.
            // NOTE this ugly state management cannot be avoided unless we use
            // some kind of a signalling mechanism between the emulator and
            // simulator(TODO).
            // Although this should handle most of the cases.
            if (allover || (ipcBase.javaThreadStarted[javaTid] == true
                    && emuThreadsFinished())) {
                ipcBase.javaThreadTermination[javaTid] = true;
                break;
            }
        }
        
        finishAllPipelines();
        
        if (unHandledCount != null) {
            sorted_map.putAll(unHandledCount);
            System.out.println(sorted_map);
        }
    }
    
    private boolean statusOfOtherThreads() {
        // returns true if any other live threads have empty inputtopipeline
        for (int i = 0; i < EMUTHREADS; i++) {
            if (emulatorThreadState[i].started
                    && threadBlockState[i].getState() == blockState.LIVE) {
                // System.out.println("in loop, size
                // "+i+":"+inputToPipeline[i].size());
                if (inputToPipeline[i].size() <= 0) {
                    return true;
                }
            }
            
        }
        return false;
    }
    
    // initialise a reader thread with the correct thread id and the buffer to
    // write the results in.
    public RunnableThread(String threadName, int javaTid, IpcBase ipcBase,
            Core[] cores) {
        
        this.ipcBase = ipcBase;
        
        this.EMUTHREADS = SystemConfig.numEmuThreadsPerJavaThread;
        emulatorThreadState = new EmulatorThreadState[EMUTHREADS];
        threadBlockState = new ThreadBlockState[EMUTHREADS];
        
        // dynamicInstructionBuffer = new DynamicInstructionBuffer[EMUTHREADS];
        inputToPipeline = (GenericCircularQueue<Instruction>[]) Array
                .newInstance(GenericCircularQueue.class, EMUTHREADS);
        
        noOfMicroOps = new long[EMUTHREADS];
        // numInstructions = new long[EMUTHREADS];
        pipelineInterfaces = new PipelineInterface[EMUTHREADS];
        for (int i = 0; i < EMUTHREADS; i++) {
            int id = javaTid * EMUTHREADS + i;
            int id_core = SystemConfig.coreAffinity[i];
            
            IpcBase.glTable.getStateTable().put(id, new ThreadState(id));
            emulatorThreadState[i] = new EmulatorThreadState();
            threadBlockState[i] = new ThreadBlockState();
            
            // TODO pipelineinterfaces & inputToPipeline should also be in the
            // IpcBase
            
            // CORE AFFINITY
            Core core = null;
            int c = 0;
            for (c = 0; c < cores.length; ++c) {
                if (cores[c].getCore_number() == SystemConfig.coreAffinity[i]) {
                    core = cores[c];
                    break;
                }
            }
            System.out.println(
                    "[DEBUG] " + threadName + " id = " + id + " core = "
                            + cores[c].getCore_number() + " (core affinity = "
                            + SystemConfig.coreAffinity[i] + ")");
            
            pipelineInterfaces[id_core] = core.getPipelineInterface();
            inputToPipeline[id_core] = new GenericCircularQueue<Instruction>(
                    Instruction.class, INSTRUCTION_THRESHOLD);
            
            // dynamicInstructionBuffer[i] = new DynamicInstructionBuffer();
            
            GenericCircularQueue<Instruction>[] toBeSet = (GenericCircularQueue<Instruction>[]) Array
                    .newInstance(GenericCircularQueue.class, 1);
            toBeSet[0] = inputToPipeline[id_core];
            pipelineInterfaces[id_core].setInputToPipeline(toBeSet);
        }
        
        this.javaTid = javaTid;
        System.out.println("--  starting java thread" + this.javaTid);
        prevTotalInstructions = -1;
        currentTotalInstructions = 0;
        // threadCoreMaping = new Hashtable<Integer, Integer>();
        prevCycles = new long[EMUTHREADS];
        
        // added by harveenk kushagra
        CoreClock = cores[0].getFrequency() * 1000000;
        
        // added later by kush
        if (SystemConfig.memControllerToUse == true)
            RAMclock = (long) (1 / (SystemConfig.mainMemoryConfig.tCK)
                    * 1000000000);
    }
    
    protected void runPipelines() {
        int minN = Integer.MAX_VALUE;
        boolean RAMcyclerun = false;
        
        for (int tidEmu = 0; tidEmu < maxCoreAssign; tidEmu++) {
            EmulatorThreadState th = emulatorThreadState[tidEmu];
            if (th.halted && !(this.inputToPipeline[tidEmu]
                    .size() > INSTRUCTION_THRESHOLD)) {
                th.halted = false;
            }
            int n = inputToPipeline[tidEmu].size()
                    / pipelineInterfaces[tidEmu].getCore().getDecodeWidth()
                    * pipelineInterfaces[tidEmu].getCoreStepSize();
            if (n < minN && n != 0)
                minN = n;
        }
        minN = (minN == Integer.MAX_VALUE) ? 0 : minN;
        
        for (int i1 = 0; i1 < minN; i1++) {
            
            /*
             * Note: DRAM simulation
             * Order of execution must be maintained for cycle accurate
             * simulation.
             * Order is:
             * MainMemoryController.oneCycleOperation()
             * processEvents() [called from within oneCycleOperation of
             * pipelines]
             * MainMemoryController.enqueueToCommandQ();
             */
            
            // added later by kush
            // run one cycle operation only when DRAM simulation enabled
            if (SystemConfig.memControllerToUse == true) {
                counter1 += RAMclock;
                
                // added by harveenk
                if (counter2 < counter1) {
                    
                    counter2 += CoreClock;
                    for (int k = 0; k < SystemConfig.mainMemoryConfig.numChans; k++) {
                        ArchitecturalComponent
                                .getMainMemoryDRAMController(null, k)
                                .oneCycleOperation();
                    }
                    // important - one cycle operation for dram must occur
                    // before events are processed
                    
                    RAMcyclerun = true;
                    
                }
                
            }
            for (int tidEmu = 0; tidEmu < maxCoreAssign; tidEmu++) {
                pipelineInterfaces[tidEmu].oneCycleOperation();
            }
            
            // added later by kush
            if (SystemConfig.memControllerToUse == true) {
                if (counter1 == counter2) {
                    counter1 = 0;
                    counter2 = 0;
                }
                
                if (RAMcyclerun == true) {
                    
                    // add the packets pending at this cycle to the queue
                    
                    for (int k = 0; k < SystemConfig.mainMemoryConfig.numChans; k++) {
                        ArchitecturalComponent
                                .getMainMemoryDRAMController(null, k)
                                .enqueueToCommandQ();
                        // print debug if RAM was run this cycle
                        
                        // if(SystemConfig.mainMemoryConfig.DEBUG_BANKSTATE)
                        // ArchitecturalComponent.getMainMemoryDRAMController(null,k).printBankStateTest();
                        // if(SystemConfig.mainMemoryConfig.DEBUG_CMDQ)
                        // ArchitecturalComponent.getMainMemoryDRAMController(null,k).commandQueue.printTest();
                        
                    }
                    
                    RAMcyclerun = false;
                }
                
            }
            GlobalClock.incrementClock();
            
            if (GlobalClock.getCurrentTime() % 1000 == 0) {
                // Every 1000 cycles, iterate over all the caches, and note the
                // MSHR sizes
                for (Cache c : ArchitecturalComponent.getCacheList()) {
                    c.noteMSHRStats();
                }
            }
        }
        
        if (prevTotalInstructions == -1) {
            prevTotalInstructions = 0;
        }
    }
    
    public void finishAllPipelines() {
        
        // added by harveenk
        boolean RAMcyclerun = false;
        
        // finishAllPipelines is called when all communication channels (shared
        // memory segments or files) have been read completely
        // compiles statistics and winds up simulation
        // NOTE: there are some UNSIMULATED instructions in the inputToPipeline
        // structures and in the pipelines themselves.
        // these are small in number (around 1200 per core at max) and should
        // not affect the final statistics
        
        for (int i = 0; i < maxCoreAssign; i++) {
            pipelineInterfaces[i].setExecutionComplete(true);
            pipelineInterfaces[i].setPerCoreMemorySystemStatistics();
            pipelineInterfaces[i].setTimingStatistics();
        }
        
        long dataRead = 0;
        for (int i = 0; i < EMUTHREADS; i++) {
            dataRead += emulatorThreadState[i].totalRead;
        }
        
        Statistics.setDataRead(dataRead, javaTid);
        Statistics.setNoOfMicroOps(noOfMicroOps, javaTid);
        
        IpcBase.free.release();
    }
    
    // returns true if all the emulator threads from which I was reading have
    // finished
    protected boolean emuThreadsFinished() {
        boolean ret = true;
        for (int i = 0; i < maxCoreAssign; i++) {
            EmulatorThreadState thread = emulatorThreadState[i];
            if (thread.started == true && thread.finished == false) {
                return false;
            }
        }
        return ret;
    }
    
    /*
     * process each packetList
     * parameters - Thread information, packetList, thread id
     * Call fuseInstruction on a outstanding micro-ops list instead of pipeline
     * buffer
     * If we are not able to add packets from outstanding micro-ops list to
     * pipeline buffer, then
     * return false (there is no space in pipeline buffer).
     */
    static int numProcessPackets = 0;
    
    protected boolean processPacket(EmulatorThreadState thread, Packet pnew,
            int tidEmu) {
        
        // System.out.println("&processPacket " + (++numProcessPackets) + " : "
        // + pnew.ip);
        
        boolean isSpaceInPipelineBuffer = true;
        
        int tidApp = javaTid * EMUTHREADS + tidEmu;
        
        sum += pnew.value;
        
        if (pnew.value == TIMER) {// leaving timer packetList now
            // resumeSleep(IpcBase.glTable.tryResumeOnWaitingPipelines(tidApp,
            // pnew.ip));
            return isSpaceInPipelineBuffer;
        }
        
        if (pnew.value > SYNCHSTART && pnew.value < SYNCHEND) { // for barrier
                                                                // enter and
                                                                // barrier exit
            ResumeSleep ret = IpcBase.glTable.update(pnew.tgt, tidApp, pnew.ip,
                    pnew.value);
            if (ret != null) {
                resumeSleep(ret);
            }
            checkForBlockingPacket(pnew.value, tidApp);
            if (threadBlockState[tidApp].getState() == blockState.BLOCK) {
                checkForUnBlockingPacket(pnew.value, tidApp);
                
            }
            return isSpaceInPipelineBuffer;
        }
        
        if (pnew.value == BARRIERINIT)  // for barrier initialization
        {
            
            // System.out.println("Packet is " + pnew.toString());
            BarrierTable.barrierListAdd(pnew);
            return isSpaceInPipelineBuffer;
        }
        
        if (thread.isFirstPacket) {
            this.pipelineInterfaces[tidApp].getCore().currentThreads++;  // current
                                                                         // number
                                                                         // of
                                                                         // threads
                                                                         // in
                                                                         // this
                                                                         // pipeline
            System.out.println("num of threads on core " + tidApp + " = "
                    + this.pipelineInterfaces[tidApp].getCore().currentThreads);
            this.pipelineInterfaces[tidApp].getCore().getExecEngine()
                    .setExecutionComplete(false);
            this.pipelineInterfaces[tidApp].getCore().getExecEngine()
                    .setExecutionBegun(true);
            currentEMUTHREADS++;
            if (tidApp >= maxCoreAssign)
                maxCoreAssign = tidApp + 1;
            
            // thread.pold.set(pnew);
            thread.packetList.add(pnew);
            liveJavaThreads++;
            threadBlockState[tidApp].gotLive();
            thread.isFirstPacket = false;
            return isSpaceInPipelineBuffer;
            
        }
        
        if (pnew.value != INSTRUCTION && !(pnew.value > 6 && pnew.value < 26)
                && pnew.value != Encoding.ASSEMBLY) {
            // just append the packet to outstanding packetList for current
            // instruction pointer
            thread.packetList.add(pnew);
        } else {
            // (numInstructions[tidEmu])++;
            // this.dynamicInstructionBuffer[tidEmu].configurePackets(thread.packets);
            
            int oldLength = inputToPipeline[tidEmu].size();
            
            long numHandledInsn = 0;
            int numMicroOpsBefore = thread.outstandingMicroOps.size();
            
            ObjParser.fuseInstruction(tidApp, thread.packetList.get(0).ip,
                    thread.packetList, thread.outstandingMicroOps);
            
            // Increment number of CISC instructions
            Statistics.setNumCISCInsn(
                    Statistics.getNumCISCInsn(javaTid, tidEmu) + 1, javaTid,
                    tidEmu);
            
            int numMicroOpsAfter = thread.outstandingMicroOps.size();
            if (numMicroOpsAfter > numMicroOpsBefore) {
                numHandledInsn = 1;
            } else {
                numHandledInsn = 0;
            }
            
            // For one CISC instruction, we generate x micro-operations.
            // We set the CISC ip of the first micro-op to the original CISC ip.
            // IP of all the remaining micro-ops is set to -1(Invalid).
            // This ensures that we do not artificially increase the hit-rate of
            // instruction cache.
            for (int i = numMicroOpsBefore; i < numMicroOpsAfter; i++) {
                if (i == numMicroOpsBefore) {
                    thread.outstandingMicroOps.peek(i)
                            .setCISCProgramCounter(thread.packetList.get(0).ip);
                } else {
                    thread.outstandingMicroOps.peek(i)
                            .setCISCProgramCounter(-1);
                }
            }
            
            // If I am running multiple benchmarks, the addresses of all the
            // benchmarks must
            // be tagged with benchmark ID. The tagging happens only if :
            // (a) there are multiple benchmarks (b) the benchmark id for this
            // thread is not zero
            if (Main.benchmarkThreadMapping.getNumBenchmarks() > 1 && tidApp > 0
                    && Main.benchmarkThreadMapping
                            .getBenchmarkIDForThread(tidApp) != 0) {
                for (int i = numMicroOpsBefore; i < numMicroOpsAfter; i++) {
                    thread.outstandingMicroOps.peek(i)
                            .changeAddressesForBenchmark(
                                    Main.benchmarkThreadMapping
                                            .getBenchmarkIDForThread(tidApp));
                }
            }
            
            //
            if (numHandledInsn == 0 && printUnHandledInsn) {
                calculateCulpritCISCInsns(thread.packetList.get(0).ip);
            }
            
            // Either add all outstanding micro-ops or none.
            if (thread.outstandingMicroOps.size() < this.inputToPipeline[tidEmu]
                    .spaceLeft()) {
                // add outstanding micro-operations to input to pipeline
                while (thread.outstandingMicroOps.isEmpty() == false) {
                    this.inputToPipeline[tidEmu]
                            .enqueue(thread.outstandingMicroOps.dequeue());
                }
            } else {
                isSpaceInPipelineBuffer = false;
            }
            
            Statistics.setNumHandledCISCInsn(
                    Statistics.getNumHandledCISCInsn(javaTid, tidEmu)
                            + numHandledInsn,
                    javaTid, tidEmu);
            
            int newLength = inputToPipeline[tidEmu].size();
            noOfMicroOps[tidEmu] += newLength - oldLength;
            
            if (!thread.halted && this.inputToPipeline[tidEmu]
                    .size() > INSTRUCTION_THRESHOLD) {
                thread.halted = true;
            }
            
            thread.packetList.clear();
            thread.packetList.add(pnew);
        }
        
        return isSpaceInPipelineBuffer;
    }
    
    private void checkForBlockingPacket(long value, int TidApp) {
        // TODO Auto-generated method stub
        int val = (int) value;
        switch (val) {
            case LOCK:
            case JOIN:
            case CONDWAIT:
            case BARRIERWAIT:
                threadBlockState[TidApp].gotBlockingPacket(val);
                
        }
    }
    
    private void checkForUnBlockingPacket(long value, int TidApp) {
        // TODO Auto-generated method stub
        int val = (int) value;
        switch (val) {
            case LOCK + 1:
            case JOIN + 1:
            case CONDWAIT + 1:
            case BARRIERWAIT + 1:
                threadBlockState[TidApp].gotUnBlockingPacket();
                
        }
    }
    
    boolean                      printUnHandledInsn = false;
    private HashMap<Long, Long>  unHandledCount;
    UnhandledInsnCountComparator bvc;
    TreeMap<Long, Long>          sorted_map;
    
    private void calculateCulpritCISCInsns(long ip) {
        
        if (printUnHandledInsn == false) {
            misc.Error.showErrorAndExit(
                    "printUnHandledInsn function should not be called. Its flag is not set !!");
        }
        
        if (unHandledCount == null) {
            unHandledCount = new HashMap<Long, Long>();
            bvc = new UnhandledInsnCountComparator(unHandledCount);
            sorted_map = new TreeMap<Long, Long>(bvc);
        }
        
        if (unHandledCount.get(ip) == null) {
            unHandledCount.put(ip, 1l);
        } else {
            unHandledCount.put(ip, unHandledCount.get(ip) + 1);
        }
    }
    
    protected boolean poolExhausted(int tidEmulator) {
        return false; // we have a growable pool now
        // return (CustomObjectPool.getInstructionPool().getNumPoolAllowed() <
        // 2000);
    }
    
    private void resumeSleep(ResumeSleep update) {
        for (int i = 0; i < update.getNumSleepers(); i++) {
            Instruction ins = Instruction.getSyncInstruction();
            ins.setCISCProgramCounter(update.barrierAddress);
            System.out.println(
                    "Enqueued a barrier packet into  " + update.sleep.get(i)
                            + " with add " + update.barrierAddress);
            this.inputToPipeline[update.sleep.get(i)].enqueue(ins);
            setThreadState(update.sleep.get(i), true);
        }
    }
    
    protected void signalFinish(int tidApp) {
        // finished pipline
        // TODO Auto-generated method stub
        // System.out.println("signalfinish thread " + tidApp + " mapping " +
        // threadCoreMaping.get(tidApp));
        this.inputToPipeline[tidApp]
                .enqueue(Instruction.getInvalidInstruction());
        IpcBase.glTable.getStateTable()
                .get((Integer) tidApp).lastTimerseen = Long.MAX_VALUE;// (long)-1>>>1;
        // System.out.println(tidApp+" pin thread got -1");
        
        // FIXME threadParams should be on tidApp. Currently it is on tidEmu
        emulatorThreadState[tidApp].finished = true;
        
    }
    
    public static void setThreadState(int tid, boolean cond) {
        // System.out.println("set thread state halted" + tid + " to " + cond);
        emulatorThreadState[tid].halted = cond;
    }
}
