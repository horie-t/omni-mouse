import time
import threading
from picamera2 import Picamera2, Preview
from pynput import keyboard

tuning = Picamera2.load_tuning_file("pi5_imx219_160d.json")
picam2 = Picamera2(tuning=tuning)
picam2.configure(picam2.create_preview_configuration(main={"format": 'XRGB8888', "size": (800, 600)}))
# picam2.configure(picam2.create_preview_configuration(main={"format": 'XRGB8888', "size": (3280, 2464)}))
capture_config = picam2.create_still_configuration(main={"size": (1640, 1232)})

picam2.start_preview(Preview.QTGL)
picam2.start()

def capture_image():
    """3秒後に画像をキャプチャする関数"""
    time.sleep(3)
    timestamp = time.strftime("%Y%m%d-%H%M%S")
    picam2.switch_mode_and_capture_file(capture_config, f"image_{timestamp}.png")
    print(f"画像が image_{timestamp}.jpg として保存されました。")

def on_press(key):
    """キーが押されたときの処理"""
    try:
        if key.char == 'y':
            print("yキーが押されました。3秒後に画像をキャプチャします。")
            # 新しいスレッドで画像キャプチャ関数を実行
            threading.Thread(target=capture_image).start()
        elif key.char == 'q':
            print("qキーが押されました。プログラムを終了します。")
            return False

    except AttributeError:
        # 特殊なキー（Shift、Ctrlなど）は無視
        pass

def on_release(key):
    """キーが離されたときの処理"""
    if key == keyboard.Key.esc:
        # ESCキーが押されたらプログラムを終了
        return False

# キーボードリスナーを設定
with keyboard.Listener(on_press=on_press, on_release=on_release) as listener:
    listener.join()

picam2.close()
