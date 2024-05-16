import rclpy
from rclpy.node import Node
import spidev
import time

class MotorDriveNode(Node):
    def __init__(self):
        print('Generate Node')
        super().__init__('motor_drive_node')
        spi = spidev.SpiDev()
        spi.open(0, 0)
        spi.bits_per_word = 8
        spi.cshigh = False
        spi.loop = True
        spi.no_cs = False
        spi.lsbfirst = False
        spi.max_speed_hz = 4000000
        spi.mode = 3
        spi.threewire = False
        
        spi.xfer([0x00])
        spi.xfer([0x00])
        spi.xfer([0x00])
        spi.xfer([0x00])

        spi.xfer([0x70])
        
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

        time.sleep(3)

        spi.xfer([0x50])
        spi.xfer([0x00])
        spi.xfer([0x20])
        spi.xfer([0x00])

        print('Rolling')

        time.sleep(5)

        spi.xfer([0xB8])
        print('Stopping')



def main():
    print('Start program')
    rclpy.init()
    node = MotorDriveNode()
    rclpy.spin(node)
    rclpy.shutdown
    print('Finish program')


if __name__ == '__main__':
    main()
