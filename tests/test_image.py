from importlib import resources

import cv2
import os

from omni_mouse.image import MazeImageProcessor

def test_load_calibration_data():
    """キャリブレーション・データをロードできる"""
    MazeImageProcessor.load_fisheye_calibration_data()
    pass

def test_undistort_image():
    """キャリブレーションできる"""
    base_dir = os.path.dirname(__file__)
    img_path = os.path.join(base_dir, "./data/fish_eye_picture/fish_eye_omni_mouse_20250424-214345.jpg")
    img_path = os.path.abspath(img_path)
    img = cv2.imread(img_path)

    MazeImageProcessor.load_fisheye_calibration_data()
    undistort_img = MazeImageProcessor.undistort_fisheye_image(img)

    undistort_img_path = os.path.join(base_dir, "./data/undistort_picture/undistort_omni_mouse_20250424-214345.jpg")
    undistort_img_path = os.path.abspath(undistort_img_path)
    cv2.imwrite(undistort_img_path, undistort_img)

def test_extract_red_area():
    """迷路の壁の上部の赤色を抽出できる"""
    base_dir = os.path.dirname(__file__)
    img_path = os.path.join(base_dir, "./data/fish_eye_picture/fish_eye_omni_mouse_20250424-214345.jpg")
    img_path = os.path.abspath(img_path)
    img = cv2.imread(img_path)

    MazeImageProcessor.load_fisheye_calibration_data()
    undistort_img = MazeImageProcessor.undistort_fisheye_image(img)

    # binary_frame = MazeImageProcessor.extract_red_area(cv2.cvtColor(undistort_img, cv2.COLOR_RGB2BGR))
    binary_frame = MazeImageProcessor.extract_red_area(undistort_img)
    extracted_img = cv2.cvtColor(binary_frame, cv2.COLOR_GRAY2RGB)

    extracted_img_path = os.path.join(base_dir, "./data/extracted_picture/extracted_omni_mouse_20250424-214345.jpg")
    extracted_img_path = os.path.abspath(extracted_img_path)
    cv2.imwrite(extracted_img_path, extracted_img)
