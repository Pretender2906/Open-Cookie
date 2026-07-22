from pathlib import Path
import runpy

runpy.run_path(str(Path(__file__).resolve().parent.parent / "android" / "design" / "generate_launcher_icon.py"))
