import rclpy
from rclpy.node import Node
import spidev

class ReadAdcNode(Node):
    def __init__(self):
        print('Generate Node')
        super().__init__('read_adc_node')

        self.Vref = 3.334

        self.spi = spidev.SpiDev()

        self.spi.open(0, 0)  # bus0,cs0
        self.spi.max_speed_hz = 100000  # 100kHz

        self.timer = self.create_timer(0.2, self.timer_callback)

    def timer_callback(self):
        data = self.readAdc(channel=0)
        volts = self.convertVolts(data, self.Vref)
        self.get_logger().info("CH0 volts: {:.2f}".format(volts))

        data = self.readAdc(channel=1)
        volts = self.convertVolts(data, self.Vref)
        self.get_logger().info("CH1 volts: {:.2f}".format(volts))


    def readAdc(self, channel):
        adc = self.spi.xfer2([1, (8 + channel) << 4, 200])
        data = ((adc[1] & 3) << 8) + adc[2]
        return data
    
    def convertVolts(self, data, vref):
        volts = (data * vref) / float(1023)
        return volts

def main():
    print('Start program')
    rclpy.init()
    node = ReadAdcNode()
    rclpy.spin(node)
    rclpy.shutdown
    print('Finish program')



if __name__ == '__main__':
    main()
