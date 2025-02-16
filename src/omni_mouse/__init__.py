import curses
import time

from omni_mouse.model import Twist, Vector3
from omni_mouse.motion_control import MotionControlActor, Console
from omni_mouse.sensor import CameraActor

def keyinput(stdscr):
    motion_controll_actor = MotionControlActor.remote()
    camera_actor = CameraActor.remote()
    camera_actor.start.remote()

    console = Console(motion_controll_actor, camera_actor)
    console.start(stdscr)

def main() -> None:
    curses.wrapper(keyinput)
