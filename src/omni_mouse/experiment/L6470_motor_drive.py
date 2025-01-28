import spidev
import time

# SPIの初期化
spi = spidev.SpiDev()
spi.open(1, 0)
spi.bits_per_word = 8
spi.cshigh = False
spi.loop = True
spi.no_cs = False
spi.lsbfirst = False
spi.max_speed_hz = 4000000
spi.mode = 3
spi.threewire = False

# しばらく何もしない(NOP)
spi.xfer([0x00])
spi.xfer([0x00])
spi.xfer([0x00])
spi.xfer([0x00])

# HOMEポジションへ
spi.xfer([0x70])

# 最大回転速度設定
spi.xfer([0x07])
spi.xfer([0x20])

# モータ停止中の電圧
spi.xfer([0x09])
spi.xfer([0xFF])

# モータ定速回転中の電圧
spi.xfer([0x0A])
spi.xfer([0xFF])

# モータ加速中の電圧
spi.xfer([0x0B])
spi.xfer([0xFF])

# モータ減速中の電圧
spi.xfer([0x0C])
spi.xfer([0xFF])

# ステップモード
spi.xfer([0x16])
spi.xfer([0x00])

time.sleep(3)

while True:
    # 目標速度で正転させる
    spi.xfer([0x50])
    # NOP
    spi.xfer([0x00])
    # 回転速度を設定
    spi.xfer([0x20])
    # NOP
    spi.xfer([0x00])

    # 1秒回転
    time.sleep(1)

    # 停止
    spi.xfer([0xB8])
    # 1秒停止
    time.sleep(1)