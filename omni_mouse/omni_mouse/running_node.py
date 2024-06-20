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

    motor0.run(200)
    motor1.run(200)
    motor2.run(200)

    time.sleep(10)

    motor0.stopSoft()
    motor1.stopSoft()
    motor2.stopSoft()

    print('Bye from omni_mouse.')

if __name__ == '__main__':
    main()
