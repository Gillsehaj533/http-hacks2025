from fastapi import FastAPI, HTTPException, Request
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel
from yt_dlp import YoutubeDL
import os
import uuid

# create downloads folder if missing
os.makedirs("downloads", exist_ok=True)

app = FastAPI()

# serve downloads folder publicly
app.mount("/downloads", StaticFiles(directory="downloads"), name="downloads")


class DownloadRequest(BaseModel):
    url: str


@app.get("/")
def home():
    return {"message": "Backend is running"}


@app.post("/download")
def download_audio(request: DownloadRequest, req: Request):  # <-- add req: Request
    unique_id = str(uuid.uuid4())
    output_path = f"downloads/{unique_id}.%(ext)s"

    ydl_opts = {
        "format": "bestaudio/best",
        "outtmpl": output_path,
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

        title = info.get("title")
        duration = info.get("duration")
        thumbnail = info.get("thumbnail")

        output_mp3 = f"{unique_id}.mp3"

        #dynamically detect base URL (local OR Fly.io)
        base_url = str(req.base_url).rstrip("/")
        download_url = f"{base_url}/downloads/{output_mp3}"

        return {
            "status": "success",
            "download_url": download_url,
            "title": title,
            "duration": duration,
            "thumbnail": thumbnail,
        }

    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Download failed: {str(e)}")
