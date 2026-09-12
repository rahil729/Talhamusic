from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from yt_dlp import YoutubeDL
import uvicorn

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
    "extractor_args": {"youtube": {"player_client": ["android"]}},
}


@app.get("/health")
def health():
    return {"ok": True}


@app.get("/stream/{video_id}")
def stream(video_id: str):
    if len(video_id) != 11:
        raise HTTPException(status_code=400, detail="Invalid YouTube video ID")

    try:
        with YoutubeDL(YDL_OPTIONS) as ydl:
            info = ydl.extract_info(
                f"https://www.youtube.com/watch?v={video_id}",
                download=False,
            )
        url = info.get("url")
        if not url:
            raise HTTPException(status_code=404, detail="No audio stream found")
        return {
            "videoId": video_id,
            "title": info.get("title"),
            "url": url,
            "mimeType": info.get("mime_type"),
            "duration": info.get("duration"),
        }
    except HTTPException:
        raise
    except Exception as error:
        raise HTTPException(status_code=502, detail=str(error))


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8080)
