# Arcade
Nikke mini game script   
- [x] Gift Factory
- [x] Dessert Rush
- [ ] BBQ Master

## Env
1080P resolution
- Windows
- Linux X.Org streaming

## Dependency
JDK 21+
<details>
<summary>Linux</summary>
xdotool
</details>

## Usage
1. Download and install [JDK 21](https://www.oracle.com/cn/java/technologies/downloads/#java21)
2. Download zip in [Release](https://github.com/zunpiau/arcade/releases) ，and unpacking
3. Enter the directory, run `start.bat`，as administrator, which will execute `java -jar arcade.jar`   

### Parameters

| Game         | Index | Name                          | Unit | Default | Min | Max |
|--------------|-------|-------------------------------|------|---------|-----|-----|
| Gift Factory | 1     | Time per round                | ms   | 250     | 100 | 500 |
| Gift Factory | 2     | Click interval during burst   | ms   | 2       | 1   | 12  |
| Dessert Rush | 1     | Click interval                | ms   | 200     | 10  | 500 |
| Dessert Rush | 2     | Click interval during burst   | ms   | 10      | 1   | 500 |
| Dessert Rush | 3     | Number of clicks during burst | /    | 10      | 1   | 100 |


<details>
<summary>### JVM options</summary>

| Option        | Usage                                                 | Default                  |
|---------------|-------------------------------------------------------|--------------------------|
| arcade.debug  | Output image recognite results in the debug directory | false                    |
| arcade.title  | Specify the game process title                        | "Nikke" \| "- Moonlight" |
| arcade.opencv | Specify the OpenCV library path                       |                          |
</details>
