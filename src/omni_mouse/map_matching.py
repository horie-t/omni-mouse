import asyncio
import ray

from omni_mouse.image import MazeImageProcessor

@ray.remote
class MapMatchingActor:
    def __init__(self, camera_actor):
        self.camera_actor = camera_actor
        self.started = False

    def start(self):
        if not self.started:
            self.started = True
            asyncio.create_task(self._map_matching())

    def stop(self):
        self.started = False

    async def _map_matching(self):
        while self.started:
            binary_frame = await self.camera_actor.get_last_frame.remote()
            contours = MazeImageProcessor.calc_contours(binary_frame)

            await asyncio.sleep(0.1)
