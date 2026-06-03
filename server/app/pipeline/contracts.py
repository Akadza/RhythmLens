from typing import Protocol


class DigitizationModule(Protocol):
    def run(self, source_path: str) -> dict:
        ...


class CompletionModule(Protocol):
    def run(self, signal: dict) -> dict:
        ...


class AnalysisModule(Protocol):
    def run(self, signal: dict) -> dict:
        ...


class SyntheticModule(Protocol):
    def run(self, params: dict) -> dict:
        ...
