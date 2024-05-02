# Raspberry Pi 5をヘッドレスでセットアップ

## 使用ハードウェア

* [Raspberry Pi 5(8GB)](https://amzn.to/3JHBZtJ)
* [Raspberry Pi 5用電源アダプター](https://amzn.to/4dkVHJA)
* [マイクロSDカード 64GB](https://amzn.to/4bgQPmT)
* Ubuntu 22.04をインストール済みPC(Raspberry Piと同一のサブネットに接続してある事)

## マイクロSDカードにRaspberry Pi OSを書き込む

###### SDカードの接続
1. SDカードをPCに接続します。

###### Raspberry Pi Imagerのインストール
1. [Raspberry Pi Imager](https://www.raspberrypi.com/software/)のWebページからImagerをダウンロードしてインストールします。

###### OSイメージのSDカードへの書き込み
1. Imagerを起動して「デバイスを選択」をクリックします。  
   ![](https://storage.googleapis.com/zenn-user-upload/005bbb6a88e3-20240502.png)
2. 「Raspberry Pi 5」をクリックします。    
   ![](https://storage.googleapis.com/zenn-user-upload/9ae89f22b117-20240502.png)
3. 「OSを選択」をクリックして「Raspberry Pi OS(64-bit)」をクリックします。  
   ![](https://storage.googleapis.com/zenn-user-upload/6702c53c0812-20240502.png)
4. 「ストレージを選択」をクリックしてSDカードを選択します。  
   ![](https://storage.googleapis.com/zenn-user-upload/ed5439bc99d6-20240502.png)
5. 以下のように選択結果が表示されるので「次へ」をクリックします。  
   ![](https://storage.googleapis.com/zenn-user-upload/95f570ce0291-20240502.png)
6. カスタマイズの選択画面で「設定を編集する」をクリックします。  
   ![](https://storage.googleapis.com/zenn-user-upload/805e6845ff95-20240502.png)
7. カスタマイズ画面の「一般」タブで、チェックボックスにチェックをして、値を入力します。  
   ![](https://storage.googleapis.com/zenn-user-upload/c6fa3e0e08d6-20240502.png)
8. 「サービス」タブで、チェックボックスにチェックをしてSSHを有効化し、「保存」をクリックします。
   ![](https://storage.googleapis.com/zenn-user-upload/2985aa96fbbd-20240502.png)
9. カスタマイズの選択画面で「はい」をクリックします。  
   ![](https://storage.googleapis.com/zenn-user-upload/25829b827132-20240502.png)
10. 「警告」画面で「はい」をクリックします。   
   ![](https://storage.googleapis.com/zenn-user-upload/58d4dfc6eb08-20240502.png)
11. 以下のように、書き込み中画面が表示されます。  
   ![](https://storage.googleapis.com/zenn-user-upload/85993a7e6ef4-20240502.png)
12. 書き込みが終わると確認中画面が表示されます。  
   ![](https://storage.googleapis.com/zenn-user-upload/346edfce6c95-20240502.png)
13. 確認が完了すると正常終了画面が表示されます。  
   ![](https://storage.googleapis.com/zenn-user-upload/ecd1f48de086-20240502.png)    
   
###### SDカードの取り外し
1. SDカードをPCから取り外します。

###### Raspberry Piの起動とログイン
1. SDカードをRaspberry Piに挿入します。
2. 電源アダプターをRaspberry Piに接続します。
3. 数分待ちます。(OSの初期設定も行われるため時間がかかります)
4. PCからSSHで接続します。(ユーザー名はOSイメージ作成時のものに合わせます)  
   ```
   tetsuya@vanadium:~$ ssh tetsuya@raspberrypi.local
   The authenticity of host 'raspberrypi.local (192.168.1.32)' can't be established.
   ED25519 key fingerprint is SHA256:hdGO5F5tPTMadZijcrCnYV4kn1A+aXXayDZZJjZF54I.
   This key is not known by any other names
   Are you sure you want to continue connecting (yes/no/[fingerprint])? yes
   Warning: Permanently added 'raspberrypi.local' (ED25519) to the list of known hosts.
   tetsuya@raspberrypi.local's password:
   Linux raspberrypi 6.6.20+rpt-rpi-2712 #1 SMP PREEMPT Debian 1:6.6.20-1+rpt1 (2024-03-07) aarch64
   
   The programs included with the Debian GNU/Linux system are free software;
   the exact distribution terms for each program are described in the
   individual files in /usr/share/doc/*/copyright.
   
   Debian GNU/Linux comes with ABSOLUTELY NO WARRANTY, to the extent
   permitted by applicable law.
   Last login: Sat Mar 16 00:12:26 2024
   tetsuya@raspberrypi:~ $
   ```

###### VNCのセットアップ
1. Raspberry Piにログインした状態で以下のコマンドを実行します。　　
   ```
   sudo raspi-config
   ```
2. 「3 Interface Options」を選択します。  
   ![](https://storage.googleapis.com/zenn-user-upload/5c8e82d7665c-20240502.png)
3. 「I2 VNC」を選択します。  
  ![](https://storage.googleapis.com/zenn-user-upload/dabbaee992d6-20240502.png)
4. 「Yes」を選択します。
   ![](https://storage.googleapis.com/zenn-user-upload/21f5ab9a263c-20240502.png)
5. 「Ok」を選択します。
   ![](https://storage.googleapis.com/zenn-user-upload/6a5ad46a9e46-20240502.png)
6. PC側のターミナルで以下のコマンドを実行して「TigerVNC」のViewerをインストールします。(BookwormベースのRaspberry Pi OSは
ディスプレイサーバがWaylandに変更になっているのでRealVNCのViewer等ではなくTigerVNCのViewerをインストールする必要があります)
   ```
   sudo apt-get -y install tigervnc-viewer
   ```
7. TigerVNCのViewerを起動します。
   ```
   vncviewer
   ```
8. 「VNC Server」にRaspberry Piのホスト名を入力し「Connect」をクリックします。
   ![](https://storage.googleapis.com/zenn-user-upload/fd5a61a710a2-20240502.png)
9. 「Username」、「Password」を入力して「OK」をクリックします。  
   ![](https://storage.googleapis.com/zenn-user-upload/ebcc77620067-20240502.png)
10. Raspberry Piのデスクトップ画面が表示されます。
   ![](https://storage.googleapis.com/zenn-user-upload/a3d34cf6e488-20240502.png)
