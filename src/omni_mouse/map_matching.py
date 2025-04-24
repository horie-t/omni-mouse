import asyncio
import cv2
import datetime
import numpy as np
import ray
import time

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

            # カメラからフレームを取得(debug用)
            main_frame = np.array(await self.camera_actor.get_last_frame.remote())
            main_frame = cv2.cvtColor(main_frame, cv2.COLOR_RGB2BGR)
            timestamp = time.strftime("%Y%m%d-%H%M%S")
            imgpath = f"fish_eye_omni_mouse_{timestamp}.jpg"
            cv2.imwrite(imgpath, main_frame)
            
            # binary_frame = await self.camera_actor.get_last_frame.remote()
            # contours = self._calc_contours(binary_frame)

            await asyncio.sleep(0.1)
    
    def _calc_contours(self, binary_frame):
        # オープニングやクロージングを使って凹凸と減らす
        # ==============================================

        # 1. カーネル(構造要素)を定義
        #    サイズや形状(長方形/楕円/十字など)は目的に応じて変更します
        open_kernel = np.ones((7, 7), np.uint8)
        close_kernel = np.ones((17, 17), np.uint8)

        # 2. オープニング (MORPH_OPEN) で小さな白ノイズを除去
        opened_frame = cv2.morphologyEx(binary_frame, cv2.MORPH_OPEN, open_kernel)

        # 3. クロージング (MORPH_CLOSE) で小さな黒の穴を埋める
        #    必要に応じて「オープニング後にさらにクロージング」という順序でもOK
        closed_frame = cv2.morphologyEx(opened_frame, cv2.MORPH_CLOSE, close_kernel)


        # 凹凸をなくしたフレームの輪郭を抽出する
        # ======================================
        # 輪郭検出（境界部分）
        contours, _ = cv2.findContours(closed_frame, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

        # 可視化用
        contour_frame = np.zeros((binary_frame.shape[0], binary_frame.shape[1], 3), np.uint8)

        # 輪郭描画
        cv2.drawContours(contour_frame, contours, -1, (255, 255, 255), 2)
        for contour in contours:
            for point in contour:
                x, y = point[0]
                cv2.circle(contour_frame, (x, y), 2, (0, 0, 255), -1)

        # 画像保存(デバッグ用)
        imgpath = f"contour_{datetime.datetime.now().strftime('%Y%m%d%H%M%S')}.png"
        cv2.imwrite(imgpath, contour_frame)

        return contours

    def _to_binary_frame(self, main_frame):
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

        return binary_frame