import json
import os
import urllib.request


class RemoteModuleClient:
    def __init__(self, base_url: str, route: str) -> None:
        self.base_url = base_url.rstrip("/")
        self.route = route

    def post_json(self, payload: dict) -> dict:
        data = json.dumps(payload).encode("utf-8")
        request = urllib.request.Request(
            url=f"{self.base_url}{self.route}",
            data=data,
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        with urllib.request.urlopen(request, timeout=600) as response:
            return json.loads(response.read().decode("utf-8"))


class RemoteDigitizationModule(RemoteModuleClient):
    def __init__(self) -> None:
        super().__init__(os.environ["DIGITIZER_URL"], "/digitize")

    def run(self, source_path: str) -> dict:
        return self.post_json({"source_path": source_path})


class RemoteCompletionModule(RemoteModuleClient):
    def __init__(self) -> None:
        super().__init__(os.environ["COMPLETION_URL"], "/complete")

    def run(self, signal: dict) -> dict:
        return self.post_json({"signal": signal})


class RemoteAnalysisModule(RemoteModuleClient):
    def __init__(self) -> None:
        super().__init__(os.environ["ANALYSIS_URL"], "/analyze")

    def run(self, signal: dict) -> dict:
        return self.post_json({"signal": signal})


class RemoteSyntheticModule(RemoteModuleClient):
    def __init__(self) -> None:
        super().__init__(os.environ["SYNTHETIC_URL"], "/generate")

    def run(self, params: dict) -> dict:
        return self.post_json(params)
