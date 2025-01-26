# OmniMouse

This is a project to create a micro mouse using a Raspberry Pi 5 and an omni wheel and compete in a micro mouse competition.

Raspberry Pi 5とオムニホイールを使ったマイクロマウスを制作し、[マイクロマウス大会(クラシックサイズ競技)](https://www.ntf.or.jp/?page_id=25)に出場するプロジェクトです。

## 文書

1. [Raspberry Pi 5をヘッドレスでセットアップ](docs/raspberry_pi_5_os_setup.md)

## 環境構築

1. このリポジトリを `git clone` してください。  
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

まだ、Rayのサンプルプログラムが起動するだけ

```bash
uv rum -m omni-mouse
```

## 実験的実装

### キーボードの入力テスト

```bash
uv run python src/omni_mouse/experiment/keyinput.py
```