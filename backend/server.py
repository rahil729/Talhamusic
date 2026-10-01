from threading import Lock
from time import monotonic
import re
import uvicorn
from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import RedirectResponse
from yt_dlp import YoutubeDL
from ytmusicapi import YTMusic

app = FastAPI(title="Talha Music Stream Resolver")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
    expose_headers=["*"],
    allow_credentials=True,
)

@app.get("/")
def root() -> dict[str, str]:
    return {
        "name": "Talha Music Stream Resolver",
        "status": "online",
        "health": "/health",
        "search": "/search?query=...",
        "stream": "/stream/{video_id}",
    }


CACHE_TTL_SECONDS = 300
VIDEO_ID_LENGTH = 11
VIDEO_ID_PATTERN = re.compile(r"^[A-Za-z0-9_-]{11}$")
cache: dict[str, tuple[float, dict]] = {}
locks: dict[str, Lock] = {}
cache_lock = Lock()
music_client = YTMusic()

CLIENT_PROFILES = (
    {"player_client": ["android"]},
    {"player_client": ["web"]},
    {"player_client": ["tv"]},
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
            "socket_timeout": 10,
            "retries": 0,
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


@app.get("/search")
@app.get("/search/")
def search(query: str = Query(min_length=1, max_length=200), limit: int = Query(20, ge=1, le=25)):
    try:
        result = music_client.search(query.strip(), filter="songs", limit=limit)
    except Exception as error:
        raise HTTPException(status_code=502, detail=str(error)[-500:]) from error

    items = []
    for entry in result[:limit]:
        video_id = entry.get("videoId") or ""
        title = entry.get("title") or ""
        if not VIDEO_ID_PATTERN.fullmatch(video_id) or not title:
            continue

        artists = entry.get("artists") or []
        artist = ", ".join(item.get("name", "") for item in artists if item.get("name"))
        artist = artist or entry.get("artist") or "YouTube"
        thumbnails = entry.get("thumbnails") or []
        thumbnail_url = next(
            (item.get("url") for item in reversed(thumbnails) if item.get("url")),
            None,
        )
        duration_text = entry.get("duration") or ""
        try:
            duration = sum(
                int(part) * 60 ** index
                for index, part in enumerate(reversed(duration_text.split(":")))
            ) or None
        except ValueError:
            duration = None
        items.append(
            {
                "videoId": video_id,
                "title": title,
                "artist": artist,
                "duration": duration,
                "thumbnailUrl": thumbnail_url,
            }
        )
    return {"items": items}


@app.get("/stream/{video_id}")
def stream(video_id: str):
    if not VIDEO_ID_PATTERN.fullmatch(video_id):
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


@app.get("/stream/{video_id}/audio")
def stream_audio(video_id: str):
    result = stream(video_id)
    return RedirectResponse(result["url"], status_code=302)


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8080)
