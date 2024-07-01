import time

from picamera2 import Picamera2, Preview

def main():
    print('Start program')
    picam2 = Picamera2()
    camera_config = picam2.create_preview_configuration()
    picam2.configure(camera_config)
    picam2.start_preview(Preview.QTGL)
    picam2.start() #?????
    time.sleep(2)
    picam2.capture_file("test.jpg")
    print('Finish program')

if __name__ == '__main__':
    main()
