package com.group_finity.mascot.script;

import javax.script.ScriptEngine;

/**
 * 全局脚本引擎提供者注册/访问点。
 * 默认使用 NashornProvider，但可以在运行时替换为其它实现（例如 GraalProvider）。
 */
public final class ScriptEngineProviders {

    private static ScriptEngineProvider provider = new NashornScriptEngineProvider();

    private ScriptEngineProviders() {
    }

    public static ScriptEngine getEngine() {
        return provider.getScriptEngine();
    }

    public static void setProvider(final ScriptEngineProvider p) {
        if (p == null) {
            throw new IllegalArgumentException("ScriptEngineProvider cannot be null");
        }
        provider = p;
    }
}
