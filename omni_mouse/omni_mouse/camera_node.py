import time

from picamera2 import Picamera2, Preview

def main():
    print('Start program')
    picam2 = Picamera2()
    camera_config = picam2.create_preview_configuration({"size": (1640, 1232)}, controls={'ColourGains': (0.90, 1.60)})
    picam2.configure(camera_config)
    picam2.start_preview(Preview.QTGL)
    picam2.start()
    time.sleep(2)
    picam2.capture_file("test.jpg")
    print('Finish program')

if __name__ == '__main__':
    main()
