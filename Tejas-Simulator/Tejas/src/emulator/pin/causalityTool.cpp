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

        Contributors:  Abhishek Sagar, Eldhose Peter, Prathmesh Kallurkar, Ritu
Jha, Kunal Kishore, Apoorva Temurnikar, Bhumika Singh, Sakshi Goel
*****************************************************************************/
#include <fcntl.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <sys/types.h>
#include <unistd.h>

#include <fstream>
#include <iostream>

#include "pin.H"

#ifndef _WIN32
#include <sys/resource.h>
#include <sys/shm.h>
#include <sys/time.h>
#define CRITICAL_SECTION pthread_mutex_t
#include <pthread.h>
#include <sched.h>
#include <unistd.h>
#endif

#include <time.h>

#include <cstdlib>
#include <cstring>
// #include <sys/timeb.h>
//#include "IPCBase.h"
#define MaxThreads (10000)
#include "shmem.h"
//#include "filePacket.h"
#include "encoding.h"
#ifdef __MACH__
#include <mach/clock.h>
#include <mach/mach.h>
#endif

OS_THREAD_ID father_id = INVALID_OS_THREAD_ID;
#ifdef _LP64
#define MASK 0xffffffffffffffff
#else
#define MASK 0x00000000ffffffff
#endif

#ifdef _WIN32
#include <io.h>
#endif

// needed to check CPU
static inline int get_cpu() {
#ifdef SYS_getcpu
  int cpu, status;
  status = syscall(SYS_getcpu, &cpu, NULL, NULL);
  return (status == -1) ? status : cpu;
#else
  return -1;  // unavailable
#endif
}

/* ===================================================================== */
/* Names of malloc and free */
/* ===================================================================== */
#if defined(TARGET_MAC)
#define MALLOC "_malloc"
#define FREE "_free"
#else
#define MALLOC "malloc"
#define FREE "free"
#endif

#define ADDR_MCDRAM 0x3040000000
#define ADDR_PATH "/home/mhorro/tejas-git/Tejas-Simulator/Tejas/addr.txt"
#define MEM_PATH "/home/mhorro/tejas-git/Tejas-Simulator/Tejas/memtrace.txt"

int memoryTrace = 0;
long knlAllocSize = 0;

// Defining  command line arguments
KNOB<UINT64> KnobMap(KNOB_MODE_WRITEONCE, "pintool", "map", "1", "Maps");
KNOB<UINT64> KnobIgnore(KNOB_MODE_WRITEONCE, "pintool", "numIgn", "0",
                        "Ignore these many profilable instructions");
KNOB<INT64> KnobSimulate(KNOB_MODE_WRITEONCE, "pintool", "numSim", "0",
                         "Simulate these many profilable instructions (-1 if "
                         "no subset simulation is desired)");
KNOB<INT64> KnobMaxNumActiveThreads(KNOB_MODE_WRITEONCE, "pintool",
                                    "maxNumActiveThreads", "0",
                                    "Number of maximum application threads");
KNOB<UINT64> KnobId(KNOB_MODE_WRITEONCE, "pintool", "id", "1",
                    "shm id to generate key");
KNOB<std::string> KnobPinPointsFile(
    KNOB_MODE_WRITEONCE, "pintool", "pinpointsFile", "nofile",
    "pinpoints file (pass numIgn = 0, numSim = -1)");
KNOB<std::string> KnobStartMarker(KNOB_MODE_WRITEONCE, "pintool", "startMarker",
                                  "", "start marker function name");
KNOB<std::string> KnobEndMarker(KNOB_MODE_WRITEONCE, "pintool", "endMarker", "",
                                "end marker function name");
KNOB<std::string> KnobTraceMethod(
    KNOB_MODE_WRITEONCE, "pintool", "traceMethod", "0",
    "Trace Method (sharedMemory,file). Compulsary argument");
KNOB<std::string> KnobTraceFileName(
    KNOB_MODE_WRITEONCE, "pintool", "traceFileName", "0",
    "Basename for compressed trace files (_x.gz will be appended to filename "
    "where x is core number). Compulsary for file trace method.");

/**
 * readn
 *
 * @param fd File descriptor
 * @param buf Buffer
 * @param count Number of bytes to read
 */
ssize_t readn(int fd, void *buf, size_t count) {
  char *cbuf = (char *)buf;
  ssize_t nr = 0;
  size_t n = 0;

  while (n < count) {
    nr = read(fd, &cbuf[n], count - n);
    if (nr == 0) {
      // EOF
      break;
    } else if (nr == -1) {
      if (errno == -EINTR) {
        // retry
        continue;
      } else {
        // error
        return -1;
      }
    }
    n += nr;
  }

  return n;
}

/**
 * Main function
 *
 * @param fd File descriptor
 * @param virtaddr Virtual address to translate
 * @return -1: error, -2: not present, other: physical address
 */
