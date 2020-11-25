// Copyright (C) 2000-2013 SuperWaba Ltda.
// Copyright (C) 2014-2020 TotalCross Global Mobile Platform Ltda.
//
// SPDX-License-Identifier: LGPL-2.1-only

static Err android_ttTM_nativeInitialize(Context currentContext, TCObject deviceIds, TCObject simSerialNumbers, TCObject lineNumbers) {
   JNIEnv* env = getJNIEnv();
   jmethodID method = (*env)->GetStaticMethodID(env, applicationClass, "requestPhoneStatePermission", "()I");
   jint result = (*env)->CallStaticIntMethod(env, applicationClass, method);
   if (result <= 0) {
       return -1;
   }

   jclass jSettingsClass = androidFindClass(env, "totalcross/android/Settings4A");
   jmethodID fillTelephonySettings = (*env)->GetStaticMethodID(env, jSettingsClass, "fillTelephonySettings", "()V");
   (*env)->CallStaticVoidMethod(env, jSettingsClass, fillTelephonySettings);

   jfieldID jfID;
   jstring jStringField;
   char strTemp[128];
   static char imei[64],imei2[64];
   static char iccid[30];

   // phone number - needed to move to here or jni on android 5 will abort
   jfID = (*env)->GetStaticFieldID(env, jSettingsClass, "lineNumber", "Ljava/lang/String;");
   jStringField = (jstring) (*env)->GetStaticObjectField(env, jSettingsClass, jfID);
   if (jStringField != null)
   {
      jstring2CharP(jStringField, strTemp);
      (*env)->DeleteLocalRef(env, jStringField);
      setObjectLock(*getStaticFieldObject(currentContext, settingsClass, "lineNumber") = createStringObjectFromCharP(currentContext, strTemp, -1), UNLOCKED);
   }

   jfID = (*env)->GetStaticFieldID(env, jSettingsClass, "imei", "Ljava/lang/String;");
   jStringField = (jstring) (*env)->GetStaticObjectField(env, jSettingsClass, jfID);
   if (jStringField != null)
   {
      jstring2CharP(jStringField, imei);
      (*env)->DeleteLocalRef(env, jStringField);
   }

   jfID = (*env)->GetStaticFieldID(env, jSettingsClass, "imei2", "Ljava/lang/String;");
   jStringField = (jstring) (*env)->GetStaticObjectField(env, jSettingsClass, jfID);
   if (jStringField != null)
   {
      jstring2CharP(jStringField, imei2);
      (*env)->DeleteLocalRef(env, jStringField);
   }

   jfID = (*env)->GetStaticFieldID(env, jSettingsClass, "iccid", "Ljava/lang/String;");
   jStringField = (jstring) (*env)->GetStaticObjectField(env, jSettingsClass, jfID);
   if (jStringField != null)
   {
      jstring2CharP(jStringField, iccid);
      (*env)->DeleteLocalRef(env, jStringField);
   }

   return NO_ERROR;
}
