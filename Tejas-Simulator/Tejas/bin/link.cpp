#include "link.h"
#include "JNIShm.c"

#include <stdio.h>
#include <stdlib.h>

void ProxyLink::setDDRaddr(long addr)
{
  setMCDRAMaddr(addr); 
}

void ProxyLink::setMCDRAMaddr(long addr)
{
  setMCDRAMaddr(addr); 
}
