import cv2
import datetime
from importlib import resources
import numpy as np

class MazeImageProcessor:
    calibrated = False
    map1 = None
    map2 = None

    @classmethod
    def load_fisheye_calibration_data(cls):
        if not cls.calibrated:
            with resources.files("omni_mouse.data").joinpath("fisheye_calibration.npz").open("rb") as f:
                # キャリブレーションデータの読み込み
                calibration_data = np.load(f)  # キャリブレーションデータを保存したファイル名
                k = calibration_data["K"]
                d = calibration_data["D"]
                dim = calibration_data["DIM"]

                # オプション: より詳細なマッピングのための新しいカメラ行列の計算
                # balance=1.0 で画像の全体を表示、balance=0.0 で中心部分のみを表示
                new_k = cv2.fisheye.estimateNewCameraMatrixForUndistortRectify(k, d, dim, np.eye(3), balance=0.0)
                cls.map1, cls.map2 = cv2.fisheye.initUndistortRectifyMap(k, d, np.eye(3), new_k, dim, cv2.CV_16SC2)

                cls.calibrated = True

    @classmethod
    def undistort_fisheye_image(cls, img: np.ndarray) -> np.ndarray:
        """
        魚眼レンズ画像を指定されたパラメータで補正します。

        Args:
            img (str): 歪んだ魚眼レンズ画像のパス
        Returns:
            歪を取り除いた画像
        """
        undistorted_img = cv2.remap(img, cls.map1, cls.map2, interpolation=cv2.INTER_LINEAR, borderMode=cv2.BORDER_CONSTANT)
        return undistorted_img

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

    @classmethod
    def calc_contours(cls, binary_frame):
        """
        二値化画像に対してモルフォロジー処理を行い、輪郭を検出します。

        入力された二値化画像に対して、ノイズ除去と穴埋めのための
        モルフォロジー演算（オープニング・クロージング）を適用した後、
        外部輪郭を検出して返します。

        処理手順:
        1. オープニング演算により小さな白ノイズを除去
        2. クロージング演算により小さな黒い穴を埋める
        3. 処理後の画像から外部輪郭を検出

        Args:
            binary_frame (numpy.ndarray): 処理対象の二値化画像（白：255、黒：0）

        Returns:
            list: 検出された輪郭のリスト。各輪郭はOpenCVの輪郭形式
              （numpy.ndarrayの座標点の配列）
        """

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

        # # 画像保存(デバッグ用)
        # imgpath = f"contour_{datetime.datetime.now().strftime('%Y%m%d%H%M%S')}.png"
        # cv2.imwrite(imgpath, contour_frame)

        return contours