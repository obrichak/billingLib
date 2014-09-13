#include "Billing.h"
#include "CommonIncludes.h"

JavaVM* javaVm;
jobject jBilling;
bool billingSupported = false;

//Callback functions pointers
void (*onBillingInitializedCallback) () = NULL;
void (*onProductPurchasedCallback) (const char* sku) = NULL;
void (*onPurchaseRestoredCallback) (const char* sku) = NULL;
void (*onPurchaseCanceledCallback) () = NULL;
void (*onPurchaseFailedCallback) (int errorCode) = NULL;

//Private methods
JNIEnv* getEnv(){
	JNIEnv* env;
	javaVm -> GetEnv((void**)&env, JNI_VERSION_1_6);

	return env;
}

//Interface methods
void Billing_Init(JavaVM* jvm){
	javaVm = jvm;
}

bool Billing_IsBillingSupported(){
	return billingSupported;
}

void Billing_PurchaseItem(const char* sku, bool consumable){
	JNIEnv* env = getEnv();
	jclass k = env->GetObjectClass(jBilling);
	jmethodID m = env->GetMethodID(k, "purchaseItem", "(Ljava/lang/String;Z)V");
	jstring jSku = env->NewStringUTF(sku);
	env->CallVoidMethod(jBilling, m, jSku, (jboolean) consumable);
}

void Billing_RestorePurchases(){
	JNIEnv* env = getEnv();
	jclass k = env->GetObjectClass(jBilling);
	jmethodID m = env->GetMethodID(k, "restorePurchases", "()V");
	env->CallVoidMethod(jBilling, m);
}

bool Billing_IsProductPurchased(const char* sku){
	JNIEnv* env = getEnv();
	jclass k = env->GetObjectClass(jBilling);
	jmethodID m = env->GetMethodID(k, "isProductPurchased", "(Ljava/lang/String;)Z");
	jstring jSku = env->NewStringUTF(sku);
	return env->CallBooleanMethod(jBilling, m, jSku);
}

void Billing_Dealloc(){
	JNIEnv* env = getEnv();
	env->DeleteGlobalRef(jBilling);
}

void Billing_SetCallbacks(void(*billingInitialized)(),
						 void(*productPurchased)(const char* sku),
						 void(*purchaseRestored)(const char *sku),
						 void(*purchaseCanceled)(),
						 void(*purchaseFailed)(int errorCode)){
	onBillingInitializedCallback = billingInitialized;
	onProductPurchasedCallback = productPurchased;
	onPurchaseRestoredCallback = purchaseRestored;
	onPurchaseCanceledCallback = purchaseCanceled;
	onPurchaseFailedCallback = purchaseFailed;
}

//Callback methods
void Java_com_demo_billing_Billing_nativeInit(JNIEnv* env, jobject obj){
	jBilling = env->NewGlobalRef(obj);

	jclass k = env->GetObjectClass(jBilling);
	jmethodID m = env->GetMethodID(k, "isBillingSupported", "()Z");
	billingSupported = env->CallBooleanMethod(jBilling, m) == JNI_TRUE;

	if(onBillingInitializedCallback!=NULL)onBillingInitializedCallback();
}

void Java_com_demo_billing_BillingObserver_nativeOnProductPurchased(JNIEnv* env, jclass clazz, jstring jSku){
	if(onProductPurchasedCallback!=NULL){
		const char* sku = env->GetStringUTFChars(jSku, NULL);
		onProductPurchasedCallback(sku);
		env->ReleaseStringUTFChars(jSku, sku);
	}
}

void Java_com_demo_billing_BillingObserver_nativeOnPurchaseRestored(JNIEnv* env, jclass clazz, jstring jSku){
	if(onPurchaseRestoredCallback!=NULL){
		const char* sku = env->GetStringUTFChars(jSku, NULL);
		onPurchaseRestoredCallback(sku);
		env->ReleaseStringUTFChars(jSku, sku);
	}
}

void Java_com_demo_billing_BillingObserver_nativeOnPurchaseCanceled(JNIEnv* env, jclass clazz){
	if(onPurchaseCanceledCallback!=NULL) onPurchaseCanceledCallback();
}

void Java_com_demo_billing_BillingObserver_nativeOnPurchaseFailed(JNIEnv* env, jclass clazz, jint errorCode){
	if(onPurchaseFailedCallback!=NULL) onPurchaseFailedCallback(errorCode);
}

