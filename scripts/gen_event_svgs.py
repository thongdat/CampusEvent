"""
Sinh lại bộ cover SVG cho event.
Yêu cầu: UTF-8 sạch (không kí tự cp1252 mangled), preserveAspectRatio slice
để fit object-cover, brand FPT.
"""
from pathlib import Path

OUT = Path(__file__).resolve().parents[1] / "src" / "main" / "resources" / "static" / "img" / "events"
OUT.mkdir(parents=True, exist_ok=True)

# (filename, title, subtitle, gradient stops, accent pill text)
THEMES = [
    ("fpt-default",    "Campus Events",       "FPT University - Student Activity Hub", ["#0b3a82", "#0865c6", "#00a651"], "FPT EVENT"),
    ("fpt-hackathon",  "Hack The Future",     "48h Coding Challenge - Innovate & Build", ["#0a0a23", "#1d1c4d", "#3d1c75"], "HACKATHON"),
    ("fpt-ai",         "AI & Machine Learning","Workshop, Talks, Hands-on Labs",        ["#0f172a", "#4338ca", "#06b6d4"], "AI LAB"),
    ("fpt-data",       "Data Engineering",    "SQL, Analytics, Big Data Pipelines",     ["#064e3b", "#047857", "#14b8a6"], "DATA"),
    ("fpt-cloud",      "Cloud Computing",     "AWS, Azure, GCP - DevOps Journey",       ["#0c4a6e", "#0284c7", "#7dd3fc"], "CLOUD"),
    ("fpt-security",   "Cyber Security CTF",  "Capture The Flag - Pentest & Defense",    ["#3b0764", "#7f1d1d", "#ef4444"], "SECURITY"),
    ("fpt-design",     "UX & Design Studio",  "Portfolio Review - Figma Workshop",       ["#831843", "#be185d", "#fb923c"], "DESIGN"),
    ("fpt-marketing",  "Marketing & Media",   "Brand, Content, Production Bootcamp",     ["#9a3412", "#c2410c", "#f59e0b"], "MARKETING"),
    ("fpt-business",   "Business Case Lab",   "Strategy, Finance, Entrepreneurship",      ["#0f172a", "#1e3a8a", "#fbbf24"], "BUSINESS"),
    ("fpt-language",   "English Day",         "Presentation, Debate, Public Speaking",   ["#064e3b", "#15803d", "#facc15"], "LANGUAGE"),
    ("fpt-career",     "Career Fair",         "Internship, CV Clinic, Mock Interview",   ["#1e1b4b", "#4f46e5", "#ec4899"], "CAREER"),
    ("fpt-graduation", "Graduation Day",      "Ceremony - Alumni Network - Memories",    ["#78350f", "#b45309", "#fde68a"], "GRADUATION"),
    ("fpt-culture",    "Culture & Music",     "Festival, Concert, Team Building",        ["#831843", "#db2777", "#fbbf24"], "CULTURE"),
]


def build_svg(title: str, subtitle: str, stops: list[str], pill_text: str) -> str:
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 600" preserveAspectRatio="xMidYMid slice" role="img" aria-label="{title} - FPT University">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="{stops[0]}"/>
      <stop offset="0.55" stop-color="{stops[1]}"/>
      <stop offset="1" stop-color="{stops[2]}"/>
    </linearGradient>
    <radialGradient id="glow" cx="0.85" cy="0.18" r="0.65">
      <stop offset="0" stop-color="#ffffff" stop-opacity="0.32"/>
      <stop offset="1" stop-color="#ffffff" stop-opacity="0"/>
    </radialGradient>
    <pattern id="grid" width="48" height="48" patternUnits="userSpaceOnUse">
      <path d="M48 0H0V48" fill="none" stroke="#ffffff" stroke-opacity="0.06" stroke-width="1"/>
    </pattern>
  </defs>
  <rect width="1200" height="600" fill="url(#bg)"/>
  <rect width="1200" height="600" fill="url(#grid)"/>
  <rect width="1200" height="600" fill="url(#glow)"/>

  <g opacity="0.18">
    <circle cx="980" cy="120" r="220" fill="#ffffff"/>
    <circle cx="1100" cy="540" r="140" fill="#fde68a"/>
    <circle cx="180" cy="520" r="180" fill="#5eead4"/>
  </g>

  <g transform="translate(64,64)">
    <rect x="0" y="0" width="64" height="26" rx="5" fill="#f37021"/>
    <text x="32" y="19" text-anchor="middle" font-family="Inter, Segoe UI, sans-serif" font-weight="900" font-size="17" fill="#ffffff">FPT</text>
    <text x="76" y="19" font-family="Inter, Segoe UI, sans-serif" font-weight="700" font-size="14" fill="#ffffff" letter-spacing="0.22em">UNIVERSITY</text>
    <text x="0" y="46" font-family="Inter, Segoe UI, sans-serif" font-weight="500" font-size="12" fill="#dbe7ff" letter-spacing="0.14em">CAMPUS EVENTS</text>
  </g>

  <g transform="translate(64,250)">
    <rect x="0" y="0" width="200" height="38" rx="19" fill="#ffffff"/>
    <text x="100" y="25" text-anchor="middle" font-family="Inter, Segoe UI, sans-serif" font-weight="800" font-size="14" fill="#0b3a82" letter-spacing="0.1em">{pill_text}</text>
  </g>

  <text x="64" y="368" font-family="Inter, Segoe UI, sans-serif" font-weight="900" font-size="62" fill="#ffffff">{title}</text>
  <text x="64" y="420" font-family="Inter, Segoe UI, sans-serif" font-weight="500" font-size="22" fill="#e7efff">{subtitle}</text>
  <text x="64" y="540" font-family="Inter, Segoe UI, sans-serif" font-weight="500" font-size="15" fill="#ffffff" fill-opacity="0.78">campus.fpt.edu.vn  -  Smart event management for FPT students</text>
</svg>
"""


for name, title, subtitle, stops, pill in THEMES:
    path = OUT / f"{name}.svg"
    path.write_text(build_svg(title, subtitle, stops, pill), encoding="utf-8")
    print(f"wrote {path}  ({path.stat().st_size} B)")

print(f"\nDone. Total: {len(THEMES)} SVGs in {OUT}")
