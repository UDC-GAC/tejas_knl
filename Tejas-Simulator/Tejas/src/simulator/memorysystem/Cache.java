/*****************************************************************************
 * Tejas Simulator
 * ------------------------------------------------------------------------------------------------------------
 * 
 * Copyright 2010 Indian Institute of Technology, Delhi
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ------------------------------------------------------------------------------------------------------------
 * 
 * Contributors: Moksh Upadhyay
 *****************************************************************************/
package memorysystem;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.RequestType;
import generic.SimulationElement;
import main.ArchitecturalComponent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.TreeSet;

import net.ID;
import net.NocInterface;
import memorysystem.coherence.Coherence;
import memorysystem.nuca.NucaCache;
import memorysystem.nuca.NucaCache.NucaType;
import misc.Util;
import config.CacheConfig;
import config.CacheConfig.WritePolicy;
import config.CacheDataType;
import config.CacheEnergyConfig;
import config.EnergyConfig;
import config.SystemConfig;
import dram.MainMemoryDRAMController;

public class Cache extends SimulationElement {
    /* cache parameters */
    public CoreMemorySystem        containingMemSys;
    protected int                  blockSize; // in bytes
    public int                     blockSizeBits; // in bits
    public int                     assoc;
    protected int                  assocBits; // in bits
    protected int                  size; // MegaBytes
    protected int                  numLines;
    protected int                  numLinesBits;
    public int                     numSetsBits;
    protected long                 timestamp;
    protected int                  numLinesMask;
    
    public Coherence               mycoherence;
    
    // public CacheType levelFromTop;
    public boolean                 isLastLevel; // Tells whether there are any
                                                // more levels of
                                                // cache
    public CacheConfig.WritePolicy writePolicy; // WRITE_BACK or WRITE_THROUGH
    public String                  nextLevelName; // Name of the next level
                                                  // cache according to
                                                  // the configuration file
    public ArrayList<Cache>        prevLevel       = new ArrayList<Cache>(); // Points
                                                                             // towards
                                                                             // the
                                                                             // previous
                                                                             // level
                                                                             // in
                                                                             // the
                                                                             // cache
                                                                             // hierarchy
    public Cache                   nextLevel; // Points towards the next level
                                              // in the cache
                                              // hierarchy
    protected CacheLine            lines[];
    
    public long                    noOfRequests;
    public long                    noOfAccesses;
    public long                    noOfResponsesReceived;
    public long                    noOfResponsesSent;
    public long                    noOfWritesForwarded;
    public long                    noOfWritesReceived;
    public long                    hits;
    public long                    misses;
    public long                    evictions;
    public boolean                 debug           = false;
    public NucaType                nucaType;
    
    public long                    invalidAccesses = 0;
    
    CacheEnergyConfig              energy;
    
    public String                  cacheName;
    
    public void createLinkToNextLevelCache(Cache nextLevelCache) {
        this.nextLevel = nextLevelCache;
        this.nextLevel.prevLevel.add(this);
    }
    
    public CacheConfig cacheConfig;
    public int         id;
    
    // to check the source
    protected MSHR     mshr;
    
    public boolean isBusy(long addr) {
        return mshr.isMSHRFull(addr);
    }
    
    public boolean isBusy() {
        return mshr.isMSHRFull();
    }
    
    public Cache(String cacheName, int id, CacheConfig cacheParameters,
            CoreMemorySystem containingMemSys) {
        super(cacheParameters.portType, cacheParameters.getAccessPorts(),
                cacheParameters.getPortOccupancy(),
                cacheParameters.getLatency(), cacheParameters.operatingFreq);
        // add myself to the global cache list
        if (cacheParameters.isDirectory == true) {
            ArchitecturalComponent.coherences.add((Coherence) this);
        } else {
            MemorySystem.addToCacheList(cacheName, this);
            if (containingMemSys == null) {
                ArchitecturalComponent.sharedCaches.add(this);
            }
            ArchitecturalComponent.caches.add(this);
        }
        
        if (cacheParameters.collectWorkingSetData == true) {
            workingSet = new TreeSet<Long>();
            workingSetChunkSize = cacheParameters.workingSetChunkSize;
        }
        
        this.containingMemSys = containingMemSys;
        
        // set the parameters
        this.blockSize = cacheParameters.getBlockSize();
        this.assoc = cacheParameters.getAssoc();
        this.size = cacheParameters.getSize();
        this.blockSizeBits = Util.logbase2(blockSize);
        this.assocBits = Util.logbase2(assoc);
        this.numLines = getNumLines();
        this.numLinesBits = Util.logbase2(numLines);
        this.numSetsBits = numLinesBits - assocBits;
        
        this.writePolicy = cacheParameters.getWritePolicy();
        
        this.cacheConfig = cacheParameters;
        if (this.containingMemSys == null) {
            // Use the core memory system of core 0 for all the shared caches.
            this.isSharedCache = true;
            // this.containingMemSys =
            // ArchitecturalComponent.getCore(0).getExecEngine().getCoreMemorySystem();
        }
        
        this.cacheName = cacheName;
        this.id = id;
        
        if (cacheParameters.nextLevel == "") {
            this.isLastLevel = true;
        } else {
            this.isLastLevel = false;
        }
        
        this.nextLevelName = cacheParameters.getNextLevel();
        // this.enforcesCoherence = cacheParameters.isEnforcesCoherence();
        
        this.timestamp = 0;
        this.numLinesMask = numLines - 1;
        this.noOfRequests = 0;
        noOfAccesses = 0;
        noOfResponsesReceived = 0;
        noOfResponsesSent = 0;
        noOfWritesForwarded = 0;
        noOfWritesReceived = 0;
        this.hits = 0;
        this.misses = 0;
        this.evictions = 0;
        // make the cache
        makeCache(cacheParameters.isDirectory);
        
        this.mshr = new MSHR(cacheConfig.mshrSize, blockSizeBits, this);
        
        this.nucaType = cacheParameters.nucaType;
        
        energy = cacheParameters.power;
        
        eventsWaitingOnMSHR = new LinkedList<AddressCarryingEvent>();
    }
    
