import cv2
import datetime
import numpy as np
import sys

photo_path = sys.argv[1]

main_frame = cv2.imread(photo_path)
gray_frame = cv2.cvtColor(main_frame, cv2.COLOR_BGR2GRAY)
binary_frame = cv2.threshold(gray_frame, 1, 255, cv2.THRESH_BINARY)[1]

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
for point in contours[0]:
    x, y = point[0]
    cv2.circle(contour_frame, (x, y), 2, (0, 0, 255), -1)

imgpath = f"contour_{datetime.datetime.now().strftime('%Y%m%d%H%M%S')}.png"
cv2.imwrite(imgpath, contour_frame)


# コーナー検出
# ============
#
# 1. コーナー検出 (Shi-Tomasi)
#    - maxCorners: 検出する最大角数
#    - qualityLevel: 最小品質 (0.01 程度)
#    - minDistance: 近い角同士を排除する距離 (10 px 程度)
corners = cv2.goodFeaturesToTrack(
    closed_frame, maxCorners=1000, qualityLevel=0.01, minDistance=10
)

# 2. 結果をカラー画像に重ねて角を強調表示
#    - 検出した角を赤い円で描画
corner_frame = cv2.cvtColor(closed_frame, cv2.COLOR_GRAY2BGR)
if corners is not None:
    corners = np.intp(corners)  # 整数座標に変換
    for corner in corners:
        x, y = corner.ravel()
        cv2.circle(corner_frame, (x, y), 5, (0, 0, 255), -1)

imgpath = f"corner_{datetime.datetime.now().strftime('%Y%m%d%H%M%S')}.png"
cv2.imwrite(imgpath, corner_frame)
