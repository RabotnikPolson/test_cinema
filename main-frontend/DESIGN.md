---
design:
  tokens:
    color:
      background:
        value: "#0a0a0c"
        type: color
        description: Deep obsidian for a cinematic, immersive experience.
      surface:
        value: "#16161a"
        type: color
        description: Elevate panels and cards against the dark background.
      primary:
        value: "#6366f1"
        type: color
        description: Electric Violet representing the AI-powered core features.
      secondary:
        value: "#00b5e2"
        type: color
        description: Qazaq Sky Blue honoring the cultural focus of the cinema catalog.
      accent:
        value: "#f59e0b"
        type: color
        description: Cinema Gold for ratings and premium highlights.
      text:
        main:
          value: "#f8fafc"
          type: color
        muted:
          value: "#94a3b8"
          type: color
      border:
        value: "rgba(255, 255, 255, 0.1)"
        type: color
    typography:
      family:
        display:
          value: "'Outfit', sans-serif"
          type: fontFamily
        body:
          value: "'Inter', sans-serif"
          type: fontFamily
      size:
        h1:
          value: "3.5rem"
          type: fontSize
        h2:
          value: "2.25rem"
          type: fontSize
        base:
          value: "1rem"
          type: fontSize
      weight:
        bold:
          value: 700
          type: fontWeight
        medium:
          value: 500
          type: fontWeight
    spacing:
      base:
        value: "4px"
        type: spacing
      unit:
        value: "0.25rem"
        type: spacing
    radius:
      card:
        value: "16px"
        type: borderRadius
      button:
        value: "8px"
        type: borderRadius
    motion:
      duration:
        fast:
          value: "150ms"
          type: duration
        smooth:
          value: "300ms"
          type: duration
      easing:
        standard:
          value: "cubic-bezier(0.4, 0, 0.2, 1)"
          type: cubicBezier
    elevation:
      glass:
        value: "blur(12px) saturate(180%)"
        type: blur
      shadow:
        value: "0 8px 32px 0 rgba(0, 0, 0, 0.8)"
        type: shadow
---

# testCinema Design Identity

## Visual Language: "The Intelligent Screen"

The testCinema design system is crafted to bridge the gap between traditional cinematic grandeur and cutting-edge AI technology. It is a **Dark-First** interface that prioritizes content immersion while using vibrant, neon-inflected accents to signpost intelligent features.

### Look & Feel
- **Immersive Obsidian**: The UI recedes into a deep black (`#0a0a0c`), allowing movie posters and high-definition covers to be the focal point.
- **AI Glow**: Features like automated subtitle translation and vector-based recommendations are highlighted with **Electric Violet** (`#6366f1`). This "glow" is used sparingly to signify machine-intelligence at work.
- **Cultural Heritage**: As a project with deep roots in Qazaq cinema, the **Sky Blue** (`#00b5e2`) provides a secondary anchor, offering a professional yet localized feel.
- **Glassmorphism**: Panels and overlays use high-blur backgrounds to maintain context while providing clear functional separation.

### Design Intent
- **Content Hierarchy**: Movie titles use the **Outfit** typeface for high-impact display, while metadata and system labels use **Inter** for maximum legibility.
- **Tactile Interaction**: Buttons and interactive elements utilize a **smooth 300ms** transition with a standard ease, giving the app a premium, weighted feel rather than a jittery one.
- **Depth & Dimension**: We avoid flat design. Instead, we use subtle borders (`rgba(255, 255, 255, 0.1)`) and heavy shadows to create a sense of physical layering, reminiscent of a theater's depth.

### Component Guidelines
- **Movie Cards**: Large radii (16px) with a subtle inner glow. On hover, they scale slightly and reveal "AI Insights" (e.g., translation status).
- **Subtitle Overlays**: High contrast text with a soft shadow for readability against any frame. Backgrounds are strictly glass-morphed.
- **Ratings**: Displayed in **Cinema Gold** (`#f59e0b`) to evoke the prestige of industry awards (IMDb/Kinopoisk).

---
*This design system is self-contained and defines the visual strategy for the testCinema ecosystem.*
