# Arcade
Nikke 小游戏脚本   
- [x] Gift Factory
- [x] Dessert Rush
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
3. 进入目录，以管理员身份运行 `start.bat`，将会执行 `java -jar arcade.jar`   

### 程序参数

| 小游戏          | 位置 | 用途       | 单位 | 默认值 | 最小值 | 最大值 |
|--------------|----|----------|----|-----|-----|-----|
| Gift Factory | 1  | 每回合时间    | 毫秒 | 250 | 100 | 500 |
| Gift Factory | 2  | 爆裂期间点击间隔 | 毫秒 | 2   | 1   | 12  |
| Dessert Rush | 1  | 点击间隔    | 毫秒 | 200 | 10  | 500 |
| Dessert Rush | 2  | 爆裂期间点击间隔 | 毫秒 | 10  | 1   | 500 |
| Dessert Rush | 3  | 爆裂期间点击次数 | /  | 10  | 1   | 100 |


<details>
<summary>### JVM 参数</summary>

| 参数            | 用途                  | 默认值                      |
|---------------|---------------------|--------------------------|
| arcade.debug  | 在 debug 目录下输出图片匹配结果 | false                    |
| arcade.title  | 指定游戏进程的标题           | "Nikke" \| "- Moonlight" |
| arcade.opencv | 指定 OpenCV 库文件路径     |                          |
</details>