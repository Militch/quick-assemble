package me.militch.quick.assemble.extension

import me.militch.quick.assemble.bean.JiaGuBao
import org.gradle.api.Project

class PackConfig {
    JiaGuBao jiaGuBao = new JiaGuBao()
    PackConfig(Project project){
    }
    void JiaGuBao(Closure closure){
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = jiaGuBao
        closure()
    }
}
