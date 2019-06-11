package main;

import dram.MainMemoryDRAMController;
import emulatorinterface.communication.IpcBase;
import generic.CommunicationInterface;
import generic.Core;
import generic.CoreBcastBus;
import generic.EventQueue;
import generic.GlobalClock;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import config.CacheConfig;
import config.MainMemoryConfig;
import config.SystemConfig;

import memorysystem.Cache;
import memorysystem.CoreMemorySystem;
import dram.MainMemoryDRAMController;
import memorysystem.MemorySystem;
import memorysystem.MainMemoryController;
import memorysystem.coherence.CHA;
import memorysystem.coherence.Coherence;
import memorysystem.coherence.Directory;
import memorysystem.nuca.NucaCache;
import net.Bus;
import net.BusInterface;
import net.InterConnect;
import net.NOC;
import net.Router;
import pipeline.outoforder.ICacheBuffer;
import pipeline.outoforder.OutOrderExecutionEngine;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

public class ArchitecturalComponent {
    
    public static Vector<Core>                     cores             = new Vector<Core>();
    public static Vector<CoreMemorySystem>         coreMemSysArray   = new Vector<CoreMemorySystem>();
    public static Vector<Coherence>                coherences        = new Vector<Coherence>();
    public static Vector<MainMemoryDRAMController> memoryControllers = new Vector<MainMemoryDRAMController>();
    public static Vector<MainMemoryDRAMController> mcdramControllers = new Vector<MainMemoryDRAMController>();
    public static Vector<Cache>                    sharedCaches      = new Vector<Cache>();
    public static Vector<Cache>                    caches            = new Vector<Cache>();
    public static HashMap<String, NucaCache>       nucaList          = new HashMap<String, NucaCache>();
    private static InterConnect                    interconnect;
    public static CoreBcastBus                     coreBroadcastBus;
    
    public static boolean                          reset             = false;
    
