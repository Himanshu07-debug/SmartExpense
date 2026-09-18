import os
import json
import re
from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import google.generativeai as genai
from kafka import KafkaProducer
from dotenv import load_dotenv

load_dotenv()

app = FastAPI(title="AI SMS Expense Parser Service")

# Allow direct/gateway cross-origin access
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Request Body Schema strictly matched with incoming JSON
class SMSRequest(BaseModel):
    user_id: int
    message: str

# 1. Gemini Configuration (Correct Stable Model)
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
genai.configure(api_key=GEMINI_API_KEY)
model = genai.GenerativeModel("gemini-3.6-flash")

# 2. Kafka Producer Setup
KAFKA_HOST = os.getenv("KAFKA_HOST", "localhost")
KAFKA_PORT = os.getenv("KAFKA_PORT", "9092")

try:
    producer = KafkaProducer(
        bootstrap_servers=f"{KAFKA_HOST}:{KAFKA_PORT}",
        value_serializer=lambda v: json.dumps(v).encode('utf-8')
    )
    print(f"Connected to Kafka Broker at {KAFKA_HOST}:{KAFKA_PORT}")
except Exception as e:
    producer = None
    print(f"Kafka Connection Failed: {e}")

# Regex Gatekeeper
BANK_KEYWORDS = re.compile(r'\b(spent|debited|card|bank|inr|rs|vpa|upi)\b', re.IGNORECASE)

@app.post("/ds/v1/extract-expense")
async def extract_expense(req: SMSRequest):
    print(f"--> [FastAPI] Inbound Request: user_id={req.user_id}, msg={req.message}")

    # 1. Regex check
    if not BANK_KEYWORDS.search(req.message):
        return {"status": "IGNORED", "reason": "Not a financial transaction SMS"}

    # 2. Gemini Prompt
    prompt = f"""
    You are an expert financial transaction parsing engine.
    Extract details from this SMS:
    SMS: "{req.message}"

    Respond ONLY with valid JSON (no markdown ticks):
    {{
        "amount": <float or null>,
        "merchant": "<merchant name or null>",
        "currency": "<INR/USD, default INR>"
    }}
    """

    try:
        response = model.generate_content(prompt)
        text_resp = response.text.replace("```json", "").replace("```", "").strip()
        parsed = json.loads(text_resp)

        if not parsed.get("amount"):
            raise HTTPException(status_code=422, detail="Amount could not be detected")

        payload = {
            "user_id": req.user_id,
            "amount": float(parsed["amount"]),
            "merchant": parsed.get("merchant", "Unknown Merchant"),
            "currency": parsed.get("currency", "INR")
        }

        # 3. Publish to Kafka
        if producer:
            producer.send("EXPENSE_EVENTS", value=payload)
            producer.flush()
            print(f"--> [FastAPI DS-Service] Published to Kafka: {payload}")

        return {"status": "SUCCESS", "extracted": payload}

    except Exception as e:
        print(f"Extraction Error: {e}")
        raise HTTPException(status_code=500, detail=str(e))