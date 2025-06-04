from picamera2 import Picamera2
import ray

from omni_mouse.image import MazeImageProcessor

@ray.remote
class CameraActor:
    def __init__(self):
        tuning = Picamera2.load_tuning_file("pi5_imx219_160d.json")
        self.camera = Picamera2(0, tuning=tuning)
        config = self.camera.create_video_configuration(main={"size": (1640, 1232)})
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
        main_frame = request.make_array("main")

        self._last_frame = MazeImageProcessor.extract_red_area(main_frame)
        # self._last_frame = main_frame # デバッグ用

