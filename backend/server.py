from threading import Lock
from time import monotonic
import uvicorn
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from yt_dlp import YoutubeDL

app = FastAPI(title="Talha Music Stream Resolver")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["GET"],
    allow_headers=["GET"],
)

CACHE_TTL_SECONDS = 300
VIDEO_ID_LENGTH = 11
cache: dict[str, tuple[float, dict]] = {}
locks: dict[str, Lock] = {}
cache_lock = Lock()

CLIENT_PROFILES = (
    {"player_client": ["web"]},
    {"player_client": ["tv"]},
    {"player_client": ["android"]},
)


def resolve_stream(video_id: str) -> dict:
    errors: list[str] = []
    for extractor_args in CLIENT_PROFILES:
        options = {
            "quiet": True,
            "no_warnings": True,
            "noplaylist": True,
            "skip_download": True,
            "format": "bestaudio/best",
            "socket_timeout": 30,
            "retries": 2,
            "extractor_args": {"youtube": extractor_args},
        }
        try:
            with YoutubeDL(options) as downloader:
                info = downloader.extract_info(
                    f"https://www.youtube.com/watch?v={video_id}",
                    download=False,
                )
            url = info.get("url")
            if url:
                return {
                    "videoId": video_id,
                    "title": info.get("title"),
                    "url": url,
                    "mimeType": info.get("mime_type"),
                    "duration": info.get("duration"),
                }
            errors.append(f"{extractor_args['player_client'][0]} returned no URL")
        except Exception as error:
            errors.append(f"{extractor_args['player_client'][0]}: {error}")
    raise RuntimeError("; ".join(errors)[-1000:])


@app.get("/health")
def health():
    return {"ok": True}


@app.get("/stream/{video_id}")
def stream(video_id: str):
    if len(video_id) != 11:
        raise HTTPException(status_code=400, detail="Invalid YouTube video ID")

    if len(video_id) != VIDEO_ID_LENGTH:
        raise HTTPException(status_code=400, detail="Invalid YouTube video ID")

    with cache_lock:
        lock = locks.setdefault(video_id, Lock())
        cached = cache.get(video_id)
    if cached and monotonic() - cached[0] < CACHE_TTL_SECONDS:
        return cached[1]

    with lock:
        with cache_lock:
            cached = cache.get(video_id)
        if cached and monotonic() - cached[0] < CACHE_TTL_SECONDS:
            return cached[1]

        try:
            result = resolve_stream(video_id)
        except Exception as error:
            raise HTTPException(status_code=502, detail=str(error)) from error
        with cache_lock:
            cache[video_id] = (monotonic(), result)
        return result


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8080)
