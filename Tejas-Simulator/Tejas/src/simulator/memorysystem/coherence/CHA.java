package memorysystem.coherence;

import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.RequestType;
import generic.Statistics;
import generic.Core;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

import dram.MainMemoryDRAMController;

import main.ArchitecturalComponent;
import memorysystem.AddressCarryingEvent;
import memorysystem.Cache;
import memorysystem.CacheLine;
import memorysystem.CoreMemorySystem;
import memorysystem.MESIF;
import memorysystem.MemorySystem;
import misc.Util;
import config.CacheConfig;
import config.EnergyConfig;
import config.SystemConfig;
import java.util.Random;
// Unlock function should call the state change function. This is called using
// the current event field inside the directory entry.
// For write hit event, there is some mismatch

public class CHA extends Cache implements Coherence {
    
    long readMissAccesses                 = 0;
    long writeHitAccesses                 = 0;
    long writeMissAccesses                = 0;
    long evictedFromCoherentCacheAccesses = 0;
    long evictedFromSharedCacheAccesses   = 0;
    
    Cache cacheOwner = null;
    
    public CHA(String cacheName, int id, CacheConfig cacheParameters,
            CoreMemorySystem containingMemSys) {
        super(cacheName, id, cacheParameters, containingMemSys);
        MemorySystem.coherenceNameMappings.put(cacheName, this);
    }
    
    public CHA(String cacheName, int id, CacheConfig cacheParameters,
            CoreMemorySystem containingMemSys, Cache cacheOwner) {
        super(cacheName, id, cacheParameters, containingMemSys);
        MemorySystem.coherenceNameMappings.put(cacheName, this);
        this.cacheOwner = cacheOwner;
    }
    
    public void writeHit(long addr, Event e, Cache c) {
        sendAnEventFromCacheToDirectory(addr, c, RequestType.DirectoryWriteHit, e);
    }
    
    public void readMiss(long addr, Event e, Cache c) {
        sendAnEventFromCacheToDirectory(addr, c, RequestType.DirectoryReadMiss, e);
    }
    
    public void writeMiss(long addr, Event e, Cache c) {
        sendAnEventFromCacheToDirectory(addr, c, RequestType.DirectoryWriteMiss, e);
    }
    
    // made for Quadrant cluster mode
    public CHA getDestinationChaQuadrant(long addr) {
        long tmpAddr = 0;
        long addrTrunc = addr - SystemConfig.mcdramAddr;
        if ((SystemConfig.mcdramAddr != -1) && (SystemConfig.mcdramAddr <= addr)
                && ((SystemConfig.mcdramSize * 100) > addrTrunc) && (addrTrunc >= 0)) {
            tmpAddr = (addr - SystemConfig.mcdramAddr)/64;
//            System.out.println("CHA address " + addr + " is within "
//                    + SystemConfig.mcdramAddr + " and "
//                    + SystemConfig.mcdramSize);
        } else {
            Random rand = new Random();
            tmpAddr = rand.nextInt(38);
//            System.out.println("CHA address is more than " + addr + " than "
//                    + SystemConfig.mcdramAddr + " and "
//                    + SystemConfig.mcdramSize);
//              tmpAddr = addr % (256*1024*1024);
        }
        CHA c = null;
        int cha = SystemConfig.mappingKNL[(int) tmpAddr];
        for (int i = 0; i < SystemConfig.mappingCHA.length; ++i) {
            CHA tmp = (CHA) SystemConfig.chaList.get(i);
            if (tmp.id == cha) {
                c = tmp;
            }
        }
        // CHA c = (CHA)SystemConfig.chaList.get(n);
        //System.out.println("CHA is " + cha + " addr " + addr);
        return c;
    }
    
    private AddressCarryingEvent sendAnEventFromCacheToDirectory(long addr,
            Cache c, RequestType request, Event e) {
        incrementHitMissInformation(addr);
        // Create an event
        CHA directory = this;
        directory = getDestinationChaQuadrant(addr);
        AddressCarryingEvent event = new AddressCarryingEvent(c.getEventQueue(),
                0, c, directory, request, addr);
        if (e != null)
            event.setCoreId(e.coreId);
        SystemConfig.controlHops += getHopsCount(this, directory);
        // 2. Send event to directory
        c.sendEvent(event);
        //System.out.println("cache " + c.id + " sending from " + this.id + " to " + directory.id
        //        + " a " + request + " with addr " + addr);
        return event;
    }
    
    private void incrementHitMissInformation(long addr) {
        CacheLine dirEntry = access(addr);
        
        if (dirEntry == null || dirEntry.getState() == MESIF.INVALID) {
            misses++;
        } else {
            hits++;
        }
    }
    
