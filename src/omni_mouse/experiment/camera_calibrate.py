import cv2
import numpy as np
import glob

# チェスボードのサイズ (内部コーナーの数)
CHECKERBOARD = (10, 7)  # 例: 横に6個、縦に9個の内部コーナー

# チェスボードの各マスのサイズ (mm)
SQUARE_SIZE = 25.0  # 例: 25mm

# 画像のパス
image_dir = "./"  # 撮影したチェスボード画像のディレクトリ
images = glob.glob(image_dir + "*.png")  # または "*.png" など

# 物体座標と画像座標を格納するリスト
objpoints = []  # 3D空間内のチェスボードコーナーの座標
imgpoints = []  # 画像内のチェスボードコーナーのピクセル座標

# チェスボードの物体座標を生成
objp = np.zeros((1, CHECKERBOARD[0] * CHECKERBOARD[1], 3), np.float32)
objp[0, :, :2] = np.mgrid[0:CHECKERBOARD[0], 0:CHECKERBOARD[1]].T.reshape(-1, 2)
objp *= SQUARE_SIZE  # マスのサイズを考慮

# 各画像に対して処理
for fname in images:
    img = cv2.imread(fname)
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    h, w = gray.shape[:2]

    # チェスボードのコーナーを検出
    ret, corners = cv2.findChessboardCorners(gray, CHECKERBOARD, None)

    if ret:
        objpoints.append(objp)

        # コーナーの精度を向上させる
        criteria = (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 30, 0.1)
        corners2 = cv2.cornerSubPix(gray, corners, (11, 11), (-1, -1), criteria)
        imgpoints.append(corners2)

        # コーナーを描画 (デバッグ用)
        cv2.drawChessboardCorners(img, CHECKERBOARD, corners2, ret)
        cv2.imshow("img", img)
        cv2.waitKey(1)  # 少し待つ

cv2.destroyAllWindows()

# 魚眼レンズのキャリブレーション
K = np.eye(3)  # 初期カメラ行列
D = np.zeros((4, 1))  # 初期歪み係数
rvecs = [np.zeros((1, 1, 3), dtype=np.float64) for i in range(len(objpoints))]
tvecs = [np.zeros((1, 1, 3), dtype=np.float64) for i in range(len(objpoints))]

rms, K, D, rvecs, tvecs = cv2.fisheye.calibrate(
    objpoints,
    imgpoints,
    (w, h),
    K,
    D,
    rvecs,
    tvecs,
    flags=cv2.fisheye.CALIB_RECOMPUTE_EXTRINSIC + cv2.fisheye.CALIB_CHECK_COND + cv2.fisheye.CALIB_FIX_SKEW,
    criteria=(cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 30, 1e-6)
)

print("RMS:", rms)
print("K:\n", K)
print("D:\n", D)

# キャリブレーションデータを保存 (オプション)
np.savez("fisheye_calibration.npz", K=K, D=D, DIM=(w, h))
print("Calibration data saved to fisheye_calibration.npz")