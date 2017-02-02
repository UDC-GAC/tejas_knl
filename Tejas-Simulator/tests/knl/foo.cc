#include <stdio.h>
#include <stdlib.h>
#include "../lib/my_malloc.h"

#ifndef N
# define N 1000
#endif

int main()
{
  
  int i;
  double a[N],b[N],c[N],d[N];
  double k = 2.3;
  
  //double *t0 = (double *) malloc(1);

  double *t1 = (double *) ddr_malloc(sizeof(double)*N);
  double *t2 = (double *) mc_malloc(sizeof(double)*N);
  //double *t3 = (double *) mc_malloc(sizeof(double)*(N+1));
  
  for (i=0; i<N; i++) {
    c[i] = a[i] + t2[i];
    d[i] = k * a[i];
    t1[i] = 12 * k * d[i];
    t2[i] = t2[i] * k * d[i] * t1[i];
    //printf("t2[%d]=%lu(%lu)\n",i,&t2[i],&t2); 
  }

  // prevent from deleting code
  //printf("%lu\n", t0);
  printf("%lu %lu %lu %lu\n",a,b,c,d);
  printf("t1: %lu\n", t1);
  printf("t2: %lu\n", t2);
  //printf("t3: %lu\n", t3);
  exit(0);
}
