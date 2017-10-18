package me.militch.quick.assemble.task

import me.militch.quick.assemble.extension.PackConfig
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class QuickAssembleTask extends DefaultTask{
    Boolean zip = false
    PackConfig packConfig = null
    @TaskAction
    void start(){
        say()
    }

    void say(){
        println('config >>>[' + packConfig.jiaGuBao.username + "]>>>[" + packConfig.jiaGuBao.password + "]>>>[" + packConfig.jiaGuBao.inChannel)
        println('zip >>> ' + zip)
    }
}
