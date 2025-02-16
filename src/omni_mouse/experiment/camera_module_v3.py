import cv2
from picamera2 import Picamera2
import time

count = 0

# 新しいフレームが来るたびに呼ばれる関数
def on_new_frame(request):
    frame = request.make_array("main")
    # 任意の処理（ここではグレースケール変換）
    gray = cv2.cvtColor(frame, cv2.COLOR_RGB2GRAY)

    global count
    if count % 30 == 0:
        imgpath = f"photo_camera_module_v3_{count}.jpg"
        cv2.imwrite(imgpath, gray)
    
    count += 1

# Picamera2セットアップ
picam2 = Picamera2(0)
picam2.configure(picam2.create_video_configuration(main={"size": (640, 480), "format": "RGB888"}))

picam2.post_callback = on_new_frame

picam2.start()

time.sleep(30)

picam2.stop()