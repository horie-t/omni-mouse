import numpy as np

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

    def __array__(self, dtype=None):
        if dtype:
            return np.array([self.x, self.y, self.z, self.w], dtype=dtype)
        else:
            return np.array([self.x, self.y, self.z, self.w])

class Pose:
    def __init__(self, position: Point, orientation: Quaternion):
        self.position = position
        self.orientation = orientation
    
    def __str__(self):
        return f"position: {self.position}, orientation: {self.orientation}"