uint64_t virt_to_phys(int fd, uint64_t virtaddr) {
  int pagesize;
  uint64_t tbloff, tblen, pageaddr, physaddr;
  uint64_t offset;
  uint64_t nr;

  uint64_t tbl_present;
  uint64_t tbl_swapped;
  // uint64_t tbl_shared;
  // uint64_t tbl_pte_dirty;
  uint64_t tbl_swap_offset;
  // uint64_t tbl_swap_type;

  /* 1 Page = typically 4KB, 1 Entry = 8bytes */
  pagesize = (int)sysconf(_SC_PAGESIZE);
  /* see: linux/Documentation/vm/pagemap.txt */
  tbloff = virtaddr / pagesize * sizeof(uint64_t);
  // dprintf("pagesize:%d, virt:0x%08llx, tblent:0x%08llx\n",
  // pagesize, (long long)virtaddr, (long long)tbloff);

  offset = lseek(fd, tbloff, SEEK_SET);
  if ((long int)offset == (off_t)-1) {
    printf("error!!!!!!\n");
    perror("lseek");
    return -1;
  }
  if (offset != tbloff) {
    fprintf(stderr,
            "Cannot found virt:0x%08llx, "
            "tblent:0x%08llx, returned offset:0x%08llx.\n",
            (long long)virtaddr, (long long)tbloff, (long long)offset);
    return -1;
  }

  nr = readn(fd, &tblen, sizeof(uint64_t));
  if ((int)nr == -1 || nr < sizeof(uint64_t)) {
    fprintf(stderr,
            "Cannot found virt:0x%08llx, "
            "tblent:0x%08llx, returned offset:0x%08llx, "
            "returned size:0x%08x.\n",
            (long long)virtaddr, (long long)tbloff, (long long)offset, (int)nr);
    return -1;
  }

  tbl_present = (tblen >> 63) & 0x1;
  tbl_swapped = (tblen >> 62) & 0x1;
  if (!tbl_swapped) {
    tbl_swap_offset = (tblen >> 0) & 0x7fffffffffffffULL;
  } else {
    tbl_swap_offset = (tblen >> 5) & 0x3ffffffffffffULL;
  }
  pageaddr = tbl_swap_offset * pagesize;
  physaddr = (uint64_t)pageaddr | (virtaddr & (pagesize - 1));
  close(fd);
  if (tbl_present) {
    return physaddr;
  } else {
    return -2;
  }
}

int open_pagemap() {
  char procname[1024] = "";
  int fd = -1;

  memset(procname, 0, sizeof(procname));
  snprintf(procname, sizeof(procname) - 1, "/proc/self/pagemap");
  fd = open(procname, O_RDONLY);
  if (fd == -1) {
    perror("error: ");
    close(fd);
    exit(-1);
  }
  return fd;
}

/**
 * Wrapper function
 *
 * @param virtaddr Virtual address to translate
 * @return -1: error, -2: not present, other: physical address
 */
uint64_t vtop(uint64_t virtaddr) {
  uint64_t physaddr;
  char procname[1024] = "";
  int fd = -1;

  memset(procname, 0, sizeof(procname));
  snprintf(procname, sizeof(procname) - 1, "/proc/self/pagemap");
  fd = open(procname, O_RDONLY);
  if (fd == -1) {
    perror("error: ");
    close(fd);
    exit(-1);
  }

  return physaddr = virt_to_phys(fd, virtaddr);
}

enum TraceMethod { SharedMemory, File };
enum TraceMethod traceMethod;

PIN_MUTEX mainLockForPintool;
INT32 numThreads = 0;
INT32 livethreads = 0;
UINT64 checkSum = 0;
IPC::Shm *tst;
bool pumpingStatus[MaxThreads];
ADDRINT curSynchVar[MaxThreads];
static UINT64 numIns = 0;
UINT64 numInsToIgnore = 0;
INT64 numInsToSimulate = 0;
std::string startMarker;
std::string endMarker;
BOOL instructionIgnorePhase = false;
BOOL outOfROIPhase = false;
UINT64 numCISC[MaxThreads];
UINT64 totalNumCISC;
bool threadAlive[MaxThreads];
std::string pinpointsFilename;
unsigned long *sliceArray;
int numberOfSlices;
int currentSlice;

ofstream memfile;
uint64_t start_addr;

uint32_t *threadMapping;
// BETTER IMPLEMENT THIS AD-HOC IN JAVA SIDE; MORE VERSATILE
// uint32_t threadMapping[64] = {
//		 0, 1, 20, 21,
//		            28, 29, 50, 51, 56, 57, 36, 37, 44, 45, 62, 63, 48,
// 49, 24, 25, 4, 		            5, 10, 11, 16, 17, 40, 41, 32, 33,
// 58,
// 59, 18, 19, 42, 43, 60, 61, 		            54, 55, 34, 35, 12, 13, 6,
// 7, 26, 27, 46, 47, 14, 15, 8, 9, 38, 39, 		            30, 31, 2,
// 3, 22, 23, 52, 53
//};
bool *isThreadActive;
long *parentId;
long *currentId;

int MaxNumActiveThreads;

// added by markos
BOOL mcdramSet = false;
BOOL searchMalloc = false;
BOOL mcdramMalloc = false;
BOOL ddrMalloc = false;
ADDRINT mcdramSize = 0;
ADDRINT ddrSize = 0;

#define PacketEpoch 50
uint32_t countPacket[MaxThreads];

#ifdef _WIN32
tejas_win::CRITICAL_SECTION *lockForWritingToCommunicationStream;
#else
CRITICAL_SECTION *lockForWritingToCommunicationStream;
#endif

void lockIAmWriting(int tid) {
#ifdef _WIN32
  tejas_win::EnterCriticalSection(&lockForWritingToCommunicationStream[tid]);
#else
  // pthread_mutex_lock(&lockForWritingToCommunicationStream[tid]);
#endif
}

void unlockIAmWriting(int tid) {
#ifdef _WIN32
  tejas_win::LeaveCriticalSection(&lockForWritingToCommunicationStream[tid]);
#else
  // pthread_mutex_unlock(&lockForWritingToCommunicationStream[tid]);
#endif
}

void waitForThreadsAndTerminatePin() {
  /*
    // Iterate over all the threads
    // If each thread is in non-alive status, terminate PIN
    for (int tid = 0; tid < MaxNumActiveThreads; tid++) {
  #ifdef _WIN32
      tejas_win::EnterCriticalSection(&lockForWritingToCommunicationStream[tid]);
  #else
      pthread_mutex_lock(&lockForWritingToCommunicationStream[tid]);
  #endif
    }
  */
  tst->unload();

  exit(0);
}

// needs -lrt (real-time lib)
// 1970-01-01 epoch UTC time, 1 nanosecond resolution

// modified for mac by shikhar

/*
#ifndef _WIN32  // OS X does not have clock_gettime, use clock_get_time
void current_utc_time(timespec *ts) {
#ifdef __MACH__
  clock_serv_t cclock;
  mach_timespec_t mts;
  host_get_clock_service(mach_host_self(), CALENDAR_CLOCK, &cclock);
  clock_get_time(cclock, &mts);
  mach_port_deallocate(mach_task_self(), cclock);
  ts->tv_sec = mts.tv_sec;
  ts->tv_nsec = mts.tv_nsec;
#else
  clock_gettime(CLOCK_REALTIME, ts);
#endif
}
#endif
*/

