package com.github.unidbg.hook.hookzz;

import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.arm.context.AbstractRegisterContext;
import com.github.unidbg.pointer.UnicornPointer;
import com.github.unidbg.spi.ValuePair;

import java.util.Map;

public abstract class HookZzRegisterContext extends AbstractRegisterContext implements RegisterContext, ValuePair {

    private final Map<String, Object> context;

    HookZzRegisterContext(Map<String, Object> context) {
        this.context = context;
    }

    @Override
    public void set(String key, Object value) {
        context.put(key, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(String key) {
        return (T) context.get(key);
    }

    @Override
    public UnicornPointer getPCPointer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getInt(int regId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLong(int regId) {
        throw new UnsupportedOperationException();
    }
}