    public void handleWriteHit(long addr, Cache c, AddressCarryingEvent event) {
        CacheLine localEntry = access(addr);  
        CacheLine dirEntry = access(addr, SystemConfig.globalDir);  

        switch (dirEntry.getState()) {
            case MODIFIED:
            case EXCLUSIVE:
            case FORWARD:
            case SHARED: {
                
                if (dirEntry.isSharer(c) == false) {
                    // Valid case : c1 and c2 are sharers address xse
                    // Both encountered a write at the same time
                    noteInvalidState(
                            "WriteHit expects cache to be a sharer. Cache : "
                                    + c + ". Addr : " + addr);
                }
                
                for (Cache sharerCache : dirEntry.getSharers()) {
                    if ((sharerCache != c) && (sharerCache != null)) {
                        sendAnEventFromMeToCache(addr, sharerCache,
                                RequestType.EvictCacheLine);
                    }
                }
                
                dirEntry.clearAllSharers();
                dirEntry.addSharer(c);
                dirEntry.setState(MESIF.MODIFIED);
                break;
            }
            case INVALID: {
                noteInvalidState(
                        "WriteHit expects entry to be in a valid state. Cache : "
                                + c + ". Addr : " + addr);
                dirEntry.clearAllSharers();
                dirEntry.setState(MESIF.MODIFIED);
                dirEntry.addSharer(c);
                break;
            }
        }
        if (localEntry==null) fill(addr, MESIF.MODIFIED);
        localEntry.setState(MESIF.MODIFIED);
        sendAnEventFromMeToCache(addr, c, RequestType.AckDirectoryWriteHit);
    }
    
    private void forceInvalidate(CacheLine dirEntry) {
        misc.Error.showErrorAndExit("Force Invalidate !!");
        // The directory is in an inconsistent state.
        // Force a consistent change by evicting the dirEntry.
        for (Cache sharerCache : dirEntry.getSharers()) {
            sharerCache.updateStateOfCacheLine(dirEntry.getAddress(),
                    MESIF.INVALID);
        }
        
        dirEntry.clearAllSharers();
        dirEntry.setState(MESIF.INVALID);
    }
    
    public AddressCarryingEvent evictedFromSharedCache(long addr, Cache c) {
        return sendAnEventFromCacheToDirectory(addr, c,
                RequestType.DirectoryEvictedFromSharedCache, null);
    }
    
    public AddressCarryingEvent evictedFromCoherentCache(long addr, Cache c) {
        return sendAnEventFromCacheToDirectory(addr, c,
                RequestType.DirectoryEvictedFromCoherentCache, null);
    }
    
    public void handleEvent(EventQueue eventQ, Event e) {
        AddressCarryingEvent event = (AddressCarryingEvent) e;
        long addr = event.getAddress();
        long lineAddr = event.getAddress() >> blockSizeBits;
        RequestType reqType = e.getRequestType();
        
        if (reqType == RequestType.DirectoryWriteHit) {
            writeHitAccesses++;
        }
        
        if (reqType == RequestType.DirectoryWriteMiss) {
            writeMissAccesses++;
        }
        
        if (reqType == RequestType.DirectoryReadMiss) {
            readMissAccesses++;
        }
        
        if (reqType == RequestType.DirectoryEvictedFromSharedCache) {
            evictedFromSharedCacheAccesses++;
        }
        
        if (reqType == RequestType.DirectoryEvictedFromCoherentCache) {
            evictedFromCoherentCacheAccesses++;
        }

        if (access(addr, SystemConfig.globalDir)==null) {
            //System.out.println("As expected, cache line is null for addr " + addr);
        } else {
            //System.out.println("Cache line is NOT null for addr " + addr + " eventtype " + event.getRequestType().name());
        }
        if (access(addr, SystemConfig.globalDir) == null && (reqType == RequestType.DirectoryWriteHit
                || reqType == RequestType.DirectoryWriteMiss
                || reqType == RequestType.DirectoryReadMiss
                || reqType == RequestType.DirectoryEvictedFromCoherentCache)) {
            
            // This events expect a directory entry to be present.
            // Create a directory entry.
            
            CacheLine tmp = fill(addr, MESIF.INVALID, SystemConfig.globalDir);
            CacheLine evictedEntry = fill(addr, MESIF.INVALID);

            //System.out.println("filling line for addr " + addr);
            if (evictedEntry != null && evictedEntry.isValid()) {
                //System.out.println("Evicted line : " +
                // (evictedEntry.getAddress()>>blockSizeBits) + "\n" +
                // evictedEntry);
                if (tmp!=null)
                    invalidateDirectoryEntry(tmp);
                evictedEntry.setState(MESIF.INVALID);
            }
        }
        
        Cache senderCache = (Cache) event.getRequestingElement();
        
        switch (event.getRequestType()) {
            case DirectoryWriteHit: {
                handleWriteHit(addr, senderCache, event);
                break;
            }
            
            case DirectoryReadMiss: {
                //System.out.println("coherence " + this.id + " received from " + senderCache.id);
                handleReadMiss(addr, senderCache, event);
                break;
            }
            
            case DirectoryWriteMiss: {
                handleWriteMiss(addr, senderCache, event);
                break;
            }
            
            case DirectoryEvictedFromSharedCache: {
                handleEvictFromSharedCache(addr);
                break;
            }
            
            case DirectoryEvictedFromCoherentCache: {
                handleEvictedFromCoherentCache(addr, senderCache);
                break;
            }
        }
    }
    
