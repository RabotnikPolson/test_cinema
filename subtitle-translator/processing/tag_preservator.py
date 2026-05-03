import re
from typing import List, Tuple, Dict

class TagPreservator:
    """
    Pre/post processor for HTML tags and special characters.
    Replaces tags with XML-like placeholders (<0>, <1>) before LLM processing and restores them after.
    Uses continuous numbering across the entire chunk.
    """
    def __init__(self):
        # Order matters: HTML tags, dialogue dashes, music symbols, newlines
        self.rules = [
            re.compile(r'<(?:/?)(?:i|b|u|font[^>]*)>'),
            re.compile(r'^- '),
            re.compile(r'♪'),
            re.compile(r'\n')
        ]

    def strip(self, lines: List[str]) -> Tuple[List[str], Dict[str, str]]:
        cleaned_lines = []
        tag_map = {}
        counter = 0

        for line in lines:
            current_line = line
            
            for pattern in self.rules:
                while True:
                    match = pattern.search(current_line)
                    if not match:
                        break
                    original = match.group(0)
                    placeholder = f"<{counter}>"
                    counter += 1
                    tag_map[placeholder] = original
                    current_line = current_line[:match.start()] + placeholder + current_line[match.end():]
                    
            cleaned_lines.append(current_line)
            
        return cleaned_lines, tag_map

    def restore(self, translated_lines: List[str], tag_map: Dict[str, str]) -> List[str]:
        restored_lines = []
        for line in translated_lines:
            current_line = line
            
            # Safely replace back placeholders.
            for placeholder, original in tag_map.items():
                current_line = current_line.replace(placeholder, original)
            
            # Rigid cleanup: remove any leftover or hallucinated tags like <99>
            current_line = re.sub(r'<\d+>', '', current_line)
            
            restored_lines.append(current_line)
            
        return restored_lines
