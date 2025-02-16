# OmniMouse

This is a project to create a micro mouse using a Raspberry Pi 5 and an omni wheel and compete in a micro mouse competition.

Raspberry Pi 5とオムニホイールを使ったマイクロマウスを制作し、[マイクロマウス大会(クラシックサイズ競技)](https://www.ntf.or.jp/?page_id=25)に出場するプロジェクトです。

## 文書

1. [Raspberry Pi 5をヘッドレスでセットアップ](docs/raspberry_pi_5_os_setup.md)

## ハードウェア制作

ハードウェアの設計は[FreeCAD](https://www.freecad.org/index.php?lang=ja)で行っています。設計データは[hardware](./hardware/) ディレクトリの `.FCStd` ファイルです。

3Dプリンタで [hardware](./hardware/) ディレクトリの `.stl` ファイルを使って3Dプリンティングしてください。

## 電装系

[こちら](./electrical_system/OmniMouse_Circuit.png) のように各種部品を接続してください。回路図は[Fritzing](https://fritzing.org/)を使用して書いています。

## Raspberry Pi 5 セットアップ

#### OSのインストール

[Raspberry Pi 5をヘッドレスでセットアップ](docs/raspberry_pi_5_os_setup.md)を参照してください。

#### EEPROMの設定変更

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

#### SPI1デバイスの有効化

`/boot/firmware/config.txt` ファイルを編集します。

```bash
sudo vi /boot/firmware/config.txt
```

`dtparam=spi=on` をアンコメントして、SPI1の定義を追加します。

```txt
# 前略
# 以下をアンコメントします。
dtparam=spi=on
# 中略
# 最後を以下のようにします。
[all]
dtoverlay=spi1-3cs,cs1_pin=27
```

#### Picamera2をシステムのPython環境に依存せずに使うための準備

Pythonのパッケージをシステムにインストールしているが、目的はPythonのパッケージが依存しているdebパッケージをインストールする事である。

```bash
sudo apt install -y python3-libcamera python3-kms++ libcap-dev
sudo apt install -y python3-prctl libatlas-base-dev ffmpeg python3-pip
sudo apt install -y python3-pyqt5 python3-opengl

sudo apt install -y libcap-dev libatlas-base-dev libopenjp2-7
sudo apt install -y libkms++-dev libfmt-dev libdrm-dev
sudo apt install -y libcamera-dev
```

## ソフトウェア実行環境構築

1. Raspberry Pi上で、このリポジトリを `git clone` してください。  
    ```bash
    git@github.com:horie-t/omni-mouse.git
    cd omni-mouse
    ```
2. パッケージをインストールしてください。  
    ```bash
    uv sync
    ```
3. 仮想環境をアクティベートしてください。
    ```bash
    source .venv/bin/activate
    ```

## 実行

まだ、Rayのサンプルプログラムが起動するだけです。

```bash
uv rum -m omni-mouse
```

## 実験的実装

### キーボードの入力テスト

```bash
uv run python src/omni_mouse/experiment/keyinput.py
```

## ディレクトリ構成

主なディレクトリ

```
.
├── docs                # 説明用の文書
├── electrical_system   # 電装系設計データ
├── hardware            # ハードウェア設計データ
└── src                 # ソースコード
```