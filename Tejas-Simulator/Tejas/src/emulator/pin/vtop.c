/**
 * based on https://github.com/katsuster/virt2phys
 */

/* headers */
#include "vtop.h"
#include <errno.h>
#include <stdio.h>
#include <string.h>

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
  // tbl_shared = (tblen >> 61) & 0x1;
  // tbl_pte_dirty = (tblen >> 55) & 0x1;
  if (!tbl_swapped) {
    tbl_swap_offset = (tblen >> 0) & 0x7fffffffffffffULL;
  } else {
    tbl_swap_offset = (tblen >> 5) & 0x3ffffffffffffULL;
    // tbl_swap_type = (tblen >> 0) & 0x1f;
  }
  // dprintf("tblen: \n"
  //  "  [63   ] present    :%d\n"
  //  "  [62   ] swapped    :%d\n"
  //  "  [61   ] shared     :%d\n"
  //  "  [55   ] pte dirty  :%d\n",
  //  (int)tbl_present,
  //  (int)tbl_swapped,
  //  (int)tbl_shared,
  //  (int)tbl_pte_dirty);
  // if (!tbl_swapped) {
  //  dprintf("  [ 0-54] PFN        :0x%08llx\n",
  //    (long long)tbl_swap_offset);
  //} else {
  //  dprintf("  [ 5-54] swap offset:0x%08llx\n"
  //    "  [ 0- 4] swap type  :%d\n",
  //    (long long)tbl_swap_offset,
  //    (int)tbl_swap_type);
  //}
  pageaddr = tbl_swap_offset * pagesize;
  physaddr = (uint64_t)pageaddr | (virtaddr & (pagesize - 1));
  // dprintf("page:0x%08llx, phys:0x%08llx\n",
  //  (long long)pageaddr, (long long)physaddr);
  // dprintf("\n");
  close(fd);
  if (tbl_present) {
    dprintf("virt:0x%08llx, phys:0x%08llx\n", (long long)virtaddr,
            (long long)physaddr);
    return physaddr;
  } else {
    dprintf("virt:0x%08llx, phys:%s\n", (long long)virtaddr, "(not present)");
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
