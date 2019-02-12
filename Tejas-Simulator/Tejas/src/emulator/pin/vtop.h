/**
 * based on https://github.com/katsuster/virt2phys
 */

#ifndef _VTOP_H
#define _VTOP_H

/* headers */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/stat.h>

/* definitions */
#define min(a, b)    ((a < b) ? (a) : (b))
#define max(a, b)    ((a > b) ? (a) : (b))

#define no_printf(x,...)  do{}while(0);

/* debug */
#if DEBUG
#define dprintf    printf
#else
#define dprintf    no_printf
#endif

/**
 * Wrapper function
 *
 * @param virtaddr Virtual address to translate
 * @return -1: error, -2: not present, other: physical address
 */
uint64_t
vtop(uint64_t virtaddr);

#endif /* end _VTOP_H */
