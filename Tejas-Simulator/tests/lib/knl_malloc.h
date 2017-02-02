namespace KNL
{
  class KNLib
  {
  public:
    static void* mc_malloc(long bytes) { return malloc(bytes); };
    static void* ddr_malloc(long bytes) { return malloc(bytes); };
  };
}
