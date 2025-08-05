# JsonKit
`JsonKit` 用于统一使用json解析,方便切换`fastjson`,`gson`等内部实现

主要用于`Android`平台 后续适配也会以`Android`平台优先

## 使用

### 配置默认的解析库
如使用 `gson` 实现
1. 添加`gson`的依赖,引入`adapter-gson`
   ```groovy
   implementation(project(":core"))
   implementation("com.google.code.gson:gson:2.10.1")
   implementation(project(":adapter-gson"))
    ```
2. 设置为默认的解析
   ```java
   JsonAdapterFactory factory = GsonAdapterFactory.getInstance();
   JsonOptions options = new JsonOptions.Builder().setSerializeNulls(false).build();
   JsonKit instance = JsonKit.builder(factory).setOptions(options).build();
   JsonKit.installDefault(instance);
    ```
3. 使用
   ```java
   JsonKit jsonKit = JsonKit.getInstance();
   jsonKit.toJson(xxx);
   jsonKit.fromJson("xx",Xxx.class);
   ```
4. 创建自定义`JsonKit`配置
   ```java
   JsonAdapterFactory factory = GsonAdapterFactory.getInstance();
   JsonOptions options = new JsonOptions.Builder().setSerializeNulls(false).build();
   JsonKit jsonKit = JsonKit.builder(factory).setOptions(options).build();
   jsonKit.toJson(xxx);
   jsonKit.fromJson("xx",Xxx.class);
   
   // 获取到的是 JsonOptions 的副本,不支持修改
   JsonOptions opt = jsonKit.getOptions(); 
   ```
   
5. 在当前`JsonKit`基础上创建新的`JsonKit`对象, 复用`JsonAdapterFactory`
   ```java
   JsonKit jsonKit = JsonKit.getInstance()
        .newBuilder()
        .setOptions(options)
        .build();
   ```


   
## 下一步计划
- 使用统一注解+反射的方式（调研中）
- 使用统一注解,在编译时进行注解转换 