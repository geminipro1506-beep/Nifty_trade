# ProGuard rules for CloudScope Money Printer
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class com.google.gson.** { *; }
-keep class com.cloudscope.moneyprinter.data.model.** { *; }
-keep class com.cloudscope.moneyprinter.data.remote.** { *; }
