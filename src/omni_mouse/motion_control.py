import curses
from math import cos, pi, sin
import numpy as np
import ray
from stspin import (
    SpinChain,
    SpinDevice,
    Constant as StConstant,
    Register as StRegister,
    utility,
)
from textwrap import dedent

from omni_mouse.model import Twist, Vector3

@ray.remote
class MotionControlActor:
    def __init__(self, wheel_radius: float = 0.024, shaft_length: float = 0.05):
        self.velocity = Twist(Vector3(0, 0, 0), Vector3(0, 0, 0))
        self.wheel_radius = wheel_radius
        self.shaft_length = shaft_length
        self.rotate_mat = (1 / wheel_radius) * np.array([
            [cos(  pi      - pi / 2), sin(  pi      - pi / 2), - shaft_length],
            [cos(  pi / 3  - pi / 2), sin(  pi / 3  - pi / 2), - shaft_length],
            [cos(-(pi / 3) - pi / 2), sin(-(pi / 3) - pi / 2), - shaft_length]
        ])

        self.motors = []
        for i in range(3):
            st_chain = SpinChain(total_devices=1, spi_select=(1, i))
            motor = st_chain.create(0)
            self.motors.append(motor)

    def run(self, velocity: Twist):
        speeds = self._calc_speed_of_wheels(np.array([velocity.linear.x, velocity.linear.y, velocity.angular.z * 20]))
        print(speeds)
        for speed, motor in zip(speeds, self.motors):
            if speed >= 0:
                motor.setDirection(StConstant.DirReverse)
                motor.run(speed)
            else:
                motor.setDirection(StConstant.DirForward)
                motor.run(-speed)

    def stop(self):
        for motor in self.motors:
            motor.hiZHard()

    def velocity(self):
        return self.velocity

    def _calc_speed_of_wheels(self, vec):
        return np.dot(self.rotate_mat, vec)

class Console:
    def __init__(self, actor: MotionControlActor):
        self.actor = actor

    def start(self, stdscr):
        self.prompt(stdscr)

        while True:
            key = stdscr.getch()
            velocity = ray.get(self.actor.velocity.remote())

            if key == ord('q'):
                self.actor.stop.remote()
                break
            elif key == curses.KEY_LEFT:
                stdscr.addstr(f"Key pressed: ←\n")
                velocity.angular.z = 1
            elif key == curses.KEY_RIGHT:
                stdscr.addstr(f"Key pressed: →\n")
                velocity.angular.z = -1
            elif key == ord('w'):
                stdscr.addstr(f"Key pressed: w\n")
                velocity.linear.x = 1
            elif key == ord('a'):
                stdscr.addstr(f"Key pressed: a\n")
                velocity.linear.y = 1
            elif key == ord('s'):
                stdscr.addstr(f"Key pressed: s\n")
                velocity.linear.x = -1
            elif key == ord('d'):
                stdscr.addstr(f"Key pressed: d\n")
                velocity.linear.y = -1
            
            self.actor.run.remote(velocity)

    def prompt(self, stdscr):
        stdscr.clear()
        stdscr.addstr(dedent("""\
        終了するには 'q' を押してください。
        上左下右はそれぞれ、'w', 'a', 's', 'd' を押してください。
        左右の旋回は、'←', '→' を押してください
        """))
