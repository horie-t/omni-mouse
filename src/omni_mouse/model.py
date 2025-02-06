class Vector3:
    def __init__(self, x, y, z):
        self.x, self.y, self.z = x, y, z

    def __array__(self, dtype=None):
        if dtype:
            return np.array([self.x, self.y, self.z], dtype=dtype)
        else:
            return np.array([self.x, self.y, self.z])

class Twist:
    def __init__(self, linear: Vector3, angular: Vector3):
        self.linear = linear
        self.angular = angular

class Point:
    def __init__(self, x, y, z):
        self.x, self.y, self.z = x, y, z

    def __array__(self, dtype=None):
        if dtype:
            return np.array([self.x, self.y, self.z], dtype=dtype)
        else:
            return np.array([self.x, self.y, self.z])

class Quaternion:
    def __init__(self, x, y, z, w):
        self.x, self.y, self.z, self.w = x, y, z, w

    def __array__(self, dtype=None):
        if dtype:
            return np.array([self.x, self.y, self.z, self.w], dtype=dtype)
        else:
            return np.array([self.x, self.y, self.z, self.w])

class Pose:
    def __init__(self, position: Point, orientation: Quaternion):
        self.position = position
        self.orientation = orientation
    