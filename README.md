# majia

将指定的包中的类移到新包名下，通过简单的配置就可以生成马甲包。



- 修改`.class`中的包名，对第三方库(`.jar`)中代码也有效
  - 通过 AGP 的 `Artifact API` 遍历所有 `.jar/.class`
  - 通过 ASM 的 `Remapper` 类可以轻松实现替换类名
- 修改布局XML中的包名，直接修改`resources-**.ap_`中的`BinaryXml`。
  - `process${variant}Resources` 后，所有资源文件都被打包在 `resources-**.ap_` 中
  - 遍历 `resources-**.ap_` 里所有的布局XML，通过 `BinaryXml` 解析并修改其中的字符串
- 修改合并后`AndroidManifest.xml`中的包名。
  - `process${variant}MainManifest` 后，可获得合并后的清单文件替换包名


## 使用

在 settings.gradle 中 

```groovy
pluginManagement {
  repositories {
    maven { url "https://gitee.com/ezy/repo/raw/cosmo/"}
    gradlePluginPortal()
    google()
    mavenCentral()
  }
}  
```

在根项目的 build.gradle 中，注册插件

```groovy 
plugins {  
    id "me.reezy.gradle.majia" version "1.0.0" apply false
}
```

在 app 的 build.gradle 中，应用插件并配置参数

```groovy 
apply plugin: "me.reezy.gradle.majia"


majia {
    // 为空处理所有的类，非空表示只处理这些包内的类
    scopes = ["com.sample.framework", "me.reezy.cosmo", "com.demo.app", "androidx.databinding.DataBinderMapperImpl"]

    // 包名映射，比如将所有的 "me.reezy.cosmo" 替换成 "hello.world"
    mappings = [
        "me.reezy.cosmo": "hello.world",
        "com.sample.framework": "hello.goodbye",
        "com.demo.app": "hello.hasayo",
    ]

    // 指定生效的variant
  variant = "release"
}
```


## Android Binary Xml 文件格式解析    

解析BinaryXml参考了此项目

https://juejin.cn/post/7005944481455439903   
https://github.com/senswrong/AndroidBinaryXml 

修复几它的几个BUG：

- 它对 isUtf8 和 isSorted 分两个字段(short)读取的，实际上这里应该读取一个flags(int)
- 当 isUtf8 为 true，且字符串长度大于127时，读到的长度是错误的
  - 这里的 utf8 字符串通常有四部分：`字符数(1) + 字节数(1) + utf8(n) + 0(1)`
  - 字符数大于127时会占2字节：`字符数(2) + 字节数(2) + utf8(n) + 0(1)`
  - 字节数大于127时会占2字节：`字符数(1) + 字节数(2) + utf8(n) + 0(1)`
- 字符串数据长度必须被4整除，否则需要用0填充


## 参考

Android Internals: Binary XML

- [Part One Example](https://justanapplication.wordpress.com/2011/09/22/android-internals-binary-xml-part-one-example/)
- [Part Two: The XML Chunk](https://justanapplication.wordpress.com/2011/09/22/android-internals-binary-xml-part-two-the-xml-chunk/)
- [Part Three: XML Node](https://justanapplication.wordpress.com/2011/09/22/android-internals-binary-xml-part-three-xml-node/)
- [Part Four: The XML Resource Map Chunk](https://justanapplication.wordpress.com/category/android/android-binary-xml/android-xml-resourcemap-chunk/)
- [Part Five: The XML Start And End Namespace Chunks](https://justanapplication.wordpress.com/2011/09/24/android-internals-binary-xml-part-five-the-start-and-end-namespace-chunks/)
- [Part Six: The XML Start Element Chunk](https://justanapplication.wordpress.com/category/android/android-binary-xml/android-xml-startelement-chunk/)
- [Part Seven: The XML End Element Chunk](https://justanapplication.wordpress.com/category/android/android-binary-xml/android-xml-endelement-chunk/)
- [Part Eight: The CDATA Chunk](https://justanapplication.wordpress.com/category/android/android-binary-xml/android-xml-cdata-chunk/) 


Android Internals: Resources

- [Part One: Resources And Chunks](https://justanapplication.wordpress.com/2011/09/13/android-internals-resources-part-one-resources-and-chunks/)
- [Part Two: Resource Table Example](https://justanapplication.wordpress.com/2011/09/14/android-internals-resources-part-two-resource-table-example/)
- [Part Three: The Table Chunk](https://justanapplication.wordpress.com/2011/09/14/android-internals-resources-part-three-the-table-chunk/)
- [Part Four: The StringPool Chunk](https://justanapplication.wordpress.com/2011/09/15/android-internals-resources-part-four-the-stringpool-chunk/)
- [Part Five: The Package Chunk](https://justanapplication.wordpress.com/2011/09/16/android-internals-resources-part-five-the-package-chunk/)
- [Part Six: The Typespec Chunk](https://justanapplication.wordpress.com/2011/09/17/android-internals-resources-part-six-the-typespec-chunk/)
- [Part Seven: The Type Chunk](https://justanapplication.wordpress.com/2011/09/18/android-internals-resources-part-seven-the-type-chunk/)
- [Part Eight: Resource Entries And Values](https://justanapplication.wordpress.com/2011/09/19/android-internals-resources-part-eight-resource-entries-and-values/)
- [Part Nine: Simple Resource Entry Examples](https://justanapplication.wordpress.com/2011/09/20/android-internals-resources-part-nine-simple-resource-entry-examples/)
- [Part Ten: Complex Resource Entry Examples](https://justanapplication.wordpress.com/2011/09/22/android-internals-resources-part-ten-complex-resource-entry-examples/)
 


ARSC 文件格式解析   
https://juejin.cn/post/6844903854165753863

Android 手把手分析resources.arsc     
https://juejin.cn/post/6844903911602683918

 



