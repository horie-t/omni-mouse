import time
from stspin import (
    SpinChain,
    Constant as StConstant,
    Register as StRegister,
)

# ディジー・チェイン(芋づる式)でモータードライバを接続できるが、1台だけ(total_devices=1)接続する
st_chain = SpinChain(total_devices=1, spi_select=(1, 0))
# 最初(と言っても1台だけだが…)のモータードライバのインスタンスを生成する
motor = st_chain.create(0)

# 200ステップ/秒で10秒間回転させる
motor.run(200)
time.sleep(10)
motor.stopSoft()
time.sleep(1)

# 逆方向に台形加速しながら500,000ステップ回転させる
motor.setRegister(StRegister.SpeedMax, 0x22)
motor.setRegister(StRegister.Acc, 0x5)
motor.setRegister(StRegister.Dec, 0x10)
motor.setDirection(StConstant.DirReverse)
motor.move(steps=500000)

# 回転しきるまで待つ
while motor.isBusy():
    print("Motor is rolling.")
    time.sleep(1)

# モーターの保持電流を止める
motor.hiZHard()