# Quick-Assemble
一款基于 `Groovy` 开发的快速打包（加固） Android APK 文件的 Gradle 插件。

## 功能介绍

接入插件后，执行 `assemble[渠道名]Release` 任务。

插件会根据相应的配置执行加固操作并输出加固后的APK文件。

**提供360加固保支持。无需在：[360加固保](http://jiagu.360.cn/qcmshtml/firm.html)上传APK文件进行加固**

## 接入
配置项目 **根目录下** 的 `builder.gradle` 文件

```gradle
dependencies {
    classpath 'me.militch:quick-assemble:1.0.0'
}
```

## 使用/配置
在 **入口 module 目录下**的 `builder.gradle` 文件依赖插件
```
apply plugin: 'me.militch.quick-assemble'
```

### 加固保配置
***配置格式示例：***
```
PackConfig{
    JiaGuBao {
        homePath = 'C:\\360Jiagubao\\jiagu'
        username = '***'
        password = '***'
        inChannel = ['channel1','channel2','channel...']
    }
}
```
* homePath：加固保路径配置
* username：加固保登录的用户名
* password：加固保登录的密码
* inChannel：需要加固的渠道名称；数组形式，支持多渠道加固

配置完成后，当执行 `assemble[渠道名]Release` 或者 `assembleRelease` 后会自动上传加固编译好的APK文件并输出。

**输出路径与默认APK打包输出路径一致**

若加固成功后输出的文件名为：*app-[渠道名]-release_[版本号]_jiagu_sign.apk*

