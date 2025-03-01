import cv2
import numpy as np
from picamera2 import Picamera2
import ray

@ray.remote
class CameraActor:
    def __init__(self):
        self.camera = Picamera2(0)
        config = self.camera.create_video_configuration(main={"size": (2304, 1296), "format": "RGB888"})
        self.camera.configure(config)
        self.camera.post_callback = self._on_new_frame
        self._last_frame = None

    def start(self):
        self.camera.start()

    def stop(self):
        self.camera.stop()

    def get_last_frame(self):
        return self._last_frame

    def _on_new_frame(self, request):
        main_frame = request.make_array("main")

        # 画像の一部を黒く塗りつぶす（マウスの赤色部分をマスク処理して抽出するため）
        cv2.rectangle(main_frame, (800, 350), (1450, 900), (0, 0, 0), -1)

        # HSV色空間に変換し赤色を抽出する
        hsv_frame = cv2.cvtColor(main_frame, cv2.COLOR_BGR2HSV)

        # 赤色の範囲（HSV）を指定
        # (一般的に赤色は色相の両端にあるので2つ必要だが、マイクロマウスで使用する赤色は1つで足りる)
        lower_red = np.array([120, 30, 140])
        upper_red = np.array([179, 255, 255])

        # 赤色領域マスク作成
        mask = cv2.inRange(hsv_frame, lower_red, upper_red)

        # 元画像とマスクを合成
        red_extracted_frame = cv2.bitwise_and(main_frame, main_frame, mask=mask)

        # 白黒画像に変換
        gray_frame = cv2.cvtColor(red_extracted_frame, cv2.COLOR_BGR2GRAY)
        binary_frame = cv2.threshold(gray_frame, 1, 255, cv2.THRESH_BINARY)[1]

        self._last_frame = binary_frame
        #self._last_frame = main_frame # デバッグ用

