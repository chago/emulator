package com.kuaishou;

import cn.banny.emulator.LibraryResolver;
import cn.banny.emulator.Module;
import cn.banny.emulator.arm.ARMEmulator;
import cn.banny.emulator.file.FileIO;
import cn.banny.emulator.file.IOResolver;
import cn.banny.emulator.linux.android.AndroidARMEmulator;
import cn.banny.emulator.linux.android.AndroidResolver;
import cn.banny.emulator.linux.android.dvm.*;
import cn.banny.emulator.memory.Memory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class KuaiShouSign extends AbstractJni implements IOResolver {

    private static final String APP_PACKAGE_NAME = "com.smile.gifmaker";

    private static LibraryResolver createLibraryResolver() {
        return new AndroidResolver(19);
    }

    private static ARMEmulator createARMEmulator() {
        return new AndroidARMEmulator(APP_PACKAGE_NAME);
    }

    private final ARMEmulator emulator;
    private final VM vm;

    private final DvmClass CPUJni;

    private static final String APK_PATH = "src/test/resources/app/kuaishou6.2.3.8614.apk";

    private final Module module;

    private KuaiShouSign() throws IOException {
        emulator = createARMEmulator();
        emulator.getSyscallHandler().addIOResolver(this);
        System.out.println("== init ===");

        final Memory memory = emulator.getMemory();
        memory.setLibraryResolver(createLibraryResolver());
        memory.setCallInitFunction();

        vm = emulator.createDalvikVM(new File(APK_PATH));
        DalvikModule dm = vm.loadLibrary("core", false);
        dm.callJNI_OnLoad(emulator);
        module = dm.getModule();

        CPUJni = vm.resolveClass("com/yxcorp/gifshow/util/CPU");
    }

    private void destroy() throws IOException {
        emulator.close();
        System.out.println("module=" + module);
        System.out.println("== destroy ===");
    }

    public static void main(String[] args) throws Exception {
        KuaiShouSign test = new KuaiShouSign();
        test.sign();
        test.destroy();
    }

    private void sign() {
        vm.setJni(this);
        Logger.getLogger("cn.banny.emulator.AbstractEmulator").setLevel(Level.DEBUG);
        String str = "app=0appver=6.2.3.8614c=ALI_CPD,17client_key=3c2cd3f3contactData=7A9IqsDstz815+zxGyC1+XgougsArgtFUPBRYcRwUhcjwTsafJBmYnLZgLc5l4g7sjINLj0nrXFq1CCsFHteQSpac+959kD0yYEJyGzukSqMQGayQCue397jX98gp0NPU26waWGh+JWMaYnZG/F1Sg==country_code=CNdid=ANDROID_9fb7792f6142ea63did_gt=1553767215144ftt=hotfix_ver=isp=iuid=iv=5okP62w8Yl7WHiG6kpf=ANDROID_PHONEkpn=KUAISHOUlanguage=zh-cnlat=40.054041lon=116.298517max_memory=192mod=LGE(Nexus 5)net=WIFIoc=ALI_CPD,17os=androidsys=ANDROID_6.0.1token=f68245ccc1344489894f963248cc3501-1082592150ud=1082592150ver=6.2";

//        emulator.traceCode();
//        emulator.attach().addBreakPoint(null, 0x40001278);
        DvmObject context = vm.resolveClass("com/yxcorp/gifshow/App").newObject(null);
        Number ret = CPUJni.callStaticJniMethod(emulator, "getClock(Ljava/lang/Object;[BI)Ljava/lang/String;",
                context,
                vm.addLocalObject(new ByteArray(str.getBytes())), 23);
        long hash = ret.intValue() & 0xffffffffL;
        StringObject obj = vm.getObject(hash);
        vm.deleteLocalRefs();
        System.out.println(obj.getValue());
    }

    @Override
    public FileIO resolve(File workDir, String pathname, int oflags) {
        return null;
    }

    @Override
    public DvmObject callObjectMethodV(VM vm, DvmObject dvmObject, String signature, String methodName, String args, VaList vaList) {
        switch (signature) {
            case "com/yxcorp/gifshow/App->getPackageName()Ljava/lang/String;":
                return new StringObject(vm, APP_PACKAGE_NAME);
            case "com/yxcorp/gifshow/App->getPackageManager()Landroid/content/pm/PackageManager;":
                return new DvmObject<Object>(vm.resolveClass("android/content/pm/PackageManager"), null);
        }

        return super.callObjectMethodV(vm, dvmObject, signature, methodName, args, vaList);
    }
}