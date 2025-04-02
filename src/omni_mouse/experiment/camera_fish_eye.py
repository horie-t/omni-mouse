from picamera2 import Picamera2, Preview

tuning = Picamera2.load_tuning_file("pi5_imx219_160d.json")

picam2 = Picamera2(tuning=tuning)
picam2.start_preview(Preview.NULL)
picam2.start_and_capture_file("fish_eye.jpg")