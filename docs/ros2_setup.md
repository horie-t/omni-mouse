# ROS2(Jazzy Jalisco)をインストール

Raspberry Pi OSはDebian 12(Bookworm)がベースであり、BookwormをサポートするROS2はJazzy Jaliscoになっています。よって、Jazz Jaliscoをインストールします。ただし、ROS2はDebianはソースコードレベルでのサポートになっているので、ソースコードからインストールします。

## ソースからのインストール手順

### システムセットアップ

#### ロケールの設定
現状の設定の確認をしたところ、イギリスの設定になっていました。
```
$ locale
LANG=en_GB.UTF-8
LANGUAGE=
LC_CTYPE="en_GB.UTF-8"
LC_NUMERIC="en_GB.UTF-8"
LC_TIME="en_GB.UTF-8"
LC_COLLATE="en_GB.UTF-8"
LC_MONETARY="en_GB.UTF-8"
LC_MESSAGES="en_GB.UTF-8"
LC_PAPER="en_GB.UTF-8"
LC_NAME="en_GB.UTF-8"
LC_ADDRESS="en_GB.UTF-8"
LC_TELEPHONE="en_GB.UTF-8"
LC_MEASUREMENT="en_GB.UTF-8"
LC_IDENTIFICATION="en_GB.UTF-8"
LC_ALL=
```
ROS2の前提は`en_US.UTF-8`になっています。今回は、この他に`C.UTF-8`と`ja_JP.UTF-8`も変更できるようにしておきます。
まず、localsパッケージをインストールします。
```
sudo apt update && sudo apt install locales
```
`/etc/locale.gen`を編集します。
```
sudo vi /etc/locale.gen
```
3行を以下のようにアンコメントします。
```
# 前略
C.UTF-8 UTF-8
# 中略
en_US.UTF-8 UTF-8
# 中略
ja_JP.UTF-8 UTF-8
# 後略
```
ロケールを生成し、更新します。
```
sudo locale-gen
sudo update-locale LC_ALL=en_US.UTF-8 LANG=en_US.UTF-8
export LANG=en_US.UTF-8
```

#### 必要なリポジトリを有効化
必要なパッケージをインストールします。
```
sudo apt install software-properties-common
```
ROS 2 GPG keyを追加します。
```
sudo apt update && sudo apt install curl -y
sudo curl -sSL https://raw.githubusercontent.com/ros/rosdistro/master/ros.key -o /usr/share/keyrings/ros-archive-keyring.gpg
```
リポジトリを追加します。
```
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/ros-archive-keyring.gpg] http://packages.ros.org/ros2-testing/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) main" | sudo tee /etc/apt/sources.list.d/ros2.list > /dev/null
```
#### 開発ツールをインストール
以下のようにコマンドを実行してパッケージをインストールします。
```
sudo apt update && sudo apt install -y \
  python3-pip \
  python3-pytest-cov \
  python3-flake8-blind-except \
  python3-flake8-class-newline \
  python3-flake8-deprecated \
  python3-pytest-repeat \
  python3-pytest-rerunfailures \
  ros-dev-tools
```

### ROS 2のビルド

#### ROS 2のコードの取得

```
mkdir -p ~/ros2_jazzy/src
cd ~/ros2_jazzy
vcs import --input https://raw.githubusercontent.com/ros2/ros2/jazzy/ros2.repos src
```

#### rosdepを使って依存パッケージをインストール
`~/ros2_jazzy`ディレクトリで、以下のコマンドを実行します。
```
sudo apt upgrade
sudo rosdep init
rosdep update
rosdep install --from-paths src --ignore-src -y --skip-keys "fastcdr rti-connext-dds-6.0.1 urdfdom_headers"
```

#### RMWの追加インストール
RMWについては、デフォルトの`FastDDS`を使うのでスキップします。

#### ワークスペースでビルド
以下のようにビルドします。3〜4時間かかりました。

```
cd ~/ros2_jazzy/
colcon build --symlink-install
```

### 環境のセットアップ

```
. ~/ros2_jazzy/install/local_setup.bash
```

### 例題を実行
新しいターミナルを開いて以下のように`talker`を実行します。

```
$ . ~/ros2_jazzy/install/local_setup.bash
$ ros2 run demo_nodes_cpp talker
[INFO] [1714799965.074857896] [talker]: Publishing: 'Hello World: 1'
[INFO] [1714799966.074809413] [talker]: Publishing: 'Hello World: 2'
[INFO] [1714799967.074837432] [talker]: Publishing: 'Hello World: 3'
[INFO] [1714799968.074872230] [talker]: Publishing: 'Hello World: 4'
# 後略
```
別ターミナルで以下のように`listener`を実行します。`talker`の実行の後から開始したので、最初のメッセージは受け取れていません。
```
$ ros2 run demo_nodes_py listener
[INFO] [1714799984.097407047] [listener]: I heard: [Hello World: 20]
[INFO] [1714799985.076156630] [listener]: I heard: [Hello World: 21]
[INFO] [1714799986.076119553] [listener]: I heard: [Hello World: 22]
[INFO] [1714799987.076113662] [listener]: I heard: [Hello World: 23]
# 後略
```

終了するには、`Ctrl + C`を入力します。

無事、インストールできてROS 2が動きました。