    private void handleEvictedFromCoherentCache(long addr, Cache c) {
        CacheLine dirEntry = access(addr, SystemConfig.globalDir);
        CacheLine localEntry = access(addr);
        
        if ((localEntry == null)&&(dirEntry == null)) {
            misc.Error.showErrorAndExit("local entry null and dirEntry ");
        }
        
        if ((localEntry == null)) {
            //System.out.println("local entry ");
            localEntry = fill(addr, MESIF.INVALID);
            localEntry = access(addr);
        }
        
        if ((dirEntry == null)) {
            //System.out.println("dirEntry");
            dirEntry = fill(addr, MESIF.INVALID);
            dirEntry = access(addr);
        }
        
        if (dirEntry.isSharer(c)) {
            dirEntry.removeSharer(c);
            if (dirEntry.getSharers().size() == 0) {
                dirEntry.setState(MESIF.INVALID);
            } else if (dirEntry.getSharers().size() == 1) {
                dirEntry.setState(MESIF.EXCLUSIVE);
                //System.out.println("handleEvictedFromCoherentCache " + addr);                
               
                sendAnEventFromMeToCache(addr, dirEntry.getOwner(),
                        RequestType.DirectorySharedToExclusive);
            }
        } else {
            // Cache c1 holds an address x
            // directory and c1 evict line for x in the same cycle
            // When c1's invalidate message reaches directory, it is not a
            // sharer
            noteInvalidState("Eviction from a non-sharer. Cache : " + c
                    + ". Addr : " + addr);
        }
        
        if (localEntry.isSharer(c)) {
            localEntry.removeSharer(c);
            if (localEntry.getSharers().size() == 0) {
                localEntry.setState(MESIF.INVALID);
            } else if (localEntry.getSharers().size() == 1) {
                localEntry.setState(MESIF.INVALID);
            }
        }
        
        sendAnEventFromMeToCache(addr, c, RequestType.AckEvictCacheLine);
    }
    
    private void handleWriteMiss(long addr, Cache c, Event e) {
        CacheLine dirEntry = access(addr, SystemConfig.globalDir);
        //System.out.println("directory " + this.id + " received from cache "
        //        + c.id + " a write miss (core " + e.coreId + ")");
        handleReadMiss(addr, c, e);
        for (Cache sharerCache : dirEntry.getSharers()) {
            if (sharerCache != c) {
                sendAnEventFromMeToCache(addr, sharerCache,
                        RequestType.EvictCacheLine);
            }
        }
        
        dirEntry.clearAllSharers();
        dirEntry.addSharer(c);
        dirEntry.setState(MESIF.MODIFIED);
        
        CacheLine localEntry = access(addr);
        if (localEntry == null) {
            localEntry = fill(addr, MESIF.MODIFIED);
            if (localEntry == null) {
                localEntry = access(addr);
            }
        }
        localEntry.clearAllSharers();
        localEntry.setState(MESIF.MODIFIED);
        localEntry.addSharer(c);        
    }
    
    private void handleEvictFromSharedCache(long addr) {
        CacheLine cl = access(addr);
        
        if (cl == null || cl.isValid() == false) {
            return;
        } else {
            invalidateDirectoryEntry(cl);
        }
    }
    
    private void invalidateDirectoryEntry(CacheLine cl) {
        long addr = cl.getAddress();
        for (Cache c : cl.getSharers()) {
            sendAnEventFromMeToCache(addr, c, RequestType.EvictCacheLine);
        }
        cl.clearAllSharers();
        cl.setState(MESIF.INVALID);
    }
    
    public void sendRequestToMCDRAM(long addr, RequestType request, Cache c,
            Event e) {
        AddressCarryingEvent event = null;
        MainMemoryDRAMController memController;
        
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
        
        event = new AddressCarryingEvent(c.getEventQueue(), 0, c, memController,
                request, addr);
        event.setCoreId(e.coreId);
        
        c.sendEvent(event);
    }
    
