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
