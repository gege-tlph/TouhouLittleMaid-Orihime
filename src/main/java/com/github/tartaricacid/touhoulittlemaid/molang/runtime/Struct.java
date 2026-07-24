package com.github.tartaricacid.touhoulittlemaid.molang.runtime;

public interface Struct {
    Object getProperty(int name);

    void putProperty(int name, Object value);


    Struct copy();
}
