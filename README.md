# OmniMouse

This is a project to create a micro mouse using a Raspberry Pi 5 and an omni wheel and compete in a micro mouse competition.

Raspberry Pi 5とオムニホイールを使ったマイクロマウスを制作し、マイクロマウス大会に出場するプロジェクトです。

## ハードウェア構成

Raspberry Pi 5とオムニホイールを組み合わせてマイクロマウスを作成します。ハードウェア構成は以下の通りです。

- Raspberry Pi 5
- オムニホイール (直径: 48 mm, 幅: 25.5 mm) 3個
- ステッピングモーター (ステップ角: 1.8°) 3個
- モータードライバー L6470 3個
- センサー（9軸センサー）
- カメラ (迷路の先読み前方カメラ、壁近接検知見下ろしカメラ)

## ソフトウェア構成

- Raspberry Pi OS (bookworm)
- Java 25
- Spring Boot / WebFlux
- Pi4J
- JavaCV

## Raspberry Pi OSのセットアップ

[こちら](./docs/setup.md)を参照してください。