# Sinh lại 13 SVG cover cho event với UTF-8 sạch + brand FPT
# Chạy: powershell -File scripts\gen_event_svgs.ps1

$ErrorActionPreference = 'Stop'
$out = Join-Path $PSScriptRoot '..\src\main\resources\static\img\events'
$out = (Resolve-Path $out).Path
Write-Host "Output dir: $out"

$themes = @(
    @{ Name='fpt-default';    Title='Campus Events';        Subtitle='FPT University - Student Activity Hub';        Stops=@('#0b3a82','#0865c6','#00a651'); Pill='FPT EVENT' },
    @{ Name='fpt-hackathon';  Title='Hack The Future';      Subtitle='48h Coding Challenge - Innovate and Build';     Stops=@('#0a0a23','#1d1c4d','#3d1c75'); Pill='HACKATHON' },
    @{ Name='fpt-ai';         Title='AI and Machine Learning'; Subtitle='Workshop, Talks, Hands-on Labs';            Stops=@('#0f172a','#4338ca','#06b6d4'); Pill='AI LAB' },
    @{ Name='fpt-data';       Title='Data Engineering';     Subtitle='SQL, Analytics, Big Data Pipelines';            Stops=@('#064e3b','#047857','#14b8a6'); Pill='DATA' },
    @{ Name='fpt-cloud';      Title='Cloud Computing';      Subtitle='AWS, Azure, GCP - DevOps Journey';              Stops=@('#0c4a6e','#0284c7','#7dd3fc'); Pill='CLOUD' },
    @{ Name='fpt-security';   Title='Cyber Security CTF';   Subtitle='Capture The Flag - Pentest and Defense';        Stops=@('#3b0764','#7f1d1d','#ef4444'); Pill='SECURITY' },
    @{ Name='fpt-design';     Title='UX and Design Studio'; Subtitle='Portfolio Review - Figma Workshop';             Stops=@('#831843','#be185d','#fb923c'); Pill='DESIGN' },
    @{ Name='fpt-marketing';  Title='Marketing and Media';  Subtitle='Brand, Content, Production Bootcamp';           Stops=@('#9a3412','#c2410c','#f59e0b'); Pill='MARKETING' },
    @{ Name='fpt-business';   Title='Business Case Lab';    Subtitle='Strategy, Finance, Entrepreneurship';           Stops=@('#0f172a','#1e3a8a','#fbbf24'); Pill='BUSINESS' },
    @{ Name='fpt-language';   Title='English Day';          Subtitle='Presentation, Debate, Public Speaking';         Stops=@('#064e3b','#15803d','#facc15'); Pill='LANGUAGE' },
    @{ Name='fpt-career';     Title='Career Fair';          Subtitle='Internship, CV Clinic, Mock Interview';         Stops=@('#1e1b4b','#4f46e5','#ec4899'); Pill='CAREER' },
    @{ Name='fpt-graduation'; Title='Graduation Day';       Subtitle='Ceremony - Alumni Network - Memories';          Stops=@('#78350f','#b45309','#fde68a'); Pill='GRADUATION' },
    @{ Name='fpt-culture';    Title='Culture and Music';    Subtitle='Festival, Concert, Team Building';              Stops=@('#831843','#db2777','#fbbf24'); Pill='CULTURE' }
)

function Build-Svg($title, $subtitle, $stops, $pill) {
@"
<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 600" preserveAspectRatio="xMidYMid slice" role="img" aria-label="$title - FPT University">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="$($stops[0])"/>
      <stop offset="0.55" stop-color="$($stops[1])"/>
      <stop offset="1" stop-color="$($stops[2])"/>
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
    <rect x="0" y="0" width="220" height="38" rx="19" fill="#ffffff"/>
    <text x="110" y="25" text-anchor="middle" font-family="Inter, Segoe UI, sans-serif" font-weight="800" font-size="14" fill="#0b3a82" letter-spacing="0.1em">$pill</text>
  </g>

  <text x="64" y="370" font-family="Inter, Segoe UI, sans-serif" font-weight="900" font-size="58" fill="#ffffff">$title</text>
  <text x="64" y="420" font-family="Inter, Segoe UI, sans-serif" font-weight="500" font-size="22" fill="#e7efff">$subtitle</text>
  <text x="64" y="540" font-family="Inter, Segoe UI, sans-serif" font-weight="500" font-size="15" fill="#ffffff" fill-opacity="0.78">campus.fpt.edu.vn  -  Smart event management for FPT students</text>
</svg>
"@
}

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

foreach ($t in $themes) {
    $svg = Build-Svg -title $t.Title -subtitle $t.Subtitle -stops $t.Stops -pill $t.Pill
    $path = Join-Path $out "$($t.Name).svg"
    [System.IO.File]::WriteAllText($path, $svg, $utf8NoBom)
    $size = (Get-Item $path).Length
    Write-Host ("  -> {0,-32} {1,6} B" -f $t.Name, $size)
}

Write-Host ""
Write-Host "Done: $($themes.Count) SVG files."
