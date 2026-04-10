import re
from pathlib import Path

from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.platypus import Paragraph, Preformatted, SimpleDocTemplate, Spacer


PROJECT_ROOT = Path("C:/Users/amand/Downloads/resume_analyser/Resume-Analyser")
OUTPUT_PATH = str(PROJECT_ROOT / "API_and_Properties_Documentation.pdf")
CONTROLLERS_DIR = PROJECT_ROOT / "src/main/java/com/ai/Resume/analyser/controller"
CONFIG_FILE = PROJECT_ROOT / "src/main/java/com/ai/Resume/analyser/configuration/securityConfiguration.java"
PROPERTIES_FILES = [PROJECT_ROOT / "src/main/resources/application.properties"]


def parse_endpoints(controller_text: str) -> list[str]:
    base_match = re.search(r'@RequestMapping\("([^"]+)"\)', controller_text)
    base = base_match.group(1).strip("/") if base_match else ""
    lines: list[str] = []

    array_route = re.search(r'@RequestMapping\(value\s*=\s*\{([^}]+)\}\)', controller_text, re.DOTALL)
    if array_route:
        routes = re.findall(r'"([^"]+)"', array_route.group(1))
        for route in routes:
            lines.append(f"ANY {route}")
        return lines

    for method in ["Get", "Post", "Put", "Delete", "Patch"]:
        pattern = r'@' + method + r'Mapping\("([^"]+)"\)'
        for sub in re.findall(pattern, controller_text):
            full = f"/{base}/{sub.strip('/')}" if base else sub
            full = re.sub(r"/+", "/", full)
            lines.append(f"{method.upper():<6} {full}")

    return lines


def collect_api_lines() -> list[str]:
    lines: list[str] = []
    files = sorted(CONTROLLERS_DIR.glob("*Controller.java"))
    for file_path in files:
        lines.append(f"[{file_path.name}]")
        text = file_path.read_text(encoding="utf-8")
        endpoints = parse_endpoints(text)
        if endpoints:
            for ep in endpoints:
                lines.append(f"  - {ep}")
        else:
            lines.append("  - No mapping found")
        lines.append("")
    return lines


def collect_config_summary() -> str:
    if not CONFIG_FILE.exists():
        return "securityConfiguration.java not found."
    text = CONFIG_FILE.read_text(encoding="utf-8")
    permit_match = re.search(r"\.requestMatchers\((.*?)\)\s*\.permitAll", text, re.DOTALL)
    permit_lines = ["permitAll routes:"]
    if permit_match:
        routes = re.findall(r'"([^"]+)"', permit_match.group(1))
        for route in routes:
            permit_lines.append(f"  - {route}")
    else:
        permit_lines.append("  - Not detected")
    permit_lines.append("")
    permit_lines.append("session policy: STATELESS")
    permit_lines.append("jwt filter: enabled before UsernamePasswordAuthenticationFilter")
    permit_lines.append("oauth2 login page: /login")
    return "\n".join(permit_lines)


def build_pdf():
    doc = SimpleDocTemplate(
        OUTPUT_PATH,
        pagesize=A4,
        rightMargin=1.6 * cm,
        leftMargin=1.6 * cm,
        topMargin=1.6 * cm,
        bottomMargin=1.6 * cm,
        title="Resume Analyser API and Properties",
    )

    styles = getSampleStyleSheet()
    title_style = styles["Title"]
    header_style = styles["Heading2"]
    mono_style = ParagraphStyle(
        "Mono",
        parent=styles["Code"],
        fontName="Courier",
        fontSize=8.8,
        leading=11,
    )
    text_style = styles["BodyText"]

    story = []
    story.append(Paragraph("Resume Analyser - APIs, Configuration and application.properties", title_style))
    story.append(Spacer(1, 0.35 * cm))
    story.append(Paragraph("All API routes discovered from all controller classes:", text_style))
    story.append(Spacer(1, 0.2 * cm))
    story.append(Preformatted("\n".join(collect_api_lines()), mono_style))
    story.append(Spacer(1, 0.35 * cm))
    story.append(Paragraph("Security Configuration Summary", header_style))
    story.append(Spacer(1, 0.15 * cm))
    story.append(Preformatted(collect_config_summary(), mono_style))
    story.append(Spacer(1, 0.35 * cm))
    story.append(Paragraph("All application*.properties", header_style))

    for properties_file in PROPERTIES_FILES:
        story.append(Spacer(1, 0.15 * cm))
        story.append(Paragraph(str(properties_file).replace(str(PROJECT_ROOT) + "/", ""), text_style))
        text = properties_file.read_text(encoding="utf-8")
        story.append(Preformatted(text, mono_style))

    doc.build(story)


if __name__ == "__main__":
    build_pdf()
