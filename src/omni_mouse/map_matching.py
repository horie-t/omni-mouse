import asyncio
import cv2
import datetime
import numpy as np
import ray

from omni_mouse.image import MazeImageProcessor

@ray.remote
class MapMatchingActor:
    def __init__(self, camera_actor):
        self.camera_actor = camera_actor
        self.started = False

    def start(self):
        if not self.started:
            self.started = True
            asyncio.create_task(self._map_matching())

    def stop(self):
        self.started = False

    async def _map_matching(self):
        while self.started:

            # # カメラからフレームを取得(debug用)
            # main_frame = np.array(await self.camera_actor.get_last_frame.remote())
            # main_frame = cv2.cvtColor(main_frame, cv2.COLOR_RGB2BGR)
            # timestamp = time.strftime("%Y%m%d-%H%M%S")
            # imgpath = f"fish_eye_omni_mouse_{timestamp}.jpg"
            # cv2.imwrite(imgpath, main_frame)

            main_frame = np.array(await self.camera_actor.get_last_frame.remote())
            binary_frame = self._to_binary_frame(main_frame)
            
            # binary_frame = await self.camera_actor.get_last_frame.remote()
            # contours = MazeImageProcessor.calc_contours(binary_frame)

            await asyncio.sleep(0.1)

    def _to_binary_frame(self, main_frame):
        # 画像の一部を黒く塗りつぶす（マウスの赤色部分をマスク処理して抽出するため）
        cv2.rectangle(main_frame, (800, 350), (1450, 900), (0, 0, 0), -1)

        # HSV色空間に変換し赤色を抽出する
        hsv_frame = cv2.cvtColor(main_frame, cv2.COLOR_RGB2HSV)

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

        return binary_frame