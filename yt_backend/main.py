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

TEMP_DIR = "downloads"
os.makedirs(TEMP_DIR, exist_ok=True)


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
            info = ydl.extract_info(request.url, download=True)

        mp3_path = f"{TEMP_DIR}/{temp_id}.mp3"
        title = info.get("title", "audio").replace("/", "-")

        if not os.path.exists(mp3_path):
            raise HTTPException(status_code=500, detail="MP3 conversion failed.")

        return {
            "status": "success",
            "download_url": f"{req.base_url}stream/{temp_id}",
            "title": title,
            "duration": info.get("duration"),
            "thumbnail": info.get("thumbnail"),
        }

    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.get("/stream/{file_id}")
def stream_file(file_id: str, background_tasks: BackgroundTasks):
    mp3_path = f"{TEMP_DIR}/{file_id}.mp3"

    if not os.path.exists(mp3_path):
        raise HTTPException(status_code=404, detail="File not found.")

    # ✅ Delete AFTER serving (ensures Android finished downloading)
    def remove_file(path):
        time.sleep(5)  # small safety buffer
        if os.path.exists(path):
            os.remove(path)

    background_tasks.add_task(remove_file, mp3_path)

    return FileResponse(mp3_path, media_type="audio/mpeg")