    public void setCoherence(Coherence c) {
        this.mycoherence = c;
    }
    
    public int numberOfLinesOfSetInMSHR(long addr) {
        int count = 0;
        int startIdx = getStartIdx(addr);
        
        for (int idx = 0; idx < assoc; idx++) {
            int index = getNextIdx(startIdx, idx);
            CacheLine ll = lines[index];
            if (mshr.isAddrInMSHR(ll.getAddress())) {
                count++;
            }
        }
        return count;
    }
    
    private boolean printCacheDebugMessages = false;
    
    public void handleEvent(EventQueue eventQ, Event e) {
        AddressCarryingEvent event = (AddressCarryingEvent) e;
        printCacheDebugMessage(event);
        
        long addr = ((AddressCarryingEvent) event).getAddress();
        RequestType requestType = event.getRequestType();
        
        if (mshr.isAddrInMSHR(addr) && (requestType == RequestType.Cache_Read
                || requestType == RequestType.Cache_Write
                || requestType == RequestType.EvictCacheLine)) {
            mshr.addToMSHR(event);
            return;
        }
        
        switch (event.getRequestType()) {
            case Cache_Read:
            case Cache_Write: {
                handleAccess(addr, requestType, event);
                break;
            }
            
            case Mem_Response: {
                handleMemResponse(event);
                break;
            }
            
            case EvictCacheLine: {
                updateStateOfCacheLine(addr, MESIF.INVALID);
                break;
            }
            
            case AckEvictCacheLine: {
                processEventsInMSHR(addr);
                break;
            }
            
            case DirectoryCachelineForwardRequest: {
                handleDirectoryCachelineForwardRequest(addr,
                        (Cache) (((AddressCarryingEvent) event)
                                .getPayloadElement()),
                        event);
                break;
            }
            
            case DirectorySharedToExclusive: {
                handleDirectorySharedToExclusive(addr);
                break;
            }
            
            case AckDirectoryWriteHit: {
                handleAckDirectoryWriteHit(event);
                break;
            }
        }
    }
    
    protected void handleAckDirectoryWriteHit(AddressCarryingEvent event) {
        // This function just ensures that the writeHit event gets a line
        long addr = event.getAddress();
        CacheLine cl = accessValid(addr);
        
        if (cl == null) {
            misc.Error.showErrorAndExit("Ack write hit expects cache line");
        } else {
            processEventsInMSHR(addr);
        }
    }
    
    protected void handleDirectorySharedToExclusive(long addr) {
        if (accessValid(addr) == null) {
            // c1 and c2 both have address x
            // both decide to evict at the same time
            // c2's evict reaches directory first. directory asks c1 to change
            // state from
            // shared to exclusive
            // c1 however does not have the line
            noteInvalidState(
                    "shared to exclusive for a line that does not exist. addr : "
                            + addr + ". cache : " + this);
        }
        updateStateOfCacheLine(addr, MESIF.EXCLUSIVE);
    }
    
    protected void handleDirectoryCachelineForwardRequest(long addr,
            Cache cache, Event e) {
        AddressCarryingEvent event = new AddressCarryingEvent(
                cache.getEventQueue(), 0, this, cache, RequestType.Mem_Response,
                addr);
        
        if (e != null) {
            event.setCoreId(e.coreId);
        } else {
            misc.Error.showErrorAndExit("Event should not be null...");
        }
        
        updateStateOfCacheLine(addr, MESIF.FORWARD);
        CacheLine dirEntry = access(addr, SystemConfig.globalDir);
        if (dirEntry != null) {
            dirEntry.addSharer(cache);
            dirEntry.setState(MESIF.FORWARD);
        } else {
            // this should never happen
            fill(addr, MESIF.FORWARD, SystemConfig.globalDir);
        }
        this.sendEvent(event);
    }
    
