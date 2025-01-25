import ray

def hi():
    import os
    import socket
    return f"Running on {socket.gethostname()} in pid {os.getpid()}"

@ray.remote
def remote_hi():
    import os
    import socket
    return f"Running on {socket.gethostname()} in pid {os.getpid()}"

if __name__ == "__main__":
    print(f"local: {hi()}")

    ray.init(num_cpus=20)
    future = remote_hi.remote()
    print(f"remote: {ray.get(future)}")
