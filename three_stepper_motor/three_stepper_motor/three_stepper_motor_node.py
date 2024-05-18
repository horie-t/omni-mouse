import rclpy
from rclpy.node import Node
import spidev
import time


class MotorDriveNode(Node):
    def __init__(self):
        print('Generate Node')
        super().__init__('motor_drive_node')
        self.ce_num = 0
        spi = self.openMotor(self.ce_num)
        self.initializeMotor(spi)
        spi.close()

        self.timer = self.create_timer(10, self.timer_callback)

    def timer_callback(self):
        spi = self.openMotor(self.ce_num)
        spi.xfer([0x50])
        spi.xfer([0x00])
        spi.xfer([0x20])
        spi.xfer([0x00])

        print('Rolling')

        time.sleep(3)

        spi.xfer([0xB8])
        print('Stopping')
        spi.close()

        return

    def openMotor(self, chip):
        print('Opening')
        spi = spidev.SpiDev()
        spi.open(1, chip)
        spi.bits_per_word = 8
        spi.cshigh = False
        spi.loop = True
        spi.no_cs = False
        spi.lsbfirst = False
        spi.max_speed_hz = 4000000
        spi.mode = 3
        spi.threewire = False

        return spi

    def initializeMotor(self, spi):
        print('Initializing')
        spi.xfer([0x00])
        spi.xfer([0x00])
        spi.xfer([0x00])
        spi.xfer([0x00])

        #spi.xfer([0x70])

        spi.xfer([0x07])
        spi.xfer([0x20])

        spi.xfer([0x09])
        spi.xfer([0xFF])

        spi.xfer([0x0A])
        spi.xfer([0xFF])

        spi.xfer([0x0B])
        spi.xfer([0xFF])

        spi.xfer([0x0C])
        spi.xfer([0xFF])

        spi.xfer([0x16])
        spi.xfer([0x00])

        return

def main():
    print('Start program')
    rclpy.init()
    node = MotorDriveNode()
    rclpy.spin(node)
    rclpy.shutdown
    print('Finish program')


if __name__ == '__main__':
    main()
