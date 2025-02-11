import asyncio
import curses
import datetime
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
    """モーション制御を行うアクター
    """
    moter_init_abs_position = 0x100000
    moter_init_abs_positions = np.array([0x100000, 0x100000, 0x100000])
    moter_init_microsteps = 128

    def __init__(self, wheel_radius: float = 0.024, shaft_length: float = 0.05, steps_per_revolution: int = 200):
        """初期化

        Args:
            wheel_radius (float, optional): 車輪の半径. Defaults to 0.024.
            shaft_length (float, optional): 車体の中心から車輪までの長さ. Defaults to 0.05.
            steps_per_revolution (int, optional): 1回転あたりのステップ数. Defaults to 200.
        """
        self.velocity = Twist(Vector3(0, 0, 0), Vector3(0, 0, 0))
        self.wheel_radius = wheel_radius
        self.shaft_length = shaft_length
        # SPS: Steps Per Secondを計算するための行列
        self.sps_mat = (steps_per_revolution / (2 * pi)) * (1 / wheel_radius) * np.array([
            [cos(  pi      - pi / 2), sin(  pi      - pi / 2), - shaft_length],
            [cos(  pi / 3  - pi / 2), sin(  pi / 3  - pi / 2), - shaft_length],
            [cos(-(pi / 3) - pi / 2), sin(-(pi / 3) - pi / 2), - shaft_length]
        ])
        self.vel_mat = np.linalg.inv(self.sps_mat)

        self.motors = []
        for i in range(3):
            st_chain = SpinChain(total_devices=1, spi_select=(1, i))
            motor = st_chain.create(0)
            motor.setRegister(StRegister.PosAbs, self.moter_init_abs_position)
            self.motors.append(motor)

        self.running = False  
        self.positions = self._get_positions()
        self.position_subscribers = []

    def run(self, velocity: Twist):
        """速度を指定してモーターを動かす。

        Args:
            velocity (Twist): 速度。linear.x, linear.y, angular.zにそれぞれx, y, z方向の速度を指定する。
                座標系はロボットの座標系(ROSのbase_link)。ROSのgeometry_msgs/Twistと同じ。
        """
        if not self.running:
            asyncio.create_task(self._odometry())
        self.velocity = velocity
        self.running = True
        steps_per_second_list = self._calc_steps_per_second_of_wheels(np.array([velocity.linear.x, velocity.linear.y, velocity.angular.z]))
        print(f"Steps per second: {steps_per_second_list}")
        for steps_per_second, motor in zip(steps_per_second_list, self.motors):
            if steps_per_second >= 0:
                motor.setDirection(StConstant.DirForward)
                motor.run(steps_per_second)
            else:
                motor.setDirection(StConstant.DirReverse)
                motor.run(-steps_per_second)

    def stop(self):
        """モーターを停止する。
        """
        self.velocity = Twist(Vector3(0, 0, 0), Vector3(0, 0, 0))
        self.running = False
        for motor in self.motors:
            motor.hiZHard()

    def velocity(self):
        """現在の速度を取得する。
        """
        return self.velocity

    async def _odometry(self):
        while self.running:
            new_positions = self._get_positions()
            print(f"Time: {datetime.datetime.now()}")
            print(f"Positions: {new_positions}")
            # フルステップでの移動量を計算
            position_diff_in_full_steps = self.moter_init_microsteps * (new_positions - self.moter_init_abs_positions)
            oddm = np.dot(self.vel_mat, position_diff_in_full_steps)
            print(f"Odometry: {oddm}")
            self.positions = new_positions
            try:
                await asyncio.sleep(1)
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
                # 旋回は1にしないと遅い。
                stdscr.addstr(f"Key pressed: ←\n")
                velocity.angular.z += 1
            elif key == curses.KEY_RIGHT:
                stdscr.addstr(f"Key pressed: →\n")
                velocity.angular.z += -1
            elif key == ord('w'):
                stdscr.addstr(f"Key pressed: w\n")
                velocity.linear.x += 0.1
            elif key == ord('a'):
                stdscr.addstr(f"Key pressed: a\n")
                velocity.linear.y += 0.1
            elif key == ord('s'):
                stdscr.addstr(f"Key pressed: s\n")
                velocity.linear.x += -0.1
            elif key == ord('d'):
                stdscr.addstr(f"Key pressed: d\n")
                velocity.linear.y += -0.1
            
            stdscr.addstr(f"velocity: {velocity}\n")
            self.actor.run.remote(velocity)

    def prompt(self, stdscr):
        stdscr.clear()
        stdscr.addstr(dedent("""\
        終了するには 'q' を押してください。
        上左下右はそれぞれ、'w', 'a', 's', 'd' を押してください。
        左右の旋回は、'←', '→' を押してください
        """))
