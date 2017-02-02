#include <stdlib.h>

void * __attribute__ ((noinline)) 
mc_malloc(long bytes)
{ 
  void *p = (void *) malloc(bytes);
  /* Assuming a page size of 4096 bytes */
  //posix_memalign(&p, 4096, bytes);
  return p;
};

void * __attribute__ ((noinline)) 
ddr_malloc(long bytes) 
{
  void *p = (void *) malloc(bytes);
  /* Assuming a page size of 4096 bytes */
  //posix_memalign(&p, 4096, bytes);
  return malloc(bytes);
};
