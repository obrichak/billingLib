billingLib
==========

Billing lib. wraps Google and Amazon billing api into a single easy to use interface.

To use it you will need:
1. Import this library project into your workspace. This library also depends on JSON library project which must also be imported.

2. Add BillingLibrary library project to the build path in your project settings;

3. Add permission com.android.vending.BILLING to your AndroidManifest.xml;

4. If you want to make purchase requests and receive callbacks from native code, add Billing.h and Billing.cpp to your project's
   jni directory.
   
5. Declare and create a Billing object instance by calling 
	
	Billing billing = new GoogleBilling(context, nativeAppEnabled);

   where nativeAppEnabled indicates if the JNI part of the application will receive callbacks. If it's set to true
   but native part of the library is not included into your application you will get UnsatisfiedLinkError.
   WARNING! For Google implementation you must provide your activity object as the Context instance.
   Only GoogleBilling implementation is available at this time.

6. Extend BillingObserver class and pass its instance as an observer to receive responses:
	
	billing.addObserver(new BillingObserver{
		/.../
	}());

6. Call billing.onActivityResult() in your activity's onActivityResult method.

7. Check if billing is supported by device before making billing requests:
	
	boolean supported = billing.isBillingSupported();
	
8. Restore purchases by calling restorePurchases() method. onPurchaseRestored() callback will be called for every non-consumable
   item you own. All consumable items which were purchased but not consumed, will be consumed and onProductPurchased() callback
   will be called for every sku.
   
9. Make purchase by calling billing.purchaseItem(sku, isConsumable). One of onProductPurchased, onPurchaseCanceled or 
   onPurchaseFailed callbacks will be called depending on purchase result.
   
10.You can check if a product is purchased by calling billing.isProductPurchased(sku) method.

11.Don't forget to free resources by calling billing.destroy(); in your activity's onDestroy() method. 

12.All purchase requests could be made from native code using the native part of the library. To be able to receive callbacks 
   pass your appropriate callback function pointers as parameters in Billing_SetCallbacks() function. Don't forget to initialize and release
   the library by calling Billing_Init() and Billing_Dealloc().
