import curses
import ray
import time

from omni_mouse.model import Twist, Vector3
from omni_mouse.map_matching import MapMatchingActor
from omni_mouse.motion_control import MotionControlActor, Console
from omni_mouse.sensor import CameraActor

def keyinput(stdscr):
    # マウスの初期化
    motion_controll_actor = MotionControlActor.remote()

    # カメラの初期化
    camera_actor = CameraActor.remote()
    camera_actor.start.remote()
    time.sleep(2) # カメラの初期化待ち

    # マップマッチングの初期化
    map_matching_actor = MapMatchingActor.remote(camera_actor)
    map_matching_actor.start.remote()

    # コンソールの初期化
    console = Console(motion_controll_actor, camera_actor)
    console.start(stdscr)

def main() -> None:
    #ray.init(log_to_driver=False) # リモートの標準出力を表示しない
    ray.init()
    curses.wrapper(keyinput)
