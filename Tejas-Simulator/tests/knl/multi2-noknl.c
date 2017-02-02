#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>

#ifndef NUM_THREADS 
#define NUM_THREADS 5
#endif

#ifndef DDR_SIZE
#define DDR_SIZE 8000000
#endif

#ifndef MCDRAM_SIZE
#define MCDRAM_SIZE 1600000
#endif

#ifndef N
#define N 1000
#endif

double *p_mcdram;
double *p_ddr;

/* create thread argument struct for thr_func() */
typedef struct _thread_data_t {
  int tid;
  double stuff;
} thread_data_t;
 
/* thread function */
void *thr_func(void *arg) {
  thread_data_t *data = (thread_data_t *)arg;
  int i,id = 0;
  double a[N],b[N],c[N],d[N];
  double k = 2.3;

  printf("hello from thr_func, thread id: %d\n", data->tid);
  for (i=0; i<N; i++) {
    id = data->tid*1000;
    c[i] = a[i] + p_ddr[i];
    d[i] = k * a[i];
    p_mcdram[id + i] = p_ddr[id + i] * k * d[i];
    p_ddr[id + i] = p_ddr[id + i] * k * d[i] * p_mcdram[i];
  }

  // prevent from deleting code
  printf("%lu %lu %lu %lu\n",a,b,c,d);
  printf("t1: %lu\n", p_mcdram);
  printf("t2: %lu\n", p_ddr);

  pthread_exit(NULL);
}
 
int main(int argc, char **argv) {
  pthread_t thr[NUM_THREADS];
  int i, rc;

  p_mcdram = (double *) malloc(MCDRAM_SIZE);
  p_ddr = (double *) malloc(MCDRAM_SIZE);

  /* create a thread_data_t argument array */
  thread_data_t thr_data[NUM_THREADS];
 
  /* create threads */
  for (i = 0; i < NUM_THREADS; ++i) {
    thr_data[i].tid = i;
    if ((rc = pthread_create(&thr[i], NULL, thr_func, &thr_data[i]))) {
      fprintf(stderr, "error: pthread_create, rc: %d\n", rc);
      return EXIT_FAILURE;
    }
  }
  /* block until all threads complete */
  for (i = 0; i < NUM_THREADS; ++i) {
    pthread_join(thr[i], NULL);
  }

  free(p_mcdram);
  free(p_ddr);
 
  return EXIT_SUCCESS;
}
