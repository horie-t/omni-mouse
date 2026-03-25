# Raspberry Piのセットアップ

## Javaのセットアップ

### SDKMANのセットアップ
```bash
sudo apt update
sudo apt install zip
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
```

### Javaのバージョン管理
```bash
sdk install java 25.0.2-tem
java -version
```

## カメラのセットアップ

```bash
sudo apt install libcamera-v4l2 rpicam-apps
```