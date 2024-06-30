import time
from math import cos, pi, sin
import numpy as np

import rclpy
from rclpy.node import Node
from geometry_msgs.msg import Twist

from stspin import (
    SpinChain,
    SpinDevice,
    Constant as StConstant,
    Register as StRegister,
    utility,
)

wheel_radius = 0.024
shaft_length = 0.05
rotate_mat = (1 / wheel_radius) *np.array([
    [cos(  pi      - pi / 2), sin(  pi      - pi / 2), - shaft_length],
    [cos(-(pi / 3) - pi / 2), sin(-(pi / 3) - pi / 2), - shaft_length],
    [cos(  pi / 3  - pi / 2), sin(  pi / 3  - pi / 2), - shaft_length]
])

def calc_speed_of_wheels(vec):
    return np.dot(rotate_mat, vec)

class RunningNode(Node):
    def __init__(self):
        print('Generate Node')
        super().__init__('running_node')
        self.speed = 0
        self.motors = []
        for i in range(3):
            st_chain = SpinChain(total_devices=1, spi_select=(1, i))
            motor = st_chain.create(0)
            self.motors.append(motor)
        self.subscription = self.create_subscription(Twist, '/cmd_vel', self.listener_callback, 10)

    def listener_callback(self, cmd_vel):
        print(cmd_vel)
        self.run(cmd_vel)

    def run(self, cmd_vel):
        speeds = calc_speed_of_wheels(np.array([cmd_vel.linear.x, - cmd_vel.linear.y, - cmd_vel.angular.z * 20]))
        print(speeds)
        for speed, motor in zip(speeds, self.motors):
            if speed >= 0:
                motor.setDirection(StConstant.DirForward)
                motor.run(speed)
            else:
                motor.setDirection(StConstant.DirReverse)
                motor.run(-speed)

def main():
    print('Start program')
    rclpy.init()
    node = RunningNode()
    rclpy.spin(node)
    rclpy.shutdown
    print('Finish program')

if __name__ == '__main__':
    main()
