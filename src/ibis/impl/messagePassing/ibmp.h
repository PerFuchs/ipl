#ifndef __IBIS_MANTA_IBIS_IMPL_MESSAGE_PASSING_IBP_H__
#define __IBIS_MANTA_IBIS_IMPL_MESSAGE_PASSING_IBP_H__

#include <jni.h>

void ibmp_object_toString(JNIEnv *env, jobject obj);
void ibmp_thread_yield(JNIEnv *env);
void ibmp_lock(JNIEnv *env);
void ibmp_unlock(JNIEnv *env);
char *ibmp_currentThread(JNIEnv *env);
void ibmp_dumpStack(JNIEnv *env);

void ibmp_lock_check_owned(JNIEnv *env);
void ibmp_lock_check_not_owned(JNIEnv *env);

#ifndef NDEBUG
#define IBP_VERBOSE
#define IBP_STATISTICS
#endif

// #define IBP_VERBOSE

#ifdef IBP_VERBOSE
extern int ibmp_verbose;
int ibmp_stderr_printf(char *fmt, ...);
#define IBP_VPRINTF(n, env, s) \
	    do if ((n) <= ibmp_verbose) { \
		fprintf(stderr, "%s.%d ", __FILE__, __LINE__); \
		if ((env) != NULL) fprintf(stderr, "%s: ", ibmp_currentThread(env)); \
		ibmp_stderr_printf s; \
	    } while (0)

#else
#define ibmp_lock_check_owned(env)
#define ibmp_lock_check_not_owned(env)
#define IBP_VPRINTF(n, env, s)
#endif

extern JNIEnv	       *ibmp_JNIEnv;

#ifndef NDEBUG
#define ibmp_set_JNIEnv(env) \
	{ \
	    JNIEnv *old_env = ibmp_JNIEnv; \
	    ibmp_JNIEnv = (env);
#define ibmp_unset_JNIEnv() \
	    ibmp_JNIEnv = old_env; \
	}
#else
#define ibmp_set_JNIEnv(env)	do ibmp_JNIEnv = (env); while (0)
#define ibmp_unset_JNIEnv()
#endif

extern jclass		ibmp_cls_Ibis;
extern jobject		ibmp_obj_Ibis_ibis;
extern JavaVM *         vm;

void ibmp_init(JNIEnv *env, jobject this);
void ibmp_end(JNIEnv *env, jobject this);

#endif
