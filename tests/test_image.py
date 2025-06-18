from importlib import resources

import cv2
import os

from omni_mouse.image import MazeImageProcessor

def test_load_calibration_data():
    """キャリブレーション・データをロードできるか"""
    MazeImageProcessor.load_fisheye_calibration_data()
    pass

def test_undistort_image():
    base_dir = os.path.dirname(__file__)
    img_path = os.path.join(base_dir, "./data/fish_eye_picture/fish_eye_omni_mouse_20250424-214327.jpg")
    img_path = os.path.abspath(img_path)
    img = cv2.imread(img_path)

    MazeImageProcessor.load_fisheye_calibration_data()
    undistort_img = MazeImageProcessor.undistort_fisheye_image(img)

    undistort_img_path = os.path.join(base_dir, "./data/undistort_picture/undistort_omni_mouse_20250424-214327.jpg")
    undistort_img_path = os.path.abspath(undistort_img_path)
    cv2.imwrite(undistort_img_path, undistort_img)
