#define VER_MINOR 6
#define VER_MAJOR 3

// Usually you do not change things below this line...
#define STR(arg) #arg
#define JAPDLL_VERSION_intern(major,minor) 00.#major.00#minor
#define JAPDLL_VERSION STR(JAPDLL_VERSION_intern(VER_MAJOR,VER_MINOR))
