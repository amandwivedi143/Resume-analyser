import { useContext, useEffect, useState } from "react";
import { usercontext } from "../appcontext";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import Styles from "./careerroadmap.module.css";

function CareerRoadmap() {
    const { serviceURL, islogged } = useContext(usercontext);
    const navigate = useNavigate();
    const [isLoading, setIsLoading] = useState(false);
    const [roadmap, setRoadmap] = useState(null);
    const [formData, setFormData] = useState({
        targetRole: "",
        currentSkills: "",
        experience: ""
    });

    useEffect(() => {
        if (!islogged) {
            toast.warn("Please login to use Career Roadmap");
            navigate("/login");
        }
    }, [islogged, navigate]);

    const onChange = (event) => {
        const { name, value } = event.target;
        setFormData((prev) => ({ ...prev, [name]: value }));
    };

    const generateRoadmap = (event) => {
        event.preventDefault();
        if (!formData.targetRole.trim() || !formData.currentSkills.trim() || !formData.experience.trim()) {
            toast.warn("Please fill all fields");
            return;
        }
        setIsLoading(true);
        setRoadmap(null);
        fetch(`${serviceURL}/career/roadmap`, {
            method: "post",
            credentials: "include",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                targetRole: formData.targetRole.trim(),
                currentSkills: formData.currentSkills.trim(),
                experience: formData.experience.trim()
            })
        })
            .then(async (response) => {
                const contentType = response.headers.get("content-type") || "";
                if (!response.ok || !contentType.includes("application/json")) {
                    if (response.status === 401 || response.status === 403) {
                        throw new Error("Please login again");
                    }
                    throw new Error("Career roadmap API failed");
                }
                return response.json();
            })
            .then((data) => {
                setRoadmap(data);
                toast.success("Roadmap generated");
            })
            .catch((error) => toast.error(error.message || "Unable to generate roadmap"))
            .finally(() => setIsLoading(false));
    };

    return (
        <div className={Styles.container}>
            <div className={Styles.nav}>
                <h1>Resume Analyser</h1>
                <button onClick={() => navigate("/")}>Home</button>
            </div>

            <div className={Styles.card}>
                <h2>AI Career Roadmap</h2>
                <p>Get a coding-focused 30-day roadmap to reach your target role.</p>
                <form onSubmit={generateRoadmap} className={Styles.form}>
                    <input name="targetRole" value={formData.targetRole} onChange={onChange} placeholder="Target role (Ex: Backend Engineer)" />
                    <input name="experience" value={formData.experience} onChange={onChange} placeholder="Experience (Ex: 1.5 years)" />
                    <textarea name="currentSkills" value={formData.currentSkills} onChange={onChange} placeholder="Current skills (Ex: Java, Spring Boot, MySQL, Git)" />
                    <button type="submit" disabled={isLoading}>{isLoading ? "Generating..." : "Generate Roadmap"}</button>
                </form>
            </div>

            {roadmap ? (
                <div className={Styles.result}>
                    <h3>Summary</h3>
                    <p>{roadmap.summary}</p>

                    <h3>Skill Gaps</h3>
                    <ul>
                        {(roadmap.skillGaps || []).map((gap, idx) => <li key={idx}>{gap}</li>)}
                    </ul>

                    <h3>Weekly Plan</h3>
                    {(roadmap.weeklyPlan || []).map((week, idx) => (
                        <div key={idx} className={Styles.week}>
                            <h4>{week.week} - {week.focus}</h4>
                            <ul>
                                {(week.tasks || []).map((task, i) => <li key={i}>{task}</li>)}
                            </ul>
                        </div>
                    ))}

                    <h3>Project Ideas</h3>
                    <ul>
                        {(roadmap.projectIdeas || []).map((item, idx) => <li key={idx}>{item}</li>)}
                    </ul>

                    <h3>Interview Checklist</h3>
                    <ul>
                        {(roadmap.interviewChecklist || []).map((item, idx) => <li key={idx}>{item}</li>)}
                    </ul>
                </div>
            ) : null}
        </div>
    );
}

export default CareerRoadmap;
