# Raspberry Pi 5をバッテリーで稼働させる

## 使用ハードウェア

* [Raspberry Pi 5(8GB)](https://amzn.to/3JHBZtJ)
* [マイクロSDカード 64GB](https://amzn.to/4bgQPmT)
* [降圧コンバータモジュール24V/12V〜5V 5A](https://amzn.to/3JOahvB)
* [LiPoバッテリー](https://amzn.to/3QvTPnu)

## 構成図

下図のように接続します。

![](images/RunOnBattery.svg)

## EEPROMの設定変更

起動後に以下の警告が表示されます。

> This power supply is not capable of supplying 5A
> 
> Power to peripherals will be restricted

以下のコマンドを実行して、電源の電流供給能力が5Aあるとみなすようにさせます。

```
sudo rpi-eeprom-config -e
```

コマンドの編集画面で以下の行を追加します。

```
PSU_MAX_CURRENT=5000
```