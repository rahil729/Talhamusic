from threading import Lock
from time import monotonic
import re
from urllib.parse import parse_qs, urlparse

import httpx
import uvicorn
from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import RedirectResponse
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
YOUTUBE_INNERTUBE_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
cache: dict[str, tuple[float, dict]] = {}
locks: dict[str, Lock] = {}
cache_lock = Lock()
music_client = YTMusic()


def youtube_player_info(video_id: str) -> dict:
    payload = {
        "context": {
            "client": {
                "clientName": "ANDROID",
                "clientVersion": "20.10.38",
                "hl": "en",
                "gl": "US",
            }
        },
        "videoId": video_id,
    }
    response = httpx.post(
        f"https://www.youtube.com/youtubei/v1/player?key={YOUTUBE_INNERTUBE_KEY}",
        json=payload,
        headers={
            "Origin": "https://www.youtube.com",
            "Referer": "https://www.youtube.com/",
            "User-Agent": "Mozilla/5.0",
        },
        timeout=20,
    )
    response.raise_for_status()
    return response.json()


def is_video_playable(video_id: str) -> bool:
    try:
        info = youtube_player_info(video_id)
    except Exception:
        return False
    status = (info.get("playabilityStatus") or {}).get("status")
    return status == "OK"


def decode_cipher_url(cipher_url: str) -> str:
    parsed = urlparse(cipher_url)
    query = parse_qs(parsed.query)
    encrypted = query.get("s", [None])[0]
    if encrypted:
        return f"{parsed.scheme}://{parsed.netloc}{parsed.path}?{parsed.query}"
    return cipher_url


def resolve_stream(video_id: str) -> dict:
    info = youtube_player_info(video_id)
    status = (info.get("playabilityStatus") or {}).get("status")
    if status != "OK":
        raise RuntimeError(f"Video is not playable: {status}")

    video_details = info.get("videoDetails") or {}
    formats = (info.get("streamingData") or {}).get("adaptiveFormats") or []
    audio_streams = [
        item for item in formats
        if (item.get("mimeType") or "").startswith("audio/")
    ]
    if not audio_streams:
        raise RuntimeError("No playable audio stream found for this video")

    best = max(audio_streams, key=lambda item: (item.get("bitrate") or 0, item.get("approxDurationMs") or 0))
    direct_url = best.get("url") or best.get("signatureCipher")
    if not direct_url:
        raise RuntimeError("No streaming URL returned by YouTube")

    if best.get("url"):
        url = best["url"]
    else:
        cipher_data = parse_qs(best["signatureCipher"])
        cipher_url = cipher_data.get("url", [None])[0]
        signature = cipher_data.get("sig", [None])[0] or cipher_data.get("s", [None])[0]
        if not cipher_url:
            raise RuntimeError("Unable to decode YouTube signature cipher")
        url = f"{cipher_url}&sig={signature}" if signature else cipher_url

    return {
        "videoId": video_id,
        "title": video_details.get("title"),
        "url": url,
        "mimeType": best.get("mimeType"),
        "duration": int((video_details.get("lengthSeconds") or 0)),
    }


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
        if not is_video_playable(video_id):
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