    protected void printCacheDebugMessage(Event event) {
        if (printCacheDebugMessages == true) {
            if (event.getClass() == AddressCarryingEvent.class) {
                System.out.println("CACHE : globalTime = "
                        + GlobalClock.getCurrentTime() + "\teventTime = "
                        + event.getEventTime() + "\t" + event.getRequestType()
                        + "\trequestingElelement = "
                        + event.getRequestingElement() + "\taddress = "
                        + ((AddressCarryingEvent) event).getAddress() + "\t"
                        + this + "\t"
                        + ((AddressCarryingEvent) event).dn_status);
            }
        }
    }
    
    public void handleAccess(long addr, RequestType requestType,
            AddressCarryingEvent event) {
        this.noOfAccesses++;
        if (requestType == RequestType.Cache_Write) {
            noOfWritesReceived++;
        }
        
        CacheLine cl = this.accessAndMark(addr);
        
        // IF HIT
        if (cl != null) {
            cacheHit(addr, requestType, cl, event);
        } else {
            // this.misses++; /* this counts writes + misses */
            if (this.mycoherence != null) {
                if (requestType == RequestType.Cache_Write) {
                    mycoherence.writeMiss(addr, event, this);
                } else if (requestType == RequestType.Cache_Read) {
                    this.misses++;
                    //if ((event.coreId==28)&&(addr > SystemConfig.mcdramStartAddr))
                    //    System.out.println("[DEBUG] core 28 asking L2[" + this.id + "] (CHA[" + ((Cache)mycoherence).id + "])miss for addr = " + addr);
                    mycoherence.readMiss(addr, event, this);
                }
            } else {
                if ((requestType == RequestType.Cache_Read)) {
                    this.misses++;
                }
                sendRequestToNextLevel(addr, RequestType.Cache_Read, event);
            }
            
            mshr.addToMSHR(event);
            // will this fix counting issue??
            this.noOfAccesses--;
        }
    }
    
    protected void cacheHit(long addr, RequestType requestType, CacheLine cl,
            AddressCarryingEvent event) {
        
        noOfRequests++;
        if (requestType == RequestType.Cache_Read) {
            this.hits++;
            sendAcknowledgement(event);
        } else if (requestType == RequestType.Cache_Write) {
            if (this.writePolicy == WritePolicy.WRITE_THROUGH) {
                sendRequestToNextLevel(addr, RequestType.Cache_Write, event);
            }
            
            if ((cl.getState() == MESIF.SHARED
                    || cl.getState() == MESIF.EXCLUSIVE
                    || cl.getState() == MESIF.FORWARD)
                    && (mycoherence != null)) {
                handleCleanToModified(addr, event);
            }
        } else {
            misc.Error.showErrorAndExit("cache hit unknown event type\n" + event
                    + "\ncache : " + this);
        }
    }
    
    protected void handleMemResponse(AddressCarryingEvent memResponseEvent) {
        long addr = memResponseEvent.getAddress();
        // System.out.println("memResponse " + addr);
        
        if (isThereAnUnlockedOrInvalidEntryInCacheSet(addr)) {
            noOfResponsesReceived++;
            this.fillAndSatisfyRequests(addr);
        } else {
            memResponseEvent.setEventTime(GlobalClock.getCurrentTime() + 1);
            this.getEventQueue().addEvent(memResponseEvent);
        }
    }
    
    public void sendRequestToNextLevel(long addr, RequestType requestType) {
        sendRequestToNextLevel(addr, requestType, null);
    }
    
    public void sendRequestToNextLevel(long addr, RequestType requestType,
            Event e) {
        Cache c = this.nextLevel;
        AddressCarryingEvent event = null;
        if ((SystemConfig.mcdramAddr == -1)) {
            checkAddr();
        }
        if (c != null) {
            if (c.nucaType != NucaType.NONE) {
                c = ((NucaCache) c).getBank(
                        ((NocInterface) this.getComInterface()).getId(), addr);
            }
            event = new AddressCarryingEvent(c.getEventQueue(), 0, this, c,
                    requestType, addr);
            if (e != null) {
                event.setCoreId(e.coreId);
            }
            addEventAtLowerCache(event, c);
        } else {
            Core core = main.ArchitecturalComponent.getCores()[0]; // to ensure
                                                                   // that
                                                                   // always has
                                                                   // a core to
                                                                   // send...
            if (e != null) {
                core = ArchitecturalComponent.getCore(e.coreId);
            }
            MainMemoryDRAMController memController;
            if (SystemConfig.mcdramAddr == -1) {
                checkAddr();
            }
            
            int modulo = (int) (addr % 512L);
            int q = this.id % 4;
            int iface = 0;
            if (modulo >= 256) {
                iface = 1 + q * 2;
            } else {
                iface = q * 2;
            }
            memController = ArchitecturalComponent.mcdramControllers
                    .get((int) iface);
            event = new AddressCarryingEvent(core.getEventQueue(), 0, this,
                    memController, requestType, addr);
            event.setCoreId(core.getCore_number());
            sendEvent(event);
        }
    }
    
