from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from yt_dlp import YoutubeDL
import uvicorn
from threading import Lock
from time import monotonic

app = FastAPI(title="Talha Music Stream Resolver")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["GET"],
    allow_headers=["*"]
)

YDL_OPTIONS = {
    "quiet": True,
    "no_warnings": True,
    "noplaylist": True,
    "skip_download": True,
    "format": "bestaudio/best",
    "retries": 3,
    "socket_timeout": 20,
    "extractor_args": {"youtube": {"player_client": ["android"]}},
}

STREAM_CACHE_TTL = 300
stream_cache: dict[str, tuple[float, dict]] = {}
stream_locks: dict[str, Lock] = {}
cache_lock = Lock()


@app.get("/health")
def health():
    return {"ok": True}


@app.get("/stream/{video_id}")
def stream(video_id: str):
    if len(video_id) != 11:
        raise HTTPException(status_code=400, detail="Invalid YouTube video ID")

    with cache_lock:
        cached = stream_cache.get(video_id)
        lock = stream_locks.setdefault(video_id, Lock())
    if cached and monotonic() - cached[0] < STREAM_CACHE_TTL:
        return cached[1]

    with lock:
        with cache_lock:
            cached = stream_cache.get(video_id)
        if cached and monotonic() - cached[0] < STREAM_CACHE_TTL:
            return cached[1]

        try:
            with YoutubeDL(YDL_OPTIONS) as ydl:
                info = ydl.extract_info(
                    f"https://www.youtube.com/watch?v={video_id}",
                    download=False,
                )
            url = info.get("url")
            if not url:
                raise HTTPException(status_code=404, detail="No audio stream found")
            result = {
                "videoId": video_id,
                "title": info.get("title"),
                "url": url,
                "mimeType": info.get("mime_type"),
                "duration": info.get("duration"),
            }
            with cache_lock:
                stream_cache[video_id] = (monotonic(), result)
            return result
        except HTTPException:
            raise
        except Exception as error:
            raise HTTPException(status_code=502, detail="Audio stream unavailable") from error


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8080)
