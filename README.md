# Arcade
Nikke 小游戏脚本   
- [x] Gift Factory
- [ ] BBQ Master

## 支持的环境
1080P 分辨率
- Windows
- Linux X.Org 串流

## 依赖
JDK 21+
<details>
<summary>Linux</summary>
xdotool
</details>

## 使用
1. 下载并安装 [JDK 21](https://www.oracle.com/cn/java/technologies/downloads/#java21)
2. 在 [Release](https://github.com/zunpiau/arcade/releases) 页面下载 zip，解压，进入目录
3. 运行
- Windows   
   以管理员身份运行 `start.bat`
- Linux   
   ```
  $ java -jar arcade.jar <回合毫秒数>
  ```

#### 额外的 JVM 参数

| 参数            | 用途                              |
|---------------|---------------------------------|
| arcade.debug  | 在 giftFactory/debug 目录下输出图片匹配结果 |
| arcade.title  | 指定游戏进程的标题                       |
| arcade.opencv | 指定 OpenCV 库文件路径                 |
