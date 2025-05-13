#ifndef native_core_h
#define native_core_h

#ifdef __cplusplus
extern "C" {
#endif

const char* native_sandbox_test(const char* input);

void native_sandbox_test_no_input();

void free_bridge_result(const char* str);

int ScalaNativeInit(void);

#ifdef __cplusplus
}
#endif

#endif /* native_core_h */
