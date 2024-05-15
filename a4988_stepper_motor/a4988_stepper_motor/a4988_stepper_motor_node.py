import rclpy
from rclpy.node import Node
from gpiozero import OutputDevice

class StepperMotorNode(Node):
    def __init__(self):
        print('Generate Node')
        super().__init__('stepper_motor_node')
        self.dir = OutputDevice("GPIO2")
        self.dir.on()
        self.step = OutputDevice("GPIO3")
        self.timer = self.create_timer(0.01, self.timer_callback)

    def timer_callback(self):
        self.get_logger().info('Hi from stepper_motor.')
        self.step.toggle()

def main():
    print('Start program')
    rclpy.init()
    node = StepperMotorNode()
    rclpy.spin(node)
    rclpy.shutdown
    print('Finish program')

if __name__ == '__main__':
    main()
