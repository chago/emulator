package cn.banny.unidbg.linux.android.dvm;

public interface Jni {

    void callStaticVoidMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList);

    boolean callStaticBooleanMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg);

    boolean callStaticBooleanMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList);

    int callStaticIntMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg);

    int callStaticIntMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList);

    long callStaticLongMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList);

    DvmObject callStaticObjectMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg);

    DvmObject callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList);

    DvmObject newObject(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg);

    DvmObject newObjectV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList);

    void callVoidMethod(BaseVM vm, DvmObject dvmObject, String signature, VarArg varArg);

    void callVoidMethodV(BaseVM vm, DvmObject dvmObject, String signature, VaList vaList);

    boolean callBooleanMethodV(BaseVM vm, DvmObject dvmObject, String signature, VaList vaList);

    int callIntMethod(BaseVM vm, DvmObject dvmObject, String signature, VarArg varArg);

    int callIntMethodV(BaseVM vm, DvmObject dvmObject, String signature, VaList vaList);

    DvmObject callObjectMethod(BaseVM vm, DvmObject dvmObject, String signature, VarArg varArg);

    DvmObject callObjectMethodV(BaseVM vm, DvmObject dvmObject, String signature, VaList vaList);

    int getStaticIntField(BaseVM vm, DvmClass dvmClass, String signature);

    DvmObject getStaticObjectField(BaseVM vm, DvmClass dvmClass, String signature);

    boolean getBooleanField(BaseVM vm, DvmObject dvmObject, String signature);

    int getIntField(BaseVM vm, DvmObject dvmObject, String signature);

    DvmObject getObjectField(BaseVM vm, DvmObject dvmObject, String signature);

    void setBooleanField(BaseVM vm, DvmObject dvmObject, String signature, boolean value);

    void setIntField(BaseVM vm, DvmObject dvmObject, String signature, int value);

    void setLongField(BaseVM vm, DvmObject dvmObject, String signature, long value);

    void setObjectField(BaseVM vm, DvmObject dvmObject, String signature, DvmObject value);
}
