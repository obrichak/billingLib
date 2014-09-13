#ifndef _BILLING_H_
#define _BILLING_H_

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_com_demo_billing_Billing_nativeInit(JNIEnv* env, jobject obj);
JNIEXPORT void JNICALL Java_com_demo_billing_BillingObserver_nativeOnProductPurchased(JNIEnv* env, jclass clazz, jstring jSku);
JNIEXPORT void JNICALL Java_com_demo_billing_BillingObserver_nativeOnPurchaseRestored(JNIEnv* env, jclass clazz, jstring jSku);
JNIEXPORT void JNICALL Java_com_demo_billing_BillingObserver_nativeOnPurchaseCanceled(JNIEnv* env, jclass clazz);
JNIEXPORT void JNICALL Java_com_demo_billing_BillingObserver_nativeOnPurchaseFailed(JNIEnv* env, jclass clazz, jint errorCode);

#ifdef __cplusplus
}
#endif

void Billing_Init(JavaVM* javaVM);
bool Billing_IsBillingSupported();
void Billing_PurchaseItem(const char* sku, bool consumable);
void Billing_RestorePurchases();
bool Billing_IsProductPurchased(const char* sku);
void Billing_Dealloc();
void Billing_SetCallbacks(void(*billingInitialized)(),
						 void(*productPurchased)(const char* sku),
						 void(*purchaseRestored)(const char *sku),
						 void(*purchaseCanceled)(),
						 void(*purchaseFailed)(int errorCode));

#endif