int time_ctr = 0;
uint64_t ClockGetTime() {
#ifdef _WIN32
  tejas_win::SYSTEMTIME st;
  tejas_win::GetSystemTime(&st);
  return st.wMilliseconds * 1000000;
#else
  //  struct timespec ts;
  //  current_utc_time(&ts);
  //  return (uint64_t)ts.tv_sec * 1000000000LL + (uint64_t)ts.tv_nsec;
  return time_ctr++;
#endif
}

// this compulsory is true if it is entering some function
// so that even if halts we have a timer packet here.
void sendTimerPacket(int tid, bool compulsory) {
  if ((countPacket[tid]++ % PacketEpoch) == 0 || compulsory) {
    PIN_MutexLock(&mainLockForPintool);
    checkSum += TIMER;
    PIN_MutexUnlock(&mainLockForPintool);

    countPacket[tid] = 0;
    uint64_t time = ClockGetTime();
    while (tst->analysisFn(tid, time, TIMER, 0) == -1) {
      PIN_Yield();
    }
  }
}
int findThreadMapping(unsigned int id) {
  int index;
  for (index = 0; index < MaxNumActiveThreads; index++) {
    if (threadMapping[index] == id) return index;
  }
  cout << "FATAL ERROR : ThreadMapping cannot resolve";
  fflush(stdout);
  exit(0);
}
#define cmp(a) (rtn_name->find(a) != string::npos)

bool isActive(int tid) { return pumpingStatus[tid]; }
void reActivate(int tid) {
  tid = findThreadMapping(tid);
  pumpingStatus[tid] = true;
  cout << "reAcivated " << tid << "\n";
  curSynchVar[tid] = 0;
}
void deActivate(int tid, ADDRINT addr) {
  tid = findThreadMapping(tid);
  curSynchVar[tid] = addr;
  cout << "deAcivated " << tid << "\n";
  pumpingStatus[tid] = false;
}
bool hasEntered(int tid, ADDRINT addr) {
  tid = findThreadMapping(tid);
  return (curSynchVar[tid] == addr);
}
int findParentSegment(long parent) {
  int index;
  for (index = 0; index < MaxNumActiveThreads; index++) {
    if (currentId[index] == parent) return index;
  }
  cout << "FATAL ERROR--- cannot find parent\n";
  return -1;
}

VOID ThreadStart(THREADID threadid, CONTEXT *ctxt, INT32 flags, VOID *v) {
  PIN_MutexLock(&mainLockForPintool);
  numThreads++;
  livethreads++;

  int i;
  for (i = 0; i < MaxNumActiveThreads; i++) {
    if (isThreadActive[i] == false) {
      isThreadActive[i] = true;
      break;
    }
  }
  threadMapping[i] = threadid;
  parentId[i] = PIN_GetParentTid();
  currentId[i] = PIN_GetTid();
  int parent = -1;
  if (parentId[i] != 0) {
    parent = findParentSegment(parentId[i]);
  }

  if (livethreads > MaxNumActiveThreads) {
    cout << "Number of live threads till now = " << livethreads << endl;
    cout << "Maximum number of active threads = " << MaxNumActiveThreads
         << " !!" << endl;
    cout << "PIN Exiting ..." << endl;
    exit(1);
  }

  threadAlive[threadid] = true;
  pumpingStatus[i] = true;
  threadid = findThreadMapping(threadid);
  cout << "threads till now " << numThreads
       << "; thread id (app) = " << threadid
       << "; core id (host) = " << get_cpu() << "\n";
  fflush(stdout);
  tst->onThread_start(threadid);
  while (tst->analysisFn(threadid, parent, CHILD_START, PIN_GetParentTid()) ==
         -1) {
    PIN_Yield();
  }
  if (parent != -1) {
    while (tst->analysisFn(parent, threadid, PARENT_SPAWN, PIN_GetTid()) ==
           -1) {
      PIN_Yield();
    }
  }
  PIN_MutexUnlock(&mainLockForPintool);
}

VOID ThreadFini(THREADID tid, const CONTEXT *ctxt, INT32 flags, VOID *v) {
  PIN_MutexLock(&mainLockForPintool);
  tid = findThreadMapping(tid);
  printf("thread %d finished exec\n", tid);
  fflush(stdout);
  while (tst->onThread_finish(tid, (numCISC[tid])) == -1) {
    PIN_Yield();
  }
  isThreadActive[tid] = false;
  cout << "wrote -1 for tid " << tid << "\n";
  livethreads--;
  threadAlive[tid] = false;
  fflush(stdout);
  PIN_MutexUnlock(&mainLockForPintool);
}

// Pass a memory read record
VOID RecordMemRead(THREADID tid, VOID *ip, VOID *addr) {
  tid = findThreadMapping(tid);
  if (!isActive(tid)) return;

  if (instructionIgnorePhase) return;

  if (outOfROIPhase) return;

  sendTimerPacket(tid, false);

  uint64_t nip = MASK & (uint64_t)ip;
  uint64_t naddr = MASK & (uint64_t)addr;

  PIN_MutexLock(&mainLockForPintool);
  checkSum += MEMREAD;
  if ((uint64_t)naddr >= start_addr) {
    memfile.open(MEM_PATH, ofstream::out | ofstream::app);
    // memfile << tid << "\t" << (uint64_t)naddr << "\n";
    memfile.close();
  }

  PIN_MutexUnlock(&mainLockForPintool);

  while (tst->analysisFn(tid, nip, MEMREAD, naddr) == -1) {
    PIN_Yield();
  }
}

