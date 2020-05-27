/*****************************************************************************
 * Tejas Simulator
 * ------------------------------------------------------------------------------------------------------------
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * Contributors: Moksh Upadhyay
 *****************************************************************************/
package config;

import generic.PortType;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;
import memorysystem.coherence.Coherence;
import memorysystem.coherence.Directory;

public class SystemConfig {
  public static enum Interconnect { Bus, Noc }

  public static enum ClusterMode { SNC, Quad, Hemi, A2A }

  public static enum MemoryMode { Flat, Cache }

  public static enum Affinity { Colocated, Scatter, Linear }

  public static int NoOfCores;
  public static int nTiles;

  public static int maxNumJavaThreads;
  public static int numEmuThreadsPerJavaThread;

  public static CoreConfig[] core;
  public static Vector<CacheConfig> sharedCacheConfigs = new Vector<CacheConfig>();

  // added later kush
  public static boolean memControllerToUse;

  // added by markos
  public static boolean knl;

  public static int mainMemoryLatency;
  public static long mainMemoryFrequency;
  public static PortType mainMemPortType;
  public static int mainMemoryAccessPorts;
  public static int mainMemoryPortOccupancy;
  public static int cacheBusLatency;
  public static String coherenceEnforcingCache;
  public static BusConfig busConfig;
  public static NocConfig nocConfig;
  public static EnergyConfig busEnergy;

  public static int mcdramLatency;
  public static long mcdramFrequency;
  public static PortType mcdramPortType;
  public static int mcdramAccessPorts;
  public static int mcdramPortOccupancy;

  public static Interconnect interconnect;
  public static ClusterMode clusterMode;
  public static MemoryMode memoryMode;
  public static Affinity coreAffinityMode;
  public static EnergyConfig mainMemoryControllerPower;
  public static EnergyConfig mcdramControllerPower;
  public static EnergyConfig globalClockPower;

  // added by markos
  public static long mcdramAddr = -1;
  public static long mcdramSize = 0; // todo
  public static long ddrAddr = -1;
  public static long ddrSize = 0; // todo

  public static long mcdramPhysStartAddr = 0x3040000000L;
  public static HashMap<Long, Long> physAddr = new HashMap<Long, Long>();

  public static Vector<Coherence> chaList = new Vector<Coherence>();

  // public static int[] mappingSNC = {0,1,4,5,6,10,11,12,16,
  // 2,3,7,8,9,13,14,15,19,
  // 17,20,21,22,26,27,28,32,33,
  // 18,23,24,25,29,30,31,34,35};

  // public static int[] mappingQuad = {0,1,2,6,7,8,12,13,14,
  // 3,4,5,9,10,11,15,16,17,
  // 18,19,20,24,25,26,30,31,32,
  // 21,22,23,27,28,29,33,34,35};

  public static int[][] mapCoresQuad = {
      {0, 1, 20, 21, 28, 29, 36, 37, 44, 45, 50, 51, 56, 57, 62, 63},
      {2, 3, 8, 9, 14, 15, 22, 23, 30, 31, 38, 39, 46, 47, 52, 53},
      {4, 5, 10, 11, 16, 17, 24, 25, 32, 33, 40, 41, 48, 49, 58, 59},
      {6, 7, 12, 13, 18, 19, 26, 27, 34, 35, 42, 43, 54, 55, 60, 61}};

  // 32 cores version
  public static int[] mappingQuad = {0, 1, 2, 6, 7, 8, 12, 13, 14, 3, 4, 5, 9, 10, 11, 15, 16, 17,
      18, 19, 20, 24, 25, 26, 30, 31, 21, 22, 23, 27, 28, 29};

  public static int[] mappingHemi = {0, 1, 2, 6, 7, 8, 12, 13, 14, 18, 19, 20, 24, 25, 26, 30, 31,
      32, 3, 4, 5, 9, 10, 11, 15, 16, 17, 21, 22, 23, 27, 28, 29, 33, 34, 35};
  public static int[] mappingA2A = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17,
      18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35};

  public static int[] mappingCHA = {0, 12, 13, 29, 4, 16, 28, 1, 17, 33, 8, 20, 32, 5, 21, 37, 24,
      36, 9, 25, 2, 14, 26, 3, 15, 27, 6, 18, 30, 7, 19, 31, 10, 22, 34, 11, 23, 35};
  public static int[] mappingCores = {0, 20, 22, 52, -1, 28, 50, 2, 30, -1, -1, 36, 56, 8, 38, -1,
      44, 62, 14, 46, 4, 24, 48, 6, 26, -1, 10, 32, -1, 12, 34, 54, 16, 40, 58, 18, 42, 60};

  /* core affinities */
  public static int[] coreAffinityLinear = {0, 1, 20, 21, 22, 23, 52, 53, 28, 29, 50, 51, 2, 3, 30,
      31, 36, 37, 56, 57, 8, 9, 38, 39, 44, 45, 62, 63, 14, 15, 46, 47, 4, 5, 24, 25, 48, 49, 6, 7,
      26, 27, 10, 11, 32, 33, 12, 13, 34, 35, 54, 55, 16, 17, 40, 41, 58, 59, 18, 19, 42, 43, 60,
      61};

  public static int[] coreAffinityColocated = {0, 1, 20, 21, 28, 29, 50, 51, 56, 57, 36, 37, 44, 45,
      62, 63, 48, 49, 24, 25, 4, 5, 10, 11, 16, 17, 40, 41, 32, 33, 58, 59, 18, 19, 42, 43, 60, 61,
      54, 55, 34, 35, 12, 13, 6, 7, 26, 27, 46, 47, 14, 15, 8, 9, 38, 39, 30, 31, 2, 3, 22, 23, 52,
      53};

  public static int[] coreAffinityScatter = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
      16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38,
      39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61,
      62, 63};

  public static int[] coreAffinity = coreAffinityScatter;

  public static int[] coreLayout = coreAffinityLinear;

  public static String addrFilePath = "/home/mhorro/tejas-git/Tejas-Simulator/Tejas/addr.txt";
  public static String mapFilePath = "/home/mhorro/mapping.knl";
  public static byte[] mappingKNL;

  public static Directory globalDir; // the Big Brother, ugly but
                                     // functional implementation...

  public static MainMemoryConfig mainMemoryConfig;
  public static MainMemoryConfig mcdramConfig;

  public static boolean dynSched = false;
  public static long dataHops = 0;
  public static long controlHops = 0;

  public static void setMCDRAMaddr(long addr) {
    System.out.println("[JAVA] MCDRAMaddr = " + addr);
    SystemConfig.mcdramAddr = addr;
  }

  public static void setMCDRAMsize(long size) {
    System.out.println("[JAVA] MCDRAMsize = " + size);
    SystemConfig.mcdramSize = size;
  }

  public static void setDDRaddr(long addr) {
    SystemConfig.ddrAddr = addr;
  }

  public static void setDDRsize(long size) {
    SystemConfig.ddrSize = size;
  }
}
