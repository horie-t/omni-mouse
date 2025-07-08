import time
from stspin import (
    SpinChain,
    Constant as StConstant,
    Register as StRegister,
)

st_chain = SpinChain(total_devices=3, spi_select=(0, 0))
# 最初(と言っても1台だけだが…)のモータードライバのインスタンスを生成する
motor0 = st_chain.create(0)
motor1 = st_chain.create(1)
motor2 = st_chain.create(2)

# 200ステップ/秒で10秒間回転させる
motor0.run(200)
motor1.run(200)
motor2.run(200)
time.sleep(10)
motor0.stopSoft()
motor1.stopSoft()
motor2.stopSoft()
time.sleep(1)
