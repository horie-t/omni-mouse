import time

from omni_mouse.model import Twist, Vector3
from omni_mouse.motion_control import MotionControllActor

def main() -> None:
    velocity = Twist(linear=Vector3(1, 0, 0), angular=Vector3(0, 0, 0))
    actor = MotionControllActor.remote()
    actor.run.remote(velocity)
    time.sleep(10)
    actor.stop.remote()
