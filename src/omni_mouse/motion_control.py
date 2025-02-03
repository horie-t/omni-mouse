import asyncio
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
    def __init__(self, wheel_radius: float = 0.024, shaft_length: float = 0.05, steps_per_revolution: int = 200, micro_steps: int = 128):
        self.velocity = Twist(Vector3(0, 0, 0), Vector3(0, 0, 0))
        self.wheel_radius = wheel_radius
        self.shaft_length = shaft_length
        # SPS: Steps Per Secondを計算するための行列
        self.sps_mat = (steps_per_revolution * micro_steps / (2 * pi)) * (1 / wheel_radius) * np.array([
            [cos(  pi      - pi / 2), sin(  pi      - pi / 2), - shaft_length],
            [cos(  pi / 3  - pi / 2), sin(  pi / 3  - pi / 2), - shaft_length],
            [cos(-(pi / 3) - pi / 2), sin(-(pi / 3) - pi / 2), - shaft_length]
        ])

        self.motors = []
        for i in range(3):
            st_chain = SpinChain(total_devices=1, spi_select=(1, i))
            motor = st_chain.create(0)
            self.motors.append(motor)

        self.running = False  
        self.positions = self._get_positions()
        self.position_subscribers = []

    def run(self, velocity: Twist):
        if not self.running:
            asyncio.create_task(self._odometry())
        self.running = True
        steps_per_second_list = self._calc_steps_per_second_of_wheels(np.array([velocity.linear.x, velocity.linear.y, velocity.angular.z]))
        for steps_per_second, motor in zip(steps_per_second_list, self.motors):
            if steps_per_second >= 0:
                motor.setDirection(StConstant.DirReverse)
                motor.run(steps_per_second)
            else:
                motor.setDirection(StConstant.DirForward)
                motor.run(-steps_per_second)

    def stop(self):
        self.running = False
        for motor in self.motors:
            motor.hiZHard()

    def velocity(self):
        return self.velocity

    async def _odometry(self):
        while self.running:
            new_positions = self._get_positions()
            print(new_positions)
            self.positions = new_positions
            try:
                await asyncio.sleep(3)
            except asyncio.CancelledError:
                break

    def _get_positions(self):
        return np.array([self.motors[i].getRegister(StRegister.PosAbs) for i in range(3)])

    def _calc_steps_per_second_of_wheels(self, vec):
        return np.dot(self.sps_mat, vec)

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
                # 旋回は0.01にしないと遅い。
                stdscr.addstr(f"Key pressed: ←\n")
                velocity.angular.z = 0.01
            elif key == curses.KEY_RIGHT:
                stdscr.addstr(f"Key pressed: →\n")
                velocity.angular.z = -0.01
            elif key == ord('w'):
                stdscr.addstr(f"Key pressed: w\n")
                velocity.linear.x = 0.001
            elif key == ord('a'):
                stdscr.addstr(f"Key pressed: a\n")
                velocity.linear.y = 0.001
            elif key == ord('s'):
                stdscr.addstr(f"Key pressed: s\n")
                velocity.linear.x = -0.001
            elif key == ord('d'):
                stdscr.addstr(f"Key pressed: d\n")
                velocity.linear.y = -0.001
            
            self.actor.run.remote(velocity)

    def prompt(self, stdscr):
        stdscr.clear()
        stdscr.addstr(dedent("""\
        終了するには 'q' を押してください。
        上左下右はそれぞれ、'w', 'a', 's', 'd' を押してください。
        左右の旋回は、'←', '→' を押してください
        """))
