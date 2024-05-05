import rclpy
from rclpy.node import Node
from gpiozero import LED

class LChikaNode(Node):
    def __init__(self):
        print('Generate Node')
        super().__init__('l_chika_node')
        self.led = LED(25)
        self.led.on()
        self.timer = self.create_timer(0.5, self.timer_callback)

    def timer_callback(self):
        self.get_logger().info('Hi from l_chika.')
        self.led.toggle()

def main():
    print('Start program')
    rclpy.init()
    node = LChikaNode()
    rclpy.spin(node)
    rclpy.shutdown
    print('Finish program')


if __name__ == '__main__':
    main()
