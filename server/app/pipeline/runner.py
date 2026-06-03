from app.domain.enums import EcgStatus


class PipelineNotConfiguredError(RuntimeError):
    pass


class EcgPipelineRunner:
    def __init__(self) -> None:
        self.digitization_module = None
        self.completion_module = None
        self.analysis_module = None
        self.synthetic_module = None

    def run_full_pipeline(self, source_path: str, on_status: callable) -> tuple[dict, dict]:
        if self.digitization_module is None:
            raise PipelineNotConfiguredError("Digitization module is not configured")
        if self.completion_module is None:
            raise PipelineNotConfiguredError("Completion module is not configured")
        if self.analysis_module is None:
            raise PipelineNotConfiguredError("Analysis module is not configured")

        on_status(EcgStatus.DIGITIZING)
        signal = self.digitization_module.run(source_path)

        on_status(EcgStatus.COMPLETING)
        completed_signal = self.completion_module.run(signal)

        on_status(EcgStatus.ANALYZING)
        analysis = self.analysis_module.run(completed_signal)

        return completed_signal, analysis

    def generate_synthetic(self, params: dict) -> dict:
        if self.synthetic_module is None:
            raise PipelineNotConfiguredError("Synthetic generation module is not configured")
        return self.synthetic_module.run(params)


pipeline_runner = EcgPipelineRunner()