// Pass a memory write record
VOID RecordMemWrite(THREADID tid, VOID *ip, VOID *addr) {
  tid = findThreadMapping(tid);
  if (!isActive(tid)) return;
  if (instructionIgnorePhase) return;

  if (outOfROIPhase) return;

  sendTimerPacket(tid, false);

  PIN_MutexLock(&mainLockForPintool);
  checkSum += MEMWRITE;
  PIN_MutexUnlock(&mainLockForPintool);

  uint64_t nip = MASK & (uint64_t)ip;
  uint64_t naddr = MASK & (uint64_t)addr;
  while (tst->analysisFn(tid, nip, MEMWRITE, naddr) == -1) {
    PIN_Yield();
  }
}

VOID BrnFun(THREADID tid, ADDRINT tadr, BOOL taken, VOID *ip) {
  tid = findThreadMapping(tid);
  if (!isActive(tid)) return;

  if (instructionIgnorePhase) return;

  if (outOfROIPhase) return;

  sendTimerPacket(tid, false);

  uint64_t nip = MASK & (uint64_t)ip;
  uint64_t ntadr = MASK & (uint64_t)tadr;
  if (taken) {
    PIN_MutexLock(&mainLockForPintool);
    checkSum += TAKEN;
    PIN_MutexUnlock(&mainLockForPintool);

    while (tst->analysisFn(tid, nip, TAKEN, ntadr) == -1) {
      PIN_Yield();
    }
  } else {
    PIN_MutexLock(&mainLockForPintool);
    checkSum += NOTTAKEN;
    PIN_MutexUnlock(&mainLockForPintool);
    while (tst->analysisFn(tid, nip, NOTTAKEN, ntadr) == -1) {
      PIN_Yield();
    }
  }
}
VOID RegValRead(THREADID tid, VOID *ip, REG *_reg) {
  if (instructionIgnorePhase) return;

  if (outOfROIPhase) return;
  checkSum += 6;
  uint64_t nip = MASK & (uint64_t)ip;
  uint64_t _nreg = MASK & (uint64_t)_reg;
  tid = findThreadMapping(tid);
  while (tst->analysisFn(tid, nip, REGREAD, _nreg) == -1) {
    PIN_Yield();
  }
}

VOID RegValWrite(THREADID tid, VOID *ip, REG *_reg) {
  if (instructionIgnorePhase) return;

  if (outOfROIPhase) return;
  tid = findThreadMapping(tid);
  checkSum += 7;
  uint64_t nip = MASK & (uint64_t)ip;
  uint64_t _nreg = MASK & (uint64_t)_reg;
  while (tst->analysisFn(tid, nip, REGWRITE, _nreg) == -1) {
    PIN_Yield();
  }
}
VOID CountIns() {
  if (!instructionIgnorePhase) return;
  numIns++;
  if (numIns > numInsToIgnore) instructionIgnorePhase = false;  // activate Now
}

VOID FunStartInstrumentation() {
  outOfROIPhase = false;
  numInsToSimulate = KnobSimulate;
  cout << "at function " << startMarker << " : beginning instrumentation"
       << endl;
  cout << "outOfROIPhase = " << outOfROIPhase << endl;
  cout << "numSim = " << totalNumCISC << endl;
  fflush(stdout);
}

VOID FunEndInstrumentation() {
  outOfROIPhase = true;
  cout << "at function " << endMarker << " : stopping instrumentation" << endl;
  cout << "outOfROIPhase = " << outOfROIPhase << endl;
  cout << "numSim = " << totalNumCISC << endl;
  fflush(stdout);
}

VOID FunEntry(ADDRINT first_arg, UINT32 encode, THREADID tid) {
  uint64_t time = ClockGetTime();
  tid = findThreadMapping(tid);
  sendTimerPacket(tid, true);

  PIN_MutexLock(&mainLockForPintool);
  checkSum += encode;
  PIN_MutexUnlock(&mainLockForPintool);

  uint64_t uarg = MASK & (uint64_t)first_arg;
  while (tst->analysisFn(tid, time, encode, uarg) == -1) {
    PIN_Yield();
  }
}

