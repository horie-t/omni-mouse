import cv2
import numpy as np

class MazeImageProcessor:
    @classmethod
    def extract_red_area(cls, frame):
        """
        画像から赤色の領域を抽出し、二値化画像として返します。

        指定された画像の一部を黒く塗りつぶした後、HSV色空間に変換して
        赤色領域のみを抽出します。抽出された赤色領域は二値化処理され、
        白黒画像として返されます。

        Args:
            frame (numpy.ndarray): 処理対象のカラー画像（BGR形式）

        Returns:
            numpy.ndarray: 赤色領域が白（255）、それ以外が黒（0）の二値化画像
        """

        # 画像の一部を黒く塗りつぶす（マウスの赤色部分をマスク処理して抽出するため）
        cv2.rectangle(frame, (800, 350), (1450, 900), (0, 0, 0), -1)

        # HSV色空間に変換し赤色を抽出する
        hsv_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)

        # 赤色の範囲（HSV）を指定
        # (一般的に赤色は色相の両端にあるので2つ必要だが、マイクロマウスで使用する赤色は1つで足りる)
        lower_red = np.array([120, 30, 140])
        upper_red = np.array([179, 255, 255])

        # 赤色領域マスク作成
        mask = cv2.inRange(hsv_frame, lower_red, upper_red)

        # 元画像とマスクを合成
        red_extracted_frame = cv2.bitwise_and(frame, frame, mask=mask)

        # 白黒画像に変換
        gray_frame = cv2.cvtColor(red_extracted_frame, cv2.COLOR_BGR2GRAY)
        binary_frame = cv2.threshold(gray_frame, 1, 255, cv2.THRESH_BINARY)[1]

        return binary_frame