    public static void createChip() {
        // Interconnect
        // Core
        // Coherence
        // Shared Cache
        // Main Memory Controller
        
        if (SystemConfig.interconnect == SystemConfig.Interconnect.Bus) {
            ArchitecturalComponent.setInterConnect(new Bus());
            createElementsOfBus();
        } else if (SystemConfig.interconnect == SystemConfig.Interconnect.Noc) {
            ArchitecturalComponent
                    .setInterConnect(new NOC(SystemConfig.nocConfig));
            createElementsOfNOC();
            ((NOC) interconnect).ConnectNOCElements();
        }
        
        MemorySystem.createLinkBetweenCaches();
        
        // MemorySystem.setCoherenceOfCaches();
        
        initCoreBroadcastBus();
        GlobalClock.systemTimingSetUp(getCores());
        Path path = Paths.get(SystemConfig.mapFilePath);
        try {
            SystemConfig.mappingKNL = Files.readAllBytes(path);
            // for (int i = 0; i < 100; ++i) {
            // System.out.println("mapping[" + i + "] = " +
            // SystemConfig.mappingKNL[i]);
            // }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    private static void createElementsOfBus() {
        
        Bus bus = new Bus();
        BusInterface busInterface;
        
        // Create Cores
        for (int i = 0; i < SystemConfig.NoOfCores; i++) {
            Core core = createCore(i);
            busInterface = new BusInterface(bus);
            core.setComInterface(busInterface);
            cores.add(core);
        }
        
        // Create Shared Cache
        // PS : Directory will be created as a special shared cache
        for (CacheConfig cacheConfig : SystemConfig.sharedCacheConfigs) {
            busInterface = new BusInterface(bus);
            Cache c = MemorySystem.createSharedCache(cacheConfig.cacheName,
                    busInterface);
            // c.setComInterface(busInterface);
        }
        
        // Create Main Memory Controller
        // Note: number of physical channels = number of Memory Controllers
        
        // added later by kush
        // in case we use simple (fixed latency) memory controllers, num
        // channels not taken into consideration currently (default to 1)
        if (SystemConfig.memControllerToUse == true) {
            for (int i = 0; i < SystemConfig.mainMemoryConfig.numChans; i++) {
                MainMemoryDRAMController mainMemController = new MainMemoryDRAMController(
                        SystemConfig.mainMemoryConfig);
                mainMemController.setChannelNumber(i);
                busInterface = new BusInterface(bus);
                mainMemController.setComInterface(busInterface);
                memoryControllers.add(mainMemController);
            }
        } else {
            MainMemoryDRAMController mainMemController = new MainMemoryDRAMController(
                    SystemConfig.mainMemoryConfig);
            mainMemController.setChannelNumber(0);
            busInterface = new BusInterface(bus);
            mainMemController.setComInterface(busInterface);
            memoryControllers.add(mainMemController);
        }
        
    }
    
    private static void createElementsOfNOC() {
        // create elements mentioned as topology file
        BufferedReader readNocConfig = NOC
                .openTopologyFile(SystemConfig.nocConfig.NocTopologyFile);
        
        // Skip the first line. It contains numrows/cols information
        try {
            readNocConfig.readLine();
        } catch (IOException e1) {
            misc.Error
                    .showErrorAndExit("Error in reading noc topology file !!");
        }
        
        int numRows = ((NOC) interconnect).getNumRows();
        int numColumns = ((NOC) interconnect).getNumColumns();
        int t = 0;
        // workaround...
        for(CacheConfig config : SystemConfig.sharedCacheConfigs) {
            if (config.isDirectory) {
                SystemConfig.globalDir = new Directory("GlobalDir", 0, config, null);
                break;
            }
        }
        SystemConfig.nTiles = (int) SystemConfig.NoOfCores / 2;
        for (int i = 0; i < numRows; i++) {
            String str = null;
            try {
                str = readNocConfig.readLine();
            } catch (IOException e) {
                misc.Error.showErrorAndExit(
                        "Error in reading noc topology file !!");
            }
            
            // StringTokenizer st = new StringTokenizer(str," ");
            StringTokenizer st = new StringTokenizer(str);
                        
            for (int j = 0; j < numColumns; j++) {
                String nextElementToken = (String) st.nextElement();
                
//                if (t < 38)
//                    System.out.println("NOC [" + i + "][" + j + "] = "
//                            + nextElementToken + " t = " + t + " CHA (L2) = "
//                            + SystemConfig.mappingCHA[t] + "; core = "
//                            + SystemConfig.mappingCores[t]);
                CommunicationInterface comInterface = ((NOC) interconnect)
                        .getNetworkElements()[i][j];
                
                if (nextElementToken.equals("C")) {
                    Core core = createCore(cores.size());
                    cores.add(core);
                    core.setComInterface(comInterface);
                } else if (nextElementToken.equals("D")) {
                    MainMemoryDRAMController mainMemController = new MainMemoryDRAMController(
                            SystemConfig.mainMemoryConfig);
                    memoryControllers.add(mainMemController);
                    mainMemController.setComInterface(comInterface);
                } else if (nextElementToken.equals("M")) {
                    MainMemoryDRAMController mcdramController = new MainMemoryDRAMController(
                            SystemConfig.mcdramConfig, true,
                            mcdramControllers.size());
                    mcdramControllers.add(mcdramController);
                    mcdramController.setComInterface(comInterface);
                } else if (nextElementToken.equals("T")) {
                    Core core1 = createCore(SystemConfig.mappingCores[t]);
                    cores.add(core1);
                    core1.setComInterface(comInterface);
                    Core core2 = createCore((SystemConfig.mappingCores[t] + 1));
                    cores.add(core2);
                    core2.setComInterface(comInterface);
                    Cache c = MemorySystem.createSharedCacheCores("L2",
                            SystemConfig.mappingCHA[t], comInterface);
                    Coherence cha = MemorySystem.createCHA("CHA",
                            SystemConfig.mappingCHA[t], comInterface, c);
                    t++;
                    c.setCoherence(cha);
                    SystemConfig.chaList.add(cha);
                } else if (nextElementToken.equals("-")) {
                    // do nothing
                } else if (nextElementToken.equals("O")) {
                    Cache c = MemorySystem.createSharedCacheCores("L2",
                            SystemConfig.mappingCHA[t], comInterface);
                    Coherence cha = MemorySystem.createCHA("CHA",
                            SystemConfig.mappingCHA[t], comInterface, c);
                    t++;
                    c.setCoherence(cha);
                    SystemConfig.chaList.add(cha);
                } else {
                    Cache c = MemorySystem.createSharedCache(nextElementToken,
                            comInterface);
                    // TODO split and multiple shared caches
                }
            }
        }
    }
    
    static Core createCore(int coreNumber) {
        return new Core(coreNumber, 1, 1, null, new int[] { 0 });
    }
    
    public static InterConnect getInterConnect() {
        return interconnect;
    }
    
    public static void setInterConnect(InterConnect i) {
        interconnect = i;
    }
    
    // TODO read a config file
    // create specified number of cores
    // map threads to cores
    public static Core[] initCores() {
        System.out.println("initializing cores...");
        
        Core[] cores = new Core[IpcBase.getEmuThreadsPerJavaThread()];
        for (int i = 0; i < IpcBase.getEmuThreadsPerJavaThread(); i++) {
            cores[i] = new Core(i, 1, 1, null, new int[] { 0 });
        }
        
        GlobalClock.systemTimingSetUp(cores);
        
        // TODO wont work in case of multiple runnable threads
        // for(int i = 0; i<IpcBase.getEmuThreadsPerJavaThread(); i++)
        // {
        // if (SimulationConfig.isPipelineInorder)
        // {
        // ((InorderExecutionEngine)cores[i].getExecEngine()).setAvailable(true);
        // }
        // else if (SimulationConfig.isPipelineMultiIssueInorder)
        // {
        //// TODO
        // ((InorderExecutionEngine)cores[i].getExecEngine()).setAvailable(true);
        // }
        // else if(SimulationConfig.isPipelineOutOfOrder)
        // {
        // ((OutOrderExecutionEngine)cores[i].getExecEngine()).setAvailable(true);
        // }
        // }
        return cores;
    }
    
    public static Core getCore(int coreNumber) {
        for (int i = 0; i < cores.size(); ++i) {
            if (cores.get(i).getCore_number() == coreNumber) {
                return cores.get(i);
            }
        }
        return null;
    }
    
    public static Vector<Core> getCoresVector() {
        return cores;
    }
    
    public static Core[] getCores() {
        Core[] coreArray = new Core[cores.size()];
        int i = 0;
        for (Core core : cores) {
            coreArray[i] = core;
            i++;
        }
        
        return coreArray;
    }
    
    public static long getNoOfInstsExecuted() {
        long noOfInstsExecuted = 0;
        for (int i = 0; i < ArchitecturalComponent.getCores().length; i++) {
            noOfInstsExecuted += ArchitecturalComponent.getCores()[i]
                    .getNoOfInstructionsExecuted();
        }
        return noOfInstsExecuted;
    }
    
    public static void dumpAllICacheBuffers() {
        System.out.println("\n\nICache Buffer DUMP\n\n");
        ICacheBuffer buffer = null;
        for (int i = 0; i < ArchitecturalComponent.getCores().length; i++) {
            buffer = ((OutOrderExecutionEngine) ArchitecturalComponent
                    .getCores()[i].getExecEngine()).getiCacheBuffer();
            System.out.println(
                    "---------------------------------------------------------------------------");
            System.out.println("CORE " + i);
            buffer.dump();
        }
    }
    
    public static void dumpAllEventQueues() {
        System.out.println("\n\nEvent Queue DUMP\n\n");
        EventQueue eventQueue = null;
        for (int i = 0; i < ArchitecturalComponent.getCores().length; i++) {
            eventQueue = ArchitecturalComponent.getCores()[i].getEventQueue();
            System.out.println(
                    "---------------------------------------------------------------------------");
            System.out.println("CORE " + i);
        }
    }
    
    public static void dumpAllMSHRs() {
        CoreMemorySystem coreMemSys = null;
        System.out.println("\n\nMSHR DUMP\n\n");
        for (Cache c : getCacheList()) {
            c.printMSHR();
        }
        
    }
    
    public static void dumpOutStandingLoads() {
        /*
         * System.out.println("Outstanding loads on core ");
         * for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
         * {
         * System.out.println( "outstanding loads on core "+i
         * +"  = "+((InorderExecutionEngine)ArchitecturalComponent.getCores()[i]
         * .getExecEngine()).noOfOutstandingLoads);
         * }
         */
    }
    
    public static CoreMemorySystem[] getCoreMemSysArray() {
        CoreMemorySystem[] toBeReturned = new CoreMemorySystem[coreMemSysArray
                .size()];
        int i = 0;
        for (CoreMemorySystem c : coreMemSysArray) {
            toBeReturned[i] = c;
            i++;
        }
        return toBeReturned;
    }
    
    private static ArrayList<Router> nocRouterList = new ArrayList<Router>();
    
    public static void addNOCRouter(Router router) {
        nocRouterList.add(router);
    }
    
    public static ArrayList<Router> getNOCRouterList() {
        return nocRouterList;
    }
    
    public static void initCoreBroadcastBus() {
        coreBroadcastBus = new CoreBcastBus();
    }
    
    public static MainMemoryDRAMController getMainMemoryDRAMController(
            CommunicationInterface comInterface, int chanNum) {
        // TODO : return the nearest memory controller based on the location of
        // the communication interface
        return memoryControllers.get(chanNum);
    }
    
    public static Vector<Cache> getCacheList() {
        return caches;
    }
    
    public static Vector<Cache> getSharedCacheList() {
        return sharedCaches;
    }
}
