import cv2
from picamera2 import Picamera2

camera0 = Picamera2(0)
# config0 = camera0.create_preview_configuration(main={"size": (1640, 1232)})
# camera0.configure(config0)
# camera0.start_preview(NullPreview())
camera0.start()

image0 = camera0.capture_array()
imgpath0 = f"photo_camera_module_v3.jpg"
cv2.imwrite(imgpath0, image0)

camera0.stop()