    private void handleReadMiss(long addr, Cache c, Event e) {
        
        CacheLine dirEntry = access(addr, SystemConfig.globalDir);

        switch (dirEntry.getState()) {
            case MODIFIED:
            case EXCLUSIVE:
            case SHARED:
            case FORWARD: {
                
                if (dirEntry.isSharer(c) == true) {
                    // Cache c1 and c2 are sharers of address x
                    // Both perform a write at the same time. Hence, both send a
                    // writeHit to the directory at the same time
                    // Assume c2's writeHit reaches directory first. It sends
                    // invalidate to c1. This invalidate is queued behind the
                    // write
                    // entry in c1's mshr entry for addr
                    // c1's writeHit reaches directory. The directory
                    // re-configured
                    // the owner to c1
                    // c1 processes invalidate from c2
                    // now, there is a read at c1. The c1 sends readMiss to
                    // directory. However, it is a sharer.
                    noteInvalidState("Miss from a sharer. Cache : " + c
                            + ". Addr : " + addr);
                    sendAnEventFromMeToCache(addr, c, RequestType.Mem_Response);
                } else {
                    Cache sharerCache = dirEntry.getFirstSharer();
                    sendCachelineForwardRequest(sharerCache, c, addr, e);
                }
                
                dirEntry.setState(MESIF.FORWARD);
                dirEntry.addSharer(c);
                
                break;
            }
            
            case INVALID: {
                dirEntry.setState(MESIF.EXCLUSIVE);
                dirEntry.clearAllSharers();
                dirEntry.addSharer(c);
                // If the line is supposed to be fetched from the next level
                // cache,
                // we will just send a cacheRead request to this cache
                // Note that the directory is not coming into the picture. This
                // is
                // just a minor hack to maintain readability of the code
                //System.out.println("directory " + this.id
                 //       + " requesting to next level to mcdram from core "
                   //     + e.coreId);
                this.sendRequestToMCDRAM(addr, RequestType.Cache_Read, c, e);
                //c.sendRequestToNextLevel(addr, RequestType.Cache_Read, e);

                break;
            }
        }
    }
    
    private void sendCachelineForwardRequest(Cache ownerCache,
            Cache destinationCache, long addr, Event e) {
        EventQueue eventQueue = ownerCache.getEventQueue();
        
        AddressCarryingEvent event = new AddressCarryingEvent(eventQueue, 0,
                this, ownerCache, RequestType.DirectoryCachelineForwardRequest,
                addr);
        
        event.payloadElement = destinationCache;
        this.sendEvent(event);
    }
    
    public void printStatistics(FileWriter outputFileWriter)
            throws IOException {
        outputFileWriter.write("\n");
        outputFileWriter.write("CHA[" + id + "] Access due to ReadMiss\t=\t"
                + readMissAccesses + "\n");
        outputFileWriter.write("CHA[" + id + "] Access due to WriteMiss\t=\t"
                + writeMissAccesses + "\n");
        outputFileWriter.write("CHA[" + id + "] Access due to WriteHit\t=\t"
                + writeHitAccesses + "\n");
        outputFileWriter
                .write("CHA[" + id + "] due to EvictionFromCoherentCache\t=\t"
                        + evictedFromCoherentCacheAccesses + "\n");
        outputFileWriter
                .write("CHA[" + id + "] due to EvictionFromSharedCache\t=\t"
                        + evictedFromSharedCacheAccesses + "\n");
        
        outputFileWriter.write("CHA[" + id + "] Hits\t=\t" + hits + "\n");
        outputFileWriter.write("CHA[" + id + "] Misses\t=\t" + misses + "\n");
        if ((hits + misses) != 0) {
            outputFileWriter.write("CHA[" + id + "] Hit-Rate\t=\t"
                    + Statistics.formatDouble((double) (hits) / (hits + misses))
                    + "\n");
            outputFileWriter
                    .write("CHA[" + id + "] Miss-Rate\t=\t"
                            + Statistics.formatDouble(
                                    (double) (misses) / (hits + misses))
                            + "\n");
        }
    }
    
    public EnergyConfig calculateAndPrintEnergy(FileWriter outputFileWriter,
            String componentName) throws IOException {
        long numAccesses = readMissAccesses + writeHitAccesses
                + writeMissAccesses + evictedFromCoherentCacheAccesses
                + evictedFromSharedCacheAccesses;
        EnergyConfig newPower = new EnergyConfig(
                cacheConfig.power.leakageEnergy,
                cacheConfig.power.readDynamicEnergy);
        EnergyConfig power = new EnergyConfig(newPower, numAccesses);
        power.printEnergyStats(outputFileWriter, componentName);
        return power;
    }
    
    @Override
    public void readMiss(long addr, Cache c) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void writeHit(long addr, Cache c) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void writeMiss(long addr, Cache c) {
        // TODO Auto-generated method stub
        
    }
}
