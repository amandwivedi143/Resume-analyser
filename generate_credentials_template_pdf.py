from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.platypus import Paragraph, Preformatted, SimpleDocTemplate, Spacer


OUTPUT_PATH = "C:/Users/amand/Downloads/resume_analyser/Resume-Analyser/Credentials_Single_PDF_Template.pdf"

CONTENT = """Resume Analyser Credentials (Fill Manually)

IMPORTANT:
- Do not share this file publicly.
- Do not commit real credentials to git/GitHub.
- Prefer environment variables or secret manager.

Database
DB_USERNAME=
DB_PASSWORD=

AI / Mail / External APIs
GEN_AI_KEY=                 # Grok/Groq key
MAIL_API_KEY=               # Brevo (Sendinblue) API key
ADZUNA_APP_ID=
ADZUNA_APP_KEY=

Google OAuth
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=

Optional
PORT=8080
"""


def main():
    doc = SimpleDocTemplate(
        OUTPUT_PATH,
        pagesize=A4,
        rightMargin=1.6 * cm,
        leftMargin=1.6 * cm,
        topMargin=1.6 * cm,
        bottomMargin=1.6 * cm,
        title="Credentials Single PDF Template",
    )
    styles = getSampleStyleSheet()
    mono_style = ParagraphStyle(
        "Mono",
        parent=styles["Code"],
        fontName="Courier",
        fontSize=10,
        leading=13,
    )
    story = [
        Paragraph("Credentials Template (Single PDF)", styles["Title"]),
        Spacer(1, 0.3 * cm),
        Preformatted(CONTENT, mono_style),
    ]
    doc.build(story)


if __name__ == "__main__":
    main()
