import numpy as np
from math import cos, pi, sin
from scipy.spatial.transform import Rotation as R

class Vector3:
    def __init__(self, x, y, z):
        self.x, self.y, self.z = x, y, z

    def __array__(self, dtype=None):
        if dtype:
            return np.array([self.x, self.y, self.z], dtype=dtype)
        else:
            return np.array([self.x, self.y, self.z])

    def __str__(self):
        return f"({self.x}, {self.y}, {self.z})"

class Twist:
    def __init__(self, linear: Vector3, angular: Vector3):
        self.linear = linear
        self.angular = angular

    def __str__(self):
        return f"linear: {self.linear}, angular: {self.angular}"

class Point:
    def __init__(self, x, y, z):
        self.x, self.y, self.z = x, y, z

    def __add__(self, other):
        return Point(self.x + other.x, self.y + other.y, self.z + other.z)

    def __sub__(self, other):
        return Point(self.x - other.x, self.y - other.y, self.z - other.z)

    def __iadd__(self, other):
        self.x += other.x
        self.y += other.y
        self.z += other.z
        return self

    def __isub__(self, other):
        self.x -= other.x
        self.y -= other.y
        self.z -= other.z
        return self

    def __str__(self):
        return f"({self.x}, {self.y}, {self.z})"

    def __array__(self, dtype=None):
        if dtype:
            return np.array([self.x, self.y, self.z], dtype=dtype)
        else:
            return np.array([self.x, self.y, self.z])

class Quaternion:
    def __init__(self, x, y, z, w):
        self.x, self.y, self.z, self.w = x, y, z, w

    def __str__(self):
        return f"({self.x}, {self.y}, {self.z}, {self.w})"

    def __mul__(self, other):
        if isinstance(other, Quaternion):
            q = R.from_quat([self.x, self.y, self.z, self.w]) * R.from_quat([other.x, other.y, other.z, other.w])
            return Quaternion(*q.as_quat())
        else:
            raise ValueError(f"unsupported type {type(other)}")

    def __array__(self, dtype=None):
        if dtype:
            return np.array([self.x, self.y, self.z, self.w], dtype=dtype)
        else:
            return np.array([self.x, self.y, self.z, self.w])

    @classmethod
    def from_euler(cls, seq, angles, degrees=False):
        rotation = R.from_euler(seq, angles, degrees)
        return Quaternion(*rotation.as_quat())

class Pose:
    def __init__(self, position: Point, orientation: Quaternion):
        self.position = position
        self.orientation = orientation

    def __add__(self, other):
        return Pose(self.position + other.position, self.orientation * other.orientation)

    def __iadd__(self, other):
        self.position += other.position
        self.orientation *= other.orientation
        return self
    
    def __str__(self):
        return f"position: {self.position}, orientation: {self.orientation}"

class Odometry:
    def __init__(self, timestamp: int, pose: Pose, twist: Twist):
        self.timestamp = timestamp
        self.pose = pose
        self.twist = twist