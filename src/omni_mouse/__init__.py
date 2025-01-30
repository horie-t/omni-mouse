import curses
import time

from omni_mouse.model import Twist, Vector3
from omni_mouse.motion_control import MotionControlActor, Console

def keyinput(stdscr):
    actor = MotionControlActor.remote()

    console = Console(actor)
    console.start(stdscr)

def main() -> None:
    curses.wrapper(keyinput)
