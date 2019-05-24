package cn.banny.unidbg.ios;

import cn.banny.unidbg.Emulator;
import cn.banny.unidbg.Module;
import cn.banny.unidbg.Symbol;
import cn.banny.unidbg.arm.*;
import cn.banny.unidbg.ios.struct.DlInfo;
import cn.banny.unidbg.ios.struct.DyldImageInfo;
import cn.banny.unidbg.memory.Memory;
import cn.banny.unidbg.memory.SvcMemory;
import cn.banny.unidbg.pointer.UnicornPointer;
import cn.banny.unidbg.pointer.UnicornStructure;
import cn.banny.unidbg.spi.InitFunction;
import com.sun.jna.Pointer;
import keystone.Keystone;
import keystone.KeystoneArchitecture;
import keystone.KeystoneEncoded;
import keystone.KeystoneMode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import unicorn.Arm64Const;
import unicorn.ArmConst;
import unicorn.Unicorn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Dyld64 extends Dyld {

    private static final Log log = LogFactory.getLog(Dyld64.class);

    private final MachOLoader loader;

    private final UnicornPointer error;

    Dyld64(MachOLoader loader, SvcMemory svcMemory) {
        this.loader = loader;

        error = svcMemory.allocate(0x40);
        assert error != null;
        error.setMemory(0, 0x40, (byte) 0);
    }

    private Pointer __dyld_image_count;
    private Pointer __dyld_get_image_name;
    private Pointer __dyld_get_image_header;
    private Pointer __dyld_get_image_vmaddr_slide;
    private Pointer __dyld_get_image_slide;
    private Pointer __dyld_register_func_for_add_image;
    private Pointer __dyld_register_func_for_remove_image;
    private Pointer __dyld_register_thread_helpers;
    private Pointer __dyld_dyld_register_image_state_change_handler;
    private Pointer __dyld_image_path_containing_address;
    private Pointer __dyld__NSGetExecutablePath;

    @Override
    final int _stub_binding_helper() {
        log.info("dyldLazyBinder");
        return 0;
    }

    private Pointer __dyld_dlopen;
    private Pointer __dyld_dlsym;
    private Pointer __dyld_dladdr;
    private long _os_trace_redirect_func;

    @Override
    final int _dyld_func_lookup(Emulator emulator, String name, Pointer address) {
        if (log.isDebugEnabled()) {
            log.debug("_dyld_func_lookup name=" + name);
        }
        final SvcMemory svcMemory = emulator.getSvcMemory();
        switch (name) {
            case "__dyld__NSGetExecutablePath":
                if (__dyld__NSGetExecutablePath == null) {
                    __dyld__NSGetExecutablePath = svcMemory.registerSvc(new ArmSvc() {
                        @Override
                        public int handle(Emulator emulator) {
                            Pointer buf = UnicornPointer.register(emulator, ArmConst.UC_ARM_REG_R0);
                            int bufSize = ((Number) emulator.getUnicorn().reg_read(ArmConst.UC_ARM_REG_R1)).intValue();
                            if (log.isDebugEnabled()) {
                                log.debug("__dyld__NSGetExecutablePath buf=" + buf + ", bufSize=" + bufSize);
                            }
                            buf.setString(0, emulator.getProcessName());
                            return 0;
                        }
                    });
                }
                address.setPointer(0, __dyld__NSGetExecutablePath);
                return 1;
            case "__dyld_get_image_name":
                if (__dyld_get_image_name == null) {
                    __dyld_get_image_name = svcMemory.registerSvc(new ArmSvc() {
                        @Override
                        public int handle(Emulator emulator) {
                            int image_index = ((Number) emulator.getUnicorn().reg_read(ArmConst.UC_ARM_REG_R0)).intValue();
                            Module[] modules = loader.getLoadedModules().toArray(new Module[0]);
                            if (image_index < 0 || image_index >= modules.length) {
                                return 0;
                            }
                            MachOModule module = (MachOModule) modules[image_index];
                            return (int) module.createPathMemory(svcMemory).peer;
                        }
                    });
                }
                address.setPointer(0, __dyld_get_image_name);
                return 1;
            case "__dyld_get_image_header":
                if (__dyld_get_image_header == null) {
                    __dyld_get_image_header = svcMemory.registerSvc(new ArmSvc() {
                        @Override
                        public int handle(Emulator emulator) {
                            int image_index = ((Number) emulator.getUnicorn().reg_read(ArmConst.UC_ARM_REG_R0)).intValue();
                            Module[] modules = loader.getLoadedModules().toArray(new Module[0]);
                            if (image_index < 0 || image_index >= modules.length) {
                                return 0;
                            }
                            MachOModule module = (MachOModule) modules[image_index];
                            return (int) module.machHeader;
                        }
                    });
                }
                address.setPointer(0, __dyld_get_image_header);
                return 1;
            case "__dyld_get_image_slide":
                if (__dyld_get_image_slide == null) {
                    __dyld_get_image_slide = svcMemory.registerSvc(new Arm64Svc() {
                        @Override
                        public int handle(Emulator emulator) {
                            UnicornPointer mh = UnicornPointer.register(emulator, Arm64Const.UC_ARM64_REG_X0);
                            int slide = mh == null ? 0 : computeSlide(emulator, mh.peer);
                            log.debug("__dyld_get_image_slide mh=" + mh + ", slide=0x" + Integer.toHexString(slide));
                            return slide;
                        }
                    });
                }
                address.setPointer(0, __dyld_get_image_slide);
                return 1;
            case "__dyld_get_image_vmaddr_slide":
                if (__dyld_get_image_vmaddr_slide == null) {
                    __dyld_get_image_vmaddr_slide = svcMemory.registerSvc(new ArmSvc() {
                        @Override
                        public int handle(Emulator emulator) {
                            int image_index = ((Number) emulator.getUnicorn().reg_read(ArmConst.UC_ARM_REG_R0)).intValue();
                            log.debug("__dyld_get_image_vmaddr_slide index=" + image_index);
                            Module[] modules = loader.getLoadedModules().toArray(new Module[0]);
                            if (image_index < 0 || image_index >= modules.length) {
                                return 0;
                            }
                            MachOModule module = (MachOModule) modules[image_index];
                            return computeSlide(emulator, module.machHeader);
                        }
                    });
                }
                address.setPointer(0, __dyld_get_image_vmaddr_slide);
                return 1;
            case "__dyld_image_count":
                if (__dyld_image_count == null) {
                    __dyld_image_count = svcMemory.registerSvc(new ArmSvc() {
                        @Override
                        public int handle(Emulator emulator) {
                            return loader.getLoadedModules().size();
                        }
                    });
                }
                address.setPointer(0, __dyld_image_count);
                return 1;
            case "__dyld_dlopen":
                if (__dyld_dlopen == null) {
                    __dyld_dlopen = svcMemory.registerSvc(new ArmSvc() {
                        @Override
                        public UnicornPointer onRegister(SvcMemory svcMemory, int svcNumber) {
                            try (Keystone keystone = new Keystone(KeystoneArchitecture.Arm, KeystoneMode.Arm)) {
                                KeystoneEncoded encoded = keystone.assemble(Arrays.asList(
                                        "push {r4-r7, lr}",
                                        "svc #0x" + Integer.toHexString(svcNumber),
                                        "pop {r7}", // manipulated stack in dlopen
                                        "cmp r7, #0",
                                        "subne lr, pc, #16", // jump to pop {r7}
                                        "bxne r7", // call init array
                                        "pop {r0, r4-r7, pc}")); // with return address
                                byte[] code = encoded.getMachineCode();
                                UnicornPointer pointer = svcMemory.allocate(code.length);
                                pointer.write(0, code, 0, code.length);
                                return pointer;
                            }
                        }
                        @Override
                        public int handle(Emulator emulator) {
                            Pointer path = UnicornPointer.register(emulator, ArmConst.UC_ARM_REG_R0);
                            int mode = ((Number) emulator.getUnicorn().reg_read(ArmConst.UC_ARM_REG_R1)).intValue();
                            if (log.isDebugEnabled()) {
                                log.debug("__dyld_dlopen path=" + path.getString(0) + ", mode=" + mode);
                            }
                            return dlopen(emulator.getMemory(), path.getString(0), emulator);
                        }
                    });
                }
                address.setPointer(0, __dyld_dlopen);
                return 1;
            case "__dyld_dladdr":
                if (__dyld_dladdr == null) {
                    __dyld_dladdr = svcMemory.registerSvc(new ArmSvc() {
                        @Override
                        public int handle(Emulator emulator) {
                            long addr = ((Number) emulator.getUnicorn().reg_read(ArmConst.UC_ARM_REG_R0)).intValue() & 0xffffffffL;
                            Pointer info = UnicornPointer.register(emulator, ArmConst.UC_ARM_REG_R1);
                            if (log.isDebugEnabled()) {
                                log.debug("__dyld_dladdr addr=0x" + Long.toHexString(addr) + ", info=" + info);
                            }
                            MachOModule module = (MachOModule) loader.findModuleByAddress(addr);
                            if (module == null) {
                                return 0;
                            }

                            Symbol symbol = module.findNearestSymbolByAddress(addr);

                            DlInfo dlInfo = new DlInfo(info);
                            dlInfo.dli_fname = module.createPathMemory(svcMemory);
                            dlInfo.dli_fbase = UnicornPointer.pointer(emulator, module.machHeader);
                            if (symbol != null) {
                                dlInfo.dli_sname = symbol.createNameMemory(svcMemory);
                                dlInfo.dli_saddr = UnicornPointer.pointer(emulator, symbol.getAddress());
                            }
                            dlInfo.pack();
                            return 1;
                        }
                    });
                }
                address.setPointer(0, __dyld_dladdr);
                return 1;
            case "__dyld_dlsym":
                if (__dyld_dlsym == null) {
                    __dyld_dlsym = svcMemory.registerSvc(new ArmSvc() {
                        @Override
                        public int handle(Emulator emulator) {
                            long handle = ((Number) emulator.getUnicorn().reg_read(ArmConst.UC_ARM_REG_R0)).intValue() & 0xffffffffL;
                            Pointer symbol = UnicornPointer.register(emulator, ArmConst.UC_ARM_REG_R1);
                            if (log.isDebugEnabled()) {
                                log.debug("__dyld_dlsym handle=0x" + Long.toHexString(handle) + ", symbol=" + symbol.getString(0));
                            }

                            String symbolName = symbol.getString(0);
                            if ((int) handle == MachO.RTLD_MAIN_ONLY && "_os_trace_redirect_func".equals(symbolName)) {
                                if (_os_trace_redirect_func == 0) {
                                    _os_trace_redirect_func = svcMemory.registerSvc(new ArmSvc() {
                                        @Override
                                        public int handle(Emulator emulator) {
                                            Pointer msg = UnicornPointer.register(emulator, ArmConst.UC_ARM_REG_R0);
//                                            Inspector.inspect(msg.getByteArray(0, 16), "_os_trace_redirect_func msg=" + msg);
                                            System.err.println("_os_trace_redirect_func msg=" + msg.getString(0));
                                            return 1;
                                        }
                                    }).peer;
                                }
                                return (int) _os_trace_redirect_func;
                            }

                            return dlsym(emulator.getMemory(), (int) handle, symbolName);
                        }
                    });
                }
                address.setPointer(0, __dyld_dlsym);
                return 1;
            case "__dyld_register_thread_helpers":
                if (__dyld_register_thread_helpers == null) {
                    __dyld_register_thread_helpers = svcMemory.registerSvc(new Arm64Svc() {
                        @Override
                        public int handle(Emulator emulator) {
                            // the table passed to dyld containing thread helpers
                            Pointer helpers = UnicornPointer.register(emulator, Arm64Const.UC_ARM64_REG_X0);
                            if (log.isDebugEnabled()) {
                                log.debug("registerThreadHelpers helpers=" + helpers);
                            }
                            return 0;
                        }
                    });
                }
                address.setPointer(0, __dyld_register_thread_helpers);
                return 1;
            case "__dyld_image_path_containing_address":
                if (__dyld_image_path_containing_address == null) {
                    __dyld_image_path_containing_address = svcMemory.registerSvc(new ArmSvc() {
                        @Override
                        public int handle(Emulator emulator) {
                            UnicornPointer address = UnicornPointer.register(emulator, ArmConst.UC_ARM_REG_R0);
                            MachOModule module = (MachOModule) loader.findModuleByAddress(address.peer);
                            if (log.isDebugEnabled()) {
                                log.debug("__dyld_image_path_containing_address address=" + address + ", module=" + module);
                            }
                            if (module != null) {
                                return (int) module.createPathMemory(svcMemory).peer;
                            } else {
                                return 0;
                            }
                        }
                    });
                }
                address.setPointer(0, __dyld_image_path_containing_address);
                return 1;
            case "__dyld_register_func_for_remove_image":
                /*
                 * _dyld_register_func_for_remove_image registers the specified function to be
                 * called when an image is removed (a bundle or a dynamic shared library) from
                 * the program.
                 */
                if (__dyld_register_func_for_remove_image == null) {
                    __dyld_register_func_for_remove_image = svcMemory.registerSvc(new ArmSvc() {
                        @Override
                        public int handle(Emulator emulator) {
                            Pointer callback = UnicornPointer.register(emulator, ArmConst.UC_ARM_REG_R0);
                            if (log.isDebugEnabled()) {
                                log.debug("__dyld_register_func_for_remove_image callback=" + callback);
                            }
                            return 0;
                        }
                    });
                }
                address.setPointer(0, __dyld_register_func_for_remove_image);
                return 1;
            case "__dyld_register_func_for_add_image":
                /*
                 * _dyld_register_func_for_add_image registers the specified function to be
                 * called when a new image is added (a bundle or a dynamic shared library) to
                 * the program.  When this function is first registered it is called for once
                 * for each image that is currently part of the program.
                 */
                if (__dyld_register_func_for_add_image == null) {
                    __dyld_register_func_for_add_image = svcMemory.registerSvc(new ArmSvc() {
                        @Override
                        public UnicornPointer onRegister(SvcMemory svcMemory, int svcNumber) {
                            try (Keystone keystone = new Keystone(KeystoneArchitecture.Arm, KeystoneMode.Arm)) {
                                KeystoneEncoded encoded = keystone.assemble(Arrays.asList(
                                        "push {r4-r7, lr}",
                                        "svc #0x" + Integer.toHexString(svcNumber),
                                        "pop {r7}", // manipulated stack in dlopen
                                        "cmp r7, #0",
                                        "subne lr, pc, #16", // jump to pop {r7}
                                        "popne {r0-r1}", // (headerType *mh, unsigned long	vmaddr_slide)
                                        "bxne r7", // call init array
                                        "pop {r0, r4-r7, pc}")); // with return address
                                byte[] code = encoded.getMachineCode();
                                UnicornPointer pointer = svcMemory.allocate(code.length);
                                pointer.write(0, code, 0, code.length);
                                return pointer;
                            }
                        }

                        @Override
                        public int handle(Emulator emulator) {
                            final Unicorn unicorn = emulator.getUnicorn();

                            UnicornPointer callback = UnicornPointer.register(emulator, ArmConst.UC_ARM_REG_R0);
                            if (log.isDebugEnabled()) {
                                log.debug("__dyld_register_func_for_add_image callback=" + callback);
                            }

                            Pointer pointer = UnicornPointer.register(emulator, ArmConst.UC_ARM_REG_SP);
                            try {
                                pointer = pointer.share(-4); // return value
                                pointer.setInt(0, 0);

                                pointer = pointer.share(-4); // NULL-terminated
                                pointer.setInt(0, 0);

                                if (callback != null && !loader.addImageCallbacks.contains(callback)) {
                                    loader.addImageCallbacks.add(callback);

                                    for (Module md : loader.getLoadedModules()) {
                                        Log log = LogFactory.getLog("cn.banny.unidbg.ios." + md.name);
                                        MachOModule mm = (MachOModule) md;
                                        if (mm.executable) {
                                            continue;
                                        }

                                        // (headerType *mh, unsigned long	vmaddr_slide)
                                        pointer = pointer.share(-4);
                                        pointer.setInt(0, (int) mm.machHeader);
                                        pointer = pointer.share(-4);
                                        pointer.setInt(0, computeSlide(emulator, mm.machHeader));

                                        if (log.isDebugEnabled()) {
                                            log.debug("[" + md.name + "]PushAddImageFunction: 0x" + Long.toHexString(mm.machHeader));
                                        } else if (Dyld64.log.isDebugEnabled()) {
                                            Dyld64.log.debug("[" + md.name + "]PushAddImageFunction: 0x" + Long.toHexString(mm.machHeader));
                                        }
                                        pointer = pointer.share(-4); // callback
                                        pointer.setPointer(0, callback);
                                    }
                                }

                                return 0;
                            } finally {
                                unicorn.reg_write(ArmConst.UC_ARM_REG_SP, ((UnicornPointer) pointer).peer);
                            }
                        }
                    });
                }
                address.setPointer(0, __dyld_register_func_for_add_image);
                return 1;
            case "__dyld_dyld_register_image_state_change_handler":
                if (__dyld_dyld_register_image_state_change_handler == null) {
                    __dyld_dyld_register_image_state_change_handler = svcMemory.registerSvc(new ArmSvc() {
                        @Override
                        public UnicornPointer onRegister(SvcMemory svcMemory, int svcNumber) {
                            try (Keystone keystone = new Keystone(KeystoneArchitecture.Arm, KeystoneMode.Arm)) {
                                KeystoneEncoded encoded = keystone.assemble(Arrays.asList(
                                        "push {r4-r7, lr}",
                                        "svc #0x" + Integer.toHexString(svcNumber),
                                        "pop {r7}", // manipulated stack in dlopen
                                        "cmp r7, #0",
                                        "subne lr, pc, #16", // jump to pop {r7}
                                        "popne {r0-r2}", // const char* (*dyld_image_state_change_handler)(enum dyld_image_states state, uint32_t infoCount, const struct dyld_image_info info[])
                                        "bxne r7", // call init array
                                        "pop {r0, r4-r7, pc}")); // with return address
                                byte[] code = encoded.getMachineCode();
                                UnicornPointer pointer = svcMemory.allocate(code.length);
                                pointer.write(0, code, 0, code.length);
                                return pointer;
                            }
                        }
                        @Override
                        public int handle(Emulator emulator) {
                            Unicorn unicorn = emulator.getUnicorn();
                            int state = ((Number) unicorn.reg_read(ArmConst.UC_ARM_REG_R0)).intValue();
                            int batch = ((Number) unicorn.reg_read(ArmConst.UC_ARM_REG_R1)).intValue();
                            UnicornPointer handler = UnicornPointer.register(emulator, ArmConst.UC_ARM_REG_R2);
                            DyldImageInfo[] imageInfos;
                            if (batch == 1) {
                                imageInfos = registerImageStateBatchChangeHandler(state, handler, emulator);
                            } else {
                                imageInfos = registerImageStateSingleChangeHandler(state, handler, emulator);
                            }

                            Pointer pointer = UnicornPointer.register(emulator, ArmConst.UC_ARM_REG_SP);
                            try {
                                pointer = pointer.share(-4); // return value
                                pointer.setInt(0, 0);

                                pointer = pointer.share(-4); // NULL-terminated
                                pointer.setInt(0, 0);

                                if (handler != null && imageInfos != null) {
                                    // (*dyld_image_state_change_handler)(enum dyld_image_states state, uint32_t infoCount, const struct dyld_image_info info[])
                                    pointer = pointer.share(-4);
                                    pointer.setPointer(0, imageInfos.length == 0 ? null : imageInfos[0].getPointer());
                                    pointer = pointer.share(-4);
                                    pointer.setInt(0, imageInfos.length);
                                    pointer = pointer.share(-4);
                                    pointer.setInt(0, state);

                                    if (log.isDebugEnabled()) {
                                        log.debug("PushImageHandlerFunction: " + handler + ", imageSize=" + imageInfos.length);
                                    }
                                    pointer = pointer.share(-4); // handler
                                    pointer.setPointer(0, handler);
                                }

                                return 0;
                            } finally {
                                unicorn.reg_write(ArmConst.UC_ARM_REG_SP, ((UnicornPointer) pointer).peer);
                            }
                        }
                    });
                }
                address.setPointer(0, __dyld_dyld_register_image_state_change_handler);
                return 1;
            default:
                log.info("_dyld_func_lookup name=" + name + ", address=" + address);
                break;
        }
        address.setPointer(0, null);
        return 0;
    }

    private int dlopen(Memory memory, String path, Emulator emulator) {
        Unicorn unicorn = emulator.getUnicorn();
        Pointer pointer = UnicornPointer.register(emulator, ArmConst.UC_ARM_REG_SP);
        try {
            Module module = memory.dlopen(path, false);
            if (module == null) {
                pointer = pointer.share(-4); // return value
                pointer.setInt(0, 0);

                pointer = pointer.share(-4); // NULL-terminated
                pointer.setInt(0, 0);

                log.info("dlopen failed: " + path);
                this.error.setString(0, "Resolve library " + path + " failed");
                return 0;
            } else {
                pointer = pointer.share(-4); // return value
                pointer.setInt(0, (int) module.base);

                pointer = pointer.share(-4); // NULL-terminated
                pointer.setInt(0, 0);

                for (Module m : memory.getLoadedModules()) {
                    MachOModule mm = (MachOModule) m;
                    if (mm.hasUnresolvedSymbol()) {
                        continue;
                    }
                    for (InitFunction initFunction : mm.initFunctionList) {
                        if (log.isDebugEnabled()) {
                            log.debug("[" + mm.name + "]PushModInitFunction: 0x" + Long.toHexString(initFunction.getAddress()));
                        }
                        pointer = pointer.share(-4); // init array
                        pointer.setInt(0, (int) initFunction.getAddress());
                    }
                    mm.initFunctionList.clear();
                }

                return (int) module.base;
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            unicorn.reg_write(ArmConst.UC_ARM_REG_SP, ((UnicornPointer) pointer).peer);
        }
    }

    private DyldImageInfo[] registerImageStateBatchChangeHandler(int state, UnicornPointer handler, Emulator emulator) {
        if (log.isDebugEnabled()) {
            log.debug("registerImageStateBatchChangeHandler state=" + state + ", handler=" + handler);
        }

        if (state != dyld_image_state_bound) {
            throw new UnsupportedOperationException("state=" + state);
        }

        if (loader.boundHandlers.contains(handler)) {
            return null;
        }
        loader.boundHandlers.add(handler);
        return generateDyldImageInfo(emulator);
    }

    private DyldImageInfo[] generateDyldImageInfo(Emulator emulator) {
        List<DyldImageInfo> list = new ArrayList<>(loader.getLoadedModules().size());
        int elementSize = UnicornStructure.calculateSize(DyldImageInfo.class);
        Pointer pointer = emulator.getSvcMemory().allocate(elementSize * loader.getLoadedModules().size());
        for (Module module : loader.getLoadedModules()) {
            MachOModule mm = (MachOModule) module;
            DyldImageInfo info = new DyldImageInfo(pointer);
            info.imageFilePath = mm.createPathMemory(emulator.getSvcMemory());
            info.imageLoadAddress = UnicornPointer.pointer(emulator, mm.machHeader);
            info.imageFileModDate = 0;
            info.pack();
            list.add(info);
            pointer = pointer.share(elementSize);
        }
        return list.toArray(new DyldImageInfo[0]);
    }

    private DyldImageInfo[] registerImageStateSingleChangeHandler(int state, UnicornPointer handler, Emulator emulator) {
        if (log.isDebugEnabled()) {
            log.debug("registerImageStateSingleChangeHandler state=" + state + ", handler=" + handler);
        }

        if (state == dyld_image_state_terminated) {
            return null;
        }

        if (state != dyld_image_state_dependents_initialized) {
            throw new UnsupportedOperationException("state=" + state);
        }

        if (loader.initializedHandlers.contains(handler)) {
            return null;
        }
        loader.initializedHandlers.add(handler);
        return generateDyldImageInfo(emulator);
    }

    private long _abort;

    @Override
    public long hook(SvcMemory svcMemory, String libraryName, String symbolName, final long old) {
        if ("libsystem_c.dylib".equals(libraryName)) {
            if ("_abort".equals(symbolName)) {
                if (_abort == 0) {
                    _abort = svcMemory.registerSvc(new Arm64Svc() {
                        @Override
                        public int handle(Emulator emulator) {
                            System.err.println("abort");
                            emulator.getUnicorn().reg_write(Arm64Const.UC_ARM64_REG_LR, AbstractARMEmulator.LR);
                            return 0;
                        }
                    }).peer;
                }
                return _abort;
            }
        } else if ("libsystem_pthread.dylib".equals(libraryName)) {
            if ("_pthread_getname_np".equals(symbolName)) {
                if (_pthread_getname_np == 0) {
                    _pthread_getname_np = svcMemory.registerSvc(new ArmHook() {
                        @Override
                        protected HookStatus hook(Unicorn u, Emulator emulator) {
                            Pointer thread = UnicornPointer.register(emulator, ArmConst.UC_ARM_REG_R0);
                            Pointer threadName = UnicornPointer.register(emulator, ArmConst.UC_ARM_REG_R1);
                            int len = ((Number) u.reg_read(ArmConst.UC_ARM_REG_R2)).intValue();
                            if (log.isDebugEnabled()) {
                                log.debug("_pthread_getname_np thread=" + thread + ", threadName=" + threadName + ", len=" + len);
                            }
                            byte[] data = Arrays.copyOf(Dyld64.this.threadName.getBytes(), len);
                            threadName.write(0, data, 0, data.length);
                            return HookStatus.LR(u, 0);
                        }
                    }).peer;
                }
                return _pthread_getname_np;
            }
        }
        return 0;
    }

    private long _pthread_getname_np;

    private int dlsym(Memory memory, long handle, String symbolName) {
        try {
            Symbol symbol = memory.dlsym(handle, symbolName);
            if (symbol == null) {
                this.error.setString(0, "Find symbol " + symbol + " failed");
                return 0;
            }
            return (int) symbol.getAddress();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private String threadName = "main";

    void pthread_setname_np(String threadName) {
        this.threadName = threadName;
        if (log.isDebugEnabled()) {
            log.debug("pthread_setname_np=" + threadName);
        }
    }

}