VOID FunExit(ADDRINT first_arg, UINT32 encode, THREADID tid) {
  uint64_t time = ClockGetTime();

  tid = findThreadMapping(tid);
  sendTimerPacket(tid, false);

  PIN_MutexLock(&mainLockForPintool);
  checkSum += encode;
  PIN_MutexUnlock(&mainLockForPintool);

  uint64_t uarg = MASK & (uint64_t)first_arg;
  while (tst->analysisFn(tid, time, encode, uarg) == -1) {
    PIN_Yield();
  }
}
/*** Called on the initialization of a barrier  ***/
VOID BarrierInit(ADDRINT first_arg, ADDRINT val, UINT32 encode, THREADID tid) {
  PIN_MutexLock(&mainLockForPintool);
  checkSum += encode;
  PIN_MutexUnlock(&mainLockForPintool);
  tid = threadMapping[tid];
  uint64_t uarg = MASK & (uint64_t)first_arg;
  uint64_t value = MASK & (uint64_t)val;
  while (tst->analysisFn(tid, value, encode, uarg) == -1) {
    PIN_Yield();
  }
}
/*** This function is called on every instruction ***/
VOID printip(THREADID tid, VOID *ip, char *asmString) {
  if (outOfROIPhase) {
    return;
  }

  tid = findThreadMapping(tid);
  PIN_MutexLock(&mainLockForPintool);
  //	if(ignoreActive == false) {
  numCISC[tid]++;
  totalNumCISC++;
  //	}

  if (pinpointsFilename.compare("nofile") == 0) {
    if (totalNumCISC >= numInsToIgnore) {
      if (numInsToSimulate < 0 ||
          totalNumCISC < numInsToIgnore + numInsToSimulate) {
        instructionIgnorePhase = false;
      } else {
        instructionIgnorePhase = true;
      }
    } else {
      instructionIgnorePhase = true;
    }

    if (numInsToSimulate > 0 &&
        totalNumCISC >= (numInsToIgnore + numInsToSimulate)) {
      // Now, we will write -2 packet in shared memory.
      // This will ensure that complete emulator (PIN) gets stopped.
      while (tst->onSubset_finish((int)tid, (numCISC[tid])) == -1) {
        PIN_Yield();
      }

      cout << "subset finish called by thread " << tid << endl;
      fflush(stdout);

      tst->setSubsetsimComplete(true);
      // threadAlive[tid] = false;
      waitForThreadsAndTerminatePin();
    }
  } else {
    if (totalNumCISC >= sliceArray[currentSlice] * 3000000) {
      if (totalNumCISC <= (sliceArray[currentSlice] + 1) * 3000000) {
        instructionIgnorePhase = false;
      } else {
        instructionIgnorePhase = true;
        cout << "completed slice : " << currentSlice
             << "\t\ttotalNumCisc = " << totalNumCISC << "\n";
        cout << totalNumCISC << "\t\t"
             << (sliceArray[numberOfSlices - 1] + 1) * 3000000 << "\t\t"
             << numberOfSlices << "\n";
        currentSlice++;
      }
    } else {
      instructionIgnorePhase = true;
    }

    if (totalNumCISC > (sliceArray[numberOfSlices - 1] + 1) * 3000000) {
      for (int i = 0; i < MaxThreads; i++) {
        if (threadAlive[i] == true) {
          int tid_1 = i;
          cout << "attempting to write -1\n";
          while (tst->onThread_finish(tid_1, (numCISC[tid_1])) == -1) {
            PIN_Yield();
          }
          cout << "wrote -1 for tid " << tid_1 << "\n";
          livethreads--;
          threadAlive[tid_1] = false;
          fflush(stdout);
        }
      }

      if (livethreads == 0) {
        cout << "subset simulation complete\n";
        fflush(stdout);
        tst->unload();
        exit(0);
      }

      ASSERT(livethreads != 0,
             "subset sim complete, but live threads not zero!!!\n");
    }
  }

  if (instructionIgnorePhase == false) {
    // For every instruction, I am sending one Instruction packet to Tejas.
    // For rep instruction, this function is called for each iteration of rep.
    uint64_t nip = MASK & (uint64_t)ip;

    if (traceMethod == SharedMemory) {
      while (tst->analysisFn(tid, nip, INSTRUCTION, 1) == -1) {
        PIN_Yield();
      }
    } else if (traceMethod == File) {
      while (tst->analysisFnAssembly(tid, nip, ASSEMBLY, asmString) == -1) {
        PIN_Yield();
      }
    }
  }

  if (numCISC[tid] % 1000000 == 0 && numCISC[tid] > 0) {
    cout << "numCISC on thread " << tid << " = " << numCISC[tid]
         << " ignoreActive = " << instructionIgnorePhase << "\n";
    fflush(stdout);
  }

  if (totalNumCISC % 1000000 == 0 && totalNumCISC > 0) {
    cout << "totalNumCISC = " << totalNumCISC
         << " ignoreActive = " << instructionIgnorePhase << "\n";
    fflush(stdout);
  }
  PIN_MutexUnlock(&mainLockForPintool);
}

VOID funcHandler(CHAR *name, int a, int b, int c) {
  cout << "function encountered\n ";
  cout << "numSim = " << totalNumCISC << "\n";
}

/* ===================================================================== */
/* MALLOC Analysis routines                                              */
/* ===================================================================== */

VOID ResetStats(char *name, ADDRINT size) {
  ofstream addrfile;
  addrfile.open(ADDR_PATH, ofstream::out | ofstream::app);
  if (addrfile.is_open()) {
    addrfile << "RESET_STATS" << endl;
    addrfile.close();
  }
}

VOID DumpStats(char *name, ADDRINT size) {
  ofstream addrfile;
  addrfile.open(ADDR_PATH, ofstream::out | ofstream::app);
  if (addrfile.is_open()) {
    addrfile << "DUMP_STATS" << endl;
    addrfile.close();
  }
}

VOID SetMCmalloc(char *name, ADDRINT size) {
  searchMalloc = true;
  mcdramMalloc = true;
  mcdramSize = size;
}

VOID SetDRAMmalloc(char *name, ADDRINT size) {
  searchMalloc = true;
  ddrMalloc = true;
  ddrSize = size;
}

VOID MallocBefore(CHAR *name, ADDRINT size) {
  if (searchMalloc) cout << name << "(" << size << ")" << endl;
}

VOID MallocAfter(ADDRINT ret) {
  ofstream addrfile;
  addrfile.open(ADDR_PATH, ofstream::out | ofstream::app);
  if (searchMalloc) {
    if (ddrMalloc) {
      cout << "ddrMalloc returns " << ret << endl;
      ddrMalloc = false;
      if (addrfile.is_open())
        addrfile << "DDR\t" << ddrSize << "\t";
      else
        cout << "unable to open file!" << endl;
    }
    if (mcdramMalloc) {
      cout << "mcdramMalloc returns " << ret << endl;
      mcdramMalloc = false;
      if (addrfile.is_open()) {
        addrfile << "MCDRAM\t" << mcdramSize << "\t";
        // mcdramSet = true;
      }
    }
    searchMalloc = false;
  } else {
    addrfile.close();
    return;
  }

  if (addrfile.is_open()) {
    // ADDRESS VALUE
    addrfile << ret << endl;
    addrfile.close();
    cout << "closing file..." << endl;
  }
}

VOID KNLbefore(CHAR *name, ADDRINT ptr, ADDRINT size) {
  cout << "knl_alloc_data found with size " << size << endl;
  knlAllocSize = size;
}

VOID KNLafter(ADDRINT ret) {
  ofstream addrfile;
  start_addr = ret;
  cout << "knl_alloc_data returns " << ret << endl;
  addrfile.open(ADDR_PATH, ofstream::out | ofstream::app);
  if (addrfile.is_open()) {
    // int npages = knlAllocSize / 4096;
    int nlines = knlAllocSize / 64;
    int n;
    uint64_t base = ret;
    uint64_t phys = vtop(base);
    uint64_t virt = base;
    for (n = 0; n < nlines; ++n) {
      phys = vtop(base);
      virt = base;
      addrfile << knlAllocSize << "\t" << virt << "\t" << phys << endl;
      // base += 4096;
      base += 64;
    }
    cout << knlAllocSize << "\t" << virt << "\t" << phys << endl;
    addrfile.close();
  }
}