    // added by markos to check the address file
    private void checkAddr() {
        BufferedReader br = null;
        try {
            System.out.println("checkAddr function has been called!");
            File orgFile = new File(SystemConfig.addrFilePath);
            br = new BufferedReader(new FileReader(orgFile));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            StringTokenizer st = new StringTokenizer(line, "\t");
            long size = Long.parseLong(st.nextToken());
            long virt = Long.parseLong(st.nextToken());
            long phys = Long.parseLong(st.nextToken());
            SystemConfig.physAddr.put(virt, phys);
            SystemConfig.mcdramAddr = virt;
            line = br.readLine();
            while (line != null) {
                st = new StringTokenizer(line, "\t");
                size = Long.parseLong(st.nextToken());
                virt = Long.parseLong(st.nextToken());
                phys = Long.parseLong(st.nextToken());
                SystemConfig.physAddr.put(virt, phys);
                line = br.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
            misc.Error.showErrorAndExit("Something went wrong with file addr.txt, exiting...");
        } finally {
            try {
                br.close();
            } catch (Exception e) {
                System.out.println("Something went wrong closing file...");
            }
        }
    }
    
    // added by kush
    public static int findChannelNumber(long physicalAddress) {
        long tempA, tempB;
        int channelBits = log2(SystemConfig.mainMemoryConfig.numChans);
        
        int DataBusBytesOffest = log2(
                SystemConfig.mainMemoryConfig.DATA_BUS_BYTES); // for 64 bit bus
                                                               // -> 8 bytes ->
                                                               // lower 3 bits
                                                               // of address
                                                               // irrelevant
        
        int ColBytesOffset = log2(SystemConfig.mainMemoryConfig.BL);
        // these are the bits we need to throw away because of "bursts". The
        // column
        // address is incremented internally on bursts
        // So for a burst length of 4, 8 bytes of data are transferred on each
        // burst
        // Each consecutive 8 byte chunk comes for the "next" column
        // So we traverse 4 columns in 1 request. Thus the lower log2(4) bits
        // become
        // irrelevant for us. Throw them away
        // Finally we get 8 bytes * 4 = 32 bytes of data for a 64 bit data bus
        // and BL =
        // 4.
        // This is equal to a cache line
        
        // For clarity
        // Throw away bits to account for data bus size in bytes
        // and for burst length
        physicalAddress >>>= (DataBusBytesOffest + ColBytesOffset); // using >>>
                                                                    // for
                                                                    // unsigned
                                                                    // right
                                                                    // shift
        // System.out.println("Shifted address by " + (DataBusBytesOffest +
        // ColBytesOffset) + " bits");
        
        // By the same logic, need to remove the burst-related column bits from
        // the
        // column bit width to be decoded
        // colEffectiveBits = colBits - ColBytesOffset;
        
        // row:rank:bank:col:chan
        
        tempA = physicalAddress;
        physicalAddress = physicalAddress >>> channelBits; // always unsigned
                                                           // shifting
        tempB = physicalAddress << channelBits;
        // System.out.println("Shifted address by " + rankBits + " bits");
        int decodedChan = (int) (tempA ^ tempB);
        return decodedChan;
    }
    
    public static int log2(int a) {
        return (int) (Math.log(a) / Math.log(2));
    }
    
    private boolean isTopLevelCache() {
        return this.cacheConfig.firstLevel;
    }
    
    public boolean isL2cache() {
        // I am not a first level cache.
        // But a cache connected on top of me is a first level cache
        return (this.cacheConfig.firstLevel == false
                && this.prevLevel.get(0).cacheConfig.firstLevel == true);
    }
    
    public boolean isIcache() {
        return (this.cacheConfig.firstLevel == true
                && (this.cacheConfig.cacheDataType == CacheDataType.Instruction
                        || this.cacheConfig.cacheDataType == CacheDataType.Unified));
    }
    
    public boolean isL1cache() {
        return (this.cacheConfig.firstLevel == true
                && (this.cacheConfig.cacheDataType == CacheDataType.Data
                        || this.cacheConfig.cacheDataType == CacheDataType.Unified));
    }
    
    private boolean isSharedCache = false;
    
    public boolean isSharedCache() {
        return isSharedCache;
    }
    
    public boolean isPrivateCache() {
        return !isSharedCache();
    }
    
    public boolean addEventAtLowerCache(AddressCarryingEvent event, Cache c) {
        if (c.isBusy(event.getAddress()) == false) {
            sendEvent(event);
            c.workingSetUpdate();
            return true;
        } else {
            // Slight approximation used. MSHR full is a rare event.
            // On occurrence of such events, we just add this event to the
            // pending events
            // list of the lower level cache.
            // The network congestion and the port occupancy of the next level
            // is not
            // modelled in such cases.
            // It must be noted that the MSHR full event of the first level
            // caches is being
            // modelled correctly.
            // This approximation applies only to non-firstlevel caches.
            c.eventsWaitingOnMSHR.add(event);
            // System.out.println();
            return false;
        }
    }
    
    public void fillAndSatisfyRequests(long addr) {
        int numPendingEvents = mshr.getNumPendingEventsForAddr(addr);
        // WHY ARE NUMPENDING EVENTS ADDED?
        // misses += numPendingEvents;
        // noOfRequests += numPendingEvents;
        // noOfAccesses += 1 + numPendingEvents;
        noOfRequests += numPendingEvents;
        // noOfAccesses++;
        
        CacheLine evictedLine = this.fill(addr, MESIF.SHARED);
        // System.out.println("fillAndSatisfyRequests " + addr);
        handleEvictedLine(evictedLine);
        processEventsInMSHR(addr);
    }
    
    protected void processEventsInMSHR(long addr) {
        LinkedList<AddressCarryingEvent> missList = mshr
                .removeEventsFromMSHR(addr);
        AddressCarryingEvent writeEvent = null;
        
        for (AddressCarryingEvent event : missList) {
            switch (event.getRequestType()) {
                case Cache_Read: {
                    sendAcknowledgement(event);
                    break;
                }
                
                case Cache_Write: {
                    CacheLine cl = accessValid(addr);
                    
                    if (cl != null) {
                        updateStateOfCacheLine(addr, MESIF.MODIFIED);
                        writeEvent = event;
                    } else {
                        misc.Error.showErrorAndExit(
                                "Cache write expects a line here : " + event);
                    }
                    
                    break;
                }
                
                case DirectoryEvictedFromCoherentCache:
                case EvictCacheLine: {
                    updateStateOfCacheLine(addr, MESIF.INVALID);
                    addUnprocessedEventsToEventQueue(missList);
                    
                    processEventsInPendingList();
                    return;
                }
            }
        }
        
        if (writeEvent != null && writePolicy == WritePolicy.WRITE_THROUGH) {
            sendRequestToNextLevel(addr, RequestType.Cache_Write);
        }
        
        processEventsInPendingList();
    }
    
    private void processEventsInPendingList() {
        int size = eventsWaitingOnMSHR.size();
        for (int i = 0; i < size; i++) {
            if (eventsWaitingOnMSHR.isEmpty()) {
                break;
            }
            
            AddressCarryingEvent event = eventsWaitingOnMSHR.get(i);
            if (mshr.isMSHRFull(event.getAddress())) {
                continue;
            }
            
            event.getProcessingElement().handleEvent(event.getEventQ(), event);
            
            eventsWaitingOnMSHR.remove(event);
            i--;
            size--;
        }
    }
    
    protected void handleEvictedLine(CacheLine evictedLine) {
        if (evictedLine != null && evictedLine.getState() != MESIF.INVALID) {
            if (mshr.isAddrInMSHR(evictedLine.getAddress())) {
                misc.Error.showErrorAndExit("evicting locked line : "
                        + evictedLine + ". cache : " + this);
            }
            if (mycoherence != null) {
                AddressCarryingEvent evictEvent = mycoherence
                        .evictedFromCoherentCache(evictedLine.getAddress(),
                                this);
                mshr.addToMSHR(evictEvent);
            } else if (evictedLine.isModified()
                    && writePolicy == WritePolicy.WRITE_BACK) {
                sendRequestToNextLevel(evictedLine.getAddress(),
                        RequestType.Cache_Write);
            }
        }
    }
    
    private void handleCleanToModified(long addr, AddressCarryingEvent event) {
        if (mycoherence != null) {
            mycoherence.writeHit(addr, this);
            if (event.coreId != -1)
                mshr.addToMSHR(event);
        } else {
            //System.out.println(RequestType.Cache_Write);
            sendRequestToNextLevel(addr, RequestType.Cache_Write, event);
        }
    }
    
    private void addUnprocessedEventsToEventQueue(
            LinkedList<AddressCarryingEvent> missList) {
        int timeToSet = missList.size() * -1;
        boolean startAddition = false;
        for (AddressCarryingEvent event : missList) {
            if (startAddition == true) {
                event.setEventTime(timeToSet);
                getEventQueue().addEvent(event);
                timeToSet++;
            }
            if (event.getRequestType() == RequestType.EvictCacheLine || event
                    .getRequestType() == RequestType.DirectoryEvictedFromCoherentCache) {
                startAddition = true;
            }
        }
    }
    
    public void sendAcknowledgement(AddressCarryingEvent event) {
        RequestType returnType = null;
        if (event.getRequestType() == RequestType.Cache_Read) {
            returnType = RequestType.Mem_Response;
        } else {
            misc.Error.showErrorAndExit(
                    "sendAcknowledgement is meant for cache read operation only : "
                            + event);
        }
        
        AddressCarryingEvent memResponseEvent = new AddressCarryingEvent(
                event.getEventQ(), 0, event.getProcessingElement(),
                event.getRequestingElement(), returnType, event.getAddress());
        
        memResponseEvent.setCoreId(event.coreId);
        
        sendEvent(memResponseEvent);
        noOfResponsesSent++;
    }
    
    public long computeTag(long addr) {
        long tag = addr >>> (numSetsBits + blockSizeBits);
        return tag;
    }
    
    public int getSetIdx(long addr) {
        int startIdx = getStartIdx(addr);
        return startIdx / assoc;
    }
    
    public int getStartIdx(long addr) {
        long SetMask = (1 << (numSetsBits)) - 1;
        int startIdx = (int) ((addr >>> blockSizeBits) & (SetMask));
        return startIdx;
    }
    
    public int getNextIdx(int startIdx, int idx) {
        int index = startIdx + (idx << numSetsBits);
        return index;
    }
    
    public CacheLine accessValid(long addr) {
        CacheLine cl = access(addr);
        if (cl != null && cl.getState() != MESIF.INVALID) {
            return cl;
        } else {
            return null;
        }
    }
    
    public CacheLine access(long addr) {
        /* compute startIdx and the tag */
        int startIdx = getStartIdx(addr);
        long tag = computeTag(addr);
        
        /* search in a set */
        for (int idx = 0; idx < assoc; idx++) {
            // calculate the index
            int index = getNextIdx(startIdx, idx);
            // fetch the cache line
            CacheLine ll = this.lines[index];
            // If the tag is matching, we have a hit
            if (ll.hasTagMatch(tag)) {
                return ll;
            }
        }
        return null;
    }
    
    public CacheLine access(long addr, Cache c) {
        /* compute startIdx and the tag */
        int startIdx = getStartIdx(addr);
        long tag = computeTag(addr);
        
        /* search in a set */
        for (int idx = 0; idx < assoc; idx++) {
            // calculate the index
            int index = getNextIdx(startIdx, idx);
            // fetch the cache line
            CacheLine ll = c.lines[index];
            // If the tag is matching, we have a hit
            if (ll.hasTagMatch(tag)) {
                return ll;
            }
        }
        return null;
    }
    
    protected void mark(CacheLine ll, long tag) {
        ll.setTag(tag);
        mark(ll);
    }
    
    private void mark(CacheLine ll) {
        ll.setTimestamp(timestamp);
        timestamp += 1.0;
    }
    
    private void makeCache(boolean isDirectory) {
        lines = new CacheLine[numLines];
        for (int i = 0; i < numLines; i++) {
            lines[i] = new CacheLine(isDirectory);
        }
    }
    
    private int getNumLines() {
        long totSize = size;
        return (int) (totSize / (long) (blockSize));
    }
    
    protected CacheLine accessAndMark(long addr) {
        CacheLine cl = accessValid(addr);
        if (cl != null) {
            mark(cl);
        }
        return cl;
    }
    
    public CacheLine fill(long addr, MESIF stateToSet) {
        CacheLine evictedLine = null;
        /* compute startIdx and the tag */
        int startIdx = getStartIdx(addr);
        long tag = computeTag(addr);
        boolean addressAlreadyPresent = false;
        /* find any invalid lines -- no eviction */
        CacheLine fillLine = null;
        boolean evicted = false;
        
        // ------- Check if address is in cache ---------
        for (int idx = 0; idx < assoc; idx++) {
            int nextIdx = getNextIdx(startIdx, idx);
            CacheLine ll = this.lines[nextIdx];
            if (ll.getTag() == tag) {
                addressAlreadyPresent = true;
                fillLine = ll;
                break;
            }
        }
        
        // ------- Check if there's an invalid line ---------
        for (int idx = 0; !addressAlreadyPresent && idx < assoc; idx++) {
            int nextIdx = getNextIdx(startIdx, idx);
            CacheLine ll = this.lines[nextIdx];
            if (ll.isValid() == false
                    && mshr.isAddrInMSHR(ll.getAddress()) == false
                    || (this.nucaType != NucaType.NONE
                            && ll.isValid() == false)) {
                fillLine = ll;
                break;
            }
        }
        
        // ------- Check if there's an unlocked valid line ---------
        if (fillLine == null) {
            evicted = true; // We need eviction in this case
            double minTimeStamp = Double.MAX_VALUE;
            for (int idx = 0; idx < assoc; idx++) {
                int index = getNextIdx(startIdx, idx);
                CacheLine ll = this.lines[index];
                
                if (mshr.isAddrInMSHR(ll.getAddress()) == true) {
                    continue;
                }
                
                if (minTimeStamp > ll.getTimestamp()) {
                    minTimeStamp = ll.getTimestamp();
                    fillLine = ll;
                }
            }
        }
        
        if (fillLine == null) {
            misc.Error.showErrorAndExit("Unholy mess !!");
        }
        
        /* if there has been an eviction */
        if (evicted) {
            evictedLine = (CacheLine) fillLine.clone();
            long evictedLinetag = evictedLine.getTag();
            evictedLinetag = (evictedLinetag << numSetsBits)
                    + (startIdx / assoc);
            evictedLine.setTag(evictedLinetag);
            this.evictions++;
        }
        
        /* This is the new fill line */
        fillLine.setState(stateToSet);
        fillLine.setAddress(addr);
        mark(fillLine, tag);
        return evictedLine;
    }
    
    public CacheLine fill(long addr, MESIF stateToSet, Cache c) {
        CacheLine evictedLine = null;
        /* compute startIdx and the tag */
        int startIdx = getStartIdx(addr);
        long tag = computeTag(addr);
        boolean addressAlreadyPresent = false;
        /* find any invalid lines -- no eviction */
        CacheLine fillLine = null;
        boolean evicted = false;
        
        // ------- Check if address is in cache ---------
        for (int idx = 0; idx < assoc; idx++) {
            int nextIdx = getNextIdx(startIdx, idx);
            CacheLine ll = c.lines[nextIdx];
            if (ll.getTag() == tag) {
                addressAlreadyPresent = true;
                fillLine = ll;
                break;
            }
        }
        
        // ------- Check if there's an invalid line ---------
        for (int idx = 0; !addressAlreadyPresent && idx < assoc; idx++) {
            int nextIdx = getNextIdx(startIdx, idx);
            CacheLine ll = c.lines[nextIdx];
            if (ll.isValid() == false
                    && mshr.isAddrInMSHR(ll.getAddress()) == false
                    || (c.nucaType != NucaType.NONE && ll.isValid() == false)) {
                fillLine = ll;
                break;
            }
        }
        
        // ------- Check if there's an unlocked valid line ---------
        if (fillLine == null) {
            evicted = true; // We need eviction in this case
            double minTimeStamp = Double.MAX_VALUE;
            for (int idx = 0; idx < assoc; idx++) {
                int index = getNextIdx(startIdx, idx);
                CacheLine ll = c.lines[index];
                
                if (mshr.isAddrInMSHR(ll.getAddress()) == true) {
                    continue;
                }
                
                if (minTimeStamp > ll.getTimestamp()) {
                    minTimeStamp = ll.getTimestamp();
                    fillLine = ll;
                }
            }
        }
        
        if (fillLine == null) {
            misc.Error.showErrorAndExit("Unholy mess !!");
        }
        
        /* if there has been an eviction */
        if (evicted) {
            evictedLine = (CacheLine) fillLine.clone();
            long evictedLinetag = evictedLine.getTag();
            evictedLinetag = (evictedLinetag << numSetsBits)
                    + (startIdx / assoc);
            evictedLine.setTag(evictedLinetag);
            c.evictions++;
        }
        
        /* This is the new fill line */
        fillLine.setState(stateToSet);
        fillLine.setAddress(addr);
        mark(fillLine, tag);
        return evictedLine;
    }
    
    public LinkedList<AddressCarryingEvent> eventsWaitingOnMSHR = new LinkedList<AddressCarryingEvent>();
    
    public String toString() {
        return cacheName;
    }
    
    public EnergyConfig calculateAndPrintEnergy(FileWriter outputFileWriter,
            String componentName) throws IOException {
        EnergyConfig newPower = new EnergyConfig(energy.leakageEnergy,
                energy.readDynamicEnergy);
        EnergyConfig cachePower = new EnergyConfig(newPower, noOfAccesses);
        cachePower.printEnergyStats(outputFileWriter, componentName);
        return cachePower;
    }
    
    public void updateStateOfCacheLine(long addr, MESIF newState) {
        CacheLine cl = this.access(addr);
        // added by markos (supporting distributed directory)
        if (mycoherence != null) {
            CacheLine dirEntry = access(addr, (Cache) mycoherence);
            if (dirEntry != null) {
                dirEntry.setState(newState);
            }
        }
        CacheLine dirEntry = access(addr, SystemConfig.globalDir);
        if (dirEntry != null)
            dirEntry.setState(newState);
        if (cl != null) {
            cl.setState(newState);
            if (newState == MESIF.INVALID && mshr.isAddrInMSHR(addr)) {
                misc.Error.showErrorAndExit(
                        "Cannot invalidate a locked line. Addr : " + addr
                                + ". Cache : " + this);
            }
            
            if (newState == MESIF.INVALID) {
                if (isBelowCoherenceLevel()) {
                    getPrevLevelCoherence().evictedFromSharedCache(addr, this);
                } else {
                    for (Cache c : prevLevel) {
                        sendAnEventFromMeToCache(addr, c,
                                RequestType.EvictCacheLine);
                    }
                }
            } else {
                // If you are not below coherence, then keep the same state in
                // the previous
                // level caches
                // This ensures that the caches in the same core have the same
                // MESI state
                if (isBelowCoherenceLevel() == false
                        && this.isTopLevelCache() == false) {
                    for (Cache c : prevLevel) {
                        c.updateStateOfCacheLine(addr, newState);
                    }
                }
            }
        }
    }
    
    public EventQueue getEventQueue() {
        if (containingMemSys != null) {
            return containingMemSys.getCore().eventQueue;
        } else {
            return (ArchitecturalComponent.getCores()[0]).eventQueue;
        }
    }
    
    public Cache getNextLevelCache(long addr) {
        return nextLevel;
    }
    
    public void workingSetUpdate() {
        // Clear the working set data after every x instructions
        if (this.containingMemSys != null && this.workingSet != null) {
            
            if (isIcache()) {
                long numInsn = containingMemSys.getiCache().hits
                        + containingMemSys.getiCache().misses;
                long numWorkingSets = numInsn / workingSetChunkSize;
                if (numWorkingSets > containingMemSys.numInstructionSetChunksNoted) {
                    this.clearWorkingSet();
                    containingMemSys.numInstructionSetChunksNoted++;
                }
            } else if (isL1cache()) {
                long numInsn = containingMemSys.getiCache().hits
                        + containingMemSys.getiCache().misses;
                long numWorkingSets = numInsn / workingSetChunkSize;
                if (numWorkingSets > containingMemSys.numDataSetChunksNoted) {
                    this.clearWorkingSet();
                    containingMemSys.numDataSetChunksNoted++;
                }
            }
        }
    }
    
    TreeSet<Long> workingSet             = null;
    long          workingSetChunkSize    = 0;
    public long   numWorkingSetHits      = 0;
    public long   numWorkingSetMisses    = 0;
    public long   numFlushesInWorkingSet = 0;
    public long   totalWorkingSetSize    = 0;
    public long   maxWorkingSetSize      = Long.MIN_VALUE;
    public long   minWorkingSetSize      = Long.MAX_VALUE;
    
    void addToWorkingSet(long addr) {
        long lineAddr = addr >>> blockSizeBits;
        if (workingSet != null) {
            if (workingSet.contains(lineAddr) == true) {
                numWorkingSetHits++;
                return;
            } else {
                numWorkingSetMisses++;
                workingSet.add(lineAddr);
            }
        }
    }
    
    float getWorkingSetHitrate() {
        if (numWorkingSetHits == 0 && numWorkingSetMisses == 0) {
            return 0.0f;
        } else {
            return (float) numWorkingSetHits
                    / (float) (numWorkingSetHits + numWorkingSetMisses);
        }
    }
    
    void clearWorkingSet() {
        numFlushesInWorkingSet++;
        totalWorkingSetSize += workingSet.size();
        if (workingSet.size() > maxWorkingSetSize) {
            maxWorkingSetSize = workingSet.size();
        }
        
        if (workingSet.size() < minWorkingSetSize) {
            minWorkingSetSize = workingSet.size();
        }
        
        // System.out.println(this + " : For chunk " +
        // (numFlushesInWorkingSet-1) +
        // "\tworkSet = " + workingSet.size() +
        // "\tminSet = " + minWorkingSetSize +
        // "\tavgSet = " +
        // (float)totalWorkingSetSize/(float)numFlushesInWorkingSet +
        // "\tmaxSet = " + maxWorkingSetSize +
        // "\tworkSetHitrate = " + getWorkingSetHitrate());
        
        if (workingSet != null) {
            workingSet.clear();
        }
    }
    
    private long getLineAddress(long addr) {
        return addr >>> blockSizeBits;
    }
    
    protected AddressCarryingEvent sendAnEventFromMeToCache(long addr, Cache c,
            RequestType request) {
        // Create an event
        
        AddressCarryingEvent event = new AddressCarryingEvent(c.getEventQueue(),
                0, this, c, request, addr);
        
        SystemConfig.dataHops += getHopsCount(this, c);
        
        // 2. Send event to cache
        this.sendEvent(event);
        
        return event;
    }
    
    private Coherence getPrevLevelCoherence() {
        return prevLevel.get(0).mycoherence;
    }
    
    private boolean isBelowCoherenceLevel() {
        if (prevLevel != null && prevLevel.size() > 0
                && prevLevel.get(0).mycoherence != null) {
            return true;
        } else {
            return false;
        }
    }
    
    protected boolean isThereAnUnlockedOrInvalidEntryInCacheSet(long addr) {
        /* compute startIdx and the tag */
        int startIdx = getStartIdx(addr);
        
        /* search in a set */
        for (int idx = 0; idx < assoc; idx++) {
            // calculate the index
            int index = getNextIdx(startIdx, idx);
            // fetch the cache line
            CacheLine ll = lines[index];
            // If the tag is matching, we have a hit
            
            if (ll.getAddress() == addr) {
                return true;
            }
            
            if ((ll.isValid() == false
                    && mshr.isAddrInMSHR(ll.getAddress()) == false
                    || (this.nucaType != NucaType.NONE
                            && ll.isValid() == false))) {
                return true;
            }
            
            if (mshr.isAddrInMSHR(ll.getAddress()) == false) {
                return true;
            }
            // else if (ll.getState() == MESIF.INVALID) {
            // return true;
            // }
        }
        
        return false;
    }
    
    public int getHopsCount(Cache c1, Cache c2) {
        int c = 0;
        ID idC1 = ((NocInterface) c1.getComInterface()).getId();
        ID idC2 = ((NocInterface) c2.getComInterface()).getId();
        c = Math.abs(idC2.gety() - idC1.gety())
                + Math.abs(idC2.getx() - idC1.getx());
        return c;
    }
    
    public void printMSHR() {
        mshr.printMSHR();
    }
    
    protected void noteInvalidState(String msg) {
        invalidAccesses++;
    }
    
    public void noteMSHRStats() {
        mshr.noteMSHRStats();
    }
    
    public double getAvgNumEventsPendingInMSHR() {
        return mshr.getAvgNumEventsPendingInMSHR();
    }
    
    public double getAvgNumEventsPendingInMSHREntry() {
        return mshr.getAvgNumEventsPendingInMSHREntry();
    }
    
}
