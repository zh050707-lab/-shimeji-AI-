package com.group_finity.mascot.script;

import javax.script.ScriptEngine;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

/**
 * 使用 Nashorn（通过项目 lib 中的 shim 或 nashorn-core）作为默认 ScriptEngine 的提供者。
 */
public class NashornScriptEngineProvider implements ScriptEngineProvider {

    @Override
    public ScriptEngine getScriptEngine() {
        // 使用 ScriptFilter 来限制可见类（与原先实现行为一致）
        return new NashornScriptEngineFactory().getScriptEngine(new ScriptFilter());
    }
}