void Image(IMG img, VOID *v) {
  RTN funcRtn = RTN_FindByName(img, "__parsec_roi_begin");
  if (RTN_Valid(funcRtn)) {
    RTN_Open(funcRtn);
    RTN_InsertCall(funcRtn, IPOINT_BEFORE, (AFUNPTR)funcHandler, IARG_ADDRINT,
                   "funcA", IARG_FUNCARG_ENTRYPOINT_VALUE, 0, IARG_END);
    RTN_Close(funcRtn);
  }
  funcRtn = RTN_FindByName(img, "__parsec_roi_end");
  if (RTN_Valid(funcRtn)) {
    RTN_Open(funcRtn);
    RTN_InsertCall(funcRtn, IPOINT_BEFORE, (AFUNPTR)funcHandler, IARG_ADDRINT,
                   "funcA", IARG_FUNCARG_ENTRYPOINT_VALUE, 0, IARG_END);
    RTN_Close(funcRtn);
  }

  //  // Instrument the malloc() and free() functions.  Print the input argument
  //  // of each malloc() or free(), and the return value of malloc().
  //  //
  //  //  Find the malloc() function.
  //  RTN mallocRtn = RTN_FindByName(img, "malloc");
  //  if (RTN_Valid(mallocRtn)) {
  //    RTN_Open(mallocRtn);
  //
  //    // Instrument malloc() to print the input argument value and the return
  //    // value. RTN_InsertCall(mallocRtn, IPOINT_BEFORE,
  //    (AFUNPTR)MallocBefore,
  //    // 		   IARG_ADDRINT, MALLOC,
  //    // 		   IARG_FUNCARG_ENTRYPOINT_VALUE, 0,
  //    // 		   IARG_END);
  //    RTN_InsertCall(mallocRtn, IPOINT_AFTER, (AFUNPTR)MallocAfter,
  //                   IARG_FUNCRET_EXITPOINT_VALUE, IARG_END);
  //
  //    RTN_Close(mallocRtn);
  //    searchMalloc = false;
  //    mcdramMalloc = false;
  //    ddrMalloc = false;
  //  }

  // RTN_Name(rtn)

  // // Find the free() function.
  // RTN freeRtn = RTN_FindByName(img, FREE);
  // if (RTN_Valid(freeRtn))
  //   {
  //     RTN_Open(freeRtn);
  //     // Instrument free() to print the input argument value.
  //     RTN_InsertCall(freeRtn, IPOINT_BEFORE, (AFUNPTR)Arg1Before,
  // 		   IARG_ADDRINT, FREE,
  // 		   IARG_FUNCARG_ENTRYPOINT_VALUE, 0,
  // 		   IARG_END);
  //     RTN_Close(freeRtn);
  //   }
}

// Pin calls this function every time a new instruction is encountered
VOID Instruction(INS ins, VOID *v) {
  // int tid = IARG_THREAD_ID;

  char *asmChar = NULL;
  if (traceMethod == File) {
    std::string *asmString = (std::string *)&Instruction;
    asmString = new string(INS_Disassemble(ins));
    asmChar = (char *)asmString->c_str();
  }

  INS_InsertCall(ins, IPOINT_BEFORE, (AFUNPTR)printip, IARG_THREAD_ID,
                 IARG_INST_PTR, IARG_PTR, asmChar, IARG_END);

  UINT32 memOperands = INS_MemoryOperandCount(ins);

  if (INS_IsBranchOrCall(ins))  // INS_IsIndirectBranchOrCall(ins))
  {
    INS_InsertCall(ins, IPOINT_BEFORE, (AFUNPTR)BrnFun, IARG_THREAD_ID,
                   IARG_BRANCH_TARGET_ADDR, IARG_BRANCH_TAKEN, IARG_INST_PTR,
                   IARG_END);
  }

  // Iterate over each memory operand of the instruction.
  for (UINT32 memOp = 0; memOp < memOperands; memOp++) {
    if (INS_MemoryOperandIsRead(ins, memOp)) {
      INS_InsertPredicatedCall(ins, IPOINT_BEFORE, (AFUNPTR)RecordMemRead,
                               IARG_THREAD_ID, IARG_INST_PTR, IARG_MEMORYOP_EA,
                               memOp, IARG_END);
    }
    // Note that in some architectures a single memory operand can be
    // both read and written (for instance incl (%eax) on IA-32)
    // In that case we instrument it once for read and once for write.
    if (INS_MemoryOperandIsWritten(ins, memOp)) {
      INS_InsertPredicatedCall(ins, IPOINT_BEFORE, (AFUNPTR)RecordMemWrite,
                               IARG_THREAD_ID, IARG_INST_PTR, IARG_MEMORYOP_EA,
                               memOp, IARG_END);
    }
  }
}

// if (RTN_Valid(rtn) && RtnMatchesName(RTN_Name(rtn), name))

