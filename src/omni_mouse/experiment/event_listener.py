import ray
import time

ray.init()

@ray.remote
class Publisher:
    def __init__(self):
        self.listeners = []  # リスナーのリスト

    def add_listener(self, listener):
        """リスナーを登録"""
        self.listeners.append(listener)

    def notify_listeners(self, message):
        """リスナーにイベントを通知"""
        for listener in self.listeners:
            listener.on_event.remote(message)  # 非同期で通知

    def start_publishing(self):
        """定期的にイベントを発行"""
        for i in range(1, 6):
            message = f"Event {i}"
            print(f"Publisher: {message}")
            self.notify_listeners(message)  # リスナーに通知
            time.sleep(2)  # 2秒ごとにイベント発行

@ray.remote
class Listener:
    def __init__(self, name):
        self.name = name

    def on_event(self, message):
        """イベントを受け取る"""
        print(f"Listener {self.name} received: {message}")

# Publisher インスタンス作成
publisher = Publisher.remote()

# Listener インスタンス作成
listener1 = Listener.remote("A")
listener2 = Listener.remote("B")

# Listener を Publisher に登録
publisher.add_listener.remote(listener1)
publisher.add_listener.remote(listener2)

# Publisher の処理開始
publisher.start_publishing.remote()

time.sleep(14)  # 14秒待つ