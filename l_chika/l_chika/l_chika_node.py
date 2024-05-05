import rclpy
from rclpy.node import Node

class LChikaNode(Node):
    def __init__(self):
        print('Generate Node')
        super().__init__('l_chika_node')
        self.get_logger().info('Hi from l_chika.')

def main():
    print('Start program')
    rclpy.init()
    node = LChikaNode()
    rclpy.shutdown
    print('Finish program')


if __name__ == '__main__':
    main()
