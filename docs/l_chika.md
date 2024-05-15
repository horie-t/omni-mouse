# ROSでLチカ

## 準備

下図のようにLEDと抵抗(330Ω)を接続します。Raspberry Piのピンは22番(GPIO 25)と39番(GND)を使用します。

![](./images/LChika_Breadboard.svg)

## 実行

以下のようにコマンドを実行します。コンソールにメッセージが表示されると共にLDEが点滅します。

```
$ ros2 run l_chika l_chika_node
Start program
Generate Node
[INFO] [1714888685.958845078] [l_chika_node]: Hi from l_chika.
[INFO] [1714888686.442169731] [l_chika_node]: Hi from l_chika.
[INFO] [1714888686.942117191] [l_chika_node]: Hi from l_chika.
[INFO] [1714888687.442118320] [l_chika_node]: Hi from l_chika.
```

## ソースコード

ソースコードは[こちら](https://github.com/horie-t/omni-mouse/tree/main/l_chika)。