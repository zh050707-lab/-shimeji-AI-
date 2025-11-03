package com.group_finity.mascot.script;

import javax.script.ScriptEngine;

/**
 * 抽象脚本引擎提供者接口。
 * 实现可以使用 Nashorn、GraalJS 等任意脚本引擎。
 */
public interface ScriptEngineProvider {
    /**
     * 返回一个可用于编译/执行脚本的 ScriptEngine 实例（可以是共享或新建）。
     */
    ScriptEngine getScriptEngine();
}
