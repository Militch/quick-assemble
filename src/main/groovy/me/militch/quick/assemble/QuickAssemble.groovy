package me.militch.quick.assemble
import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.builder.model.SigningConfig
import me.militch.quick.assemble.bean.JiaGuBao
import me.militch.quick.assemble.extension.PackConfig
import me.militch.quick.assemble.task.QuickAssembleTask
import org.gradle.api.DomainObjectSet
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
class QuickAssemble implements Plugin<Project>{
    static final String PACK_CONFIG_EXTENSION_NAME = "PackConfig"
    static final String QUICK_ASSEMBLE_TASK_NAME = "quickAssemble"

    PackConfig packConfig
    @Override
    void apply(Project project) {
        // 检查是否应用于Android 插件
        if(!checkHashAndroidPlugin(project)){
            throw new GradleException("You must apply the Android plugin!")
        }
        packConfig = project.extensions
                .create(PACK_CONFIG_EXTENSION_NAME,PackConfig,project)
        // 配置 QuickAssemble Task
        configQuickAssembleTask(project)
        project.afterEvaluate {
            /*
             * 在PROJECT 初始化后 初始化渠道列表.
             * 并且定义在需要加固的渠道执行 `assembleRelease` 任务之后执行Task
             */
            DomainObjectSet<ApplicationVariant> domainObjectSet = project.extensions.findByType(AppExtension).applicationVariants
            if(getConfigJiaGuBao() != null){
                project.logger.lifecycle("[加固保] - 正在初始化 - 渠道列表 - ${getConfigJiaGuBao().inChannel}")
                configJiaGuBao(domainObjectSet,getJiaGuBaoClosure())
            }
        }
    }
    /**
     * 加固保执行函数
     * @return （闭包函数）
     */
    def getJiaGuBaoClosure(){
        return { SigningConfig signingConfig,Task assemble,File filepath,String versionName ->
            Collection<String> tasks = getJiaGuBaoInChannelTask()
            tasks.each {
                if(assemble.name == it){
                    assemble.doLast {
                        logger.lifecycle("[加固保] - 正在加载配置...")
                        if(getConfigJiaGuBao().homePath == null){
                            logger.error("[加固保] - HOME路径为空")
                            return
                        }
                        File jarFile = new File(jarPath(getConfigJiaGuBao()))
                        if(!jarFile.exists()){
                            logger.error("[加固保] - HOME路径错误 - ${jarFile.absolutePath}")
                            return
                        }
                        File runJarFile = new File(runJarPath(getConfigJiaGuBao()))
                        if(!runJarFile.exists()){
                            logger.error("[加固保] - HOME路径错误 - ${runJarFile.absolutePath}")
                            return
                        }
                        logger.lifecycle("[加固保] - 路径配置加载成功")
                        def loginCmd = loadLoginCmd(getConfigJiaGuBao())
                        def loginResult = loginCmd.execute()
                        loginResult.waitFor()
                        def resultText = loginResult.text
                        if(resultText.contains("login success")){
                            logger.lifecycle("[加固保] - 登录成功，正在导入签名...")
                            def signCmd = loadSignCmd(getConfigJiaGuBao(),signingConfig)
                            def signResult = signCmd.execute()
                            signResult.waitFor()
                            def signResultText = signResult.text
                            if(signResultText.contains("save sign success")){
                                logger.lifecycle("[加固保] - 导入签名文件成功，正在加固中...")
                                def jiaguCmd = loadJiaGuCmd(getConfigJiaGuBao(),filepath,filepath.getParentFile())
                                def jiaguResult = jiaguCmd.execute()
                                jiaguResult.waitFor()
                                def jiaguResultText = jiaguResult.text
                                if(jiaguResultText.contains("任务完成_已签名")){
                                    File file = new File(filepath.getParentFile().getAbsolutePath() ,
                                            "${filepath.getName().replace(".apk","")}_${versionName.replace(".","")}_jiagu_temp.apk")
                                    if(file.exists()){
                                        file.delete()
                                    }
                                    if(filepath.exists()){
                                        filepath.delete()
                                    }
                                    logger.lifecycle("[加固保] - 加固完成 - ${file.absolutePath}")
                                }else{
                                    logger.error("[加固保] - 加固失败")
                                }
                            }else{
                                logger.error("[加固保] - 导入签名文件失败")
                            }
                        }else if(resultText.contains("login error 20002")){
                            logger.error("[加固保] - 登录错误，账号被封")
                        }else if(resultText.contains("login error 220")){
                            logger.error("[加固保] - 登录失败，密码错误")
                        }else if(resultText.contains("login error 1036")){
                            logger.error("[加固保] - 登录失败，账号不存在")
                        }else if(resultText.contains("param error")){
                            logger.error("[加固保] - 登录错误，参数错误")
                        }else {
                            logger.error("[加固保] - 登录错误")
                        }
                    }
                }
            }
        }
    }
    /**
     * 获取 JAVA 运行路径
     * @param jiaGuBao 配置信息
     * @return JAVA 运行路径
     */
    static String jarPath(JiaGuBao jiaGuBao){
        return "${jiaGuBao.homePath}\\java\\bin\\java.exe"
    }
    /**
     * 获取 JAR 运行路径
     * @param jiaGuBao 配置信息
     * @return JAR 运行路径
     */
    static String runJarPath(JiaGuBao jiaGuBao){
        return "${jiaGuBao.homePath}\\jiagu.jar"
    }
    /**
     * 加载签名 CMD 执行语句
     * @param jiaGuBao 配置信息
     * @param signingConfig 签名配置
     * @return CMD 执行语句
     */
    static String loadSignCmd(JiaGuBao jiaGuBao,SigningConfig signingConfig){
        return "${jarPath(jiaGuBao)} -jar " +
                "${runJarPath(jiaGuBao)} -importsign " +
                "${signingConfig.storeFile.getAbsolutePath()} ${signingConfig.storePassword} " +
                "${signingConfig.keyAlias} ${signingConfig.keyPassword}"
    }
    /**
     * 加载登录 CMD 执行语句
     * @param jiaGuBao 配置信息
     * @return CMD 执行语句
     */
    static String loadLoginCmd(JiaGuBao jiaGuBao){
        return "${jarPath(jiaGuBao)} -jar " +
                "${runJarPath(jiaGuBao)} -login " +
                "${jiaGuBao.username}  ${jiaGuBao.password}"
    }
    /**
     * 加载加固 CMD 执行语句
     * @param jiaGuBao 配置信息
     * @param apkFile 需要加固的APK文件
     * @param outPath 输出的文件
     * @return CMD 执行语句
     */
    static String loadJiaGuCmd(JiaGuBao jiaGuBao,File apkFile,File outPath){
        return "${jarPath(jiaGuBao)} -jar " +
                "${runJarPath(jiaGuBao)} -jiagu " +
                "${apkFile.getAbsolutePath()} ${outPath.getAbsolutePath()} -autosign -automulpkg"
    }
    /**
     * 加固保任务配置
     * @param domainObjectSet
     * @param closure 执行函数（闭包）
     */
    static void configJiaGuBao(DomainObjectSet<ApplicationVariant> domainObjectSet,Closure closure){
        domainObjectSet.all {variant ->
            if(variant.buildType.name == "release"&&variant.buildType.signingConfig != null){
                variant.outputs.each { output ->
                    closure(variant.buildType.signingConfig,output.assemble,output.outputFile,variant.versionName)
                }
            }
        }
    }
    /**
     * 检查是否应用于 Android Application
     * @param project Gradle 项目
     * @return 是否应用于 Android Application
     */
    static boolean checkHashAndroidPlugin(Project project){
        return project.plugins.hasPlugin('com.android.application')
    }
    /**
     * 配置 QuickAssemble Task
     * @param project Gradle 项目
     */
    private void configQuickAssembleTask(Project project){
        def task = project.tasks.create(QUICK_ASSEMBLE_TASK_NAME,QuickAssembleTask)
        task.packConfig = packConfig
        task.group = "build"
        task.dependsOn("assembleRelease")
    }
    /**
     * 获取加固保配置
     * @return 配置信息
     */
    private JiaGuBao getConfigJiaGuBao(){
        return packConfig.jiaGuBao
    }
    /**
     * 获取需要加固打包的 TASK 列表
     * @return TASK 列表
     */
    private Collection<String> getJiaGuBaoInChannelTask(){
        def tasks = []
        packConfig.jiaGuBao.inChannel.each {
            tasks << "assemble${it}Release"
        }
        return tasks
    }
}
