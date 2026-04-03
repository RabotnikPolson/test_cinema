import re
import asyncio
from typing import List
from sentence_transformers import SentenceTransformer

class SubtitleVectorizer:
    def __init__(self):
        # paraphrase-multilingual-MiniLM-L12-v2 yields 384 dimensions.
        # It handles Kazakh, Russian, and English well.
        self.model = SentenceTransformer('sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2')

    def parse_vtt(self, vtt_content: str) -> str:
        """
        Hard parser for WebVTT format.
        Strips WEBVTT header, timestamps (e.g. 00:00:01.000 --> 00:00:04.000), 
        HTML-like tags, and empty lines, leaving contiguous dialogue text.
        """
        lines = vtt_content.splitlines()
        cleaned_lines = []
        
        # Regex to match timestamps
        timestamp_pattern = re.compile(r'\d{2}:\d{2}:\d{2}\.\d{3}\s*-->\s*\d{2}:\d{2}:\d{2}\.\d{3}')
        # Regex to match tags like <i>, </i>, <c.color>, etc.
        tag_pattern = re.compile(r'<[^>]+>')
        
        for line in lines:
            line = line.strip()
            # Skip empty lines, WEBVTT header, sequence numbers or metadata
            if not line:
                continue
            if line.startswith("WEBVTT") or line.startswith("Kind:") or line.startswith("Language:"):
                continue
            # Skip timestamp lines
            if timestamp_pattern.search(line):
                continue
            # Skip single numbers which are often subtitle sequence indices
            if line.isdigit():
                continue
                
            # Remove VTT formatting tags
            clean_line = tag_pattern.sub('', line)
            
            # Avoid duplicate lines that sometimes appear in stuttering subs
            if cleaned_lines and cleaned_lines[-1] == clean_line:
                continue
                
            cleaned_lines.append(clean_line)
            
        return " ".join(cleaned_lines)

    def chunk_text(self, text: str, chunk_size: int = 500, overlap: int = 50) -> List[str]:
        """
        Slices text into chunks of `chunk_size` characters with `overlap` characters.
        """
        if not text:
            return []
            
        chunks = []
        start = 0
        text_length = len(text)
        
        while start < text_length:
            end = start + chunk_size
            chunk = text[start:end]
            
            # If not the last chunk, try to break at a meaningful character (space or punctuation)
            if end < text_length:
                # Find the last space within the overlap window to avoid splitting words
                last_space = chunk.rfind(' ')
                if last_space > chunk_size - overlap * 2:
                    chunk = chunk[:last_space]
                    end = start + last_space + 1
                    
            chunks.append(chunk.strip())
            start = end - overlap
            
        return [c for c in chunks if c]

    async def vectorize(self, text: str) -> List[float]:
        """
        Runs the CPU-bound embedding generation in a separate thread to avoid blocking the asyncio event loop.
        """
        # Execute the model encoding in a thread pool
        embedding = await asyncio.to_thread(self.model.encode, text)
        return embedding.tolist()
