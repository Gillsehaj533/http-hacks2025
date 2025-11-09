from fastapi import FastAPI, HTTPException, BackgroundTasks, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel
from yt_dlp import YoutubeDL
import os
import uuid
import time
import uvicorn

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

TEMP_DIR = "/app/downloads"
os.makedirs(TEMP_DIR, exist_ok=True)

# ✅ read cookie file from Fly secret
COOKIES_PATH = "/app/cookies.txt"
if "YT_COOKIES" in os.environ:
    with open(COOKIES_PATH, "w", encoding="utf-8") as f:
        f.write(os.environ["YT_COOKIES"])

class DownloadRequest(BaseModel):
    url: str


@app.get("/")
def home():
    return {"status": "running", "message": "🎧 YouTube → MP3 API ready"}


@app.post("/download")
def download_audio(request: DownloadRequest, req: Request):
    temp_id = str(uuid.uuid4())
    output_template = f"{TEMP_DIR}/{temp_id}.%(ext)s"

    ydl_opts = {
        "format": "bestaudio/best",
        "outtmpl": output_template,
        "cookiefile": COOKIES_PATH,
        "postprocessors": [
            {
                "key": "FFmpegExtractAudio",
                "preferredcodec": "mp3",
                "preferredquality": "192",
            }
        ],
    }

    try:
        with YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(request.url, download=False)  # get info only
            ydl.download([request.url])                           # download/write mp3 fully

        mp3_path = f"{TEMP_DIR}/{temp_id}.mp3"
        title = info.get("title", "audio").replace("/", "-")

        if not os.path.exists(mp3_path):
            raise HTTPException(status_code=500, detail="MP3 conversion failed.")

        return {
            "status": "success",
            "download_url": f"https://httphacks-yt-converter.fly.dev/stream/{temp_id}.mp3",
            "title": title,
            "duration": info.get("duration"),
            "thumbnail": info.get("thumbnail"),
        }

    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.get("/stream/{file_id}")
def stream_file(file_id: str, background_tasks: BackgroundTasks):
    # NOTE: file_id already includes ".mp3" in your URL. Keep it as-is.
    mp3_path = f"{TEMP_DIR}/{file_id}"

    print("DEBUG — TEMP_DIR contents:", os.listdir(TEMP_DIR))
    print("DEBUG — Looking for:", mp3_path)

    if not os.path.exists(mp3_path):
        raise HTTPException(status_code=404, detail="File not found.")

    file_size = os.path.getsize(mp3_path)  # good for Android DownloadManager

    def remove_file(path):
        time.sleep(5)
        if os.path.exists(path):
            os.remove(path)

    background_tasks.add_task(remove_file, mp3_path)

    # ❌ remove: content_length=file_size
    # ✅ either let Starlette set Content-Length, or set it explicitly in headers
    return FileResponse(
        mp3_path,
        media_type="audio/mpeg",
        filename=file_id,  # same name Android will see
        headers={
            "Content-Disposition": f'attachment; filename="{file_id}"',
            "Cache-Control": "no-cache",
            "Content-Length": str(file_size),  # optional (Starlette will set it anyway)
        },
    )


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    uvicorn.run("main:app", host="0.0.0.0", port=5000, reload=False)