// This is a routine level instrumentation
VOID FlagRtn(RTN rtn, VOID *v) {
  RTN_Open(rtn);
  const string *rtn_name = new string(RTN_Name(rtn));
  INT32 encode;

  if ((cmp("knl_alloc_data")) && (RTN_Valid(rtn))) {
    // Instrument malloc() to print the input argument value and the return
    // value.
    RTN_InsertCall(rtn, IPOINT_BEFORE, (AFUNPTR)KNLbefore, IARG_PTR,
                   "knl_alloc_data", IARG_FUNCARG_ENTRYPOINT_VALUE, 0,
                   IARG_FUNCARG_ENTRYPOINT_VALUE, 1, IARG_END);
    RTN_InsertCall(rtn, IPOINT_AFTER, (AFUNPTR)KNLafter,
                   IARG_FUNCRET_EXITPOINT_VALUE, IARG_END);

    RTN_Close(rtn);
    return;
  }

  //  if ((cmp("mc_malloc")) && (RTN_Valid(rtn))) {
  //    // Instrument malloc() to print the input argument value and the return
  //    // value.
  //    RTN_InsertCall(rtn, IPOINT_BEFORE, (AFUNPTR)SetMCmalloc, IARG_PTR,
  //                   "mc_malloc", IARG_FUNCARG_ENTRYPOINT_VALUE, 0, IARG_END);
  //    RTN_InsertCall(rtn, IPOINT_AFTER, (AFUNPTR)MallocAfter,
  //                   IARG_FUNCRET_EXITPOINT_VALUE, IARG_END);
  //
  //    RTN_Close(rtn);
  //    return;
  //  }

  if ((cmp("ddr_malloc")) && (RTN_Valid(rtn))) {
    // Instrument malloc() to print the input argument value and the return
    // value.
    RTN_InsertCall(rtn, IPOINT_BEFORE, (AFUNPTR)SetDRAMmalloc, IARG_PTR,
                   "ddr_malloc", IARG_FUNCARG_ENTRYPOINT_VALUE, 0, IARG_END);
    RTN_Close(rtn);
    return;
  }

  if (cmp("pthread_cond_broadcast"))
    encode = BCAST;
  else if (cmp("pthread_cond_signal"))
    encode = SIGNAL;
  else if (cmp("pthread_mutex_lock"))
    encode = LOCK;
  else if (cmp("pthread_mutex_unlock_"))
    encode = UNLOCK;  // pthread_mutex_unlock is just a wrapper
  else if (cmp("pthread_join"))
    encode = JOIN;
  else if (cmp("pthread_cond_wait"))
    encode = CONDWAIT;
  /*** For barriers. Used for research purpose ***/
  else if (cmp("pthread_barrier_wait")) {
    encode = BARRIERWAIT;
  } else if (cmp("parsec_barrier_wait"))
    encode = BARRIERWAIT;
  else if (cmp("pthread_barrier_init")) {
    encode = BARRIERINIT;
  }

  else {
    encode = -1;
    if (startMarker.compare("") != 0) {
      if (startMarker.compare(*rtn_name) == 0) {
        RTN_InsertCall(rtn, IPOINT_AFTER, (AFUNPTR)FunStartInstrumentation,
                       IARG_FUNCARG_ENTRYPOINT_VALUE, 0, IARG_END);
      }
    }
    if (endMarker.compare("") != 0) {
      if (endMarker.compare(*rtn_name) == 0) {
        RTN_InsertCall(rtn, IPOINT_BEFORE, (AFUNPTR)FunEndInstrumentation,
                       IARG_FUNCARG_ENTRYPOINT_VALUE, 0, IARG_END);
      }
    }
  }

  /*** For barriers. Used for research purpose ***/
  if (encode != -1 && RTN_Valid(rtn) && encode != BARRIERINIT) {
    RTN_InsertCall(rtn, IPOINT_BEFORE, (AFUNPTR)FunEntry,
                   IARG_FUNCARG_ENTRYPOINT_VALUE, 0, IARG_UINT32, encode,
                   IARG_THREAD_ID, IARG_END);

    RTN_InsertCall(rtn, IPOINT_AFTER, (AFUNPTR)FunExit,
                   IARG_FUNCARG_ENTRYPOINT_VALUE, 0, IARG_UINT32, encode + 1,
                   IARG_THREAD_ID, IARG_END);

  } else if (encode != -1 && RTN_Valid(rtn)) {
    RTN_InsertCall(rtn, IPOINT_BEFORE, (AFUNPTR)BarrierInit,
                   IARG_FUNCARG_ENTRYPOINT_VALUE, 0,
                   IARG_FUNCARG_ENTRYPOINT_VALUE, 2, IARG_UINT32, encode,
                   IARG_THREAD_ID, IARG_END);
  }
  RTN_Close(rtn);
}

// This function is called when the application exits
VOID Fini(INT32 code, VOID *v) {
  cout << "checkSum is " << checkSum << "\n";
  fflush(stdout);

  tst->setSubsetsimComplete(true);
  // Now, we will write -2 packet in shared memory.
  // This will ensure that complete emulator (PIN) gets stopped.

  // FIXME : We are trying to write in the communication stream for thread 0
  // Hopefully this function is called for the master thread i.e. thread 0
  while (tst->onSubset_finish((int)0, (numCISC[0])) == -1) {
    PIN_Yield();
  }
  cout << "subset finish called by thread " << 0 << endl;
  fflush(stdout);

  waitForThreadsAndTerminatePin();
}

/* ===================================================================== */
/* Print Help Message                                                    */
/* ===================================================================== */

INT32 Usage() {
  cerr << "This tool instruments the benchmarks" << endl;
  cerr << endl << KNOB_BASE::StringKnobSummary() << endl;
  return -1;
}

/* ===================================================================== */
/* Main                                                                  */
/* ===================================================================== */

