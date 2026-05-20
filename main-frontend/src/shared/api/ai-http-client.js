import axios from "axios";

// AI recommendation service runs on port 8000 (test-ai container)
const AI_BACKENDS = [
  "http://localhost:8000",
  "https://271e5230940a.ngrok-free.app", // fallback ngrok if needed
];

const host = window.location.hostname;
const aiBaseURL = host.includes("ngrok") ? AI_BACKENDS[1] : AI_BACKENDS[0];

const aiHttp = axios.create({
  baseURL: aiBaseURL,
  timeout: 15000, // AI may take longer to respond
  withCredentials: false,
});

export default aiHttp;
