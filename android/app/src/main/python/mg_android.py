"""
Matchering Android Bridge
==========================

Provides on-device audio matching using the matchering library.

Key design: the matchering library imports `soundfile` (which needs the native
libsndfile library, unavailable on Android).  This script installs a lightweight
shim into sys.modules *before* importing matchering, replacing soundfile's
read/write with Python's built-in `wave` module.  The Kotlin side converts any
audio format to WAV before calling us, so the shim only ever handles WAV files.

It also patches:
  - scipy.ndimage.filters (deprecated path used by matchering/limiter/hyrax.py)
  - statsmodels (heavy dep; a fallback LOWESS is provided if it can't be imported)
"""

import sys
import types
import wave

import numpy as np


# ---------------------------------------------------------------------------
# scipy.ndimage.filters compatibility
# ---------------------------------------------------------------------------
try:
    from scipy.ndimage.filters import maximum_filter1d  # noqa: F401
except ImportError:
    from scipy.ndimage import maximum_filter1d
    _filters_mod = types.ModuleType("scipy.ndimage.filters")
    _filters_mod.maximum_filter1d = maximum_filter1d
    sys.modules["scipy.ndimage.filters"] = _filters_mod


# ---------------------------------------------------------------------------
# statsmodels fallback (heavy dependency — may not install via Chaquopy)
# ---------------------------------------------------------------------------
try:
    import statsmodels.api as sm  # noqa: F401
except Exception:
    _sm = types.ModuleType("statsmodels")
    _sm_api = types.ModuleType("statsmodels.api")
    _sm_nonparam = types.ModuleType("statsmodels.api.nonparametric")

    def _lowess_fallback(endog, exog, frac=0.0375, it=0, delta=0.001):
        """Simplified LOWESS using triangular-weighted local means."""
        n = len(endog)
        window = max(3, int(n * frac))
        result = np.empty(n)
        for i in range(n):
            lo = max(0, i - window // 2)
            hi = min(n, i + window // 2 + 1)
            dists = np.abs(np.arange(lo, hi) - i)
            w = np.clip(1.0 - dists / max(dists.max(), 1), 0, 1)
            w /= w.sum()
            result[i] = np.sum(w * endog[lo:hi])
        return np.column_stack([exog, result])

    _sm_nonparam.lowess = _lowess_fallback
    _sm_api.nonparametric = _sm_nonparam
    _sm.api = _sm_api
    sys.modules["statsmodels"] = _sm
    sys.modules["statsmodels.api"] = _sm_api
    sys.modules["statsmodels.api.nonparametric"] = _sm_nonparam


# ---------------------------------------------------------------------------
# soundfile shim — uses Python's built-in wave module (no libsndfile needed)
# ---------------------------------------------------------------------------
_sf = types.ModuleType("soundfile")

_DTYPE_MAP = {1: np.uint8, 2: np.int16, 4: np.int32}


def _sf_read(file, always_2d=False, **kwargs):
    """Read a WAV file using the stdlib wave module, return (float64 array, sr)."""
    with wave.open(file, "rb") as wf:
        n_frames = wf.getnframes()
        raw = wf.readframes(n_frames)
        channels = wf.getnchannels()
        sampwidth = wf.getsampwidth()
        sr = wf.getframerate()

    if sampwidth == 3:
        # 24-bit signed little-endian — wave module gives raw bytes
        raw_arr = np.frombuffer(raw, dtype=np.uint8).reshape(-1, 3)
        audio = (
            raw_arr[:, 0].astype(np.int32)
            | (raw_arr[:, 1].astype(np.int32) << 8)
            | (raw_arr[:, 2].astype(np.int32) << 16)
        )
        audio = np.where(audio >= 2**23, audio - 2**24, audio).astype(np.float64)
        audio /= float(2**23)
    else:
        dt = _DTYPE_MAP.get(sampwidth, np.int16)
        audio = np.frombuffer(raw, dtype=dt).astype(np.float64)
        if sampwidth == 1:
            audio = audio - 128.0
            audio /= 128.0
        else:
            audio /= float(2 ** (sampwidth * 8 - 1))

    audio = audio.reshape(-1, channels)
    if always_2d and audio.ndim == 1:
        audio = audio[:, np.newaxis]
    return audio, sr


def _sf_write(file, data, sample_rate, subtype="PCM_16", **kwargs):
    """Write a WAV file using the stdlib wave module."""
    audio = np.clip(np.asarray(data), -1.0, 1.0)
    n_channels = audio.shape[1] if audio.ndim > 1 else 1

    if "24" in str(subtype).upper():
        audio_int = (audio * 8388607).astype(np.int32)
        with wave.open(file, "wb") as wf:
            wf.setnchannels(n_channels)
            wf.setsampwidth(3)
            wf.setframerate(sample_rate)
            raw = bytearray()
            for s in audio_int.flatten():
                raw += int(s).to_bytes(3, "little", signed=True)
            wf.writeframes(bytes(raw))
    else:
        audio_int = (audio * 32767).astype(np.int16)
        with wave.open(file, "wb") as wf:
            wf.setnchannels(n_channels)
            wf.setsampwidth(2)
            wf.setframerate(sample_rate)
            wf.writeframes(audio_int.tobytes())


def _sf_check_format(fmt, subtype=None):
    return True


_sf.read = _sf_read
_sf.write = _sf_write
_sf.check_format = _sf_check_format
sys.modules["soundfile"] = _sf


# ---------------------------------------------------------------------------
# Import and configure matchering
# ---------------------------------------------------------------------------
from matchering import Config, Result, process  # noqa: E402
from matchering.log.handlers import set_handlers  # noqa: E402

_log_lines: list[str] = []


def _log_handler(msg):
    _log_lines.append(str(msg))


set_handlers(
    default_handler=_log_handler,
    warning_handler=_log_handler,
    info_handler=_log_handler,
    debug_handler=_log_handler,
)


def run_matchering(target_path: str, reference_path: str, output_path: str) -> str:
    """
    Run the matchering process on two WAV files.

    Args:
        target_path:    Path to the target WAV file (the track to master).
        reference_path: Path to the reference WAV file (the sound to match).
        output_path:    Path where the mastered result WAV will be written.

    Returns:
        The output_path on success.  Raises an exception on failure.
    """
    _log_lines.clear()
    result = Result(output_path, "PCM_16", use_limiter=True, normalize=True)
    config = Config()
    process(target_path, reference_path, [result], config)
    return output_path


def get_logs() -> str:
    """Return accumulated log lines from the last run."""
    return "\n".join(_log_lines)