// argc, argv are the entire command line, including pin -t <toolname> -- ...
int main(int argc, char *argv[]) {
  ofstream addrfile;
  addrfile.open(ADDR_PATH, ofstream::out | ofstream::trunc);
  addrfile.close();

  ofstream memfile;
  memfile.open(MEM_PATH, ofstream::out | ofstream::trunc);
  memfile.close();

  // Knobs get initialized only after initializing PIN

  // if (numInsToIgnore>0)
  instructionIgnorePhase = true;
  outOfROIPhase = true;
  /*UINT64 mask = KnobMap;

  if (sched_setaffinity(0, sizeof(mask), (cpu_set_t *)&mask) <0) {
          perror("sched_setaffinity");
  }*/

  PIN_InitSymbols();
  // Initialize pin
  if (PIN_Init(argc, argv)) return Usage();

  std::string traceMethodStr = KnobTraceMethod;
  if (strcmp(traceMethodStr.c_str(), "sharedMemory") == 0) {
    traceMethod = SharedMemory;
  } else if (strcmp(traceMethodStr.c_str(), "file") == 0) {
    traceMethod = File;
  } else {
    printf("Invalid trace method : %s !!\n", traceMethodStr.c_str());
    exit(1);
  }

  MaxNumActiveThreads = KnobMaxNumActiveThreads;

  threadMapping = new uint32_t[MaxNumActiveThreads];
  isThreadActive = new bool[MaxNumActiveThreads];
  parentId = new long[MaxNumActiveThreads];
  currentId = new long[MaxNumActiveThreads];
  int index;
  for (index = 0; index < MaxNumActiveThreads; index++) {
    parentId[index] = -1;
    currentId[index] = -1;
    isThreadActive[index] = false;
  }

  numInsToIgnore = KnobIgnore;
  startMarker = KnobStartMarker;
  endMarker = KnobEndMarker;
  numInsToSimulate = KnobSimulate;
  pinpointsFilename = KnobPinPointsFile;
  UINT64 id = KnobId;

  if (startMarker.compare("") != 0) {
    outOfROIPhase = true;
  } else {
    outOfROIPhase = false;
  }

  cout << "numIgn = " << numInsToIgnore << endl;
  cout << "numSim = " << numInsToSimulate << endl;
  cout << "id received = " << id << endl;
  cout << "pinpoints file received = " << pinpointsFilename << endl;
  cout << "maxNumActiveThreads = " << MaxNumActiveThreads << "\n";
  cout << "start marker = " << startMarker << endl;
  cout << "end marker = " << endMarker << endl;
  cout << "outOfROIPhase = " << outOfROIPhase << endl;

  // lockForWritingToCommunicationStream =
  //    new pthread_mutex_t[MaxNumActiveThreads];
  for (int i = 0; i < MaxNumActiveThreads; i++) {
    threadAlive[i] = false;
    // pthread_mutex_init(&lockForWritingToCommunicationStream[i], NULL);
  }

  if (pinpointsFilename.compare("nofile") != 0) {
    ifstream pinpointsFile;
    std::string line;
    numberOfSlices = 0;
    pinpointsFile.open(pinpointsFilename.c_str(), ios::in);
    while (pinpointsFile.good()) {
      getline(pinpointsFile, line);
      numberOfSlices++;
    }
    pinpointsFile.close();

    numberOfSlices--;  // required because of the way good() works
    sliceArray = new unsigned long[numberOfSlices];
    int sliceArrayIndex = 0;

    pinpointsFile.open(pinpointsFilename.c_str(), ios::in);
    while (pinpointsFile.good()) {
      getline(pinpointsFile, line);
      std::string temp;
      int index = 0;
      if (line.length() != 0) {
        while (line.at(index) != ' ') {
          temp.append(1, line.at(index++));
        }
        sliceArray[sliceArrayIndex++] = strtol(temp.c_str(), NULL, 10);
      }
    }
    pinpointsFile.close();
  }

  // if (traceMethod == SharedMemory) {
  tst = new IPC::Shm(id, MaxNumActiveThreads);
  /*  tst = new IPC::Shm(id, MaxNumActiveThreads,
                       &lockIAmWriting, &unlockIAmWriting);*/
  //  } else if (traceMethod == File) {
  //    std::string tmp = KnobTraceFileName;
  //    if (tmp.empty()) {
  //      printf(
  //          "Must provide a base name for the trace file using -traceFileName
  //          " "option");
  //      exit(1);
  //    }
  //    tst = new IPC::FilePacket(MaxNumActiveThreads, tmp.c_str(),
  //    &lockIAmWriting,
  //                              &unlockIAmWriting);
  //  } else {
  //    printf("Invalid trace method : %s !!\n", traceMethodStr.c_str());
  //    exit(1);
  //  }

  for (int i = 0; i < MaxThreads; i++) {
    numCISC[i] = 0;
    threadAlive[i] = false;
  }
  totalNumCISC = 0;

  PIN_AddThreadStartFunction(ThreadStart, 0);

  // Register Instruction to be called to instrument instructions
  INS_AddInstrumentFunction(Instruction, 0);

  IMG_AddInstrumentFunction(Image, 0);
  // Register ThreadFini to be called when a thread exits
  PIN_AddThreadFiniFunction(ThreadFini, 0);

  // Register FlagRtn whenever you get a routine
  RTN_AddInstrumentFunction(FlagRtn, 0);

  // Register Fini to be called when the application exits
  PIN_AddFiniFunction(Fini, 0);

  // Start the program, never returns
  PIN_StartProgram();

  return 0;
}

const char *findType(int type) {
  switch (type) {
    case (MEMREAD):
      return "MEMREAD";
    case (MEMWRITE):
      return "MEMWRITE";
    case (TAKEN):
      return "TAKEN";
    case (NOTTAKEN):
      return "NOTTAKEN";
    case (REGREAD):
      return "REGREAD";
    case (REGWRITE):
      return "REGWRITE";
    case (BCAST):
      return "BCAST ENTER";
    case (BCAST + 1):
      return "BCAST EXIT";
    case (SIGNAL):
      return "SIGNAL ENTER";
    case (SIGNAL + 1):
      return "SIGNAL EXIT";
    case (LOCK):
      return "LOCK ENTER";
    case (LOCK + 1):
      return "LOCK EXIT";
    case (UNLOCK):
      return "UNLOCK ENTER";
    case (UNLOCK + 1):
      return "UNLOCK EXIT";
    case (JOIN):
      return "JOIN ENTER";
    case (JOIN + 1):
      return "JOIN EXIT";
    case (CONDWAIT):
      return "WAIT ENTER";
    case (CONDWAIT + 1):
      return "WAIT EXIT";
    case (BARRIERWAIT):
      return "BARRIER ENTER";
    case (BARRIERWAIT + 1):
      return "BARRIER EXIT";
    case (TIMER):
      return "Timer packet";
    case (BARRIERINIT):
      return "BARRIER INIT";
    default:
      return "ADD THIS IN encoding.h";
  }
}
