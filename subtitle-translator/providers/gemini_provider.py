import json
import logging
from typing import List
from providers.base import BaseLLMProvider, RateLimitError, ServiceUnavailableError, AuthenticationError
from config.kazakh_prompt import build_system_prompt, build_user_prompt

logger = logging.getLogger(__name__)

class GeminiProvider(BaseLLMProvider):
    """
    Direct Gemini SDK integration.
    """
    def __init__(self, model: str, api_key: str, config: dict, tag_preservator):
        super().__init__(model, api_key, config, tag_preservator)
        # Import inside to avoid dependency error if not installed
        import google.generativeai as genai
        genai.configure(api_key=self.api_key)
        
        self._gen_config = genai.types.GenerationConfig(
            temperature=config.get("temperature", 0.15),
            response_mime_type="application/json"
        )
        self._gemini_model = genai.GenerativeModel(self.model, generation_config=self._gen_config)

    async def _call_api(self, lines: List[str], context: str, movie_title: str, source_language: str = "ru", genre: str = "general", glossary: dict = None, fix_instructions: str = None) -> List[str]:
        import google.generativeai as genai
        
        # 1. Build prompts
        sys_prompt = build_system_prompt(movie_title, source_language, genre, glossary)
        user_prompt = build_user_prompt(json.dumps(lines, ensure_ascii=False, indent=2), context)
        
        if fix_instructions:
            user_prompt += f"\n\n{fix_instructions}"
        
        # Initialize model with system instruction
        model = genai.GenerativeModel(
            self.model,
            generation_config=self._gen_config,
            system_instruction=sys_prompt
        )
            
        try:
            # 2. Call genai async
            response = await model.generate_content_async(user_prompt)
            text = response.text.strip()
            
            # Remove potential markdown code blocks (```json ... ```)
            if text.startswith("```"):
                lines_split = text.split("\n")
                if len(lines_split) >= 3:
                    text = "\n".join(lines_split[1:-1])
                else:
                    text = text.replace("```json", "").replace("```", "")
                
            # 3. Parse JSON response
            translated = json.loads(text)
            
            if not isinstance(translated, list):
                raise ValueError(f"Expected a JSON list, got {type(translated)}")
                
            return [str(item) for item in translated]
            
        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse JSON from Gemini: {text}")
            raise ValueError(f"JSON decode error: {e}")
        except Exception as e:
            error_msg = str(e).lower()
            if "429" in error_msg or "quota" in error_msg or "rate limit" in error_msg:
                raise RateLimitError(str(e))
            if "503" in error_msg or "unavailable" in error_msg or "500" in error_msg:
                raise ServiceUnavailableError(str(e))
            if "401" in error_msg or "403" in error_msg or "api key" in error_msg:
                raise AuthenticationError(str(e))
            raise e
