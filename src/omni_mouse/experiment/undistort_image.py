import cv2
import numpy as np
import sys
def undistort_fisheye_image(input_image_path, output_image_path, K, D, DIM):
    """
    魚眼レンズ画像を指定されたパラメータで補正します。

    Args:
        input_image_path (str): 歪んだ魚眼レンズ画像のパス
        output_image_path (str): 補正後の画像の保存パス
        K (numpy.ndarray): カメラ行列 (3x3)
        D (numpy.ndarray): 歪み係数 (4x1 または 8x1)
        DIM (tuple): 歪んだ画像の寸法 (幅, 高さ)
    """

    img = cv2.imread(input_image_path)
    if img is None:
        print(f"Error: Could not read image from {input_image_path}")
        return

    # オプション: より詳細なマッピングのための新しいカメラ行列の計算
    # balance=1.0 で画像の全体を表示、balance=0.0 で中心部分のみを表示
    new_K = cv2.fisheye.estimateNewCameraMatrixForUndistortRectify(K, D, DIM, np.eye(3), balance=0.0)
    map1, map2 = cv2.fisheye.initUndistortRectifyMap(K, D, np.eye(3), new_K, DIM, cv2.CV_16SC2)
    undistorted_img = cv2.remap(img, map1, map2, interpolation=cv2.INTER_LINEAR, borderMode=cv2.BORDER_CONSTANT)

    cv2.imwrite(output_image_path, undistorted_img)
    print(f"Undistorted image saved to {output_image_path}")

# キャリブレーションデータの読み込み
calibration_data = np.load("fisheye_calibration.npz")  # キャリブレーションデータを保存したファイル名
K = calibration_data["K"]
D = calibration_data["D"]
DIM = calibration_data["DIM"]

args = sys.argv
if len(args) != 2:
    print("Usage: python undistort_image.py <input_image_path>")
    sys.exit(1)

input_image_path = args[1]
output_image_path = input_image_path.replace(".png", "_undistorted.png")

undistort_fisheye_image(input_image_path, output_image_path, K, D, DIM)

# 補足: 画像の表示
# 補正結果をすぐに確認したい場合は、以下のコードを追加できます。
# undistorted_img = cv2.imread(output_image_path)
# cv2.imshow("Undistorted Image", undistorted_img)
# cv2.waitKey(0)
# cv2.destroyAllWindows()