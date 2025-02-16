import cv2
from picamera2 import Picamera2
import ray

@ray.remote
class CameraActor:
    def __init__(self):
        self.camera = Picamera2(0)
        config = self.camera.create_video_configuration(main={"size": (640, 480), "format": "RGB888"})
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
        frame = request.make_array("main")
        # 任意の処理（ここではグレースケール変換）
        self._last_frame = cv2.cvtColor(frame, cv2.COLOR_RGB2GRAY)
        


