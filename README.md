# [Tejas Intel KNL - Simulating the Network Activity of Modern Manycores (IEEE Access paper)](https://ieeexplore.ieee.org/document/8740875)

Manycore architectures are one of the most promising candidates to reach the exascale. However, the increase in the number of cores on a single die exacerbates the memory wall problem. Modern manycore architectures integrate increasingly complex and heterogeneous memory systems to work around the memory bottleneck while increasing computational power. The Intel Mesh Interconnect architecture is the latest interconnect designed by Intel for its HPC product lines. Processors are organized in a rectangular network-on-chip (NoC), connected to several different memory interfaces, and using a distributed directory to guarantee coherent memory accesses. Since the traffic on the NoC is completely opaque to the programmer, simulation tools are needed to understand the performance trade-offs of code optimizations. Recently featured in Intel's Xeon Scalable lines, this interconnect was first included in the Knights Landing (KNL), a manycore processor with up to 72 cores. This work analyzes the behavior of the Intel Mesh Interconnect through the KNL architecture, proposing ways to discover the physical layout of its logical components. We have designed and developed an extension to the Tejas memory system simulator to replicate and study the low-level data traffic of the processor network. The reliability and accuracy of the proposed simulator is assessed using several state-of-the-art sequential and parallel benchmarks, and a particular Intel Mesh Interconnect-focused locality optimization is proposed and studied using the simulator and a real KNL system.

## Cite this

```
@article{tejasknl,
  author={M. {Horro} and G. {Rodríguez} and J. {Touriño}},
  journal={IEEE Access}, 
  title={Simulating the Network Activity of Modern Manycores}, 
  year={2019},
  volume={7},
  number={1},
  pages={81195-81210},
}
```

or

```
M. Horro, G. Rodríguez and J. Touriño, "Simulating the Network Activity of Modern Manycores," in IEEE Access, vol. 7, pp. 81195-81210, 2019, doi: 10.1109/ACCESS.2019.2923855.
```
