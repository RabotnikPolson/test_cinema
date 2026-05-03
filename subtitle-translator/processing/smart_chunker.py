from dataclasses import dataclass
from typing import List

@dataclass
class SubtitleEntry:
    index: int
    start_time: float  # in seconds
    end_time: float    # in seconds
    text: str

class SmartChunker:
    """
    Chunks subtitle entries based on natural pauses (gaps in time)
    with hard limits on chunk size.
    """
    def __init__(self, gap_threshold_sec: float = 4.0, min_lines_per_chunk: int = 10, max_lines_per_chunk: int = 40):
        self.gap_threshold_sec = gap_threshold_sec
        self.min_lines_per_chunk = min_lines_per_chunk
        self.max_lines_per_chunk = max_lines_per_chunk

    def chunk(self, entries: List[SubtitleEntry]) -> List[List[SubtitleEntry]]:
        chunks = []
        current_chunk = []
        
        for i, entry in enumerate(entries):
            current_chunk.append(entry)
            
            is_gap = (i + 1 < len(entries) and
                      entries[i+1].start_time - entry.end_time > self.gap_threshold_sec)
            
            is_full = len(current_chunk) >= self.max_lines_per_chunk
            
            if (is_gap and len(current_chunk) >= self.min_lines_per_chunk) or is_full:
                chunks.append(current_chunk)
                current_chunk = []
                
        if current_chunk:
            chunks.append(current_chunk)
            
        return chunks
