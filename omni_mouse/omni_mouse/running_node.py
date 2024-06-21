import time
from stspin import (
    SpinChain,
    SpinDevice,
    Constant as StConstant,
    Register as StRegister,
    utility,
)

def main():
    print('Hi from omni_mouse.')

    st_chain0 = SpinChain(total_devices=1, spi_select=(1, 0))
    st_chain1 = SpinChain(total_devices=1, spi_select=(1, 1))
    st_chain2 = SpinChain(total_devices=1, spi_select=(1, 2))

    motor0 = st_chain0.create(0)
    motor1 = st_chain1.create(0)
    motor2 = st_chain2.create(0)

    motor0.setRegister(StRegister.SpeedMax, 0x05)
    motor1.setRegister(StRegister.SpeedMax, 0x05)
    motor2.setRegister(StRegister.SpeedMax, 0x05)

    motor0.setRegister(StRegister.SpeedMin, 0x05)
    motor1.setRegister(StRegister.SpeedMin, 0x05)
    motor2.setRegister(StRegister.SpeedMin, 0x05)

    motor0.setRegister(StRegister.Acc, 0x5)
    motor0.setRegister(StRegister.Dec, 0x10)
    motor0.setRegister(StRegister.Acc, 0x20)
    motor1.setRegister(StRegister.Acc, 0x5)
    motor1.setRegister(StRegister.Dec, 0x10)
    motor1.setRegister(StRegister.Acc, 0x20)
    motor2.setRegister(StRegister.Acc, 0x5)
    motor2.setRegister(StRegister.Dec, 0x10)
    motor2.setRegister(StRegister.Acc, 0x20)

    motor0.move(steps=54000)
    motor1.move(steps=54000)
    motor2.move(steps=54000)

    time.sleep(10)

    motor0.stopSoft()
    motor1.stopSoft()
    motor2.stopSoft()

    print('Bye from omni_mouse.')

if __name__ == '__main__':
    main()
