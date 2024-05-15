# OmniMouse Documents

## Raspberry Pi

* [GPIO and the 40-pin header](https://www.raspberrypi.com/documentation/computers/raspberry-pi.html#gpio-and-the-40-pin-headera)

## Raspberry Piのセットアップ

1. [Raspberry Pi 5をヘッドレスでセットアップ](./raspberry_pi_5_os_setup)
2. [ROS2(Jazzy Jalisco)をインストール](./ros2_setup)
3. [Raspberry Pi 5をバッテリーで稼働させる](./running_raspberry_pi_5_on_battery)

## 本リポジトリのセットアップ

Raspberry Piのコンソールで以下のコマンドを実行します。

```
mkdir -p ~/ros2_ws/src
cd ~/ros2_ws/src
git clone git@github.com:horie-t/omni-mouse.git
cd ~/ros2_ws
colcon build --symlink-install
```

## 動作例

* [Lチカ](./l_chika)
* [SPI通信でA/Dコンバーターの値を読み込む](./read_adc)