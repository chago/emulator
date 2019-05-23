package cn.banny.unidbg.hook.hookzz;

import cn.banny.unidbg.Symbol;
import cn.banny.unidbg.arm.RegisterContext;
import cn.banny.unidbg.hook.ReplaceCallback;

public interface IHookZz {

    int RS_SUCCESS = 1;

    void enable_arm_arm64_b_branch();
    void disable_arm_arm64_b_branch();

    <T extends RegisterContext> void wrap(long functionAddress, WrapCallback<T> callback);
    <T extends RegisterContext> void wrap(Symbol symbol, WrapCallback<T> callback);

    void replace(long functionAddress, ReplaceCallback callback);
    void replace(Symbol symbol, ReplaceCallback callback);

}
