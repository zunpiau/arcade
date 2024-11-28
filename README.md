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
2. 在 [Release](https://github.com/zunpiau/arcade/releases) 页面下载 zip，解压
3. 进入目录，以管理员身份运行 `start.bat`，将会执行 `java -jar arcade.jar 300 6`   

#### 程序参数
   第一个参数 `300` 为每回合毫秒数，范围 100～500，过小可能造成卡壳   
   第二个参数 `6` 为爆裂期间点击间隔毫秒数，范围 1～12，过小会导致游戏短暂卡顿

<details>
<summary>#### JVM 参数</summary>

| 参数           | 用途                              | 默认值                      |
|--------------|---------------------------------|--------------------------|
| arcade.debug | 在 giftFactory/debug 目录下输出图片匹配结果 | false                    |
| arcade.title | 指定游戏进程的标题                       | "Nikke" \| "- Moonlight" |
| arcade.openc | 指定 OpenCV 库文件路径                 |                          |
</details>